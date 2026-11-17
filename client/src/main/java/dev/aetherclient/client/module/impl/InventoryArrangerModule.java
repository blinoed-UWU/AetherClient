package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.audio.SoundEngine;
import dev.aetherclient.client.notify.ToastManager;
import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.KeybindSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
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
    public final KeybindSetting arrangeKey = addSetting(new KeybindSetting("key", "Клавиша раскладки", GLFW.GLFW_KEY_V).withEn("Arrange Key"));
    public final ModeSetting preset = addSetting(new ModeSetting("preset", "Шаблон", "Анархия PvP",
            "Анархия PvP", "Кристаллы", "Грифер", "Пользовательский").withEn("Preset"));
    public final BooleanSetting arrangeOffhand = addSetting(new BooleanSetting("offhand", "Вторая рука (Тотем)", true).withEn("Arrange Offhand"));
    public final BooleanSetting autoArmor = addSetting(new BooleanSetting("auto_armor", "Надевать лучшую броню", true).withEn("Auto Armor"));
    public final BooleanSetting cleanTrash = addSetting(new BooleanSetting("clean_trash", "Очищать мусор из хотбара", false).withEn("Clean Trash"));
    public final NumberSetting slotDelay = addSetting(new NumberSetting("delay", "Задержка кликов (мс)", 0.0, 0.0, 100.0, 10.0).withEn("Click Delay"));
    public final BooleanSetting playSound = addSetting(new BooleanSetting("sound", "Звуковой эффект", true).withEn("Play Sound"));
    public final BooleanSetting showToast = addSetting(new BooleanSetting("toast", "Уведомления", true).withEn("Show Toast"));

    private final Map<Integer, Item> customPreset = new HashMap<>();

    public InventoryArrangerModule() {
        super("inv_arranger", "Раскладка инвентаря", "По нажатию одной кнопки мгновенно раскладывает хотбар, тотем во 2-й руке и броню по шаблонам (Анархия, Кристаллы, Грифер)", Category.COMBAT);
        setEn("Inventory Arranger", "Instantly organizes hotbar, offhand totem and armor according to presets (Anarchy, Crystals, Griefer)");
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
                        waitDelay();
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
                        waitDelay();
                    }
                }
            }
        }

        // Auto Armor
        if (autoArmor.isEnabled()) {
            movedCount += equipBestArmor(mc, inv);
        }

        // Clean trash from hotbar if enabled
        if (cleanTrash.isEnabled()) {
            for (int i = 0; i < 9; i++) {
                if (!targetLayout.containsKey(i)) {
                    ItemStack st = inv.getStack(i);
                    if (!st.isEmpty() && isTrashItem(st.getItem())) {
                        int emptySlot = findEmptyStorageSlot(inv);
                        if (emptySlot != -1) {
                            int screenFrom = 36 + i;
                            mc.interactionManager.clickSlot(0, screenFrom, 0, SlotActionType.QUICK_MOVE, mc.player);
                            movedCount++;
                            waitDelay();
                        }
                    }
                }
            }
        }

        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
        }

        if (playSound.isEnabled()) {
            SoundEngine.get().playClick(mc, true, 1.2f);
        }
        if (showToast.isEnabled()) {
            if (movedCount > 0) {
                ToastManager.get().push("Раскладка", "Разложено предметов: " + movedCount, 0xFF10B981);
            } else {
                ToastManager.get().push("Раскладка", "Инвентарь уже разложен идеально", 0xFF38BDF8);
            }
        }
    }

    private void waitDelay() {
        long d = (long) slotDelay.asFloat();
        if (d > 0) {
            try {
                Thread.sleep(d);
            } catch (InterruptedException ignored) {}
        }
    }

    private int equipBestArmor(MinecraftClient mc, PlayerInventory inv) {
        // Armor slots in PlayerScreenHandler: 5: Helm, 6: Chest, 7: Legs, 8: Boots
        Item[] bestHelms = { Items.NETHERITE_HELMET, Items.DIAMOND_HELMET, Items.IRON_HELMET };
        Item[] bestChests = { Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.IRON_CHESTPLATE };
        Item[] bestLegs = { Items.NETHERITE_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.IRON_LEGGINGS };
        Item[] bestBoots = { Items.NETHERITE_BOOTS, Items.DIAMOND_BOOTS, Items.IRON_BOOTS };

        int equipped = 0;
        equipped += equipPiece(mc, inv, 5, 39, bestHelms);
        equipped += equipPiece(mc, inv, 6, 38, bestChests);
        equipped += equipPiece(mc, inv, 7, 37, bestLegs);
        equipped += equipPiece(mc, inv, 8, 36, bestBoots);
        return equipped;
    }

    private int equipPiece(MinecraftClient mc, PlayerInventory inv, int containerSlot, int invArmorSlot, Item[] priorities) {
        ItemStack current = inv.getStack(invArmorSlot);
        if (!current.isEmpty()) return 0;
        for (Item p : priorities) {
            int found = findItemSlot(inv, p);
            if (found != -1) {
                int screenSlot = found < 9 ? 36 + found : found;
                mc.interactionManager.clickSlot(0, screenSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
                waitDelay();
                return 1;
            }
        }
        return 0;
    }

    private boolean isTrashItem(Item item) {
        return item == Items.DIRT || item == Items.COBBLESTONE || item == Items.GRAVEL ||
                item == Items.ANDESITE || item == Items.DIORITE || item == Items.GRANITE ||
                item == Items.ROTTEN_FLESH || item == Items.WHEAT_SEEDS || item == Items.POISONOUS_POTATO;
    }

    private int findEmptyStorageSlot(PlayerInventory inv) {
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    private Map<Integer, Item> resolveTargetLayout() {
        Map<Integer, Item> layout = new HashMap<>();
        String currentPreset = preset.getValue();
        if ("Пользовательский".equals(currentPreset) && !customPreset.isEmpty()) {
            return customPreset;
        }

        if ("Кристаллы".equals(currentPreset)) {
            layout.put(0, Items.NETHERITE_SWORD);
            layout.put(1, Items.END_CRYSTAL);
            layout.put(2, Items.RESPAWN_ANCHOR);
            layout.put(3, Items.GLOWSTONE);
            layout.put(4, Items.OBSIDIAN);
            layout.put(5, Items.ENCHANTED_GOLDEN_APPLE);
            layout.put(6, Items.ENDER_PEARL);
            layout.put(7, Items.TOTEM_OF_UNDYING);
            layout.put(8, Items.TOTEM_OF_UNDYING);
            layout.put(40, Items.TOTEM_OF_UNDYING);
        } else if ("Грифер".equals(currentPreset)) {
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
