package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;

public final class AntiBlindModule extends Module {
    public final BooleanSetting removeBlindness = addSetting(new BooleanSetting("blindness", "Убрать слепоту", true).withEn("Remove Blindness"));
    public final BooleanSetting removeDarkness = addSetting(new BooleanSetting("darkness", "Убрать тьму", true).withEn("Remove Darkness"));
    public final BooleanSetting removeNausea = addSetting(new BooleanSetting("nausea", "Убрать тошноту", true).withEn("Remove Nausea"));

    public AntiBlindModule() {
        super("antiblind", "Анти-тьма", "Убирает слепоту, эффект тьмы и тошноту", Category.VISUAL);
        setEn("AntiBlind", "Removes blindness, darkness, and nausea effects");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled() || mc.player == null) return;
        if (removeBlindness.isEnabled() && mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) {
            mc.player.removeStatusEffect(StatusEffects.BLINDNESS);
        }
        if (removeDarkness.isEnabled() && mc.player.hasStatusEffect(StatusEffects.DARKNESS)) {
            mc.player.removeStatusEffect(StatusEffects.DARKNESS);
        }
        if (removeNausea.isEnabled() && mc.player.hasStatusEffect(StatusEffects.NAUSEA)) {
            mc.player.removeStatusEffect(StatusEffects.NAUSEA);
        }
    }
}
