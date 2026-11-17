package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Automatically selects the best tool in the hotbar when breaking a block.
 * Features durability saving, sword prioritization for cobwebs, shears for leaves,
 * and automatic hotbar slot restoration upon mining completion.
 */
public final class AutoToolModule extends Module {
    public final BooleanSetting switchBack = addSetting(new BooleanSetting("switch_back", "Возвращать слот обратно", true).withEn("Switch Back"));
    public final BooleanSetting saveDurability = addSetting(new BooleanSetting("save_durability", "Беречь прочность (<=5)", true).withEn("Save Durability"));
    public final BooleanSetting swordForCobweb = addSetting(new BooleanSetting("cobweb", "Меч для паутины", true).withEn("Sword for Cobweb"));
    public final BooleanSetting shearsForLeaves = addSetting(new BooleanSetting("shears", "Ножницы для листвы", true).withEn("Shears for Leaves"));
    public final BooleanSetting ignoreWeapons = addSetting(new BooleanSetting("weapons", "Не трогать при атаке мобов", true).withEn("Ignore on Attack"));

    private int previousSlot = -1;
    private boolean isSwapped = false;

    public AutoToolModule() {
        super("auto_tool", "Авто-инструмент", "Автоматически выбирает наиболее эффективный инструмент из хотбара для добычи блока", Category.COMBAT);
        setEn("Auto Tool", "Automatically equips the most effective hotbar tool for mined blocks");
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

            boolean isCobweb = state.isOf(Blocks.COBWEB);
            boolean isLeaves = state.isIn(BlockTags.LEAVES);

            for (int i = 0; i < 9; i++) {
                ItemStack stack = inv.getStack(i);
                if (stack == null || stack.isEmpty()) continue;

                if (saveDurability.isEnabled() && stack.isDamageable()) {
                    if (stack.getMaxDamage() - stack.getDamage() <= 5) {
                        continue; // Preserve nearly broken tool
                    }
                }

                if (isCobweb && swordForCobweb.isEnabled() && (stack.getItem().getTranslationKey().contains("sword") || stack.isOf(Items.SHEARS))) {
                    bestSlot = i;
                    break;
                }

                if (isLeaves && shearsForLeaves.isEnabled() && stack.isOf(Items.SHEARS)) {
                    bestSlot = i;
                    break;
                }

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
