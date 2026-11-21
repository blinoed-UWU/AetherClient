package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;

public final class NoRenderModule extends Module {
    public final BooleanSetting hideFire = addSetting(new BooleanSetting("fire", "Скрыть огонь на экране", true).withEn("Hide Fire Overlay"));
    public final BooleanSetting hideHurtCam = addSetting(new BooleanSetting("hurtcam", "Убрать тряску урона", true).withEn("Hide Hurt Cam"));
    public final BooleanSetting hideTotem = addSetting(new BooleanSetting("totem", "Скрыть анимацию тотема", true).withEn("Hide Totem Pop"));
    public final BooleanSetting hidePumpkin = addSetting(new BooleanSetting("pumpkin", "Скрыть тыкву", true).withEn("Hide Pumpkin Overlay"));
    public final BooleanSetting hidePortal = addSetting(new BooleanSetting("portal", "Скрыть анимацию портала", true).withEn("Hide Portal Distortion"));
    public final BooleanSetting hideElderGuardian = addSetting(new BooleanSetting("elder_guardian", "Убрать скример Стража", true).withEn("Hide Elder Guardian"));
    public final BooleanSetting hideUnderwater = addSetting(new BooleanSetting("underwater", "Убрать рябь под водой", true).withEn("Clear Underwater"));
    public final BooleanSetting hideBlindness = addSetting(new BooleanSetting("blindness", "Убрать слепоту", true).withEn("Hide Blindness"));
    public final BooleanSetting hideDarkness = addSetting(new BooleanSetting("darkness", "Убрать тьму", true).withEn("Hide Darkness"));
    public final BooleanSetting hideNausea = addSetting(new BooleanSetting("nausea", "Убрать тошноту", true).withEn("Hide Nausea"));
    public final BooleanSetting hideFog = addSetting(new BooleanSetting("fog", "Убрать туман", true).withEn("Hide Fog"));
    public final BooleanSetting hideBossBar = addSetting(new BooleanSetting("bossbar", "Скрыть полосу босса", false).withEn("Hide Boss Bar"));
    public final BooleanSetting hideRain = addSetting(new BooleanSetting("rain", "Убрать дождь", true).withEn("Hide Rain"));
    public final BooleanSetting hideExplosions = addSetting(new BooleanSetting("explosions", "Скрыть частицы взрывов", true).withEn("Hide Explosions"));
    public final BooleanSetting hideLightning = addSetting(new BooleanSetting("lightning", "Скрыть вспышки молний", true).withEn("Hide Lightning"));
    public final BooleanSetting hideArmorStands = addSetting(new BooleanSetting("armor_stands", "Скрыть стойки для брони", false).withEn("Hide Armor Stands"));

    private Double savedDamageTilt = null;

    public NoRenderModule() {
        super("no_render", "No Render", "Скрывает огонь, анимацию тотема, тряску урона, тыкву, портал, туман и частицы взрывов", Category.VISUAL);
        setEn("No Render", "Hides fire, totem pop, hurt cam, pumpkin overlay, portal, fog and explosion particles for max FPS");
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
            if (savedDamageTilt == null) {
                savedDamageTilt = option.getValue();
            }
            if (option.getValue() > 0.0) {
                option.setValue(0.0);
            }
        } catch (Throwable ignored) {
        }
    }

    private void restoreDamageTilt(MinecraftClient mc) {
        if (mc == null || mc.options == null || savedDamageTilt == null) return;
        try {
            mc.options.getDamageTiltStrength().setValue(savedDamageTilt);
            savedDamageTilt = null;
        } catch (Throwable ignored) {
        }
    }
}
