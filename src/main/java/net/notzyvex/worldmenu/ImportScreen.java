package net.notzyvex.worldmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.concurrent.atomic.AtomicInteger;

/** Shown while a world is being copied in, and once it is finished. */
class ImportScreen extends Screen {
    private static final ResourceLocation BAR_BACKGROUND =
            ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private static final ResourceLocation BAR_PROGRESS =
            ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int DOT_INTERVAL_MS = 400;

    private final AtomicInteger done = new AtomicInteger();
    private final String worldName;

    private volatile int total;
    private volatile boolean finished;
    private float shownProgress;

    ImportScreen(String worldName) {
        super(Component.translatable("worldmenu.import.title"));
        this.worldName = worldName;
    }

    void setTotal(int total) {
        this.total = total;
    }

    void step() {
        done.incrementAndGet();
    }

    /** Swaps the screen over to its finished state, with a button back to the world list. */
    void markFinished() {
        finished = true;
        Minecraft.getInstance().execute(this::rebuildWidgets);
    }

    @Override
    protected void init() {
        if (!finished) {
            return;
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button ->
                        minecraft.setScreen(new SelectWorldScreen(new TitleScreen())))
                .bounds((width - BUTTON_WIDTH) / 2, height / 2 + 30, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return finished;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(new SelectWorldScreen(new TitleScreen()));
    }

    /**
     * Vanilla blurs the frame behind any open screen, which smears the progress
     * text along with it. Everything else about the background stays vanilla.
     */
    //? if <1.21.2 {
    @Override
    protected void renderBlurredBackground(float delta) {
    }
    //?}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);

        int centreX = width / 2;
        int barY = height / 2;

        Component heading = finished
                ? Component.translatable("worldmenu.import.done")
                : title.copy().append(animatedDots());

        graphics.drawCenteredString(font, heading, centreX, barY - 34, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal(worldName), centreX, barY - 20, 0xA0A0A0);

        int barX = centreX - BAR_WIDTH / 2;
        //? if >=1.21.6 {
        /*graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                BAR_BACKGROUND, barX, barY, BAR_WIDTH, BAR_HEIGHT);
        *///?} elif >=1.21.4 {
        /*graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured,
                BAR_BACKGROUND, barX, barY, BAR_WIDTH, BAR_HEIGHT);
        *///?} else {
        graphics.blitSprite(BAR_BACKGROUND, barX, barY, BAR_WIDTH, BAR_HEIGHT);
        //?}

        // Ease towards the real value so the bar glides instead of jumping.
        float target = finished ? 1.0f : progress();
        shownProgress = Mth.lerp(Math.min(1.0f, delta * 0.4f), shownProgress, target);

        int filled = Math.round(shownProgress * BAR_WIDTH);
        if (filled > 0) {
            //? if >=1.21.6 {
            /*graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    BAR_PROGRESS, BAR_WIDTH, BAR_HEIGHT, 0, 0, barX, barY, filled, BAR_HEIGHT);
            *///?} elif >=1.21.4 {
            /*graphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured,
                    BAR_PROGRESS, BAR_WIDTH, BAR_HEIGHT, 0, 0, barX, barY, filled, BAR_HEIGHT);
            *///?} else {
            graphics.blitSprite(BAR_PROGRESS, BAR_WIDTH, BAR_HEIGHT, 0, 0, barX, barY, filled, BAR_HEIGHT);
            //?}
        }

        if (!finished) {
            graphics.drawCenteredString(font, Component.literal(countText()), centreX, barY + 12, 0xA0A0A0);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    private float progress() {
        int count = total;
        return count <= 0 ? 0.0f : Mth.clamp((float) done.get() / count, 0.0f, 1.0f);
    }

    private String countText() {
        int count = total;
        if (count <= 0) {
            return "";
        }
        return Math.round(progress() * 100) + "%  (" + Math.min(done.get(), count) + " / " + count + ")";
    }

    private Component animatedDots() {
        int dots = (int) ((System.currentTimeMillis() / DOT_INTERVAL_MS) % 4);
        return Component.literal(".".repeat(dots));
    }
}
