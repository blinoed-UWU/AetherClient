package dev.aetherclient.client.theme;

import java.awt.Color;

/**
 * Computes Material You tonal palette colors from a single seed color and dark/light flag.
 * Mirrors the Material You aesthetic of AetherLoader.
 */
public final class AetherPalette {
    public int seedArgb;
    public boolean darkMode;

    public int primary;
    public int onPrimary;
    public int primaryContainer;
    public int onPrimaryContainer;
    public int secondaryContainer;
    public int surface;
    public int surfaceContainer;
    public int surfaceContainerHigh;
    public int surfaceBright;
    public int tertiary;
    public int onSurface;
    public int onSurfaceVariant;
    public int outline;

    public AetherPalette(int seedArgb, boolean darkMode) {
        update(seedArgb, darkMode);
    }

    public void update(int seedArgb, boolean darkMode) {
        this.seedArgb = seedArgb;
        this.darkMode = darkMode;

        float[] hsb = Color.RGBtoHSB(
                (seedArgb >> 16) & 0xFF,
                (seedArgb >> 8) & 0xFF,
                seedArgb & 0xFF,
                null
        );
        float h = hsb[0];
        float s = Math.max(0.28f, hsb[1]);
        float tertiaryHue = (h + 0.14f) % 1.0f;

        if (darkMode) {
            primary = hsbArgb(h, Math.min(0.68f, s), 0.96f, 255);
            onPrimary = hsbArgb(h, s, 0.22f, 255);
            primaryContainer = hsbArgb(h, Math.min(0.75f, s), 0.44f, 255);
            onPrimaryContainer = hsbArgb(h, 0.30f, 0.96f, 255);
            secondaryContainer = hsbArgb((h + 0.04f) % 1.0f, 0.32f, 0.28f, 225);
            tertiary = hsbArgb(tertiaryHue, 0.58f, 0.92f, 255);

            surface = hsbArgb(h, 0.18f, 0.09f, 255);
            surfaceContainer = hsbArgb(h, 0.22f, 0.13f, 196);
            surfaceContainerHigh = hsbArgb(h, 0.24f, 0.18f, 218);
            surfaceBright = hsbArgb(h, 0.22f, 0.24f, 230);

            onSurface = 0xFFF3F2F8;
            onSurfaceVariant = 0xFFB8B6C8;
            outline = 0x44FFFFFF;
        } else {
            primary = hsbArgb(h, Math.min(0.82f, s + 0.1f), 0.72f, 255);
            onPrimary = 0xFFFFFFFF;
            primaryContainer = hsbArgb(h, 0.35f, 0.95f, 255);
            onPrimaryContainer = hsbArgb(h, 0.75f, 0.24f, 255);
            secondaryContainer = hsbArgb((h + 0.04f) % 1.0f, 0.22f, 0.90f, 232);
            tertiary = hsbArgb(tertiaryHue, 0.65f, 0.68f, 255);

            surface = hsbArgb(h, 0.08f, 0.96f, 255);
            surfaceContainer = hsbArgb(h, 0.10f, 0.94f, 208);
            surfaceContainerHigh = hsbArgb(h, 0.12f, 0.90f, 224);
            surfaceBright = hsbArgb(h, 0.06f, 0.98f, 236);

            onSurface = 0xFF171620;
            onSurfaceVariant = 0xFF565466;
            outline = 0x38000000;
        }
    }

    public static int withAlpha(int argb, int alpha0to255) {
        int a = Math.max(0, Math.min(255, alpha0to255));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * Resolves an animated accent color from a palette mode string.
     * Modes: "Статика" (static base color), "Радуга" (rainbow sweep),
     * "Тема" (theme primary), "Градиент" (theme primary/tertiary shift).
     */
    public int resolvePalette(String mode, int baseArgb, float time, float speed) {
        if (mode == null) return baseArgb;
        switch (mode) {
            case "Радуга" -> {
                return rainbow(time * speed);
            }
            case "Тема" -> {
                return primary;
            }
            case "Градиент" -> {
                return lerpArgb(primary, tertiary, 0.5f + 0.5f * (float) Math.sin(time * speed));
            }
            default -> {
                return baseArgb;
            }
        }
    }

    public static int rainbow(float t) {
        float hue = ((t % 1f) + 1f) % 1f;
        int rgb = Color.HSBtoRGB(hue, 0.82f, 1.0f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    public static int lerpArgb(int c1, int c2, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * clamped);
        int r = Math.round(r1 + (r2 - r1) * clamped);
        int g = Math.round(g1 + (g2 - g1) * clamped);
        int b = Math.round(b1 + (b2 - b1) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int hsbArgb(float h, float s, float b, int alpha) {
        int rgb = Color.HSBtoRGB(h, Math.max(0f, Math.min(1f, s)), Math.max(0f, Math.min(1f, b)));
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }
}
