package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.module.impl.NameProtectModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.nanovg.NanoVG;

import java.util.Locale;

public final class StatsHud extends HudElement {
    private static final String[] DIRS_RU = {"Ю", "ЮЗ", "З", "СЗ", "С", "СВ", "В", "ЮВ"};
    private static final String[] DIRS_EN = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    private final HudModule hudModule;
    private final NameProtectModule nameProtect;
    private final long sessionStartMs = System.currentTimeMillis();

    public StatsHud(HudModule hudModule, NameProtectModule nameProtect) {
        super("stats", "Статус и скорость", 0.5f, 0.02f);
        setTitleEn("Speed & Stats");
        this.hudModule = hudModule;
        this.nameProtect = nameProtect;
        this.width = 164f;
        this.height = 26f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.stats.isEnabled();
    }

    private String buildText(MinecraftClient mc) {
        double bps = 0.0;
        String dir = "N";
        if (mc.player != null) {
            double dx = mc.player.getX() - mc.player.lastRenderX;
            double dz = mc.player.getZ() - mc.player.lastRenderZ;
            bps = Math.hypot(dx, dz) * 20.0;
            float yaw = ((mc.player.getYaw() % 360f) + 360f) % 360f;
            int idx = Math.round(yaw / 45f) % 8;
            dir = I18n.tr(DIRS_RU[idx], DIRS_EN[idx]);
        }
        long sec = Math.max(0L, (System.currentTimeMillis() - sessionStartMs) / 1000L);
        String time = String.format("%02d:%02d", sec / 60L, sec % 60L);
        String user = mc.player != null ? nameProtect.mask(mc.player.getName().getString()) : "Player";
        return String.format(Locale.ROOT, "%s  •  %.1f BPS  •  %s  •  %s", user, bps, dir, time);
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        this.elementScale = hudModule.scaleStats.asFloat();
        String text = buildText(mc);
        this.width = Math.max(120f, Render2D.measureTextBold(engine.handle(), text, 10.0f) + 20f);
        this.height = 26f;
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        String text = buildText(mc);
        long vg = engine.handle();
        this.width = Math.max(120f, Render2D.measureTextBold(vg, text, 10.0f) + 20f);
        this.height = 26f;

        Render2D.drawLiquidGlassPanel(
                engine, x, y, width, height, 10f, screenW, screenH, palette,
                hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
        );
        Render2D.drawTextBold(vg, text, x + width * 0.5f, y + height * 0.5f + 0.3f, 10.0f,
                palette.onSurface, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
    }
}
