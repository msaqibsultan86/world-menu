package net.notzyvex.worldmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class WorldImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldMenuClient.MOD_ID);
    private static final int MAX_NAME_ATTEMPTS = 1000;
    private static final String LEVEL_DAT = "level.dat";

    private WorldImporter() {
    }

    static void run() {
        Thread picker = new Thread(WorldImporter::pickAndImport, "worldmenu-picker");
        picker.setDaemon(true);
        picker.start();
    }

    /** Handles files dropped onto the game window. */
    static void importDropped(List<Path> paths) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (Path path : paths) {
            importAny(path, client);
        }
    }

    // TinyFileDialogs ships with LWJGL and is safe alongside GLFW. Swing's
    // JFileChooser deadlocks against the render thread on macOS.
    //
    // The file dialog is used rather than the folder dialog on purpose: Windows
    // only has a real Explorer window for picking files. Its folder picker is
    // the old cramped tree widget.
    private static void pickAndImport() {
        String chosen;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(2);
            filters.put(stack.UTF8(LEVEL_DAT));
            filters.put(stack.UTF8("*.zip"));
            filters.flip();

            chosen = TinyFileDialogs.tinyfd_openFileDialog(
                    Text.translatable("worldmenu.dialog.title").getString(),
                    SavesDirectory.path() + File.separator,
                    filters,
                    Text.translatable("worldmenu.dialog.filter").getString(),
                    false);
        }

        if (chosen == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        importAny(Path.of(chosen), client);
    }

    /** Accepts a world folder, a level.dat inside one, or a zipped world. */
    private static void importAny(Path path, MinecraftClient client) {
        if (isZip(path)) {
            runOffThread(() -> importZip(path, client));
            return;
        }

        Path folder = Files.isDirectory(path) ? path : path.getParent();
        if (folder == null) {
            client.execute(() -> notify(client, "worldmenu.import.not_a_world"));
            return;
        }
        runOffThread(() -> importFolder(folder, client));
    }

    // Copying can take a while for a large world, so it must not block the
    // render thread. Only the toast and the screen refresh go back to it.
    private static void runOffThread(Runnable work) {
        Thread thread = new Thread(work, "worldmenu-import");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean isZip(Path path) {
        return Files.isRegularFile(path)
                && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    private static void importFolder(Path source, MinecraftClient client) {
        if (!Files.isRegularFile(source.resolve(LEVEL_DAT))) {
            client.execute(() -> notify(client, "worldmenu.import.not_a_world"));
            return;
        }

        String name = source.getFileName().toString();
        Path destination;
        try {
            destination = availableDestination(name);
        } catch (IOException e) {
            LOGGER.error("Could not pick a destination folder for {}", source, e);
            client.execute(() -> notify(client, "worldmenu.import.failed"));
            return;
        }

        ImportScreen screen = showProgress(client, name);
        try {
            screen.setTotal(countFiles(source));
            copyTree(source, destination, screen);
        } catch (IOException e) {
            LOGGER.error("Could not copy {} into the saves folder", source, e);
            deleteTree(destination);
            client.execute(() -> notify(client, "worldmenu.import.failed"));
            finish(client);
            return;
        }

        screen.markFinished();
    }

    private static ImportScreen showProgress(MinecraftClient client, String worldName) {
        ImportScreen screen = new ImportScreen(worldName);
        client.execute(() -> client.setScreen(screen));
        return screen;
    }

    private static int countFiles(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return (int) paths.filter(Files::isRegularFile).count();
        }
    }

    private static void importZip(Path zipPath, MinecraftClient client) {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            String root = findWorldRoot(zip);
            if (root == null) {
                client.execute(() -> notify(client, "worldmenu.import.not_a_world"));
                return;
            }

            String name = worldNameFor(root, zipPath);
            Path destination = availableDestination(name);

            ImportScreen screen = showProgress(client, name);
            try {
                screen.setTotal(countEntries(zip, root));
                extract(zip, root, destination, screen);
            } catch (IOException e) {
                LOGGER.error("Could not extract {} into the saves folder", zipPath, e);
                deleteTree(destination);
                client.execute(() -> notify(client, "worldmenu.import.failed"));
                finish(client);
                return;
            }

            screen.markFinished();
        } catch (IOException e) {
            LOGGER.error("Could not read {}", zipPath, e);
            client.execute(() -> notify(client, "worldmenu.import.failed"));
        }
    }

    /**
     * Returns the path prefix inside the zip that contains level.dat, or null if
     * there is none. Maps are commonly zipped with a wrapper folder, sometimes
     * two, so the shallowest level.dat wins.
     */
    private static String findWorldRoot(ZipFile zip) {
        String best = null;
        Enumeration<? extends ZipEntry> entries = zip.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;

            String name = entry.getName().replace('\\', '/');
            if (name.startsWith("__MACOSX/")) continue;
            if (!name.equals(LEVEL_DAT) && !name.endsWith("/" + LEVEL_DAT)) continue;

            String prefix = name.substring(0, name.length() - LEVEL_DAT.length());
            if (best == null || prefix.length() < best.length()) {
                best = prefix;
            }
        }
        return best;
    }

    private static String worldNameFor(String root, Path zipPath) {
        String trimmed = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
        if (!trimmed.isEmpty()) {
            int slash = trimmed.lastIndexOf('/');
            return slash < 0 ? trimmed : trimmed.substring(slash + 1);
        }

        String fileName = zipPath.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static int countEntries(ZipFile zip, String root) {
        int count = 0;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName().replace('\', '/');
            if (!entry.isDirectory() && !name.startsWith("__MACOSX/") && name.startsWith(root)) {
                count++;
            }
        }
        return count;
    }

    private static void extract(ZipFile zip, String root, Path destination, ImportScreen screen) throws IOException {
        Files.createDirectories(destination);
        Path base = destination.toAbsolutePath().normalize();

        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName().replace('\\', '/');
            if (name.startsWith("__MACOSX/") || !name.startsWith(root)) continue;

            String relative = name.substring(root.length());
            if (relative.isEmpty()) continue;

            // A crafted zip can name entries like ../../evil. Anything that
            // resolves outside the destination is refused.
            Path target = base.resolve(relative).normalize();
            if (!target.startsWith(base)) {
                throw new IOException("Zip entry escapes the destination: " + name);
            }

            if (entry.isDirectory()) {
                Files.createDirectories(target);
                continue;
            }

            Files.createDirectories(target.getParent());
            try (InputStream in = zip.getInputStream(entry)) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            screen.step();
        }
    }

    private static void finish(MinecraftClient client) {
        client.execute(() -> client.setScreen(new SelectWorldScreen(new TitleScreen())));
    }

    private static Path availableDestination(String folderName) throws IOException {
        Path saves = SavesDirectory.path();
        Files.createDirectories(saves);

        Path candidate = saves.resolve(folderName);
        for (int suffix = 1; Files.exists(candidate) && suffix <= MAX_NAME_ATTEMPTS; suffix++) {
            candidate = saves.resolve(folderName + " (" + suffix + ")");
        }

        if (Files.exists(candidate)) {
            throw new IOException("No free folder name for " + folderName);
        }
        return candidate;
    }

    private static void copyTree(Path source, Path destination, ImportScreen screen) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(destination.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, destination.resolve(source.relativize(file).toString()));
                screen.step();
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteTree(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    LOGGER.warn("Left behind {} while cleaning up a failed import", path, e);
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Could not clean up {} after a failed import", root, e);
        }
    }

    private static void notify(MinecraftClient client, String messageKey) {
        client.getToastManager().add(SystemToast.create(
                client,
                SystemToast.Type.WORLD_ACCESS_FAILURE,
                Text.translatable("worldmenu.import.toast"),
                Text.translatable(messageKey)));
    }
}
