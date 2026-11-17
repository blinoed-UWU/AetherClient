package dev.aetherclient.client.render;

import dev.aetherclient.client.theme.AetherPalette;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

/**
 * High-level 2D NanoVG drawing helpers implementing Aether's Material You + Liquid Glass style
 * with crisp vector icons, radial hover lighting, and zero heap allocations in hot draw paths.
 */
public final class Render2D {
    private Render2D() {}

    public enum IconType {
        ALL,
        VISUAL,
        HUD,
        COMBAT,
        CLIENT,
        SEARCH,
        MODULES,
        EDIT_HUD,
        UNLOAD,
        SUN,
        TARGET,
        KEYBIND,
        SLIDERS,
        EYE,
        LINES,
        SPARKLES,
        CLOCK,
        SHIELD,
        RING,
        ARROW,
        USER,
        RADAR,
        POTION,
        HEART
    }

    public static void drawRoundedRect(long vg, float x, float y, float w, float h, float r, int argb) {
        if (w <= 0f || h <= 0f || ((argb >>> 24) & 0xFF) == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, Math.min(r, Math.min(w, h) * 0.5f));
            NanoVG.nvgFillColor(vg, color);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawRoundedGradient(
            long vg, float x, float y, float w, float h, float r,
            int startArgb, int endArgb, boolean vertical
    ) {
        if (w <= 0f || h <= 0f) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor c1 = NanoVgEngine.argbToNvg(startArgb, NVGColor.malloc(stack));
            NVGColor c2 = NanoVgEngine.argbToNvg(endArgb, NVGColor.malloc(stack));
            NVGPaint paint = NVGPaint.malloc(stack);
            float ex = vertical ? x : x + w;
            float ey = vertical ? y + h : y;
            NanoVG.nvgLinearGradient(vg, x, y, ex, ey, c1, c2, paint);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, Math.min(r, Math.min(w, h) * 0.5f));
            NanoVG.nvgFillPaint(vg, paint);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawRoundedOutline(long vg, float x, float y, float w, float h, float r, float stroke, int argb) {
        if (w <= 0f || h <= 0f || ((argb >>> 24) & 0xFF) == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, Math.min(r, Math.min(w, h) * 0.5f));
            NanoVG.nvgStrokeWidth(vg, stroke);
            NanoVG.nvgStrokeColor(vg, color);
            NanoVG.nvgStroke(vg);
        }
    }

