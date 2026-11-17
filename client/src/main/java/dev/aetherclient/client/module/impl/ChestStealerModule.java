package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.audio.SoundEngine;
import dev.aetherclient.client.notify.ToastManager;
import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Random;

/**
 * Fast Chest Stealer / Quick Container Looter.
 * Rapidly loots valuable items from chests, barrels, and shulkers into player inventory
 * with safe humanized delays, auto-close conditions, and trash filtering.
 */
public final class ChestStealerModule extends Module {
    public final NumberSetting delayMs = addSetting(new NumberSetting("delay", "Базовая задержка (мс)", 35.0, 5.0, 200.0, 5.0).withEn("Loot Delay"));
    public final NumberSetting randomDelay = addSetting(new NumberSetting("random_delay", "Рандомизация (мс)", 15.0, 0.0, 60.0, 5.0).withEn("Random Jitter"));
    public final BooleanSetting autoClose = addSetting(new BooleanSetting("auto_close", "Авто-закрытие сундука", true).withEn("Auto Close"));
    public final BooleanSetting closeOnFull = addSetting(new BooleanSetting("close_on_full", "Закрывать при полном инвентаре", true).withEn("Close on Full"));
    public final BooleanSetting onlyValuables = addSetting(new BooleanSetting("only_valuables", "Только ценные предметы", false).withEn("Only Valuables"));
    public final BooleanSetting ignoreTrash = addSetting(new BooleanSetting("ignore_trash", "Пропускать мусорные блоки", true).withEn("Ignore Trash"));
    public final BooleanSetting silent = addSetting(new BooleanSetting("silent", "Без звуков кликов", false).withEn("Silent"));

    private final Random random = new Random();
    private long lastLootTime = 0L;
    private long currentRequiredDelay = 35L;

    public ChestStealerModule() {
        super("chest_stealer", "Быстрый лут (Стиллер)", "Автоматически забирает все ценные предметы из сундуков и шалкеров с безопасной задержкой", Category.COMBAT);
        setEn("Chest Stealer", "Fast automated looting of chests, barrels and shulker boxes with humanized delays");
        setEnabled(false);
    }

    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.interactionManager == null) return;

        if (mc.currentScreen instanceof GenericContainerScreen gcs) {
            GenericContainerScreenHandler handler = gcs.getScreenHandler();
            Inventory containerInv = handler.getInventory();
            lootInventory(mc, handler.syncId, containerInv.size(), handler);
        } else if (mc.currentScreen instanceof ShulkerBoxScreen sbs) {
            ShulkerBoxScreenHandler handler = sbs.getScreenHandler();
            lootInventory(mc, handler.syncId, 27, handler);
        }
    }

    private void lootInventory(MinecraftClient mc, int syncId, int containerSlotCount, net.minecraft.screen.ScreenHandler handler) {
        long now = System.currentTimeMillis();
        if (now - lastLootTime < currentRequiredDelay) {
            return;
        }

        // Check if player inventory is completely full
        if (closeOnFull.isEnabled() && isPlayerInventoryFull(mc.player.getInventory())) {
            mc.player.closeHandledScreen();
            if (!silent.isEnabled()) SoundEngine.get().playClick(mc, true, 1.2f);
            ToastManager.get().push("Стиллер", "Инвентарь полон", 0xFFF59E0B);
            return;
        }

        boolean foundAny = false;
        for (int i = 0; i < containerSlotCount; i++) {
            Slot slot = handler.getSlot(i);
            if (slot == null) continue;
            ItemStack stack = slot.getStack();
            if (stack != null && !stack.isEmpty()) {
                if (ignoreTrash.isEnabled() && isTrashItem(stack)) {
                    continue;
                }
                if (onlyValuables.isEnabled() && !isValuableItem(stack)) {
                    continue;
                }

                foundAny = true;
                lastLootTime = now;
                float base = delayMs.asFloat();
                float jitter = randomDelay.asFloat();
                currentRequiredDelay = (long) (base + (jitter > 0.01f ? random.nextFloat() * jitter : 0f));

                mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                return;
            }
        }

        // If no more qualifying items left in container, auto-close
        if (!foundAny && autoClose.isEnabled()) {
            mc.player.closeHandledScreen();
            if (!silent.isEnabled()) SoundEngine.get().playClick(mc, true, 1.3f);
            ToastManager.get().push("Стиллер", "Контейнер очищен", 0xFF10B981);
        }
    }

    private boolean isPlayerInventoryFull(PlayerInventory inv) {
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isEmpty()) return false;
        }
        return true;
    }

    private boolean isTrashItem(ItemStack stack) {
        return stack.isOf(Items.COBBLESTONE) || stack.isOf(Items.DIRT) || stack.isOf(Items.GRAVEL)
                || stack.isOf(Items.SAND) || stack.isOf(Items.WHEAT_SEEDS) || stack.isOf(Items.POISONOUS_POTATO)
                || stack.isOf(Items.ROTTEN_FLESH) || stack.isOf(Items.ANDESITE) || stack.isOf(Items.DIORITE)
                || stack.isOf(Items.GRANITE);
    }

    private boolean isValuableItem(ItemStack stack) {
        if (stack.isOf(Items.TOTEM_OF_UNDYING) || stack.isOf(Items.GOLDEN_APPLE)
                || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE) || stack.isOf(Items.ENDER_PEARL)
                || stack.isOf(Items.END_CRYSTAL) || stack.isOf(Items.RESPAWN_ANCHOR)
                || stack.isOf(Items.OBSIDIAN) || stack.isOf(Items.EXPERIENCE_BOTTLE)
                || stack.isOf(Items.FIREWORK_ROCKET) || stack.isOf(Items.POTION)
                || stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.MACE)
                || stack.isOf(Items.WIND_CHARGE)) {
            return true;
        }
        String key = stack.getItem().getTranslationKey();
        if (key.contains("helmet") || key.contains("chestplate") || key.contains("leggings") || key.contains("boots")
                || key.contains("sword") || key.contains("pickaxe") || key.contains("axe") || key.contains("shovel")
                || key.contains("mace")) {
            return true;
        }
        // Custom RPG named items (Talismans/Spheres on ReallyWorld/FunTime)
        var lore = stack.get(DataComponentTypes.LORE);
        var name = stack.get(DataComponentTypes.CUSTOM_NAME);
        return lore != null || name != null;
    }
}
