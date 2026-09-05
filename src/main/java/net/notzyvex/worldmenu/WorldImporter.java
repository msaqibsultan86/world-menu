package net.notzyvex.worldmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

final class WorldImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldMenuClient.MOD_ID);
    private static final int MAX_NAME_ATTEMPTS = 1000;

    private WorldImporter() {
    }

    static void run() {
        Thread picker = new Thread(WorldImporter::pickAndImport, "worldmenu-folder-picker");
        picker.setDaemon(true);
        picker.start();
    }

    // TinyFileDialogs ships with LWJGL and is safe alongside GLFW. Swing's
    // JFileChooser deadlocks against the render thread on macOS.
    private static void pickAndImport() {
        String chosen = TinyFileDialogs.tinyfd_selectFolderDialog(
                Text.translatable("worldmenu.dialog.title").getString(),
                SavesDirectory.path().toString());

        if (chosen == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> importFrom(Path.of(chosen), client));
    }

    private static void importFrom(Path source, MinecraftClient client) {
        if (!Files.isRegularFile(source.resolve("level.dat"))) {
            notify(client, "worldmenu.import.not_a_world");
            return;
        }

        Path destination;
        try {
            destination = availableDestination(source.getFileName().toString());
        } catch (IOException e) {
            LOGGER.error("Could not pick a destination folder for {}", source, e);
            notify(client, "worldmenu.import.failed");
            return;
        }

        try {
            copyTree(source, destination);
        } catch (IOException e) {
            LOGGER.error("Could not copy {} into the saves folder", source, e);
            deleteTree(destination);
            notify(client, "worldmenu.import.failed");
            return;
        }

        notify(client, "worldmenu.import.done");
        client.setScreen(new SelectWorldScreen(new TitleScreen()));
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

    private static void copyTree(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(destination.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, destination.resolve(source.relativize(file).toString()));
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
