package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.nanovg.NanoVG;

public final class CoordsHud extends HudElement {
    private final HudModule hudModule;

    public CoordsHud(HudModule hudModule) {
        super("coords", "Координаты", 0.014f, 0.965f);
        setTitleEn("Coordinates");
        this.hudModule = hudModule;
        this.width = 168f;
        this.height = 32f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.coords.isEnabled();
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        long vg = engine.handle();
        int px = mc.player != null ? (int) Math.floor(mc.player.getX()) : 0;
        int py = mc.player != null ? (int) Math.floor(mc.player.getY()) : 64;
        int pz = mc.player != null ? (int) Math.floor(mc.player.getZ()) : 0;

        String dimPath = mc.world != null ? mc.world.getRegistryKey().getValue().getPath() : "overworld";
        String dimLabel;
        String altCoords = "";
        if ("the_nether".equals(dimPath)) {
            dimLabel = I18n.tr("Незер", "Nether");
            altCoords = I18n.tr("Верхний мир: ", "Overworld: ") + (px * 8) + ", " + (pz * 8);
        } else if ("the_end".equals(dimPath)) {
            dimLabel = I18n.tr("Энд", "End");
        } else {
            dimLabel = I18n.tr("Мир", "Overworld");
            altCoords = I18n.tr("Незер: ", "Nether: ") + Math.floorDiv(px, 8) + ", " + Math.floorDiv(pz, 8);
        }

        String mainText = String.format("XYZ  %d, %d, %d", px, py, pz);
        float mainW = Render2D.measureTextBold(vg, mainText, 11.2f);
        float dimW = Render2D.measureTextBold(vg, dimLabel, 9.8f) + 12f;
        float altW = altCoords.isEmpty() ? 0f : Render2D.measureText(vg, altCoords, 9.8f);
        this.width = Math.max(mainW + dimW + 24f, altW + 22f);
        this.height = altCoords.isEmpty() ? 28f : 38f;
        this.elementScale = hudModule.scaleCoords.asFloat();
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        long vg = engine.handle();
        int px = mc.player != null ? (int) Math.floor(mc.player.getX()) : 0;
        int py = mc.player != null ? (int) Math.floor(mc.player.getY()) : 64;
        int pz = mc.player != null ? (int) Math.floor(mc.player.getZ()) : 0;

        String dimPath = mc.world != null ? mc.world.getRegistryKey().getValue().getPath() : "overworld";
        String dimLabel;
        String altCoords = "";
        if ("the_nether".equals(dimPath)) {
            dimLabel = I18n.tr("Незер", "Nether");
            altCoords = I18n.tr("Верхний мир: ", "Overworld: ") + (px * 8) + ", " + (pz * 8);
        } else if ("the_end".equals(dimPath)) {
            dimLabel = I18n.tr("Энд", "End");
        } else {
            dimLabel = I18n.tr("Мир", "Overworld");
            altCoords = I18n.tr("Незер: ", "Nether: ") + Math.floorDiv(px, 8) + ", " + Math.floorDiv(pz, 8);
        }

        String mainText = String.format("XYZ  %d, %d, %d", px, py, pz);
        float mainW = Render2D.measureTextBold(vg, mainText, 11.2f);
        float dimW = Render2D.measureTextBold(vg, dimLabel, 9.8f) + 12f;
        float altW = altCoords.isEmpty() ? 0f : Render2D.measureText(vg, altCoords, 9.8f);
        this.width = Math.max(mainW + dimW + 24f, altW + 22f);
        this.height = altCoords.isEmpty() ? 28f : 38f;

        Render2D.drawLiquidGlassPanel(
                engine, x, y, width, height, 8.5f, screenW, screenH, palette,
                hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
        );

        Render2D.drawTextBold(vg, mainText, x + 9.5f, y + 13.5f, 11.2f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        float badgeX = x + width - dimW - 6.5f;
        Render2D.drawRoundedRect(vg, badgeX, y + 5.5f, dimW, 15.5f, 5.5f, AetherPalette.withAlpha(palette.primaryContainer, 210));
        Render2D.drawTextBold(vg, dimLabel, badgeX + dimW * 0.5f, y + 13.5f, 9.8f, palette.onSurface, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        if (!altCoords.isEmpty()) {
            Render2D.drawText(vg, altCoords, x + 9.5f, y + 28f, 9.8f, palette.onSurfaceVariant, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        }
    }
}
