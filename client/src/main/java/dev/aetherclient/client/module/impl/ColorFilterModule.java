package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class ColorFilterModule extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("mode", "Фильтр", "Тёплый", "Тёплый", "Холодный", "Сепия", "Моно").withEn("Filter"));
    public final NumberSetting intensity = addSetting(new NumberSetting("intensity", "Сила", 0.35, 0.05, 0.85, 0.05).withEn("Intensity"));

    public ColorFilterModule() {
        super("color_filter", "Цветокоррекция", "Полноэкранная цветовая тонировка изображения", Category.VISUAL);
        setEn("Color Filter", "Full-screen color grading tint overlay");
    }

    public int filterArgb() {
        return switch (mode.getValue()) {
            case "Холодный" -> 0xFF3D7BFF;
            case "Сепия" -> 0xFFC89B4B;
            case "Моно" -> 0xFF9AA0B0;
            default -> 0xFFFF9E5A; // Тёплый
        };
    }

    @Override
    public String getDisplaySuffix() {
        return mode.getValue();
    }
}
