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
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Fast Chest Stealer / Quick Container Looter.
 * Rapidly loots items from chests, barrels, and shulkers into player inventory.
 */
public final class ChestStealerModule extends Module {
    public final NumberSetting delayMs = new NumberSetting("delay", "Задержка (мс)", 40.0, 0.0, 250.0, 5.0);
    public final BooleanSetting autoClose = new BooleanSetting("auto_close", "Авто-закрытие сундука", true);
    public final BooleanSetting ignoreTrash = new BooleanSetting("ignore_trash", "Пропускать мусорные блоки", false);

    private long lastLootTime = 0L;

    public ChestStealerModule() {
        super("chest_stealer", "Быстрый лут (Стиллер)", "Автоматически забирает все ценные предметы из сундуков и шалкеров с безопасной задержкой", Category.COMBAT);
        addSetting(delayMs);
        addSetting(autoClose);
        addSetting(ignoreTrash);
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
        if (now - lastLootTime < (long) delayMs.asFloat()) {
            return;
        }

        boolean foundAny = false;
        for (int i = 0; i < containerSlotCount; i++) {
            Slot slot = handler.getSlot(i);
            if (slot == null) continue;
            ItemStack stack = slot.getStack();
            if (stack != null && !stack.isEmpty()) {
                foundAny = true;
                lastLootTime = now;
                mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                return;
            }
        }

        // If no items left in container, auto-close
        if (!foundAny && autoClose.isEnabled()) {
            mc.player.closeHandledScreen();
            SoundEngine.get().playClick(mc, true, 1.3f);
            ToastManager.get().push("Стиллер", "Контейнер полностью опустошен", 0xFF10B981);
        }
    }
}
