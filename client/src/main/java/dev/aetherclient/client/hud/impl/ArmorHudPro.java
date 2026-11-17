package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.lwjgl.nanovg.NanoVG;

public final class ArmorHudPro extends HudElement {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final String[] NAMES_RU = {"Шлем", "Грудь", "Поножи", "Ботинки"};
    private static final String[] NAMES_EN = {"Helmet", "Chest", "Legs", "Boots"};

    private final HudModule hudModule;

    public ArmorHudPro(HudModule hudModule) {
        super("armorpro", "Броня Pro", 0.985f, 0.6f);
        setTitleEn("Armor Pro");
        this.hudModule = hudModule;
        this.width = 150f;
        this.height = 88f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.armorPro.isEnabled();
    }

    private static int equippedCount(MinecraftClient mc) {
        int c = 0;
        if (mc.player != null) {
            for (EquipmentSlot slot : SLOTS) {
                if (!mc.player.getEquippedStack(slot).isEmpty()) c++;
            }
        }
        return c;
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        int rows = Math.max(4, equippedCount(mc));
        this.width = 150f;
        this.height = 12f + rows * 19f;
        this.elementScale = hudModule.scaleArmorPro.asFloat();
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        long vg = engine.handle();
        int count = equippedCount(mc);
        int rows = (editMode && count == 0) ? 4 : count;
        if (rows == 0) {
            this.width = 150f;
            this.height = 20f;
            return;
        }
        this.width = 150f;
        this.height = 12f + rows * 19f;

        Render2D.drawLiquidGlassPanel(
                engine, x, y, width, height, 9f, screenW, screenH, palette,
                hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
        );

        float cy = y + 10f;
        if (editMode && count == 0) {
            for (int i = 0; i < 4; i++) {
                drawRow(vg, palette, x + 9f, cy, width - 18f,
                        I18n.tr(NAMES_RU[i], NAMES_EN[i]), 0.82f, 245);
                cy += 19f;
            }
            return;
        }

        if (mc.player == null) return;
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = mc.player.getEquippedStack(SLOTS[i]);
            if (stack.isEmpty()) continue;
            float ratio = 1f;
            int hits = -1;
            if (stack.isDamageable() && stack.getMaxDamage() > 0) {
                int rem = Math.max(0, stack.getMaxDamage() - stack.getDamage());
                ratio = (float) rem / stack.getMaxDamage();
                hits = rem;
            }
            drawRow(vg, palette, x + 9f, cy, width - 18f,
                    I18n.tr(NAMES_RU[i], NAMES_EN[i]), ratio, hits);
            cy += 19f;
        }
    }

    private void drawRow(long vg, AetherPalette palette, float x, float y, float w, String label, float ratio, int hits) {
        Render2D.drawText(vg, label, x, y + 6f, 9.6f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        String right;
        if (hits >= 0) {
            right = Math.round(ratio * 100f) + "%  •  " + hits + I18n.tr(" уд.", " hits");
        } else {
            right = "100%";
        }
        Render2D.drawText(vg, right, x + w, y + 6f, 9.4f, palette.onSurfaceVariant,
                NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);

        float barY = y + 12f;
        Render2D.drawRoundedRect(vg, x, barY, w, 3.2f, 1.6f, AetherPalette.withAlpha(palette.onSurface, 36));
        int fillCol = ratio < 0.25f ? 0xFFFF6B6B : (ratio < 0.55f ? palette.tertiary : palette.primary);
        Render2D.drawRoundedRect(vg, x, barY, Math.max(3.2f, w * ratio), 3.2f, 1.6f, fillCol);
    }
}
