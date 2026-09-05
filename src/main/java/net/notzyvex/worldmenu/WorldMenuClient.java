package net.notzyvex.worldmenu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

public class WorldMenuClient implements ClientModInitializer {
    public static final String MOD_ID = "worldmenu";

    private static final int SELECT_SCREEN_TOP = 6;
    // The create screen puts its tab bar along the top, so the buttons sit
    // below it rather than over it.
    private static final int CREATE_SCREEN_TOP = 32;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof SelectWorldScreen) {
                WorldScreenButtons.addTo(screen, scaledWidth, SELECT_SCREEN_TOP);
            } else if (screen instanceof CreateWorldScreen) {
                WorldScreenButtons.addTo(screen, scaledWidth, CREATE_SCREEN_TOP);
            }
        });

        // The window only exists once the client has started.
        ClientLifecycleEvents.CLIENT_STARTED.register(WorldDropTarget::install);
    }
}
