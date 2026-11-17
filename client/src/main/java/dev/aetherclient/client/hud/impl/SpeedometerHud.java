package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.nanovg.NanoVG;

import java.util.Locale;

public final class SpeedometerHud extends HudElement {
    private static final int MAX_SAMPLES = 64;

    private final HudModule hudModule;
    private final float[] samples = new float[MAX_SAMPLES];
    private int head = 0;
    private int count = 0;
    private float currentBps = 0f;
    private float peakBps = 0f;

    public SpeedometerHud(HudModule hudModule) {
        super("speedometer", "Спидометр", 0.985f, 0.5f);
        setTitleEn("Speedometer");
        this.hudModule = hudModule;
        this.width = 148f;
        this.height = 46f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.speedometer.isEnabled();
    }

    private void sample(MinecraftClient mc) {
        double bps = 0.0;
        if (mc.player != null) {
            double dx = mc.player.getX() - mc.player.lastRenderX;
            double dz = mc.player.getZ() - mc.player.lastRenderZ;
            bps = Math.hypot(dx, dz) * 20.0;
        }
        currentBps = (float) bps;
        if (currentBps > peakBps) peakBps = currentBps;
        samples[head] = currentBps;
        head = (head + 1) % MAX_SAMPLES;
        if (count < MAX_SAMPLES) count++;
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        this.width = 148f;
        this.height = 46f;
        this.elementScale = hudModule.scaleSpeedometer.asFloat();
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        sample(mc);
        this.width = 148f;
        this.height = 46f;
        long vg = engine.handle();

        Render2D.drawLiquidGlassPanel(
                engine, x, y, width, height, 9.5f, screenW, screenH, palette,
                hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
        );

        String value = String.format(Locale.ROOT, "%.1f", currentBps);
        Render2D.drawTextBold(vg, value, x + 12f, y + 16f, 17f, palette.onSurface,
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        Render2D.drawText(vg, I18n.tr("блок/с", "b/s"), x + 12f + Render2D.measureTextBold(vg, value, 17f) + 4f,
                y + 18f, 9.2f, palette.onSurfaceVariant, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        // Mini graph
        float gx = x + 8f;
        float gy = y + 30f;
        float gw = width - 16f;
        float gh = 10f;
        Render2D.drawRoundedRect(vg, gx, gy, gw, gh, 3f, AetherPalette.withAlpha(palette.onSurface, 22));
        if (count > 1) {
            float max = Math.max(1f, peakBps);
            float stepX = gw / (MAX_SAMPLES - 1);
            for (int i = 0; i < count - 1; i++) {
                int i0 = (head - count + i + MAX_SAMPLES * 2) % MAX_SAMPLES;
                int i1 = (i0 + 1) % MAX_SAMPLES;
                float v0 = samples[i0];
                float v1 = samples[i1];
                float x0 = gx + i * stepX;
                float x1 = gx + (i + 1) * stepX;
                float y0 = gy + gh - (v0 / max) * (gh - 2f) - 1f;
                float y1 = gy + gh - (v1 / max) * (gh - 2f) - 1f;
                Render2D.drawLine(vg, x0, y0, x1, y1, 1.2f, palette.primary);
            }
        }
    }
}
