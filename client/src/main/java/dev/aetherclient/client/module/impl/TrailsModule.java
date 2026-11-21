package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class TrailsModule extends Module {
    public final ModeSetting style = addSetting(
            new ModeSetting("style", "Стиль шлейфа", "Стекло",
                    "Стекло", "Лента", "Неон", "Точки", "Двойная лента", "Квантовый след", "Электрическая дуга")
                    .withEn("Trail Style")
    );
    public final ModeSetting palette = addSetting(
            new ModeSetting("palette", "Палитра", "Тема",
                    "Тема", "Статика", "Радуга", "Градиент", "Огонь", "Неоновый закат")
                    .withEn("Palette")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Основной цвет", 0xFF7C5CFF).withEn("Primary Color"));
    public final ColorSetting secondaryColor = addSetting(new ColorSetting("sec_color", "Второй цвет (Градиент)", 0xFF38BDF8).withEn("Secondary Color"));
    public final NumberSetting length = addSetting(new NumberSetting("length", "Длина шлейфа", 36.0, 10.0, 120.0, 2.0).withEn("Trail Length"));
    public final NumberSetting width = addSetting(new NumberSetting("width", "Толщина шлейфа", 2.8, 0.8, 7.0, 0.2).withEn("Width"));
    public final NumberSetting fadeTime = addSetting(new NumberSetting("fade_time", "Время затухания (с)", 1.2, 0.2, 4.0, 0.1).withEn("Fade Time (s)"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Неоновый ореол", true).withEn("Glow"));
    public final BooleanSetting interpolate = addSetting(new BooleanSetting("interp", "Плавная сплайн-кривая", true).withEn("Smooth Spline"));
    public final NumberSetting yOffset = addSetting(new NumberSetting("y_offset", "Смещение по высоте", 0.1, -0.4, 1.4, 0.05).withEn("Height Offset"));

    public TrailsModule() {
        super("trails", "Шлейф", "Плавная неоновая 3D лента и световой след за игроком в стиле Liquid Glass", Category.VISUAL);
        setEn("Trails", "Smooth glowing 3D ribbon and light trail behind player in Liquid Glass style");
    }
}
