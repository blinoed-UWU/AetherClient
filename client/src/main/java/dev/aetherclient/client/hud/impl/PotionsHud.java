package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;
import java.util.List;

public final class PotionsHud extends HudElement {
    private final HudModule hudModule;
    private final List<String[]> rows = new ArrayList<>();

    public PotionsHud(HudModule hudModule) {
        super("potions", "Эффекты зелий", 0.014f, 0.66f);
        setTitleEn("Potions");
        this.hudModule = hudModule;
        this.width = 148f;
        this.height = 54f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.potions.isEnabled();
    }

    private void collectRows(MinecraftClient mc, boolean editMode) {
        rows.clear();
        if (mc.player != null) {
            for (StatusEffectInstance inst : mc.player.getStatusEffects()) {
                String name = inst.getEffectType().value().getName().getString();
                if (inst.getAmplifier() > 0) {
                    name = name + " " + (inst.getAmplifier() + 1);
                }
                int sec = Math.max(0, inst.getDuration() / 20);
                String time = String.format("%d:%02d", sec / 60, sec % 60);
                rows.add(new String[]{name, time});
                if (rows.size() >= 6) break;
            }
        }
        if (rows.isEmpty() && editMode) {
            rows.add(new String[]{I18n.tr("Скорость 2", "Speed 2"), "1:24"});
            rows.add(new String[]{I18n.tr("Сила 1", "Strength"), "0:48"});
        }
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        this.elementScale = hudModule.scalePotions.asFloat();
        collectRows(mc, true);
        long vg = engine.handle();
        float maxW = 112f;
        for (String[] row : rows) {
            float rw = Render2D.measureTextBold(vg, row[0], 10f) + Render2D.measureText(vg, row[1], 9.6f) + 28f;
            if (rw > maxW) maxW = rw;
        }
        this.width = Math.min(200f, maxW);
        this.height = Math.max(24f, 12f + rows.size() * 17f);
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        collectRows(mc, editMode);
        if (rows.isEmpty()) return;

        long vg = engine.handle();
        float maxW = 112f;
        for (String[] row : rows) {
            float rw = Render2D.measureTextBold(vg, row[0], 10f) + Render2D.measureText(vg, row[1], 9.6f) + 28f;
            if (rw > maxW) maxW = rw;
        }
        this.width = Math.min(200f, maxW);
        this.height = 12f + rows.size() * 17f;

        Render2D.drawLiquidGlassPanel(
                engine, x, y, width, height, 9.5f, screenW, screenH, palette,
                hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
        );

        float cy = y + 14.5f;
        for (String[] row : rows) {
            Render2D.drawCircle(vg, x + 10f, cy, 2.4f, palette.primary);
            Render2D.drawTextBold(vg, row[0], x + 17f, cy, 10f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            Render2D.drawText(vg, row[1], x + width - 9f, cy, 9.6f, palette.tertiary, NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);
            cy += 17f;
        }
    }
}
