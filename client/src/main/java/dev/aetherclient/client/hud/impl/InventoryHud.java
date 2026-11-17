package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.lwjgl.nanovg.NanoVG;

public final class InventoryHud extends HudElement {
    private static final int HOTBAR = 9;

    private final HudModule hudModule;

    public InventoryHud(HudModule hudModule) {
        super("inventory", "Инвентарь", 0.5f, 0.72f);
        setTitleEn("Inventory");
        this.hudModule = hudModule;
        this.width = 208f;
        this.height = 44f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.inventory.isEnabled();
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        this.width = 208f;
        this.height = 44f;
        this.elementScale = hudModule.scaleInventory.asFloat();
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        this.width = 208f;
        this.height = 44f;
        long vg = engine.handle();

        Render2D.drawLiquidGlassPanel(
                engine, x, y, width, height, 9f, screenW, screenH, palette,
                hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
        );

        ItemStack mainHand = mc.player != null ? mc.player.getMainHandStack() : ItemStack.EMPTY;
        float slot = 19f;
        float gap = (width - 16f - slot * HOTBAR) / (HOTBAR - 1);

        for (int i = 0; i < HOTBAR; i++) {
            float sx = x + 8f + i * (slot + gap);
            float sy = y + 8f;
            ItemStack stack = mc.player != null ? mc.player.getInventory().getStack(i) : ItemStack.EMPTY;

            boolean sel = stack == mainHand;
            int bg = sel ? AetherPalette.withAlpha(palette.primary, 210) : AetherPalette.withAlpha(palette.onSurface, 26);
            Render2D.drawRoundedRect(vg, sx, sy, slot, slot, 5f, bg);
            if (sel) {
                Render2D.drawRoundedOutline(vg, sx, sy, slot, slot, 5f, 1.4f, palette.onPrimary);
            }

            if (!stack.isEmpty()) {
                String abbr = abbreviate(stack.getName().getString());
                float fontSize = abbr.length() > 2 ? 8.5f : 10.5f;
                Render2D.drawTextBold(vg, abbr, sx + slot * 0.5f, sy + slot * 0.5f - 2f, fontSize,
                        sel ? palette.onPrimary : palette.onSurface, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
                if (stack.getCount() > 1) {
                    Render2D.drawText(vg, Integer.toString(stack.getCount()), sx + slot - 2.5f, sy + slot - 4f, 8.4f,
                            palette.onSurfaceVariant, NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);
                }
                if (stack.isDamageable() && stack.getMaxDamage() > 0) {
                    float ratio = 1f - (float) stack.getDamage() / stack.getMaxDamage();
                    int fillCol = ratio < 0.25f ? 0xFFFF6B6B : (ratio < 0.55f ? palette.tertiary : palette.primary);
                    Render2D.drawRoundedRect(vg, sx + 2f, sy + slot - 3f, slot - 4f, 2f, 1f,
                            AetherPalette.withAlpha(palette.onSurface, 90));
                    Render2D.drawRoundedRect(vg, sx + 2f, sy + slot - 3f, (slot - 4f) * ratio, 2f, 1f, fillCol);
                }
            }
        }
    }

    private static String abbreviate(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() >= 3) break;
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }
}
