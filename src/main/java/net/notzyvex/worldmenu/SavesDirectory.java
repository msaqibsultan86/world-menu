package net.notzyvex.worldmenu;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class SavesDirectory {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldMenuClient.MOD_ID);

    private SavesDirectory() {
    }

    static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("saves");
    }

    static void open() {
        Path saves = path();
        try {
            Files.createDirectories(saves);
        } catch (IOException e) {
            LOGGER.error("Could not create the saves directory at {}", saves, e);
            return;
        }
        Util.getPlatform().openUri(saves.toUri());
    }
}
