package dev.aetherclient.client.render;

import dev.aetherclient.client.module.impl.HitColorModule;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

import java.lang.reflect.Field;

/**
 * Directly updates Minecraft's 16x16 OverlayTexture NativeImage for genuine 3D model hurt tints.
 * Recolors rows 0..7 of the overlay texture (the exact rows Minecraft samples when an entity is damaged).
 */
public final class HitColorApplier {
    private static final int VANILLA_HURT_COLOR = -1291911168; // 0xB3FF0000 (~70% red)
    private static Field textureField;
    private static int lastAppliedColor = 0;
    private static boolean failed = false;

    private HitColorApplier() {}

    public static void update(MinecraftClient mc, HitColorModule module, AetherPalette palette, float globalTime) {
        if (mc == null || mc.gameRenderer == null || failed) return;
        OverlayTexture overlay = mc.gameRenderer.getOverlayTexture();
        if (overlay == null) return;

        try {
            if (textureField == null) {
                for (Field f : OverlayTexture.class.getDeclaredFields()) {
                    if (NativeImageBackedTexture.class.isAssignableFrom(f.getType()) || f.getType().getName().endsWith("class_1043")) {
                        f.setAccessible(true);
                        textureField = f;
                        break;
                    }
                }
            }
            if (textureField == null) {
                failed = true;
                System.err.println("[AetherClient] Could not find NativeImageBackedTexture field in OverlayTexture");
                return;
            }

            Object texObj = textureField.get(overlay);
            if (!(texObj instanceof NativeImageBackedTexture tex)) return;
            NativeImage img = tex.getImage();
            if (img == null) return;

            int targetColor;
            if (module.isEnabled()) {
                int col = palette.resolvePalette(module.palette.getValue(), module.color.getValue(), globalTime, module.colorSpeed.asFloat());
                float intensity = module.intensity.asFloat();
                int alpha = Math.max(20, Math.min(255, Math.round(intensity * 210f)));
                targetColor = AetherPalette.withAlpha(col, alpha);
            } else {
                targetColor = VANILLA_HURT_COLOR;
            }

            if (targetColor == lastAppliedColor) return;
            lastAppliedColor = targetColor;

            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 16; x++) {
                    img.setColorArgb(x, y, targetColor);
                }
            }
            tex.upload();
        } catch (Throwable t) {
            System.err.println("[AetherClient] HitColorApplier update error: " + t.getMessage());
        }
    }

    public static void restore(MinecraftClient mc) {
        if (mc == null || mc.gameRenderer == null || failed) return;
        OverlayTexture overlay = mc.gameRenderer.getOverlayTexture();
        if (overlay == null) return;

        try {
            if (textureField != null) {
                Object texObj = textureField.get(overlay);
                if (texObj instanceof NativeImageBackedTexture tex) {
                    NativeImage img = tex.getImage();
                    if (img != null) {
                        for (int y = 0; y < 8; y++) {
                            for (int x = 0; x < 16; x++) {
                                img.setColorArgb(x, y, VANILLA_HURT_COLOR);
                            }
                        }
                        tex.upload();
                        lastAppliedColor = VANILLA_HURT_COLOR;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
