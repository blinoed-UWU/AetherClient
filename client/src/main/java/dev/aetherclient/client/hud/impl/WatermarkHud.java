package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import org.lwjgl.nanovg.NanoVG;

public final class WatermarkHud extends HudElement {
    private final HudModule hudModule;

    public WatermarkHud(HudModule hudModule) {
        super("watermark", "Водяной знак", 0.014f, 0.02f);
        setTitleEn("Watermark");
        this.hudModule = hudModule;
        this.width = 166f;
        this.height = 26f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.watermark.isEnabled();
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        long vg = engine.handle();
        int fps = mc.getCurrentFps();
        int ping = -1;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        String fpsText = fps + " FPS";
        String pingText = ping >= 0 ? (ping + " ms") : I18n.tr("Локально", "Local");
        float brandW = Render2D.measureTextBold(vg, "Aether", 11.4f);
        float fpsW = Render2D.measureText(vg, fpsText, 10.2f);
        float pingW = Render2D.measureText(vg, pingText, 10.2f);
        this.width = 7f + 16f + 6f + brandW + 9f + fpsW + 11f + pingW + 10f;
        this.height = 27f;
        this.elementScale = hudModule.scaleWatermark.asFloat();
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        long vg = engine.handle();
        int fps = mc.getCurrentFps();
        int ping = -1;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }

        String fpsText = fps + " FPS";
        String pingText = ping >= 0 ? (ping + " ms") : I18n.tr("Локально", "Local");

        float brandW = Render2D.measureTextBold(vg, "Aether", 11.4f);
        float fpsW = Render2D.measureText(vg, fpsText, 10.2f);
        float pingW = Render2D.measureText(vg, pingText, 10.2f);
        this.width = 7f + 16f + 6f + brandW + 9f + fpsW + 11f + pingW + 10f;
        this.height = 27f;

        Render2D.drawLiquidGlassPanel(
                engine, x, y, width, height, 8.5f, screenW, screenH, palette,
                hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
        );

        Render2D.drawLogo(engine, x + 5.5f, y + 5f, 16f, 5f);

        float cx = x + 27.5f;
        float cy = y + height * 0.5f + 0.5f;
        Render2D.drawTextBold(vg, "Aether", cx, cy, 11.4f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        cx += brandW + 7f;

        Render2D.drawCircle(vg, cx, cy, 1.7f, AetherPalette.withAlpha(palette.primary, 200));
        cx += 6.5f;

        Render2D.drawText(vg, fpsText, cx, cy, 10.2f, palette.onSurfaceVariant, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        cx += fpsW + 6.5f;

        Render2D.drawCircle(vg, cx, cy, 1.3f, AetherPalette.withAlpha(palette.outline, 160));
        cx += 5.5f;

        int pingColor = ping > 140 ? 0xFFFFB4AB : palette.primary;
        Render2D.drawText(vg, pingText, cx, cy, 10.2f, pingColor, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
    }
}
