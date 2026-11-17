package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import org.lwjgl.nanovg.NanoVG;

public final class FpsPingGraphHud extends HudElement {
    private static final int MAX_SAMPLES = 80;

    private final HudModule hudModule;
    private final float[] fpsSamples = new float[MAX_SAMPLES];
    private final float[] pingSamples = new float[MAX_SAMPLES];
    private int head = 0;
    private int count = 0;
    private int currentFps = 0;
    private int currentPing = -1;

    public FpsPingGraphHud(HudModule hudModule) {
        super("fpsping", "График FPS и пинга", 0.5f, 0.85f);
        setTitleEn("FPS & Ping Graph");
        this.hudModule = hudModule;
        this.width = 156f;
        this.height = 48f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.fpsPing.isEnabled();
    }

    private void sample(MinecraftClient mc) {
        currentFps = mc.getCurrentFps();
        currentPing = -1;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) currentPing = entry.getLatency();
        }
        fpsSamples[head] = currentFps;
        pingSamples[head] = Math.min(400f, currentPing >= 0 ? currentPing : 0f);
        head = (head + 1) % MAX_SAMPLES;
        if (count < MAX_SAMPLES) count++;
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        this.width = 156f;
        this.height = 48f;
        this.elementScale = hudModule.scaleFpsPing.asFloat();
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        sample(mc);
        this.width = 156f;
        this.height = 48f;
        long vg = engine.handle();

        Render2D.drawLiquidGlassPanel(
                engine, x, y, width, height, 9.5f, screenW, screenH, palette,
                hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
        );

        Render2D.drawTextBold(vg, currentFps + " FPS", x + 10f, y + 14f, 11f, palette.primary,
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        String ping = currentPing >= 0 ? currentPing + " ms" : "--";
        int pingCol = currentPing > 140 ? 0xFFFFB4AB : palette.tertiary;
        Render2D.drawTextBold(vg, ping, x + width - 10f, y + 14f, 11f, pingCol,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);

        float gx = x + 8f;
        float gy = y + 26f;
        float gw = width - 16f;
        float gh = 15f;
        Render2D.drawRoundedRect(vg, gx, gy, gw, gh, 3f, AetherPalette.withAlpha(palette.onSurface, 22));
        if (count > 1) {
            float maxFps = 1f;
            float maxPing = 1f;
            for (int i = 0; i < count; i++) {
                maxFps = Math.max(maxFps, fpsSamples[i]);
                maxPing = Math.max(maxPing, pingSamples[i]);
            }
            float stepX = gw / (MAX_SAMPLES - 1);
            for (int i = 0; i < count - 1; i++) {
                int i0 = (head - count + i + MAX_SAMPLES * 2) % MAX_SAMPLES;
                int i1 = (i0 + 1) % MAX_SAMPLES;
                float f0 = fpsSamples[i0];
                float f1 = fpsSamples[i1];
                float p0 = pingSamples[i0];
                float p1 = pingSamples[i1];
                float x0 = gx + i * stepX;
                float x1 = gx + (i + 1) * stepX;
                float fy0 = gy + gh - (f0 / maxFps) * (gh - 2f) - 1f;
                float fy1 = gy + gh - (f1 / maxFps) * (gh - 2f) - 1f;
                float py0 = gy + gh - (p0 / Math.max(1f, maxPing)) * (gh - 2f) - 1f;
                float py1 = gy + gh - (p1 / Math.max(1f, maxPing)) * (gh - 2f) - 1f;
                Render2D.drawLine(vg, x0, fy0, x1, fy1, 1.2f, palette.primary);
                Render2D.drawLine(vg, x0, py0, x1, py1, 1.0f, palette.tertiary);
            }
        }
    }
}
