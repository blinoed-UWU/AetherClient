package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.nanovg.NanoVG;

import java.lang.reflect.Method;

public final class CompassHud extends HudElement {
    private static final String[] DIRS_RU = {"С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"};
    private static final String[] DIRS_EN = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private final HudModule hudModule;

    public CompassHud(HudModule hudModule) {
        super("compass", "Компас", 0.5f, 0.03f);
        setTitleEn("Compass");
        this.hudModule = hudModule;
        this.width = 216f;
        this.height = 34f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.compass.isEnabled();
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        this.width = 216f;
        this.height = 34f;
        this.elementScale = hudModule.scaleCompass.asFloat();
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        this.width = 216f;
        this.height = 34f;
        long vg = engine.handle();

        Render2D.drawLiquidGlassPanel(
                engine, x, y, width, height, 10f, screenW, screenH, palette,
                hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
        );

        float yaw = mc.player != null ? mc.player.getYaw() : 0f;
        float cx = x + width * 0.5f;
        float cy = y + 15f;

        // Rotating direction tape
        float half = width * 0.5f - 12f;
        for (int i = 0; i < 8; i++) {
            float dir = i * 45f;
            float off = normalizeAngle(dir - yaw);
            if (off < -88f || off > 88f) continue;
            float px = cx + off / 88f * half;
            boolean card = (i % 2 == 0);
            String label = I18n.tr(DIRS_RU[i], DIRS_EN[i]);
            Render2D.drawTextBold(vg, label, px, cy + 0.4f, card ? 11.2f : 9.2f,
                    card ? palette.primary : palette.onSurfaceVariant,
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            Render2D.drawRoundedRect(vg, px - 0.5f, y + (card ? 22.5f : 24.5f), 1f, 3f, 0.5f,
                    card ? palette.primary : AetherPalette.withAlpha(palette.onSurfaceVariant, 160));
        }

        // Center needle
        Render2D.drawRoundedRect(vg, cx - 0.6f, y + 8f, 1.2f, 18f, 0.6f, palette.onSurface);

        // Biome / dimension caption
        String caption = biomeLabel(mc);
        if (caption.isEmpty()) caption = dimensionLabel(mc);
        if (!caption.isEmpty()) {
            Render2D.drawTextBold(vg, caption, cx, y + height + 8f, 9.4f,
                    palette.onSurface, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        }
    }

    private static String dimensionLabel(MinecraftClient mc) {
        if (mc.world == null) return "";
        String path = mc.world.getRegistryKey().getValue().getPath();
        return switch (path) {
            case "the_nether" -> I18n.tr("Незер", "Nether");
            case "the_end" -> I18n.tr("Энд", "End");
            default -> I18n.tr("Мир", "Overworld");
        };
    }

    private static String biomeLabel(MinecraftClient mc) {
        try {
            if (mc.world == null || mc.player == null) return "";
            BlockPos pos = mc.player.getBlockPos();
            Object entry = mc.world.getBiome(pos);
            if (entry == null) return "";
            Method getKey = entry.getClass().getMethod("getKey");
            Object opt = getKey.invoke(entry);
            Object key = opt.getClass().getMethod("get").invoke(opt);
            Object id = key.getClass().getMethod("getValue").invoke(key);
            String path = (String) id.getClass().getMethod("getPath").invoke(id);
            if (path == null || path.isEmpty()) return "";
            return path.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + path.substring(1);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static float normalizeAngle(float deg) {
        float d = deg % 360f;
        if (d > 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }
}
