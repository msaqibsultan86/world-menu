package net.notzyvex.worldmenu;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

final class WorldScreenButtons {
    private static final Identifier FOLDER_ICON =
            Identifier.of(WorldMenuClient.MOD_ID, "textures/gui/folder.png");
    private static final Identifier IMPORT_ICON =
            Identifier.of(WorldMenuClient.MOD_ID, "textures/gui/import.png");

    private static final int WIDTH = 120;
    private static final int HEIGHT = 20;
    private static final int MARGIN = 6;

    private WorldScreenButtons() {
    }

    static void addTo(Screen screen, int scaledWidth, int topOffset) {
        // A screen re-initialises when the window is resized or a tab is
        // switched, and the event fires again each time. Without this the
        // buttons stack up on top of each other.
        Screens.getButtons(screen).removeIf(widget -> widget instanceof IconButton);

        IconButton openFolder = new IconButton(
                MARGIN, topOffset, WIDTH, HEIGHT,
                Text.translatable("worldmenu.button.open_folder"),
                FOLDER_ICON,
                button -> SavesDirectory.open());

        IconButton importWorld = new IconButton(
                scaledWidth - WIDTH - MARGIN, topOffset, WIDTH, HEIGHT,
                Text.translatable("worldmenu.button.import_world"),
                IMPORT_ICON,
                button -> WorldImporter.run());

        Screens.getButtons(screen).add(openFolder);
        Screens.getButtons(screen).add(importWorld);
    }
}
