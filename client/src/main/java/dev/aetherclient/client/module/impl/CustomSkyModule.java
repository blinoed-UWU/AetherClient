package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class CustomSkyModule extends Module {
    public final ModeSetting preset = addSetting(
            new ModeSetting("preset", "Пресет неба", "Галактика", "Галактика", "Закат", "Киберпанк", "Бездна", "Северное сияние", "Кастом").withEn("Preset")
    );
    public final ColorSetting topColor = addSetting(new ColorSetting("top_col", "Верхний цвет", 0xFF0D0B24).withEn("Top Color"));
    public final ColorSetting botColor = addSetting(new ColorSetting("bot_col", "Нижний цвет", 0xFF2A164D).withEn("Bottom Color"));
    public final NumberSetting stars = addSetting(new NumberSetting("stars", "Звезды", 60.0, 10.0, 150.0, 5.0).withEn("Stars Count"));
    public final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Плотность", 0.85, 0.2, 1.0, 0.05).withEn("Opacity"));
    public final BooleanSetting auroraWave = addSetting(new BooleanSetting("aurora", "Эффект сияния", true).withEn("Aurora Waves"));

    public CustomSkyModule() {
        super("custom_sky", "Кастомное небо", "Атмосферные космические градиенты, сияние и звезды на горизонте", Category.VISUAL);
        setEn("Custom Sky", "Atmospheric celestial gradients, auroras and stars");
    }

    public int getTopColor(float globalTime) {
        String p = preset.getValue();
        return switch (p) {
            case "Закат" -> 0xFF311432;
            case "Киберпанк" -> 0xFF14052B;
            case "Бездна" -> 0xFF05050A;
            case "Северное сияние" -> 0xFF021B1A;
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
            case "Кастом" -> botColor.getValue();
            default -> 0xFF4F46E5; // "Галактика"
        };
    }
}
