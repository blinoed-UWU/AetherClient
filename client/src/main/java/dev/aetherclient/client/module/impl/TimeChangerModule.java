package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

public final class TimeChangerModule extends Module {
    public final ModeSetting preset = addSetting(
            new ModeSetting("preset", "Пресет", "Кастом", "Кастом", "День", "Полдень", "Закат", "Ночь", "Полночь").withEn("Preset")
    );
    public final BooleanSetting customTime = addSetting(new BooleanSetting("custom_time", "Своё время", true).withEn("Custom Time"));
    public final NumberSetting timeOfDay = addSetting(
            new NumberSetting("time_of_day", "Время суток", 12650.0, 0.0, 24000.0, 100.0)
                    .<NumberSetting>withEn("Time of Day").visibleIf(() -> "Кастом".equals(preset.getValue()))
    );
    public final NumberSetting timeSpeed = addSetting(
            new NumberSetting("time_speed", "Скорость времени", 0.0, 0.0, 10.0, 0.5)
                    .<NumberSetting>withEn("Time Speed").visibleIf(() -> "Кастом".equals(preset.getValue()))
    );
    public final BooleanSetting skyboxTint = addSetting(new BooleanSetting("sky_tint", "Тонировка неба", false).withEn("Skybox Tint"));
    public final ColorSetting topColor = addSetting(
            new ColorSetting("top_color", "Верхний цвет", 0xFF4A3BFF).<ColorSetting>withEn("Top Color").visibleIf(skyboxTint::isEnabled)
    );
    public final ColorSetting bottomColor = addSetting(
            new ColorSetting("bottom_color", "Нижний цвет", 0xFF0B0B2B).<ColorSetting>withEn("Bottom Color").visibleIf(skyboxTint::isEnabled)
    );
    public final NumberSetting tintOpacity = addSetting(
            new NumberSetting("tint_opacity", "Плотность тона", 0.35, 0.05, 0.85, 0.05).<NumberSetting>withEn("Tint Opacity").visibleIf(skyboxTint::isEnabled)
    );

    private long accumulatedTime = -1L;

    public TimeChangerModule() {
        super("time_changer", "Время суток", "Пресеты времени (День, Закат, Ночь) и тонировка неба", Category.VISUAL);
        setEn("Time Changer", "Time of day presets (Day, Sunset, Night) and gradient skybox tint");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled() || mc.world == null) return;
        long targetTicks;
        String p = preset.getValue();
        targetTicks = switch (p) {
            case "День" -> 1000L;
            case "Полдень" -> 6000L;
            case "Закат" -> 12500L;
            case "Ночь" -> 14000L;
            case "Полночь" -> 18000L;
            default -> {
                if (accumulatedTime < 0L) {
                    accumulatedTime = timeOfDay.getValue().longValue();
                }
                if (timeSpeed.getValue() > 0.01) {
                    accumulatedTime += Math.round(timeSpeed.getValue());
                } else {
                    accumulatedTime = timeOfDay.getValue().longValue();
                }
                if (accumulatedTime >= 24000L) accumulatedTime -= 24000L;
                yield accumulatedTime;
            }
        };
        mc.world.setTime(mc.world.getTime(), targetTicks, false);
    }

    @Override
    protected void onDisable() {
        accumulatedTime = -1L;
    }

    @Override
    public String getDisplaySuffix() {
        return customTime.isEnabled() ? formatTime(timeOfDay.getValue()) : "";
    }

    private static String formatTime(double ticks) {
        long t = ((long) ticks) % 24000L;
        long hours = ((t / 1000L) + 6L) % 24L;
        long minutes = (t % 1000L) * 60L / 1000L;
        return String.format(java.util.Locale.ROOT, "%02d:%02d", hours, minutes);
    }
}
