package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.audio.SoundEngine;
import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.KeybindSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import dev.aetherclient.client.notify.ToastManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

/**
 * Mace Helper ("Помощник булавы").
 * Automatically equips Mace during fall to deal devastating smash attack damage,
 * and assists with instant Wind Charge boosting.
 */
public final class MaceHelperModule extends Module {
    public final NumberSetting minFallDistance = addSetting(
            new NumberSetting("min_fall", "Мин. высота падения", 1.5, 1.0, 8.0, 0.5).withEn("Min Fall Distance")
    );
    public final BooleanSetting swapBack = addSetting(
            new BooleanSetting("swap_back", "Возврат на оружие после удара", true).withEn("Swap Back on Land")
    );
    public final BooleanSetting onlyWithTarget = addSetting(
            new BooleanSetting("only_target", "Только рядом с врагом", true).withEn("Only Near Target")
    );
    public final NumberSetting targetDistance = addSetting(
            new NumberSetting("target_dist", "Радиус до врага", 4.5, 2.0, 6.0, 0.5).withEn("Target Distance")
    );
    public final KeybindSetting windChargeKey = addSetting(
            new KeybindSetting("wind_key", "Клавиша Wind Charge прыжка", GLFW.GLFW_KEY_UNKNOWN).withEn("Wind Charge Boost Key")
    );
    public final BooleanSetting windChargePitch = addSetting(
            new BooleanSetting("wind_pitch", "Смотреть вниз для прыжка", true).withEn("Look Down on Boost")
    );
    public final BooleanSetting notify = addSetting(
            new BooleanSetting("notify", "Уведомления", false).withEn("Notifications")
    );
    public final BooleanSetting playSound = addSetting(
            new BooleanSetting("sound", "Звуковой эффект", true).withEn("Play Sound")
    );

    private int previousSlot = -1;
    private boolean swappedToMace = false;

    public MaceHelperModule() {
        super("mace_helper", "Помощник булавы", "Автоматически достает булаву (Mace) в прыжке и падении для сокрушительного удара, с мгновенным Wind Charge", Category.COMBAT);
        setEn("Mace Helper", "Auto equips Mace during fall for max smash damage, with Wind Charge boost assist");
    }

    @Override
    protected void onDisable() {
        resetState();
    }

    public boolean onKey(int key, int action) {
        if (!isEnabled()) return false;
        if (action == GLFW.GLFW_PRESS && key == windChargeKey.getValue() && key != GLFW.GLFW_KEY_UNKNOWN) {
            executeWindChargeBoost();
            return true;
        }
        return false;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled() || mc.player == null || mc.interactionManager == null) return;

        PlayerInventory inv = mc.player.getInventory();
        int maceSlot = findItemInHotbar(inv, Items.MACE);

        // If player has no Mace in hotbar, nothing to auto-swap
        if (maceSlot == -1) {
            if (swappedToMace) resetState();
            return;
        }

        boolean isFalling = !mc.player.isOnGround() && mc.player.getVelocity().y < -0.15
                && mc.player.fallDistance >= minFallDistance.asFloat();

        if (isFalling) {
            if (onlyWithTarget.isEnabled() && !hasTargetNearby(mc)) {
                return;
            }

            int currentSlot = inv.getSelectedSlot();
            if (currentSlot != maceSlot && !swappedToMace) {
                previousSlot = currentSlot;
                inv.setSelectedSlot(maceSlot);
                swappedToMace = true;

                if (playSound.isEnabled()) {
                    SoundEngine.get().playClick(mc, true, 1.4f);
                }
                if (notify.isEnabled()) {
                    ToastManager.get().push("Булава", "Взята булава для удара в падении", 0xFFF59E0B);
                }
            }
        } else if (mc.player.isOnGround()) {
            if (swappedToMace && swapBack.isEnabled() && previousSlot != -1) {
                inv.setSelectedSlot(previousSlot);
                if (notify.isEnabled()) {
                    ToastManager.get().push("Булава", "Возврат на прежнее оружие", 0xFF10B981);
                }
            }
            resetState();
        }
    }

    private void executeWindChargeBoost() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.interactionManager == null) return;

        PlayerInventory inv = mc.player.getInventory();
        boolean hasOffhand = mc.player.getOffHandStack().isOf(Items.WIND_CHARGE);
        int hotbarSlot = findItemInHotbar(inv, Items.WIND_CHARGE);

        if (!hasOffhand && hotbarSlot == -1) {
            ToastManager.get().push("Wind Charge", "Нет Wind Charge в хотбаре или во второй руке", 0xFFEF4444);
            return;
        }

        float savedPitch = mc.player.getPitch();
        if (windChargePitch.isEnabled()) {
            mc.player.setPitch(89.5f);
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                        mc.player.getYaw(), 89.5f, mc.player.isOnGround(), mc.player.horizontalCollision
                ));
            }
        }

        if (hasOffhand) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
        } else {
            int oldSlot = inv.getSelectedSlot();
            inv.setSelectedSlot(hotbarSlot);
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
            }
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            inv.setSelectedSlot(oldSlot);
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
            }
        }

        if (windChargePitch.isEnabled()) {
            mc.player.setPitch(savedPitch);
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                        mc.player.getYaw(), savedPitch, mc.player.isOnGround(), mc.player.horizontalCollision
                ));
            }
        }

        if (playSound.isEnabled()) {
            SoundEngine.get().playClick(mc, true, 1.6f);
        }
        if (notify.isEnabled()) {
            ToastManager.get().push("Wind Charge", "Прыжок выполнен", 0xFF38BDF8);
        }
    }

    private boolean hasTargetNearby(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return false;
        double maxDistSq = Math.pow(targetDistance.asFloat(), 2);
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && entity != mc.player && entity.isAlive()) {
                if (mc.player.squaredDistanceTo(entity) <= maxDistSq) {
                    return true;
                }
            }
        }
        return false;
    }

    private int findItemInHotbar(PlayerInventory inv, net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private void resetState() {
        swappedToMace = false;
        previousSlot = -1;
    }
}
