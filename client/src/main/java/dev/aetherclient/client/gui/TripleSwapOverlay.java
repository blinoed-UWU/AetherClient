package dev.aetherclient.client.gui;

import dev.aetherclient.client.module.impl.TripleSwapModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * Modern Liquid Glass radial 3-way swap overlay.
 * Renders 3 interactive sectors at 120-degree positions around screen center.
 */
public final class TripleSwapOverlay {
    private final TripleSwapModule tripleSwap;

    public TripleSwapOverlay(TripleSwapModule tripleSwap) {
        this.tripleSwap = tripleSwap;
    }

    public void render(long vg, float scaledW, float scaledH, AetherPalette palette, float delta) {
        tripleSwap.updateAnimation(delta);
        float progress = tripleSwap.getMenuProgress();
        if (progress <= 0.01f) return;

        float cx = scaledW / 2.0f;
        float cy = scaledH / 2.0f;
        int selected = tripleSwap.getSelectedSector();
        float baseRadius = tripleSwap.radius.asFloat() * progress;

        // 1. Subtle fullscreen backdrop
        int backdropAlpha = (int) (progress * 90.0f);
        Render2D.drawRoundedRect(vg, 0, 0, scaledW, scaledH, 0, (backdropAlpha << 24));

        // 2. Center hub circle
        float hubRadius = 26.0f * progress;
        int hubBg = palette.darkMode ? 0xCC111827 : 0xEEFFFFFF;
        int hubBorder = palette.primary;
        Render2D.drawCircle(vg, cx, cy, hubRadius, hubBg);
        Render2D.drawCircleOutline(vg, cx, cy, hubRadius, 1.5f, (0x80 << 24) | (hubBorder & 0x00FFFFFF));

        Render2D.drawTextBold(
                vg, "SWAP", cx, cy - 5f * progress,
                10.0f * progress, palette.primary,
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE
        );
        Render2D.drawText(
                vg, selected >= 0 ? "ВЫБРАНО" : "УДЕРЖИВАЙТЕ",
                cx, cy + 6f * progress,
                7.5f * progress, palette.onSurfaceVariant,
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE
        );

        // 3. Sectors at 120-degree angles
        // Sector 0: Top (270 deg / -90 deg)
        // Sector 1: Bottom Right (30 deg)
        // Sector 2: Bottom Left (150 deg)
        float[][] angles = {
                { (float) Math.toRadians(-90), 0 },
                { (float) Math.toRadians(30), 1 },
                { (float) Math.toRadians(150), 2 }
        };

        float cardW = 120.0f * progress;
        float cardH = 46.0f * progress;

        for (int i = 0; i < 3; i++) {
            float rad = angles[i][0];
            int sectorIdx = (int) angles[i][1];
            boolean isHovered = (selected == sectorIdx);

            float dist = baseRadius * (isHovered ? 1.08f : 1.0f);
            float px = cx + (float) Math.cos(rad) * dist;
            float py = cy + (float) Math.sin(rad) * dist;

            float x = px - cardW / 2.0f;
            float y = py - cardH / 2.0f;

            // Connecting indicator line from center to sector
            try (MemoryStack stack = MemoryStack.stackPush()) {
                int lineColor = isHovered ? palette.primary : (0x33 << 24) | (palette.onSurfaceVariant & 0x00FFFFFF);
                NVGColor cLine = NanoVgEngine.argbToNvg(lineColor, NVGColor.malloc(stack));
                NanoVG.nvgBeginPath(vg);
                NanoVG.nvgMoveTo(vg, cx + (float) Math.cos(rad) * (hubRadius + 4f), cy + (float) Math.sin(rad) * (hubRadius + 4f));
                NanoVG.nvgLineTo(vg, px - (float) Math.cos(rad) * (cardW * 0.35f), py - (float) Math.sin(rad) * (cardH * 0.35f));
                NanoVG.nvgStrokeWidth(vg, isHovered ? 2.5f : 1.0f);
                NanoVG.nvgStrokeColor(vg, cLine);
                NanoVG.nvgStroke(vg);
            }

            // Card background
            int cardBg = isHovered
                    ? (palette.darkMode ? 0xE61E293B : 0xF0F8FAFC)
                    : (palette.darkMode ? 0xB30F172A : 0xD9FFFFFF);
            int cardBorder = isHovered ? palette.primary : (0x40 << 24) | (palette.outline & 0x00FFFFFF);

            Render2D.drawRoundedRect(vg, x, y, cardW, cardH, 12f * progress, cardBg);
            Render2D.drawRoundedOutline(vg, x, y, cardW, cardH, 12f * progress, isHovered ? 2.0f : 1.0f, cardBorder);

            // Sector label & item name
            String sectorTitle = "СЕКТОР " + (sectorIdx + 1);
            String itemName = tripleSwap.getItemNameForSector(sectorIdx);

            Render2D.drawTextBold(
                    vg, sectorTitle, x + 12f * progress, y + 14f * progress,
                    8.0f * progress, isHovered ? palette.primary : palette.onSurfaceVariant,
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE
            );

            Render2D.drawTextBold(
                    vg, itemName, x + 12f * progress, y + 29f * progress,
                    11.0f * progress, palette.onSurface,
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE
            );

            // Glowing status pill on right of card
            float dotX = x + cardW - 14f * progress;
            float dotY = y + cardH / 2.0f;
            int dotColor = isHovered ? palette.primary : 0x5594A3B8;
            Render2D.drawCircle(vg, dotX, dotY, 4.0f * progress, dotColor);
        }
    }
}
