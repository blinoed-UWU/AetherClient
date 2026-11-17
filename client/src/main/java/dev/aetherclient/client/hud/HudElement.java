package dev.aetherclient.client.hud;

import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;

/**
 * Draggable HUD element positioned in normalized [0..1] screen coordinates
 * so widgets automatically stay anchored cleanly across window resizes.
 */
public abstract class HudElement {
    private final String id;
    private final String title;
    private String titleEn;
    private float normX;
    private float normY;
    protected float width = 120f;
    protected float height = 32f;
    public float hoverAnim;
    protected float elementScale = 1.0f;
    /** 0..1 spring-driven enter/exit progress; eases with a soft overshoot on enter. */
    public float enterAnim;

    protected HudElement(String id, String title, float defaultNormX, float defaultNormY) {
        this.id = id;
        this.title = title;
        this.titleEn = title;
        this.normX = clamp01(defaultNormX);
        this.normY = clamp01(defaultNormY);
    }

    protected void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return I18n.tr(title, titleEn);
    }

    public float getNormX() {
        return normX;
    }

    public float getNormY() {
        return normY;
    }

    public void setNormPosition(float nx, float ny) {
        this.normX = clamp01(nx);
        this.normY = clamp01(ny);
    }

    public float getPixelX(float screenW) {
        return normX * Math.max(1f, screenW - getScaledWidth());
    }

    public float getPixelY(float screenH) {
        return normY * Math.max(1f, screenH - getScaledHeight());
    }

    public void setPixelPosition(float px, float py, float screenW, float screenH) {
        float availW = Math.max(1f, screenW - getScaledWidth());
        float availH = Math.max(1f, screenH - getScaledHeight());
        this.normX = clamp01(px / availW);
        this.normY = clamp01(py / availH);
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getScaledWidth() {
        return width * elementScale;
    }

    public float getScaledHeight() {
        return height * elementScale;
    }

    public float getElementScale() {
        return elementScale;
    }

    /**
     * Advances the enter/exit animation toward the visibility target.
     *
     * @return current raw progress in [0..1]
     */
    public float updateEnterAnim(float dt, boolean visible) {
        if (visible) {
            if (enterAnim < 1f) enterAnim = Math.min(1f, enterAnim + dt * 5.2f);
        } else {
            if (enterAnim > 0f) enterAnim = Math.max(0f, enterAnim - dt * 7.0f);
        }
        return enterAnim;
    }

    /**
     * easeOutBack over the raw progress — soft spring overshoot when a widget pops in.
     * Can exceed 1 briefly (overshoot) or dip below 0 on exit; callers must clamp alpha.
     */
    public static float enterEase(float x) {
        float t = Math.max(0f, Math.min(1f, x)) - 1f;
        return 1f + 2.70158f * t * t * t + 1.70158f * t * t;
    }

    public abstract boolean isActive();

    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
    }

    public abstract void render(
            MinecraftClient mc,
            NanoVgEngine engine,
            AetherPalette palette,
            float x,
            float y,
            float screenW,
            float screenH,
            boolean editMode,
            float dt
    );

    private static float clamp01(float v) {
        if (!Float.isFinite(v)) return 0.5f;
        return Math.max(0f, Math.min(1f, v));
    }
}
