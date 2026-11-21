package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CooldownsHud extends HudElement {
    private final HudModule hudModule;

    private static final record TrackedCooldown(Item item, String nameRu, String nameEn, int color) {}

    private static final List<TrackedCooldown> TRACKED = List.of(
            new TrackedCooldown(Items.ENDER_PEARL, "Жемчуг", "Pearl", 0xFF38BDF8),
            new TrackedCooldown(Items.GOLDEN_APPLE, "Яблоко", "GApple", 0xFFFBBF24),
            new TrackedCooldown(Items.ENCHANTED_GOLDEN_APPLE, "Чарка", "Ench GApple", 0xFFFF00FF),
            new TrackedCooldown(Items.CHORUS_FRUIT, "Хорус", "Chorus", 0xFFC084FC),
            new TrackedCooldown(Items.WIND_CHARGE, "Ветер", "Wind Charge", 0xFFE0E7FF),
            new TrackedCooldown(Items.SHIELD, "Щит", "Shield", 0xFF94A3B8),
            new TrackedCooldown(Items.MACE, "Булава", "Mace", 0xFFF97316)
    );

    private final List<ActiveCd> activeList = new ArrayList<>();

    private static final class ActiveCd {
        String label;
        float progress;
        int color;
    }

    public CooldownsHud(HudModule hudModule) {
        super("cooldowns", "Кулдауны предметов", 0.5f, 0.88f);
        setTitleEn("Item Cooldowns");
        this.hudModule = hudModule;
        this.width = 160f;
        this.height = 32f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.cooldowns.isEnabled();
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        this.elementScale = hudModule.scaleCooldowns.asFloat();
        activeList.clear();
        if (mc.player != null) {
            var itemCooldowns = mc.player.getItemCooldownManager();
            for (var t : TRACKED) {
                float cd = itemCooldowns.getCooldownProgress(t.item().getDefaultStack(), 0f);
                if (cd > 0.001f) {
                    ActiveCd a = new ActiveCd();
                    a.label = I18n.tr(t.nameRu(), t.nameEn());
                    a.progress = cd;
                    a.color = t.color();
                    activeList.add(a);
                }
            }
        }
        int count = Math.max(1, activeList.size());
        this.width = Math.max(140f, count * 52f + 16f);
        this.height = 36f;
    }

    @Override
    public void render(MinecraftClient mc, NanoVgEngine engine, AetherPalette palette, float x, float y, float screenW, float screenH, boolean editMode, float dt) {
        long vg = engine.handle();
        Render2D.drawLiquidGlassPanel(engine, x, y, width, height, 8.5f, screenW, screenH, palette, hudModule.glassOpacity.asFloat(), -999f, -999f, 0f);

        if (activeList.isEmpty()) {
            if (editMode) {
                Render2D.drawText(vg, I18n.tr("Кулдауны (нет активных)", "Cooldowns (none active)"),
                        x + width * 0.5f, y + height * 0.5f, 10f, palette.onSurfaceVariant,
                        NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            }
            return;
        }

        float curX = x + 10f;
        float centerY = y + height * 0.5f;
        for (ActiveCd cd : activeList) {
            float rad = 11f;
            float cx = curX + rad;
            float cy = centerY;

            // Background ring
            Render2D.drawCircleOutline(vg, cx, cy, rad, 2.5f, AetherPalette.withAlpha(palette.onSurface, 35));

            // Circular sweep progress
            float sweepAngle = cd.progress * 6.2831855f;
            if (sweepAngle > 0.05f) {
                Render2D.drawCircleOutline(vg, cx, cy, rad, 2.5f, cd.color);
            }

            // Percentage / label
            String pct = String.format(Locale.ROOT, "%.0f%%", cd.progress * 100f);
            Render2D.drawTextBold(vg, pct, cx, cy, 7.5f, palette.onSurface,
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            Render2D.drawText(vg, cd.label, cx + rad + 6f, cy, 9.2f, palette.onSurface,
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

            curX += rad * 2f + Render2D.measureTextBold(vg, cd.label, 9.2f) + 16f;
        }
    }
}
