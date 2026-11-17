package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.KeybindSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.notify.ToastManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

public final class FastSwapModule extends Module {
    public static final String[] ITEM_CHOICES = {
            "Талисман (Талик)",
            "Шар / Сфера",
            "Обычный тотем",
            "Любой тотем",
            "Щит",
            "Золотое яблоко",
            "Чар. яблоко",
            "Эндер-жемчуг",
            "Фейерверк",
            "Булава",
            "Ветровой заряд"
    };

    public final ModeSetting targetHand = addSetting(
            new ModeSetting("target_hand", "Целевой слот", "Левая рука (Offhand)", "Левая рука (Offhand)", "Хотбар (Выбор)").withEn("Target Slot")
    );
    public final ModeSetting item1 = addSetting(
            new ModeSetting("item1", "Первый предмет", "Талисман (Талик)", ITEM_CHOICES).withEn("First Item")
    );
    public final ModeSetting item2 = addSetting(
            new ModeSetting("item2", "Второй предмет", "Шар / Сфера", ITEM_CHOICES).withEn("Second Item")
    );
    public final KeybindSetting key = addSetting(new KeybindSetting("key", "Клавиша смены", GLFW.GLFW_KEY_UNKNOWN).withEn("Swap Key"));
    public final BooleanSetting freezeMovement = addSetting(new BooleanSetting("freeze_movement", "Останавливать движение при инв-свапе", true).withEn("Freeze Movement On Inv Swap"));
    public final BooleanSetting autoCloseInv = addSetting(new BooleanSetting("auto_close", "Закрывать инвентарь для сервера", true).withEn("Auto Close Server Inventory"));

    private boolean swapping = false;
    private int swapTicks = 0;
    private int targetInvSlot = -1;
    private String pendingDesired = null;
    private boolean isOffhandSwap = true;
    private String lastSwapped = null;

    public FastSwapModule() {
        super("fast_swap", "Быстрый свап", "Переключение между 2 предметами в левую руку или хотбар по 1 кнопке", Category.COMBAT);
        setEn("Fast Swap", "Toggle between 2 items in offhand or hotbar with one key");
    }

    public boolean isSwapping() {
        return swapping;
    }

