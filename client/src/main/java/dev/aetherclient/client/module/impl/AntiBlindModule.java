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
    public final BooleanSetting removeMiningFatigue = addSetting(new BooleanSetting("fatigue", "Убрать утомление", true).withEn("Remove Mining Fatigue"));
    public final BooleanSetting removeBadOmen = addSetting(new BooleanSetting("bad_omen", "Убрать дурное знамение", true).withEn("Remove Bad Omen"));
    public final BooleanSetting removeSlownessFov = addSetting(new BooleanSetting("slowness_fov", "Убрать замедление FOV", true).withEn("Fix Slowness FOV"));
    public final BooleanSetting removeWither = addSetting(new BooleanSetting("wither", "Чистые сердца (без иссушения)", false).withEn("Clear Wither Hearts"));
    public final BooleanSetting removePoison = addSetting(new BooleanSetting("poison", "Чистые сердца (без яда)", false).withEn("Clear Poison Hearts"));

    public AntiBlindModule() {
        super("antiblind", "Анти-тьма", "Убирает слепоту, эффект тьмы, тошноту, утомление и искажения интерфейса", Category.VISUAL);
        setEn("AntiBlind", "Removes blindness, darkness, nausea, mining fatigue and negative status overlays");
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
        if (removeMiningFatigue.isEnabled() && mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            mc.player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
        }
        if (removeBadOmen.isEnabled() && mc.player.hasStatusEffect(StatusEffects.BAD_OMEN)) {
            mc.player.removeStatusEffect(StatusEffects.BAD_OMEN);
        }
    }
}
