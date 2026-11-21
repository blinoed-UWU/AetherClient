package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

public final class AmbienceModule extends Module {
    public final BooleanSetting customTime = addSetting(new BooleanSetting("custom_time", "Своё время суток", true).withEn("Custom Time"));
    public final ModeSetting timePreset = addSetting(
            new ModeSetting("time_preset", "Пресет времени", "Закат",
                    "День", "Полдень", "Золотой час", "Закат", "Ночь", "Полночь", "Кастом")
                    .<ModeSetting>withEn("Time Preset").visibleIf(customTime::isEnabled)
    );
    public final NumberSetting customHour = addSetting(
            new NumberSetting("custom_hour", "Тики времени (0-24k)", 12650.0, 0.0, 24000.0, 200.0)
                    .<NumberSetting>withEn("Custom Time Ticks").visibleIf(() -> customTime.isEnabled() && "Кастом".equals(timePreset.getValue()))
    );
    public final BooleanSetting customFog = addSetting(new BooleanSetting("custom_fog", "Кастомный туман", true).withEn("Custom Fog"));
    public final ColorSetting fogColor = addSetting(
            new ColorSetting("fog_col", "Цвет тумана", 0xFF1E1B4B)
                    .<ColorSetting>withEn("Fog Color").visibleIf(customFog::isEnabled)
    );
    public final NumberSetting fogDistance = addSetting(
            new NumberSetting("fog_dist", "Дистанция тумана", 1.0, 0.2, 3.0, 0.1)
                    .<NumberSetting>withEn("Fog Distance Scale").visibleIf(customFog::isEnabled)
    );
    public final BooleanSetting vignette = addSetting(new BooleanSetting("vignette", "Виньетка экрана", true).withEn("Screen Vignette"));
    public final NumberSetting vignetteAlpha = addSetting(
            new NumberSetting("vignette_alpha", "Сила виньетки", 0.35, 0.05, 0.85, 0.05)
                    .<NumberSetting>withEn("Vignette Strength").visibleIf(vignette::isEnabled)
    );
    public final ColorSetting vignetteColor = addSetting(
            new ColorSetting("vignette_col", "Цвет виньетки", 0xFF0A0A14)
                    .<ColorSetting>withEn("Vignette Color").visibleIf(vignette::isEnabled)
    );
    public final BooleanSetting customLightmap = addSetting(new BooleanSetting("custom_lightmap", "Тонировка освещения мира", true).withEn("World Lightmap Tint"));
    public final ModeSetting lightmapTone = addSetting(
            new ModeSetting("lightmap_tone", "Цветовой тон мира", "Холодный / Лунный",
                    "Холодный / Лунный", "Теплый / Закатный", "Киберпанк", "Сепия", "Черно-белый")
                    .<ModeSetting>withEn("Lightmap Tone").visibleIf(customLightmap::isEnabled)
    );

    public AmbienceModule() {
        super("ambience", "Атмосфера", "Настройка времени суток, кастомного тумана, освещения мира и мягкой виньетки экрана", Category.VISUAL);
        setEn("Ambience", "Custom world time of day, atmospheric fog, world lightmap tone and screen vignette");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled() || mc.world == null || !customTime.isEnabled()) return;
        long tod = switch (timePreset.getValue()) {
            case "День" -> 1000L;
            case "Полдень" -> 6000L;
            case "Золотой час" -> 11500L;
            case "Закат" -> 12650L;
            case "Ночь" -> 15000L;
            case "Полночь" -> 18000L;
            case "Кастом" -> customHour.getValue().longValue();
            default -> 12650L;
        };
        mc.world.setTime(mc.world.getTime(), tod, false);
    }
}
