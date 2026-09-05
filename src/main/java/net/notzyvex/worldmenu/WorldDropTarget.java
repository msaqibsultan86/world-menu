package net.notzyvex.worldmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class WorldDropTarget {
    private static GLFWDropCallback previous;

    private WorldDropTarget() {
    }

    static void install(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        previous = GLFW.glfwSetDropCallback(handle, WorldDropTarget::onDrop);
    }

    private static void onDrop(long window, int count, long names) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = client.currentScreen;

        // Anywhere else in the game, drops belong to vanilla — that is how
        // resource packs are installed by dragging them in.
        if (!(screen instanceof SelectWorldScreen) && !(screen instanceof CreateWorldScreen)) {
            if (previous != null) {
                previous.invoke(window, count, names);
            }
            return;
        }

        List<Path> dropped = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            dropped.add(Path.of(GLFWDropCallback.getName(names, i)));
        }
        WorldImporter.importDropped(dropped);
    }
}
