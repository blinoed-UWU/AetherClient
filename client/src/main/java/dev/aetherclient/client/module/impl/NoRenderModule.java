package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;

public final class NoRenderModule extends Module {
    public final BooleanSetting hideFire = addSetting(new BooleanSetting("fire", "Скрыть огонь", true).withEn("Hide Fire Overlay"));
    public final BooleanSetting hideHurtCam = addSetting(new BooleanSetting("hurtcam", "Убрать тряску урона", true).withEn("Hide Hurt Cam"));
    public final BooleanSetting hideBlindness = addSetting(new BooleanSetting("blindness", "Убрать слепоту", true).withEn("Hide Blindness"));
    public final BooleanSetting hideDarkness = addSetting(new BooleanSetting("darkness", "Убрать тьму", true).withEn("Hide Darkness"));
    public final BooleanSetting hideNausea = addSetting(new BooleanSetting("nausea", "Убрать тошноту", true).withEn("Hide Nausea"));

    private Double savedDamageTilt = null;

    public NoRenderModule() {
        super("no_render", "No Render", "Скрывает огонь, тряску урона и эффекты слепоты/тьмы/тошноты", Category.VISUAL);
        setEn("No Render", "Hides fire, hurt cam shake and blindness/darkness/nausea overlays");
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        restoreDamageTilt(mc);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled() || mc.player == null) return;
        if (hideFire.isEnabled() && mc.player.getFireTicks() > 0) {
            mc.player.setFireTicks(0);
        }
        if (hideBlindness.isEnabled() && mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) {
            mc.player.removeStatusEffect(StatusEffects.BLINDNESS);
        }
        if (hideDarkness.isEnabled() && mc.player.hasStatusEffect(StatusEffects.DARKNESS)) {
            mc.player.removeStatusEffect(StatusEffects.DARKNESS);
        }
        if (hideNausea.isEnabled() && mc.player.hasStatusEffect(StatusEffects.NAUSEA)) {
            mc.player.removeStatusEffect(StatusEffects.NAUSEA);
        }
        if (hideHurtCam.isEnabled()) {
            applyDamageTiltZero(mc);
        } else {
            restoreDamageTilt(mc);
        }
    }

    private void applyDamageTiltZero(MinecraftClient mc) {
        if (mc == null || mc.options == null) return;
        try {
            var option = mc.options.getDamageTiltStrength();
            if (option == null) return;
            if (savedDamageTilt == null) {
                savedDamageTilt = option.getValue();
            }
            option.setValue(0.0);
        } catch (Throwable ignored) {
        }
    }

    private void restoreDamageTilt(MinecraftClient mc) {
        if (savedDamageTilt == null) return;
        try {
            var option = mc.options.getDamageTiltStrength();
            if (option != null) {
                option.setValue(savedDamageTilt);
            }
        } catch (Throwable ignored) {
        }
        savedDamageTilt = null;
    }
}