    public void triggerSwap(MinecraftClient mc) {
        if (!isEnabled() || mc == null || mc.player == null) return;
        if (swapping) return; // Wait for active artificial inventory swap to finish

        String type1 = item1.getValue();
        String type2 = item2.getValue();

        boolean offhandMode = "Левая рука (Offhand)".equals(targetHand.getValue());
        ItemStack currentStack = offhandMode ? mc.player.getOffHandStack() : mc.player.getMainHandStack();

        String desired;
        boolean matches1 = isMatching(currentStack, type1);
        boolean matches2 = isMatching(currentStack, type2);

        if (matches1 && !matches2) {
            desired = type2;
        } else if (matches2 && !matches1) {
            desired = type1;
        } else {
            // If current stack is neither (e.g. empty, or holding food/shield) or ambiguous
            if (type1.equals(lastSwapped)) {
                desired = type2;
            } else {
                desired = type1;
            }
        }

        // 1. Check hotbar first (slots 0-8)
        int hotbarSlot = findHotbarSlot(mc, desired);
        if (hotbarSlot != -1) {
            if (offhandMode) {
                int prevSelected = mc.player.getInventory().getSelectedSlot();
                if (mc.player.networkHandler != null) {
                    if (prevSelected != hotbarSlot) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
                        mc.player.getInventory().setSelectedSlot(hotbarSlot);
                    }
                    mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                            BlockPos.ORIGIN,
                            Direction.DOWN
                    ));
                    if (prevSelected != hotbarSlot) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(prevSelected));
                        mc.player.getInventory().setSelectedSlot(prevSelected);
                    }
                }
                lastSwapped = desired;
                ToastManager.get().push("Свап", "В левую руку: " + desired, 0xFF10B981);
                return;
            } else {
                // Hotbar selection mode
                if (mc.player.networkHandler != null) {
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
                }
                mc.player.getInventory().setSelectedSlot(hotbarSlot);
                lastSwapped = desired;
                ToastManager.get().push("Свап", "Выбран слот: " + (hotbarSlot + 1) + " (" + desired + ")", 0xFF10B981);
                return;
            }
        }

        // 2. Check main inventory (slots 9-35)
        int invSlot = findInventorySlot(mc, desired);
        if (invSlot != -1) {
            startInventorySwap(mc, invSlot, desired, offhandMode);
            return;
        }

        // Item not found anywhere in inventory
        ToastManager.get().push("Свап", desired + " не найден в инвентаре!", 0xFFF43F5E);
    }

    private void startInventorySwap(MinecraftClient mc, int invSlot, String desired, boolean offhandMode) {
        swapping = true;
        swapTicks = 0;
        targetInvSlot = invSlot;
        pendingDesired = desired;
        isOffhandSwap = offhandMode;

        // Immediately cancel sprint and freeze movement velocity so server does not flag
        if (freezeMovement.isEnabled()) {
            mc.player.setSprinting(false);
            mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
            if (mc.player.input != null) {
                mc.player.input.playerInput = PlayerInput.DEFAULT;
            }
        }
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!swapping || mc == null || mc.player == null) return;

        // While artificial inventory is open, strictly prevent moving
        if (freezeMovement.isEnabled()) {
            mc.player.setSprinting(false);
            mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
            if (mc.player.input != null) {
                mc.player.input.playerInput = PlayerInput.DEFAULT;
            }
        }

        swapTicks++;

        if (swapTicks == 1) {
            // Step 1: Perform the inventory slot swap on PlayerScreenHandler
            if (mc.interactionManager != null) {
                int syncId = mc.player.playerScreenHandler.syncId;
                if (isOffhandSwap) {
                    // Button 40 swaps with offhand slot
                    mc.interactionManager.clickSlot(syncId, targetInvSlot, 40, SlotActionType.SWAP, mc.player);
                } else {
                    int selected = mc.player.getInventory().getSelectedSlot();
                    mc.interactionManager.clickSlot(syncId, targetInvSlot, selected, SlotActionType.SWAP, mc.player);
                }
            }
        } else if (swapTicks == 2) {
            // Step 2: Artificially close the inventory container for the server
            if (autoCloseInv.isEnabled() && mc.player.networkHandler != null) {
                int syncId = mc.player.playerScreenHandler.syncId;
                mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
            }
        } else if (swapTicks >= 3) {
            // Step 3: Sequence complete - unfreeze movement
            swapping = false;
            swapTicks = 0;
            lastSwapped = pendingDesired;
            ToastManager.get().push("Свап", "Свапнут из инвентаря: " + pendingDesired, 0xFF10B981);
        }
    }

    private static int findHotbarSlot(MinecraftClient mc, String desiredType) {
        for (int i = 0; i < 9; i++) {
            ItemStack st = mc.player.getInventory().getStack(i);
            if (isMatching(st, desiredType)) return i;
        }
        return -1;
    }

    private static int findInventorySlot(MinecraftClient mc, String desiredType) {
        for (int i = 9; i < 36; i++) {
            ItemStack st = mc.player.getInventory().getStack(i);
            if (isMatching(st, desiredType)) return i;
        }
        return -1;
    }

    public static boolean isMatching(ItemStack stack, String typeName) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stack.getName().getString().toLowerCase();
        StringBuilder textBuilder = new StringBuilder(name);
        try {
            var loreComp = stack.get(DataComponentTypes.LORE);
            if (loreComp != null) {
                for (var line : loreComp.lines()) {
                    textBuilder.append(" ").append(line.getString().toLowerCase());
                }
            }
        } catch (Throwable ignored) {}
        String fullText = textBuilder.toString();

        boolean isTalik = fullText.contains("талисман") || fullText.contains("талик") || fullText.contains("talisman");
        boolean isBall = (fullText.contains("шар") || fullText.contains("сфер") || fullText.contains("sphere") || fullText.contains("ball")) && !isTalik;

        return switch (typeName) {
            case "Талисман (Талик)" -> isTalik;
            case "Шар / Сфера" -> isBall || (stack.getItem() == Items.PLAYER_HEAD && !isTalik);
            case "Обычный тотем" -> stack.getItem() == Items.TOTEM_OF_UNDYING && !isTalik && !isBall;
            case "Любой тотем" -> stack.getItem() == Items.TOTEM_OF_UNDYING;
            case "Щит" -> stack.getItem() == Items.SHIELD;
            case "Золотое яблоко" -> stack.getItem() == Items.GOLDEN_APPLE;
            case "Чар. яблоко" -> stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
            case "Эндер-жемчуг" -> stack.getItem() == Items.ENDER_PEARL;
            case "Фейерверк" -> stack.getItem() == Items.FIREWORK_ROCKET;
            case "Булава" -> stack.getItem() == Items.MACE;
            case "Ветровой заряд" -> stack.getItem() == Items.WIND_CHARGE;
            default -> false;
        };
    }
}