    public static void drawRadialHoverOutline(
            long vg, float x, float y, float w, float h, float r, float stroke,
            float cursorX, float cursorY, float radius, int innerArgb, int outerArgb
    ) {
        if (w <= 0f || h <= 0f) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor cIn = NanoVgEngine.argbToNvg(innerArgb, NVGColor.malloc(stack));
            NVGColor cOut = NanoVgEngine.argbToNvg(outerArgb, NVGColor.malloc(stack));
            NVGPaint rad = NVGPaint.malloc(stack);
            NanoVG.nvgRadialGradient(vg, cursorX, cursorY, 2f, radius, cIn, cOut, rad);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, Math.min(r, Math.min(w, h) * 0.5f));
            NanoVG.nvgStrokeWidth(vg, stroke);
            NanoVG.nvgStrokePaint(vg, rad);
            NanoVG.nvgStroke(vg);
        }
    }

    public static void drawRadialGlowInRoundedRect(
            long vg, float x, float y, float w, float h, float r,
            float cx, float cy, float innerRadius, float outerRadius, int centerArgb
    ) {
        if (w <= 0f || h <= 0f || ((centerArgb >>> 24) & 0xFF) == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor cIn = NanoVgEngine.argbToNvg(centerArgb, NVGColor.malloc(stack));
            NVGColor cOut = NanoVgEngine.argbToNvg(centerArgb & 0x00FFFFFF, NVGColor.malloc(stack));
            NVGPaint rad = NVGPaint.malloc(stack);
            NanoVG.nvgRadialGradient(vg, cx, cy, innerRadius, outerRadius, cIn, cOut, rad);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, Math.min(r, Math.min(w, h) * 0.5f));
            NanoVG.nvgFillPaint(vg, rad);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawShadow(long vg, float x, float y, float w, float h, float r, float feather, int shadowArgb) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor inner = NanoVgEngine.argbToNvg(shadowArgb, NVGColor.malloc(stack));
            NVGColor outer = NanoVgEngine.argbToNvg(0x00000000, NVGColor.malloc(stack));
            NVGPaint paint = NVGPaint.malloc(stack);
            NanoVG.nvgBoxGradient(vg, x, y + 3f, w, h, r, feather, inner, outer, paint);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRect(vg, x - feather, y - feather + 3f, w + feather * 2f, h + feather * 2f);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
            NanoVG.nvgPathWinding(vg, NanoVG.NVG_HOLE);
            NanoVG.nvgFillPaint(vg, paint);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawLiquidGlassPanel(
            NanoVgEngine engine,
            float x, float y, float w, float h, float radius,
            float screenW, float screenH,
            AetherPalette palette,
            float glassOpacity,
            float cursorX, float cursorY,
            float spotlightStrength
    ) {
        long vg = engine.handle();
        float r = Math.min(radius, Math.min(w, h) * 0.5f);

        drawShadow(vg, x, y, w, h, r, 24f, palette.darkMode ? 0x6A000000 : 0x36000000);

        int blurImg = engine.blurImage();
        if (blurImg != 0 && screenW > 0 && screenH > 0) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                NVGPaint blurPaint = NVGPaint.malloc(stack);
                NanoVG.nvgImagePattern(vg, 0f, 0f, screenW, screenH, 0f, blurImg, 1.0f, blurPaint);
                NanoVG.nvgBeginPath(vg);
                NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
                NanoVG.nvgFillPaint(vg, blurPaint);
                NanoVG.nvgFill(vg);
            }
        }

        int tintAlpha = Math.round(Math.max(40f, Math.min(235f, glassOpacity * 255f)));
        int topTint = AetherPalette.withAlpha(palette.surfaceContainerHigh, Math.min(240, tintAlpha + 16));
        int bottomTint = AetherPalette.withAlpha(palette.surfaceContainer, tintAlpha);
        drawRoundedGradient(vg, x, y, w, h, r, topTint, bottomTint, true);

        // Specular top sheen for Liquid Glass depth
        int sheenTop = palette.darkMode ? 0x26FFFFFF : 0x55FFFFFF;
        int sheenMid = 0x00FFFFFF;
        drawRoundedGradient(vg, x, y, w, Math.min(h * 0.42f, 64f), r, sheenTop, sheenMid, true);

        // Cursor-tracking spotlight
        if (spotlightStrength > 0.01f) {
            int glowAlpha = Math.round(Math.min(68f, spotlightStrength * 58f));
            drawRadialGlowInRoundedRect(
                    vg, x, y, w, h, r,
                    cursorX, cursorY, 4f, 150f,
                    AetherPalette.withAlpha(palette.primary, glowAlpha)
            );
        }

        // Specular outer glass border with cursor rim reflection
        int baseBorder = palette.darkMode ? 0x36FFFFFF : 0x52FFFFFF;
        drawRoundedOutline(vg, x + 0.5f, y + 0.5f, w - 1f, h - 1f, Math.max(0f, r - 0.5f), 1.1f, baseBorder);
        if (spotlightStrength > 0.05f) {
            drawRadialHoverOutline(
                    vg, x + 0.5f, y + 0.5f, w - 1f, h - 1f, Math.max(0f, r - 0.5f), 1.25f,
                    cursorX, cursorY, 175f,
                    AetherPalette.withAlpha(palette.primary, 130),
                    0x00FFFFFF
            );
        }
    }

    /**
     * Frosted Glass + Liquid Glass + Material 3 tonal card for sub-panels, module tiles, and navigation rails.
     */
    public static void drawFrostedGlassCard(
            NanoVgEngine engine,
            float x, float y, float w, float h, float radius,
            float screenW, float screenH,
            AetherPalette palette,
            float activeMix,
            float hoverMix,
            float cursorX, float cursorY
    ) {
        long vg = engine.handle();
        float r = Math.min(radius, Math.min(w, h) * 0.5f);

        int blurImg = engine.blurImage();
        if (blurImg != 0 && screenW > 0 && screenH > 0) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                NVGPaint blurPaint = NVGPaint.malloc(stack);
                NanoVG.nvgImagePattern(vg, 0f, 0f, screenW, screenH, 0f, blurImg, 0.72f, blurPaint);
                NanoVG.nvgBeginPath(vg);
                NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
                NanoVG.nvgFillPaint(vg, blurPaint);
                NanoVG.nvgFill(vg);
            }
        }

        int baseTop = AetherPalette.withAlpha(palette.surfaceContainerHigh, palette.darkMode ? 148 : 186);
        int baseBot = AetherPalette.withAlpha(palette.surfaceContainer, palette.darkMode ? 132 : 170);
        int activeTop = AetherPalette.withAlpha(palette.primaryContainer, palette.darkMode ? 168 : 206);
        int activeBot = AetherPalette.withAlpha(palette.secondaryContainer, palette.darkMode ? 145 : 185);
        float mix = Math.max(0f, Math.min(1f, activeMix * 0.85f + hoverMix * 0.15f));
        drawRoundedGradient(vg, x, y, w, h, r,
                AetherPalette.lerpArgb(baseTop, activeTop, mix),
                AetherPalette.lerpArgb(baseBot, activeBot, mix),
                true);

        // Top glass specular sheen
        int sheenTop = palette.darkMode ? (0x1DFFFFFF + (Math.round(hoverMix * 16f) << 24)) : 0x4CFFFFFF;
        drawRoundedGradient(vg, x + 0.5f, y + 0.5f, w - 1f, Math.min(h * 0.45f, 22f),
                Math.max(0f, r - 0.5f), sheenTop, 0x00FFFFFF, true);

        // Liquid Glass cursor glow
        if (hoverMix > 0.02f) {
            drawRadialGlowInRoundedRect(vg, x, y, w, h, r, cursorX, cursorY, 2f, 96f,
                    AetherPalette.withAlpha(palette.primary, Math.round(hoverMix * 60f)));
        }

        // Frosted rim border + reactive cursor highlight
        int idleStroke = palette.darkMode ? 0x2EFFFFFF : 0x48FFFFFF;
        int activeStroke = AetherPalette.withAlpha(palette.primary, 175);
        int borderCol = AetherPalette.lerpArgb(idleStroke, activeStroke, Math.max(activeMix * 0.7f, hoverMix * 0.85f));
        drawRoundedOutline(vg, x + 0.5f, y + 0.5f, w - 1f, h - 1f, Math.max(0f, r - 0.5f), 0.95f, borderCol);

        if (hoverMix > 0.04f) {
            drawRadialHoverOutline(
                    vg, x + 0.5f, y + 0.5f, w - 1f, h - 1f, Math.max(0f, r - 0.5f), 1.35f,
                    cursorX, cursorY, 92f,
                    AetherPalette.withAlpha(palette.primary, Math.round(hoverMix * 210f)),
                    0x00FFFFFF
            );
        }
    }

    public static void drawLine(long vg, float x1, float y1, float x2, float y2, float stroke, int argb) {
        if (((argb >>> 24) & 0xFF) == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgMoveTo(vg, x1, y1);
            NanoVG.nvgLineTo(vg, x2, y2);
            NanoVG.nvgStrokeWidth(vg, stroke);
            NanoVG.nvgStrokeColor(vg, color);
            NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
            NanoVG.nvgStroke(vg);
        }
    }

    public static void drawRing(long vg, float cx, float cy, float r, float stroke, int argb) {
        if (r <= 0f || ((argb >>> 24) & 0xFF) == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgCircle(vg, cx, cy, r);
            NanoVG.nvgStrokeWidth(vg, stroke);
            NanoVG.nvgStrokeColor(vg, color);
            NanoVG.nvgStroke(vg);
        }
    }

    /**
     * Tapered tracer line: thick at the origin, thin at the target, with a soft
     * under-glow for depth. Drawn as a filled quad so the width truly tapers.
     */
    public static void drawTaperedLine(long vg, float x1, float y1, float x2, float y2,
                                       float wStart, float wEnd, int argb, int glowArgb) {
        if (((argb >>> 24) & 0xFF) == 0) return;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lenSq = dx * dx + dy * dy;
        if (lenSq < 1e-4f) return;
        float invLen = 1f / (float) Math.sqrt(lenSq);
        float nx = -dy * invLen;
        float ny = dx * invLen;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Glow pass (wider, low alpha)
            if (((glowArgb >>> 24) & 0xFF) > 0) {
                float gw1 = wStart * 2.6f;
                float gw2 = wEnd * 2.6f + 1.2f;
                NVGColor glow = NanoVgEngine.argbToNvg(glowArgb, NVGColor.malloc(stack));
                NanoVG.nvgBeginPath(vg);
                NanoVG.nvgMoveTo(vg, x1 + nx * gw1, y1 + ny * gw1);
                NanoVG.nvgLineTo(vg, x2 + nx * gw2, y2 + ny * gw2);
                NanoVG.nvgLineTo(vg, x2 - nx * gw2, y2 - ny * gw2);
                NanoVG.nvgLineTo(vg, x1 - nx * gw1, y1 - ny * gw1);
                NanoVG.nvgClosePath(vg);
                NanoVG.nvgFillColor(vg, glow);
                NanoVG.nvgFill(vg);
            }

            // Core tapered quad
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgMoveTo(vg, x1 + nx * wStart, y1 + ny * wStart);
            NanoVG.nvgLineTo(vg, x2 + nx * wEnd, y2 + ny * wEnd);
            NanoVG.nvgLineTo(vg, x2 - nx * wEnd, y2 - ny * wEnd);
            NanoVG.nvgLineTo(vg, x1 - nx * wStart, y1 - ny * wStart);
            NanoVG.nvgClosePath(vg);
            NanoVG.nvgFillColor(vg, color);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawCornerRect(long vg, float x, float y, float w, float h, float len, float stroke, int argb) {
        if (w <= 0f || h <= 0f || ((argb >>> 24) & 0xFF) == 0) return;
        float lx = Math.min(len, w * 0.42f);
        float ly = Math.min(len, h * 0.42f);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgMoveTo(vg, x, y + ly);
            NanoVG.nvgLineTo(vg, x, y);
            NanoVG.nvgLineTo(vg, x + lx, y);

            NanoVG.nvgMoveTo(vg, x + w - lx, y);
            NanoVG.nvgLineTo(vg, x + w, y);
            NanoVG.nvgLineTo(vg, x + w, y + ly);

            NanoVG.nvgMoveTo(vg, x, y + h - ly);
            NanoVG.nvgLineTo(vg, x, y + h);
            NanoVG.nvgLineTo(vg, x + lx, y + h);

            NanoVG.nvgMoveTo(vg, x + w - lx, y + h);
            NanoVG.nvgLineTo(vg, x + w, y + h);
            NanoVG.nvgLineTo(vg, x + w, y + h - ly);

            NanoVG.nvgStrokeWidth(vg, stroke);
            NanoVG.nvgStrokeColor(vg, color);
            NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
            NanoVG.nvgLineJoin(vg, NanoVG.NVG_ROUND);
            NanoVG.nvgStroke(vg);
        }
    }

    /**
     * Bracket-style outline: four L-shaped corners with perpendicular end ticks.
     */
    public static void drawBracketRect(long vg, float x, float y, float w, float h, float len, float stroke, int argb) {
        if (w <= 0f || h <= 0f || ((argb >>> 24) & 0xFF) == 0) return;
        float l = Math.min(len, Math.min(w, h) * 0.5f);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
            NanoVG.nvgLineJoin(vg, NanoVG.NVG_ROUND);
            NanoVG.nvgStrokeWidth(vg, stroke);
            NanoVG.nvgStrokeColor(vg, color);
            NanoVG.nvgBeginPath(vg);
            addBracketCorner(vg, x, y, l, +1f, +1f);
            addBracketCorner(vg, x + w, y, l, -1f, +1f);
            addBracketCorner(vg, x, y + h, l, +1f, -1f);
            addBracketCorner(vg, x + w, y + h, l, -1f, -1f);
            NanoVG.nvgStroke(vg);
        }
    }

    private static void addBracketCorner(long vg, float cx, float cy, float l, float dx, float dy) {
        NanoVG.nvgMoveTo(vg, cx, cy);
        NanoVG.nvgLineTo(vg, cx + dx * l, cy);
        NanoVG.nvgMoveTo(vg, cx, cy);
        NanoVG.nvgLineTo(vg, cx, cy + dy * l);
        NanoVG.nvgMoveTo(vg, cx + dx * l, cy - 0.32f * l * dy);
        NanoVG.nvgLineTo(vg, cx + dx * l, cy + 0.32f * l * dy);
        NanoVG.nvgMoveTo(vg, cx - 0.32f * l * dx, cy + dy * l);
        NanoVG.nvgLineTo(vg, cx + 0.32f * l * dx, cy + dy * l);
    }

    /**
     * Glow-style box: soft outer halo + crisp inner outline + faint interior fill.
     */
    public static void drawGlowBox(long vg, float x, float y, float w, float h, float r, float stroke, int argb) {
        if (w <= 0f || h <= 0f || ((argb >>> 24) & 0xFF) == 0) return;
        int a = (argb >>> 24) & 0xFF;
        drawRoundedOutline(vg, x - 2.5f, y - 2.5f, w + 5f, h + 5f, r + 2.5f, stroke * 2.4f,
                AetherPalette.withAlpha(argb, Math.min(80, a / 3)));
        drawRoundedRect(vg, x, y, w, h, r, AetherPalette.withAlpha(argb, 26));
        drawRoundedOutline(vg, x, y, w, h, r, stroke, argb);
    }

    /** Partial ring (arc) with an explicit start angle and sweep in radians. */
    public static void drawArc(long vg, float cx, float cy, float r, float stroke, float startRad, float sweepRad, int argb) {
        if (r <= 0f || sweepRad == 0f || ((argb >>> 24) & 0xFF) == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgArc(vg, cx, cy, r, startRad, startRad + sweepRad,
                    sweepRad >= 0 ? NanoVG.NVG_CW : NanoVG.NVG_CCW);
            NanoVG.nvgStrokeWidth(vg, stroke);
            NanoVG.nvgStrokeColor(vg, color);
            NanoVG.nvgStroke(vg);
        }
    }

    /** Full-screen linear gradient fading from one edge toward the opposite edge (motion blur streak). */
    public static void drawScreenEdgeGradient(long vg, float w, float h, int edgeArgb, boolean horizontal, boolean fromStart) {
        if (w <= 0f || h <= 0f || ((edgeArgb >>> 24) & 0xFF) == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor c0 = NanoVgEngine.argbToNvg(fromStart ? edgeArgb : 0x00000000, NVGColor.malloc(stack));
            NVGColor c1 = NanoVgEngine.argbToNvg(fromStart ? 0x00000000 : edgeArgb, NVGColor.malloc(stack));
            NVGPaint paint = NVGPaint.malloc(stack);
            if (horizontal) {
                NanoVG.nvgLinearGradient(vg, 0f, 0f, w, 0f, c0, c1, paint);
            } else {
                NanoVG.nvgLinearGradient(vg, 0f, 0f, 0f, h, c0, c1, paint);
            }
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRect(vg, 0f, 0f, w, h);
            NanoVG.nvgFillPaint(vg, paint);
            NanoVG.nvgFill(vg);
        }
    }

    /** Blends the blurred framebuffer texture shifted along velocity vector for true motion blur. */
    public static void drawMotionBlurPass(NanoVgEngine engine, float w, float h, float offX, float offY, float alpha) {
        if (engine == null || w <= 0f || h <= 0f || alpha <= 0.01f) return;
        int blurTex = engine.blurImage();
        if (blurTex == 0) return;
        long vg = engine.handle();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGPaint paint = NVGPaint.malloc(stack);
            NanoVG.nvgImagePattern(vg, offX, offY, w, h, 0f, blurTex, Math.min(0.85f, alpha), paint);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRect(vg, 0f, 0f, w, h);
            NanoVG.nvgFillPaint(vg, paint);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawPolygon(long vg, float[] xs, float[] ys, int count, int argb) {
        if (((argb >>> 24) & 0xFF) == 0 || count < 3) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgMoveTo(vg, xs[0], ys[0]);
            for (int i = 1; i < count; i++) {
                NanoVG.nvgLineTo(vg, xs[i], ys[i]);
            }
            NanoVG.nvgClosePath(vg);
            NanoVG.nvgFillColor(vg, color);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawCircle(long vg, float cx, float cy, float radius, int argb) {
        if (((argb >>> 24) & 0xFF) == 0 || radius <= 0f) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgCircle(vg, cx, cy, radius);
            NanoVG.nvgFillColor(vg, color);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawCircleOutline(long vg, float cx, float cy, float radius, float stroke, int argb) {
        if (((argb >>> 24) & 0xFF) == 0 || radius <= 0f) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgCircle(vg, cx, cy, radius);
            NanoVG.nvgStrokeWidth(vg, stroke);
            NanoVG.nvgStrokeColor(vg, color);
            NanoVG.nvgStroke(vg);
        }
    }

    public static void drawArcDeg(long vg, float cx, float cy, float radius, float startAngleDeg, float endAngleDeg, float stroke, int argb) {
        if (((argb >>> 24) & 0xFF) == 0 || radius <= 0f || stroke <= 0f) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgBeginPath(vg);
            float a0 = (float) Math.toRadians(startAngleDeg);
            float a1 = (float) Math.toRadians(endAngleDeg);
            NanoVG.nvgArc(vg, cx, cy, radius, a0, a1, NanoVG.NVG_CW);
            NanoVG.nvgStrokeWidth(vg, stroke);
            NanoVG.nvgStrokeColor(vg, color);
            NanoVG.nvgStroke(vg);
        }
    }

    public static void drawFilledPoly3(long vg, float x1, float y1, float x2, float y2, float x3, float y3, int fillArgb, int outlineArgb, float stroke) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgMoveTo(vg, x1, y1);
            NanoVG.nvgLineTo(vg, x2, y2);
            NanoVG.nvgLineTo(vg, x3, y3);
            NanoVG.nvgClosePath(vg);
            if (((fillArgb >>> 24) & 0xFF) > 0) {
                NVGColor fColor = NanoVgEngine.argbToNvg(fillArgb, NVGColor.malloc(stack));
                NanoVG.nvgFillColor(vg, fColor);
                NanoVG.nvgFill(vg);
            }
            if (stroke > 0f && ((outlineArgb >>> 24) & 0xFF) > 0) {
                NVGColor oColor = NanoVgEngine.argbToNvg(outlineArgb, NVGColor.malloc(stack));
                NanoVG.nvgStrokeWidth(vg, stroke);
                NanoVG.nvgStrokeColor(vg, oColor);
                NanoVG.nvgStroke(vg);
            }
        }
    }

    public static void drawFilledPoly4(long vg, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int fillArgb, int outlineArgb, float stroke) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgMoveTo(vg, x1, y1);
            NanoVG.nvgLineTo(vg, x2, y2);
            NanoVG.nvgLineTo(vg, x3, y3);
            NanoVG.nvgLineTo(vg, x4, y4);
            NanoVG.nvgClosePath(vg);
            if (((fillArgb >>> 24) & 0xFF) > 0) {
                NVGColor fColor = NanoVgEngine.argbToNvg(fillArgb, NVGColor.malloc(stack));
                NanoVG.nvgFillColor(vg, fColor);
                NanoVG.nvgFill(vg);
            }
            if (stroke > 0f && ((outlineArgb >>> 24) & 0xFF) > 0) {
                NVGColor oColor = NanoVgEngine.argbToNvg(outlineArgb, NVGColor.malloc(stack));
                NanoVG.nvgStrokeWidth(vg, stroke);
                NanoVG.nvgStrokeColor(vg, oColor);
                NanoVG.nvgStroke(vg);
            }
        }
    }

    public static void drawText(long vg, String text, float x, float y, float size, int argb, int align) {
        drawTextFace(vg, NanoVgEngine.FONT_REGULAR, text, x, y, size, argb, align);
    }

    public static void drawTextBold(long vg, String text, float x, float y, float size, int argb, int align) {
        drawTextFace(vg, NanoVgEngine.FONT_BOLD, text, x, y, size, argb, align);
    }

    public static void drawTextFace(long vg, String fontFace, String text, float x, float y, float size, int argb, int align) {
        if (text == null || text.isEmpty()) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgFontFace(vg, fontFace);
            NanoVG.nvgFontSize(vg, size);
            NanoVG.nvgTextAlign(vg, align);
            NanoVG.nvgFillColor(vg, color);
            NanoVG.nvgText(vg, x, y, text);
        }
    }

    public static float measureText(long vg, String text, float size) {
        return measureTextFace(vg, NanoVgEngine.FONT_REGULAR, text, size);
    }

    public static float measureTextBold(long vg, String text, float size) {
        return measureTextFace(vg, NanoVgEngine.FONT_BOLD, text, size);
    }

    public static float measureTextFace(long vg, String fontFace, String text, float size) {
        if (text == null || text.isEmpty()) return 0f;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NanoVG.nvgFontFace(vg, fontFace);
            NanoVG.nvgFontSize(vg, size);
            var bounds = stack.mallocFloat(4);
            return NanoVG.nvgTextBounds(vg, 0f, 0f, text, bounds);
        }
    }

    public static void drawLogo(NanoVgEngine engine, float x, float y, float size, float radius) {
        int img = engine.logoImage();
        if (img == 0) return;
        long vg = engine.handle();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGPaint paint = NVGPaint.malloc(stack);
            NanoVG.nvgImagePattern(vg, x, y, size, size, 0f, img, 1.0f, paint);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, size, size, radius);
            NanoVG.nvgFillPaint(vg, paint);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawChevron(long vg, float cx, float cy, float size, float expandProgress, int argb) {
        if (((argb >>> 24) & 0xFF) == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgSave(vg);
            NanoVG.nvgTranslate(vg, cx, cy);
            NanoVG.nvgRotate(vg, (float) (Math.PI * expandProgress));
            float r = size * 0.34f;
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgMoveTo(vg, -r, -r * 0.45f);
            NanoVG.nvgLineTo(vg, 0f, r * 0.55f);
            NanoVG.nvgLineTo(vg, r, -r * 0.45f);
            NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
            NanoVG.nvgLineJoin(vg, NanoVG.NVG_ROUND);
            NanoVG.nvgStrokeWidth(vg, 1.65f);
            NanoVG.nvgStrokeColor(vg, color);
            NanoVG.nvgStroke(vg);
            NanoVG.nvgRestore(vg);
        }
    }

    public static void drawIcon(long vg, IconType icon, float cx, float cy, float size, int argb) {
        if (icon == null || ((argb >>> 24) & 0xFF) == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NanoVG.nvgSave(vg);
            NVGColor color = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
            NanoVG.nvgLineJoin(vg, NanoVG.NVG_ROUND);
            NanoVG.nvgStrokeWidth(vg, Math.max(1.25f, size * 0.12f));
            NanoVG.nvgStrokeColor(vg, color);
            NanoVG.nvgFillColor(vg, color);
            float h = size * 0.5f;

            switch (icon) {
                case ALL -> {
                    float box = size * 0.36f;
                    float off = size * 0.08f;
                    float rad = box * 0.32f;
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgRoundedRect(vg, cx - off - box, cy - off - box, box, box, rad);
                    NanoVG.nvgRoundedRect(vg, cx + off, cy - off - box, box, box, rad);
                    NanoVG.nvgRoundedRect(vg, cx - off - box, cy + off, box, box, rad);
                    NanoVG.nvgRoundedRect(vg, cx + off, cy + off, box, box, rad);
                    NanoVG.nvgFill(vg);
                }
                case VISUAL, SUN -> {
                    if (icon == IconType.VISUAL) {
                        NanoVG.nvgBeginPath(vg);
                        NanoVG.nvgMoveTo(vg, cx - h * 0.92f, cy);
                        NanoVG.nvgQuadTo(vg, cx, cy - h * 0.85f, cx + h * 0.92f, cy);
                        NanoVG.nvgQuadTo(vg, cx, cy + h * 0.85f, cx - h * 0.92f, cy);
                        NanoVG.nvgClosePath(vg);
                        NanoVG.nvgStroke(vg);
                        NanoVG.nvgBeginPath(vg);
                        NanoVG.nvgCircle(vg, cx, cy, size * 0.14f);
                        NanoVG.nvgFill(vg);
                    } else {
                        NanoVG.nvgBeginPath(vg);
                        NanoVG.nvgCircle(vg, cx, cy, size * 0.20f);
                        NanoVG.nvgFill(vg);
                        NanoVG.nvgBeginPath(vg);
                        for (int i = 0; i < 8; i++) {
                            double a = i * (Math.PI / 4.0);
                            float r1 = size * 0.31f;
                            float r2 = size * 0.44f;
                            NanoVG.nvgMoveTo(vg, cx + (float) Math.cos(a) * r1, cy + (float) Math.sin(a) * r1);
                            NanoVG.nvgLineTo(vg, cx + (float) Math.cos(a) * r2, cy + (float) Math.sin(a) * r2);
                        }
                        NanoVG.nvgStroke(vg);
                    }
                }
                case HUD, EDIT_HUD -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgRoundedRect(vg, cx - h * 0.85f, cy - h * 0.85f, h * 0.72f, h * 1.15f, 1.8f);
                    NanoVG.nvgRoundedRect(vg, cx + h * 0.10f, cy - h * 0.85f, h * 0.75f, h * 0.65f, 1.8f);
                    NanoVG.nvgRoundedRect(vg, cx + h * 0.10f, cy + h * 0.05f, h * 0.75f, h * 0.80f, 1.8f);
                    NanoVG.nvgRoundedRect(vg, cx - h * 0.85f, cy + h * 0.50f, h * 0.72f, h * 0.35f, 1.5f);
                    NanoVG.nvgStroke(vg);
                }
                case COMBAT, TARGET -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgCircle(vg, cx, cy, size * 0.34f);
                    NanoVG.nvgStroke(vg);
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgCircle(vg, cx, cy, size * 0.12f);
                    NanoVG.nvgFill(vg);
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgMoveTo(vg, cx - h * 0.95f, cy);
                    NanoVG.nvgLineTo(vg, cx - size * 0.24f, cy);
                    NanoVG.nvgMoveTo(vg, cx + size * 0.24f, cy);
                    NanoVG.nvgLineTo(vg, cx + h * 0.95f, cy);
                    NanoVG.nvgMoveTo(vg, cx, cy - h * 0.95f);
                    NanoVG.nvgLineTo(vg, cx, cy - size * 0.24f);
                    NanoVG.nvgMoveTo(vg, cx, cy + size * 0.24f);
                    NanoVG.nvgLineTo(vg, cx, cy + h * 0.95f);
                    NanoVG.nvgStroke(vg);
                }
                case CLIENT, SLIDERS -> {
                    float y1 = cy - size * 0.18f;
                    float y2 = cy + size * 0.18f;
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgMoveTo(vg, cx - h * 0.85f, y1);
                    NanoVG.nvgLineTo(vg, cx + h * 0.85f, y1);
                    NanoVG.nvgMoveTo(vg, cx - h * 0.85f, y2);
                    NanoVG.nvgLineTo(vg, cx + h * 0.85f, y2);
                    NanoVG.nvgStroke(vg);
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgCircle(vg, cx - size * 0.14f, y1, size * 0.11f);
                    NanoVG.nvgCircle(vg, cx + size * 0.16f, y2, size * 0.11f);
                    NanoVG.nvgFill(vg);
                }
                case SEARCH -> {
                    float r = size * 0.28f;
                    float sx = cx - size * 0.08f;
                    float sy = cy - size * 0.08f;
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgCircle(vg, sx, sy, r);
                    NanoVG.nvgMoveTo(vg, sx + r * 0.72f, sy + r * 0.72f);
                    NanoVG.nvgLineTo(vg, cx + h * 0.82f, cy + h * 0.82f);
                    NanoVG.nvgStroke(vg);
                }
                case MODULES -> {
                    float r = size * 0.36f;
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgMoveTo(vg, cx, cy - r);
                    NanoVG.nvgLineTo(vg, cx + r * 0.88f, cy - r * 0.45f);
                    NanoVG.nvgLineTo(vg, cx + r * 0.88f, cy + r * 0.45f);
                    NanoVG.nvgLineTo(vg, cx, cy + r);
                    NanoVG.nvgLineTo(vg, cx - r * 0.88f, cy + r * 0.45f);
                    NanoVG.nvgLineTo(vg, cx - r * 0.88f, cy - r * 0.45f);
                    NanoVG.nvgClosePath(vg);
                    NanoVG.nvgMoveTo(vg, cx, cy);
                    NanoVG.nvgLineTo(vg, cx, cy + r);
                    NanoVG.nvgMoveTo(vg, cx, cy);
                    NanoVG.nvgLineTo(vg, cx + r * 0.88f, cy - r * 0.45f);
                    NanoVG.nvgMoveTo(vg, cx, cy);
                    NanoVG.nvgLineTo(vg, cx - r * 0.88f, cy - r * 0.45f);
                    NanoVG.nvgStroke(vg);
                }
                case UNLOAD -> {
                    float r = size * 0.33f;
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgArc(vg, cx, cy, r, (float) (-Math.PI * 0.28), (float) (Math.PI * 1.28), NanoVG.NVG_CW);
                    NanoVG.nvgMoveTo(vg, cx, cy - h * 0.85f);
                    NanoVG.nvgLineTo(vg, cx, cy - size * 0.04f);
                    NanoVG.nvgStroke(vg);
                }
                case KEYBIND -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgRoundedRect(vg, cx - h * 0.86f, cy - h * 0.62f, size * 0.86f, size * 0.62f, 2.2f);
                    NanoVG.nvgStroke(vg);
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgRoundedRect(vg, cx - h * 0.45f, cy + h * 0.14f, size * 0.45f, size * 0.12f, 0.8f);
                    NanoVG.nvgFill(vg);
                }
                case EYE -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgRoundedRect(vg, cx - h * 0.8f, cy - h * 0.8f, size * 0.8f, size * 0.8f, 2.4f);
                    NanoVG.nvgStroke(vg);
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgCircle(vg, cx, cy, size * 0.15f);
                    NanoVG.nvgFill(vg);
                }
                case LINES -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgMoveTo(vg, cx - h * 0.75f, cy + h * 0.6f);
                    NanoVG.nvgLineTo(vg, cx + h * 0.75f, cy - h * 0.6f);
                    NanoVG.nvgMoveTo(vg, cx - h * 0.75f, cy - h * 0.25f);
                    NanoVG.nvgLineTo(vg, cx + h * 0.15f, cy - h * 0.75f);
                    NanoVG.nvgStroke(vg);
                }
                case SPARKLES -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgMoveTo(vg, cx, cy - h * 0.85f);
                    NanoVG.nvgLineTo(vg, cx, cy + h * 0.85f);
                    NanoVG.nvgMoveTo(vg, cx - h * 0.85f, cy);
                    NanoVG.nvgLineTo(vg, cx + h * 0.85f, cy);
                    NanoVG.nvgMoveTo(vg, cx - h * 0.48f, cy - h * 0.48f);
                    NanoVG.nvgLineTo(vg, cx + h * 0.48f, cy + h * 0.48f);
                    NanoVG.nvgMoveTo(vg, cx + h * 0.48f, cy - h * 0.48f);
                    NanoVG.nvgLineTo(vg, cx - h * 0.48f, cy + h * 0.48f);
                    NanoVG.nvgStroke(vg);
                }
                case CLOCK -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgCircle(vg, cx, cy, size * 0.38f);
                    NanoVG.nvgMoveTo(vg, cx, cy - size * 0.22f);
                    NanoVG.nvgLineTo(vg, cx, cy);
                    NanoVG.nvgLineTo(vg, cx + size * 0.18f, cy + size * 0.08f);
                    NanoVG.nvgStroke(vg);
                }
                case SHIELD -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgMoveTo(vg, cx, cy - h * 0.85f);
                    NanoVG.nvgLineTo(vg, cx + h * 0.72f, cy - h * 0.55f);
                    NanoVG.nvgLineTo(vg, cx + h * 0.62f, cy + h * 0.2f);
                    NanoVG.nvgQuadTo(vg, cx, cy + h * 0.9f, cx, cy + h * 0.9f);
                    NanoVG.nvgQuadTo(vg, cx, cy + h * 0.9f, cx - h * 0.62f, cy + h * 0.2f);
                    NanoVG.nvgLineTo(vg, cx - h * 0.72f, cy - h * 0.55f);
                    NanoVG.nvgClosePath(vg);
                    NanoVG.nvgStroke(vg);
                }
                case RING -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgCircle(vg, cx, cy, size * 0.34f);
                    NanoVG.nvgStroke(vg);
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgCircle(vg, cx, cy, size * 0.18f);
                    NanoVG.nvgStroke(vg);
                }
                case ARROW -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgMoveTo(vg, cx, cy - h * 0.82f);
                    NanoVG.nvgLineTo(vg, cx + h * 0.68f, cy + h * 0.68f);
                    NanoVG.nvgLineTo(vg, cx, cy + h * 0.34f);
                    NanoVG.nvgLineTo(vg, cx - h * 0.68f, cy + h * 0.68f);
                    NanoVG.nvgClosePath(vg);
                    NanoVG.nvgFill(vg);
                }
                case USER -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgCircle(vg, cx, cy - size * 0.14f, size * 0.18f);
                    NanoVG.nvgStroke(vg);
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgArc(vg, cx, cy + size * 0.38f, size * 0.30f, (float) Math.PI, (float) (Math.PI * 2.0), NanoVG.NVG_CW);
                    NanoVG.nvgStroke(vg);
                }
                case RADAR -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgCircle(vg, cx, cy, size * 0.38f);
                    NanoVG.nvgMoveTo(vg, cx, cy);
                    NanoVG.nvgLineTo(vg, cx + size * 0.26f, cy - size * 0.24f);
                    NanoVG.nvgStroke(vg);
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgCircle(vg, cx, cy, size * 0.10f);
                    NanoVG.nvgFill(vg);
                }
                case POTION -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgMoveTo(vg, cx - size * 0.12f, cy - h * 0.82f);
                    NanoVG.nvgLineTo(vg, cx + size * 0.12f, cy - h * 0.82f);
                    NanoVG.nvgMoveTo(vg, cx - size * 0.08f, cy - h * 0.82f);
                    NanoVG.nvgLineTo(vg, cx - size * 0.08f, cy - size * 0.12f);
                    NanoVG.nvgLineTo(vg, cx - size * 0.30f, cy + size * 0.26f);
                    NanoVG.nvgLineTo(vg, cx + size * 0.30f, cy + size * 0.26f);
                    NanoVG.nvgLineTo(vg, cx + size * 0.08f, cy - size * 0.12f);
                    NanoVG.nvgLineTo(vg, cx + size * 0.08f, cy - h * 0.82f);
                    NanoVG.nvgStroke(vg);
                }
                case HEART -> {
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgMoveTo(vg, cx, cy + size * 0.36f);
                    NanoVG.nvgBezierTo(vg, cx - size * 0.44f, cy + size * 0.04f, cx - size * 0.36f, cy - size * 0.34f, cx, cy - size * 0.12f);
                    NanoVG.nvgBezierTo(vg, cx + size * 0.36f, cy - size * 0.34f, cx + size * 0.44f, cy + size * 0.04f, cx, cy + size * 0.36f);
                    NanoVG.nvgStroke(vg);
                }
                default -> {
                }
            }
            NanoVG.nvgRestore(vg);
        }
    }

    public static void pushScissor(long vg, float x, float y, float w, float h) {
        NanoVG.nvgSave(vg);
        NanoVG.nvgIntersectScissor(vg, x, y, Math.max(0f, w), Math.max(0f, h));
    }

    public static void popScissor(long vg) {
        NanoVG.nvgRestore(vg);
    }

    public static boolean isHovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
