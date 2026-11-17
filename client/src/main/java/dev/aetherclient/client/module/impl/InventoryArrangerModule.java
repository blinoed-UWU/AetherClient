package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.audio.SoundEngine;
import dev.aetherclient.client.notify.ToastManager;
import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.KeybindSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inventory Arranger ("Раскладка инвентаря").
 * Automatically arranges hotbar and offhand items according to a saved layout preset or PvP template.
 */
public final class InventoryArrangerModule extends Module {
    public final KeybindSetting arrangeKey = new KeybindSetting("key", "Клавиша раскладки", GLFW.GLFW_KEY_V);
    public final ModeSetting preset = new ModeSetting("preset", "Шаблон", "Анархия PvP",
            "Анархия PvP", "Грифер", "Пользовательский");
    public final BooleanSetting arrangeOffhand = new BooleanSetting("offhand", "Раскладывать вторую руку", true);

    private final Map<Integer, Item> customPreset = new HashMap<>();

    public InventoryArrangerModule() {
        super("inv_arranger", "Раскладка инвентаря", "По нажатию одной кнопки раскладывает предметы из инвентаря в нужные слоты хотбара", Category.COMBAT);
        addSetting(arrangeKey);
        addSetting(preset);
        addSetting(arrangeOffhand);
        setEnabled(true);
    }

    public boolean onKey(int key, int action) {
        if (!isEnabled()) return false;
        if (action == GLFW.GLFW_PRESS && key == arrangeKey.getValue() && key != -1) {
            executeArrange();
            return true;
        }
        return false;
    }

    public void saveCurrentLayout() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        PlayerInventory inv = mc.player.getInventory();
        customPreset.clear();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                customPreset.put(i, stack.getItem());
            }
        }
        ItemStack offhand = mc.player.getOffHandStack();
        if (!offhand.isEmpty()) {
            customPreset.put(40, offhand.getItem());
        }
        ToastManager.get().push("Раскладка", "Текущий шаблон инвентаря сохранен", 0xFF10B981);
    }

    public void executeArrange() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.interactionManager == null) return;

        PlayerInventory inv = mc.player.getInventory();
        mc.player.setSprinting(false);

        Map<Integer, Item> targetLayout = resolveTargetLayout();
        int movedCount = 0;

        for (Map.Entry<Integer, Item> entry : targetLayout.entrySet()) {
            int slot = entry.getKey();
            Item desiredItem = entry.getValue();
            if (desiredItem == null || desiredItem == Items.AIR) continue;

            if (slot == 40) {
                // Offhand
                if (arrangeOffhand.isEnabled() && mc.player.getOffHandStack().getItem() != desiredItem) {
                    int sourceSlot = findItemSlot(inv, desiredItem);
                    if (sourceSlot != -1) {
                        int screenSlot = sourceSlot < 9 ? 36 + sourceSlot : sourceSlot;
                        mc.interactionManager.clickSlot(0, screenSlot, 40, SlotActionType.SWAP, mc.player);
                        movedCount++;
                    }
                }
            } else if (slot >= 0 && slot < 9) {
                ItemStack current = inv.getStack(slot);
                if (current.getItem() != desiredItem) {
                    int sourceSlot = findItemSlot(inv, desiredItem);
                    if (sourceSlot != -1 && sourceSlot != slot) {
                        int screenSlot = sourceSlot < 9 ? 36 + sourceSlot : sourceSlot;
                        mc.interactionManager.clickSlot(0, screenSlot, slot, SlotActionType.SWAP, mc.player);
                        movedCount++;
                    }
                }
            }
        }

        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
        }

        SoundEngine.get().playClick(mc, true, 1.2f);
        if (movedCount > 0) {
            ToastManager.get().push("Раскладка", "Разложено предметов: " + movedCount, 0xFF10B981);
        } else {
            ToastManager.get().push("Раскладка", "Инвентарь уже разложен идеально", 0xFF38BDF8);
        }
    }

    private Map<Integer, Item> resolveTargetLayout() {
        Map<Integer, Item> layout = new HashMap<>();
        String currentPreset = preset.getValue();
        if ("Пользовательский".equals(currentPreset) && !customPreset.isEmpty()) {
            return customPreset;
        }

        if ("Грифер".equals(currentPreset)) {
            layout.put(0, Items.NETHERITE_SWORD);
            layout.put(1, Items.TOTEM_OF_UNDYING);
            layout.put(2, Items.ENDER_PEARL);
            layout.put(3, Items.ENCHANTED_GOLDEN_APPLE);
            layout.put(4, Items.WIND_CHARGE);
            layout.put(5, Items.SPLASH_POTION);
            layout.put(6, Items.TOTEM_OF_UNDYING);
            layout.put(7, Items.TOTEM_OF_UNDYING);
            layout.put(8, Items.NETHERITE_PICKAXE);
            layout.put(40, Items.TOTEM_OF_UNDYING);
        } else {
            // "Анархия PvP"
            layout.put(0, Items.NETHERITE_SWORD);
            layout.put(1, Items.TOTEM_OF_UNDYING);
            layout.put(2, Items.MACE);
            layout.put(3, Items.ENDER_PEARL);
            layout.put(4, Items.WIND_CHARGE);
            layout.put(5, Items.GOLDEN_APPLE);
            layout.put(6, Items.TOTEM_OF_UNDYING);
            layout.put(7, Items.TOTEM_OF_UNDYING);
            layout.put(8, Items.TOTEM_OF_UNDYING);
            layout.put(40, Items.TOTEM_OF_UNDYING);
        }
        return layout;
    }

    private int findItemSlot(PlayerInventory inv, Item item) {
        // Search main inventory first (9..35)
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).getItem() == item) return i;
        }
        // Fallback search hotbar (0..8)
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).getItem() == item) return i;
        }
        return -1;
    }
}
