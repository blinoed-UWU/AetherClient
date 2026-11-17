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

public final class ArmorHud extends HudElement {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND
    };
    private static final String[] SLOT_NAMES = {"Шлем", "Грудь", "Поножи", "Ботинки", "Рука", "Левая"};
    private static final String[] SLOT_NAMES_EN = {"Helmet", "Chest", "Legs", "Boots", "Main", "Offhand"};

    private final HudModule hudModule;

    public ArmorHud(HudModule hudModule) {
        super("armor", "Броня и руки", 0.985f, 0.72f);
        setTitleEn("Armor & Hands");
        this.hudModule = hudModule;
        this.width = 138f;
        this.height = 78f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.armor.isEnabled();
    }

    private static int countEquipped(MinecraftClient mc) {
        int count = 0;
        if (mc.player != null) {
            for (EquipmentSlot slot : SLOTS) {
                if (!mc.player.getEquippedStack(slot).isEmpty()) count++;
            }
        }
        return count;
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        int count = countEquipped(mc);
        int visibleRows = count > 0 ? count : 4;
        this.width = 142f;
        this.height = 11f + visibleRows * 17f;
        this.elementScale = hudModule.scaleArmor.asFloat();
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        long vg = engine.handle();
        int count = countEquipped(mc);
        int visibleRows = (editMode && count == 0) ? 4 : count;
        if (visibleRows == 0) {
            this.width = 142f;
            this.height = 20f;
            return;
        }

        float rowH = 17f;
        this.width = 142f;
        this.height = 11f + visibleRows * rowH;

        Render2D.drawLiquidGlassPanel(
                engine, x, y, width, height, 8.5f, screenW, screenH, palette,
                hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
        );

        float cy = y + 7f;
        if (editMode && count == 0) {
            for (int i = 0; i < 4; i++) {
                drawSlotRow(vg, palette, x + 8.5f, cy, width - 17f, I18n.tr(SLOT_NAMES[i], SLOT_NAMES_EN[i]), 0.85f - i * 0.15f, "85%");
                cy += rowH;
            }
            return;
        }

        if (mc.player == null) return;
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = mc.player.getEquippedStack(SLOTS[i]);
            if (stack.isEmpty()) continue;

            float ratio = 1f;
            String statText;
            if (stack.isDamageable() && stack.getMaxDamage() > 0) {
                int rem = Math.max(0, stack.getMaxDamage() - stack.getDamage());
                ratio = Math.max(0f, Math.min(1f, rem / (float) stack.getMaxDamage()));
                statText = Math.round(ratio * 100f) + "%";
            } else {
                statText = stack.getCount() + I18n.tr(" шт", "x");
            }
            drawSlotRow(vg, palette, x + 8.5f, cy, width - 17f, I18n.tr(SLOT_NAMES[i], SLOT_NAMES_EN[i]), ratio, statText);
            cy += rowH;
        }
    }

    private void drawSlotRow(long vg, AetherPalette palette, float x, float y, float w, String label, float ratio, String stat) {
        Render2D.drawText(vg, label, x, y + 6.5f, 9.8f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        Render2D.drawText(vg, stat, x + w, y + 6.5f, 9.6f, palette.onSurfaceVariant, NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);

        float barY = y + 12.5f;
        Render2D.drawRoundedRect(vg, x, barY, w, 2.8f, 1.4f, AetherPalette.withAlpha(palette.onSurface, 36));
        int fillColor = ratio < 0.25f ? 0xFFFF6B6B : (ratio < 0.55f ? palette.tertiary : palette.primary);
        Render2D.drawRoundedRect(vg, x, barY, Math.max(2.8f, w * ratio), 2.8f, 1.4f, fillColor);
    }
}
