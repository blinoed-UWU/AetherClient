package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class TrailsModule extends Module {
    public final ModeSetting style = addSetting(
            new ModeSetting("style", "Стиль", "Стекло", "Стекло", "Лента", "Неон", "Точки").withEn("Style")
    );
    public final ModeSetting palette = addSetting(
            new ModeSetting("palette", "Палитра", "Тема", "Тема", "Статика", "Радуга", "Градиент").withEn("Palette")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет", 0xFF7C5CFF).withEn("Color"));
    public final NumberSetting length = addSetting(new NumberSetting("length", "Длина шлейфа", 32.0, 10.0, 100.0, 2.0).withEn("Trail Length"));
    public final NumberSetting width = addSetting(new NumberSetting("width", "Толщина", 2.6, 1.0, 6.0, 0.2).withEn("Width"));
    public final NumberSetting fadeTime = addSetting(new NumberSetting("fade_time", "Время затухания (с)", 1.2, 0.3, 3.5, 0.1).withEn("Fade Time (s)"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Неоновый ореол", true).withEn("Glow"));

    public TrailsModule() {
        super("trails", "Шлейф", "Плавная неоновая линия за игроком в стиле Liquid Glass", Category.VISUAL);
        setEn("Trails", "Smooth glowing ribbon trailing behind player in Liquid Glass style");
    }
}
