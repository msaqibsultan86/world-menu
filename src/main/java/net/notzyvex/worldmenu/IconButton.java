package net.notzyvex.worldmenu;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

class IconButton extends Button {
    private static final int ICON_SIZE = 16;
    private static final int ICON_INSET = 4;
    private static final int ICON_TEXT_GAP = 4;
    private static final int TEXT_HEIGHT = 8;

    private final ResourceLocation icon;

    IconButton(int x, int y, int width, int height, Component message, ResourceLocation icon, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.icon = icon;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderWidget(graphics, mouseX, mouseY, delta);

        int iconX = getX() + ICON_INSET;
        int iconY = getY() + (getHeight() - ICON_SIZE) / 2;
        //? if >=1.21.4 {
        /*graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, icon, iconX, iconY,
                0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        *///?} else {
        graphics.blit(icon, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        //?}
    }

    @Override
    public void renderString(GuiGraphics graphics, Font font, int color) {
        int textLeft = getX() + ICON_INSET + ICON_SIZE + ICON_TEXT_GAP;
        int textRight = getX() + getWidth() - ICON_INSET;
        graphics.drawCenteredString(
                font,
                getMessage(),
                (textLeft + textRight) / 2,
                getY() + (getHeight() - TEXT_HEIGHT) / 2,
                color);
    }
}
