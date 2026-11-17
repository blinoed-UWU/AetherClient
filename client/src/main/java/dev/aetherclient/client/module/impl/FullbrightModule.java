package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public final class FullbrightModule extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("mode", "Способ", "Ночное зрение", "Ночное зрение", "Гамма").withEn("Mode"));
    public final BooleanSetting autoToggle = addSetting(new BooleanSetting("auto_toggle", "Авто-включение", false).withEn("Auto Toggle"));
    public final ModeSetting autoCondition = addSetting(
            new ModeSetting("auto_cond", "Условие авто", "В темноте", "В темноте", "Ночью", "Под землей")
                    .<ModeSetting>withEn("Auto Condition").visibleIf(autoToggle::isEnabled)
    );
    public final NumberSetting lightThreshold = addSetting(
            new NumberSetting("light_threshold", "Порог света", 7.0, 0.0, 15.0, 1.0)
                    .<NumberSetting>withEn("Light Threshold").visibleIf(autoToggle::isEnabled)
    );
    public final NumberSetting gammaLevel = addSetting(
            new NumberSetting("gamma_level", "Уровень гаммы", 16.0, 2.0, 25.0, 1.0)
                    .<NumberSetting>withEn("Gamma Level").visibleIf(() -> "Гамма".equals(mode.getValue()))
    );

    private boolean appliedNightVision = false;
    private double savedGamma = -1.0;
    private int debounceTicks = 0;
    private boolean lastActiveState = true;

    public FullbrightModule() {
        super("fullbright", "Яркость", "Полное освещение с авто-включением в темноте", Category.VISUAL);
        setEn("Fullbright", "Full brightness with auto-activation in darkness");
    }

    @Override
    protected void onDisable() {
        debounceTicks = 0;
        lastActiveState = true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null && appliedNightVision) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            appliedNightVision = false;
        }
        if (mc != null && mc.options != null && savedGamma >= 0.0) {
            mc.options.getGamma().setValue(savedGamma);
            savedGamma = -1.0;
        }
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled() || mc.player == null) return;

        boolean active = true;
        if (autoToggle.isEnabled() && mc.world != null) {
            net.minecraft.util.math.BlockPos eyePos = net.minecraft.util.math.BlockPos.ofFloored(mc.player.getEyePos());
            net.minecraft.util.math.BlockPos feetPos = mc.player.getBlockPos();
            int light = Math.max(mc.world.getLightLevel(eyePos), mc.world.getLightLevel(feetPos));
            int thresh = (int) Math.round(lightThreshold.getValue());

            boolean conditionMet = switch (autoCondition.getValue()) {
                case "Ночью" -> !mc.world.isDay();
                case "Под землей" -> mc.player.getY() < 58 || light <= thresh;
                default -> lastActiveState ? (light <= thresh + 2) : (light <= thresh);
            };

            if (conditionMet) {
                debounceTicks = 0;
                lastActiveState = true;
            } else {
                debounceTicks++;
                if (debounceTicks > 20) {
                    lastActiveState = false;
                }
            }
            active = lastActiveState;
        }

        if (!active) {
            if (appliedNightVision) {
                mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                appliedNightVision = false;
            }
            if (mc.options != null && savedGamma >= 0.0) {
                mc.options.getGamma().setValue(savedGamma);
                savedGamma = -1.0;
            }
            return;
        }

        if ("Ночное зрение".equals(mode.getValue())) {
            if (mc.options != null && savedGamma >= 0.0) {
                mc.options.getGamma().setValue(savedGamma);
                savedGamma = -1.0;
            }
            StatusEffectInstance current = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < 260) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 420, 0, false, false, false));
                appliedNightVision = true;
            }
        } else {
            if (appliedNightVision) {
                mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                appliedNightVision = false;
            }
            if (mc.options != null) {
                if (savedGamma < 0.0) {
                    savedGamma = mc.options.getGamma().getValue();
                }
                forceOptionGamma(mc.options.getGamma(), gammaLevel.asFloat());
            }
        }
    }

    @Override
    public String getDisplaySuffix() {
        return mode.getValue();
    }

    private static void forceOptionGamma(Object simpleOption, double target) {
        for (String name : new String[]{"value", "field_37868"}) {
            try {
                var field = simpleOption.getClass().getDeclaredField(name);
                field.setAccessible(true);
                field.set(simpleOption, target);
                return;
            } catch (Throwable ignored) {
            }
        }
        try {
            // In MC 1.21.11 SimpleOption, defaultValue (field_37866) precedes value (field_37868).
            java.lang.reflect.Field candidate = null;
            for (var f : simpleOption.getClass().getDeclaredFields()) {
                String fname = f.getName();
                if ("defaultValue".equals(fname) || "field_37866".equals(fname)) continue;
                if (f.getType() == Object.class || f.getType() == Double.class) {
                    f.setAccessible(true);
                    if (f.get(simpleOption) instanceof Double) {
                        candidate = f;
                    }
                }
            }
            if (candidate != null) {
                candidate.set(simpleOption, target);
            }
        } catch (Throwable ignored) {
        }
    }
}
