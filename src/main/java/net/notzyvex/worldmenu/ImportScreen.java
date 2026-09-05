package net.notzyvex.worldmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.atomic.AtomicInteger;

/** Shown while a world is being copied in, and once it is finished. */
class ImportScreen extends Screen {
    private static final Identifier BAR_BACKGROUND = Identifier.ofVanilla("hud/experience_bar_background");
    private static final Identifier BAR_PROGRESS = Identifier.ofVanilla("hud/experience_bar_progress");

    private static final int BACKDROP = 0xFF14161A;
    private static final int TEXT = 0xFFFFFF;
    private static final int TEXT_DIM = 0xA0A0A0;

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
        super(Text.translatable("worldmenu.import.title"));
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
        MinecraftClient.getInstance().execute(this::clearAndInit);
    }

    @Override
    protected void init() {
        if (!finished) {
            return;
        }
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button ->
                        client.setScreen(new SelectWorldScreen(new TitleScreen())))
                .dimensions((width - BUTTON_WIDTH) / 2, height / 2 + 30, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return finished;
    }

    @Override
    public void close() {
        client.setScreen(new SelectWorldScreen(new TitleScreen()));
    }

    // Vanilla blurs whatever is behind an overlay screen. That reads as a
    // mistake on a progress screen, so this draws a flat backdrop instead.
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BACKDROP);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int centreX = width / 2;
        int barY = height / 2;

        Text heading = finished
                ? Text.translatable("worldmenu.import.done")
                : title.copy().append(animatedDots());

        context.drawCenteredTextWithShadow(textRenderer, heading, centreX, barY - 34, TEXT);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(worldName), centreX, barY - 20, TEXT_DIM);

        int barX = centreX - BAR_WIDTH / 2;
        context.drawGuiTexture(BAR_BACKGROUND, barX, barY, BAR_WIDTH, BAR_HEIGHT);

        // Ease towards the real value so the bar glides instead of jumping.
        float target = finished ? 1.0f : progress();
        shownProgress = MathHelper.lerp(Math.min(1.0f, delta * 0.4f), shownProgress, target);

        int filled = Math.round(shownProgress * BAR_WIDTH);
        if (filled > 0) {
            context.drawGuiTexture(BAR_PROGRESS, BAR_WIDTH, BAR_HEIGHT, 0, 0, barX, barY, filled, BAR_HEIGHT);
        }

        if (!finished) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(countText()),
                    centreX, barY + 12, TEXT_DIM);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private float progress() {
        int count = total;
        return count <= 0 ? 0.0f : MathHelper.clamp((float) done.get() / count, 0.0f, 1.0f);
    }

    private String countText() {
        int count = total;
        if (count <= 0) {
            return "";
        }
        return Math.round(progress() * 100) + "%  (" + Math.min(done.get(), count) + " / " + count + ")";
    }

    private Text animatedDots() {
        int dots = (int) ((System.currentTimeMillis() / DOT_INTERVAL_MS) % 4);
        return Text.literal(".".repeat(dots));
    }
}
