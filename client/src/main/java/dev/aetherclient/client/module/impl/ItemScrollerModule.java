package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.audio.SoundEngine;
import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Item Scroller ("Быстрый перенос предметов").
 * Fast bulk transfer of matching items between inventories with anti-cheat delay protection.
 */
public final class ItemScrollerModule extends Module {
    public final NumberSetting delayMs = addSetting(
            new NumberSetting("delay", "Задержка (мс)", 25.0, 5.0, 150.0, 5.0).withEn("Transfer Delay")
    );
    public final BooleanSetting moveAllSame = addSetting(
            new BooleanSetting("all_same", "Переносить все одинаковые (Shift+Ctrl)", true).withEn("Transfer All Matching")
    );
    public final BooleanSetting playSound = addSetting(
            new BooleanSetting("sound", "Звуковой эффект", true).withEn("Play Sound")
    );

    private boolean isTransferring = false;

    public ItemScrollerModule() {
        super("item_scroller", "Быстрый перенос предметов", "Мгновенный перенос всех одинаковых предметов из сундука или инвентаря по Shift+Ctrl клику", Category.COMBAT);
        setEn("Item Scroller", "Fast mass transfer of matching items between containers on Shift+Ctrl click");
    }

    public boolean onSlotClick(HandledScreen<?> screen, Slot slot, int slotId, int button, SlotActionType actionType) {
        if (!isEnabled() || !moveAllSame.isEnabled() || isTransferring || slot == null || !slot.hasStack()) {
            return false;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return false;

        boolean isShift = actionType == SlotActionType.QUICK_MOVE;
        long winHandle = mc.getWindow().getHandle();
        boolean isCtrlDown = org.lwjgl.glfw.GLFW.glfwGetKey(winHandle, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(winHandle, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        if (isShift && isCtrlDown) {
            Item targetItem = slot.getStack().getItem();
            ScreenHandler handler = mc.player.currentScreenHandler;
            if (handler == null) return false;

            int syncId = handler.syncId;
            int totalSlots = handler.slots.size();

            new Thread(() -> {
                isTransferring = true;
                long delay = (long) delayMs.asFloat();
                int transferred = 0;

                try {
                    for (int i = 0; i < totalSlots; i++) {
                        Slot s = handler.getSlot(i);
                        if (s != null && s.hasStack() && s.inventory == slot.inventory) {
                            ItemStack st = s.getStack();
                            if (st.getItem() == targetItem) {
                                final int currentSlotIndex = i;
                                mc.execute(() -> {
                                    if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler.syncId == syncId) {
                                        mc.interactionManager.clickSlot(syncId, currentSlotIndex, 0, SlotActionType.QUICK_MOVE, mc.player);
                                    }
                                });
                                transferred++;
                                if (delay > 0) {
                                    Thread.sleep(delay);
                                }
                            }
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    isTransferring = false;
                }

                if (playSound.isEnabled() && transferred > 0) {
                    mc.execute(() -> SoundEngine.get().playClick(mc, true, 1.25f));
                }
            }, "Aether-ItemScroller").start();

            return true;
        }
        return false;
    }
}
