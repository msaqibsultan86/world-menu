package net.notzyvex.worldmenu;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

class IconButton extends ButtonWidget {
    private static final int ICON_SIZE = 16;
    private static final int ICON_INSET = 4;
    private static final int ICON_TEXT_GAP = 4;
    private static final int TEXT_HEIGHT = 8;

    private final Identifier icon;

    IconButton(int x, int y, int width, int height, Text message, Identifier icon, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.icon = icon;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);

        int iconX = getX() + ICON_INSET;
        int iconY = getY() + (getHeight() - ICON_SIZE) / 2;
        context.drawTexture(icon, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        int textLeft = getX() + ICON_INSET + ICON_SIZE + ICON_TEXT_GAP;
        int textRight = getX() + getWidth() - ICON_INSET;
        context.drawCenteredTextWithShadow(
                textRenderer,
                getMessage(),
                (textLeft + textRight) / 2,
                getY() + (getHeight() - TEXT_HEIGHT) / 2,
                color);
    }
}
