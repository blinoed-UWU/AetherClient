package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Automatically selects the best tool in the hotbar when breaking a block.
 */
public final class AutoToolModule extends Module {
    public final BooleanSetting switchBack = new BooleanSetting("switch_back", "Возвращать слот обратно", true);
    public final BooleanSetting weapons = new BooleanSetting("weapons", "Игнорировать мечи", false);

    private int previousSlot = -1;
    private boolean isSwapped = false;

    public AutoToolModule() {
        super("auto_tool", "Авто-инструмент", "Автоматически выбирает наиболее эффективный инструмент из хотбара для добычи блока", Category.COMBAT);
        addSetting(switchBack);
        addSetting(weapons);
        setEnabled(false);
    }

    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null || mc.interactionManager == null) {
            isSwapped = false;
            return;
        }

        boolean isMining = mc.options.attackKey.isPressed() && mc.crosshairTarget != null
                && mc.crosshairTarget.getType() == HitResult.Type.BLOCK;

        if (isMining) {
            BlockHitResult bhr = (BlockHitResult) mc.crosshairTarget;
            BlockState state = mc.world.getBlockState(bhr.getBlockPos());
            if (state == null || state.isAir()) return;

            PlayerInventory inv = mc.player.getInventory();
            int bestSlot = -1;
            float bestSpeed = 1.0f;

            for (int i = 0; i < 9; i++) {
                ItemStack stack = inv.getStack(i);
                if (stack == null || stack.isEmpty()) continue;
                float speed = stack.getMiningSpeedMultiplier(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }

            if (bestSlot != -1 && bestSlot != inv.getSelectedSlot()) {
                if (!isSwapped) {
                    previousSlot = inv.getSelectedSlot();
                    isSwapped = true;
                }
                inv.setSelectedSlot(bestSlot);
            }
        } else {
            if (isSwapped && switchBack.isEnabled() && previousSlot != -1) {
                PlayerInventory inv = mc.player.getInventory();
                inv.setSelectedSlot(previousSlot);
                previousSlot = -1;
                isSwapped = false;
            }
        }
    }
}
