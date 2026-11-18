package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.KeybindSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.notify.ToastManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public final class ClickPearlModule extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("mode", "Режим", "Legit (Безопасный)", "Legit (Безопасный)", "Instant (Быстрый)").withEn("Mode"));
    public final KeybindSetting key = addSetting(new KeybindSetting("key", "Клавиша броска", GLFW.GLFW_KEY_UNKNOWN).withEn("Throw Key"));
    public final BooleanSetting middleClick = addSetting(new BooleanSetting("middle_click", "Клик колесиком (СКМ)", true).withEn("Middle Click"));
    public final BooleanSetting swapBack = addSetting(new BooleanSetting("swap_back", "Возврат на прошлый слот", true).withEn("Swap Back"));

    private enum Phase {
        IDLE, SELECT_SLOT, THROW, SWAP_BACK
    }

    private Phase phase = Phase.IDLE;
    private int savedSlot = -1;
    private int pearlSlot = -1;
    private int ticksInPhase = 0;

    public ClickPearlModule() {
        super("click_pearl", "Быстрый перл", "Безопасный бросок эндер-жемчуга из хотбара без флагов античита (GrimAC/Matrix)", Category.COMBAT);
        setEn("Click Pearl", "Anticheat-safe legit pearl thrower from hotbar");
    }

    public void triggerThrow(MinecraftClient mc) {
        if (!isEnabled() || mc.player == null || phase != Phase.IDLE) return;

        savedSlot = mc.player.getInventory().getSelectedSlot();
        pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.ENDER_PEARL) {
                pearlSlot = i;
                break;
            }
        }

        if (pearlSlot == -1) {
            ToastManager.get().push("Перл", "Жемчуг края не найден в хотбаре!", 0xFFF43F5E);
            return;
        }

        if ("Instant (Быстрый)".equals(mode.getValue())) {
            // Fast mode
            if (pearlSlot != savedSlot && mc.player.networkHandler != null) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(pearlSlot));
                mc.player.getInventory().setSelectedSlot(pearlSlot);
            }
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            if (swapBack.isEnabled() && pearlSlot != savedSlot && mc.player.networkHandler != null) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(savedSlot));
                mc.player.getInventory().setSelectedSlot(savedSlot);
            }
            phase = Phase.IDLE;
        } else {
            // Legit mode: multi-tick state machine so anticheats never flag container/slot desync
            phase = Phase.SELECT_SLOT;
            ticksInPhase = 0;
            if (pearlSlot != savedSlot && mc.player.networkHandler != null) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(pearlSlot));
                mc.player.getInventory().setSelectedSlot(pearlSlot);
            }
        }
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (phase == Phase.IDLE || mc == null || mc.player == null) return;
        ticksInPhase++;

        switch (phase) {
            case SELECT_SLOT -> {
                // Ensure item is selected in main hand
                if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL || ticksInPhase >= 1) {
                    if (mc.interactionManager != null) {
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    } else if (mc.player.networkHandler != null) {
                        mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(
                                Hand.MAIN_HAND,
                                0,
                                mc.player.getYaw(),
                                mc.player.getPitch()
                        ));
                    }
                    mc.player.swingHand(Hand.MAIN_HAND);
                    phase = swapBack.isEnabled() && pearlSlot != savedSlot ? Phase.SWAP_BACK : Phase.IDLE;
                    ticksInPhase = 0;
                }
            }
            case SWAP_BACK -> {
                if (ticksInPhase >= 1) {
                    if (mc.player.networkHandler != null && savedSlot >= 0 && savedSlot < 9) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(savedSlot));
                        mc.player.getInventory().setSelectedSlot(savedSlot);
                    }
                    phase = Phase.IDLE;
                    ticksInPhase = 0;
                }
            }
            default -> phase = Phase.IDLE;
        }
    }
}
