package net.notzyvex.worldmenu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

public class WorldMenuClient implements ClientModInitializer {
    public static final String MOD_ID = "worldmenu";

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof SelectWorldScreen) {
                SelectWorldScreenButtons.addTo(screen, scaledWidth);
            }
        });
    }
}
