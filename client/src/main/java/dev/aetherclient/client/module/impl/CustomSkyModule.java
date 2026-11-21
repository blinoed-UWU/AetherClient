package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class CustomSkyModule extends Module {
    public final ModeSetting preset = addSetting(
            new ModeSetting("preset", "Пресет неба", "Галактика",
                    "Галактика", "Закат", "Киберпанк", "Бездна", "Северное сияние", "Неоновый рассвет", "Кастом")
                    .withEn("Preset")
    );
    public final ColorSetting topColor = addSetting(new ColorSetting("top_col", "Верхний цвет", 0xFF0D0B24).withEn("Top Color"));
    public final ColorSetting botColor = addSetting(new ColorSetting("bot_col", "Нижний цвет", 0xFF2A164D).withEn("Bottom Color"));
    public final NumberSetting stars = addSetting(new NumberSetting("stars", "Количество звёзд", 75.0, 10.0, 250.0, 5.0).withEn("Stars Count"));
    public final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Плотность неба", 0.85, 0.2, 1.0, 0.05).withEn("Opacity"));
    public final BooleanSetting auroraWave = addSetting(new BooleanSetting("aurora", "Северное сияние", true).withEn("Aurora Waves"));
    public final NumberSetting auroraSpeed = addSetting(new NumberSetting("aurora_spd", "Скорость сияния", 1.2, 0.2, 4.0, 0.1).withEn("Aurora Speed"));
    public final BooleanSetting shootingStars = addSetting(new BooleanSetting("shooting_stars", "Падающие звёзды / кометы", true).withEn("Shooting Stars"));
    public final NumberSetting starTwinkleSpeed = addSetting(new NumberSetting("twinkle_spd", "Мерцание звёзд", 1.5, 0.5, 5.0, 0.25).withEn("Star Twinkle Speed"));
    public final BooleanSetting customSunMoon = addSetting(new BooleanSetting("sun_moon", "Стилизация солнца и луны", true).withEn("Stylize Sun & Moon"));
    public final NumberSetting sunMoonScale = addSetting(new NumberSetting("sun_moon_scale", "Масштаб светил", 1.0, 0.5, 3.0, 0.1).withEn("Sun & Moon Scale"));

    public CustomSkyModule() {
        super("custom_sky", "Кастомное небо", "Атмосферные космические градиенты, северное сияние, кометы и мерцающие звезды", Category.VISUAL);
        setEn("Custom Sky", "Atmospheric celestial gradients, auroras, shooting stars and twinkling cosmos");
    }

    public int getTopColor(float globalTime) {
        String p = preset.getValue();
        return switch (p) {
            case "Закат" -> 0xFF311432;
            case "Киберпанк" -> 0xFF14052B;
            case "Бездна" -> 0xFF05050A;
            case "Северное сияние" -> 0xFF021B1A;
            case "Неоновый рассвет" -> 0xFF1E1035;
            case "Кастом" -> topColor.getValue();
            default -> 0xFF0B0E23; // "Галактика"
        };
    }

    public int getBottomColor(float globalTime) {
        String p = preset.getValue();
        return switch (p) {
            case "Закат" -> 0xFFFF6E40;
            case "Киберпанк" -> 0xFFEC4899;
            case "Бездна" -> 0xFF1E1035;
            case "Северное сияние" -> 0xFF10B981;
            case "Неоновый рассвет" -> 0xFFFF007F;
            case "Кастом" -> botColor.getValue();
            default -> 0xFF4F46E5; // "Галактика"
        };
    }
}
