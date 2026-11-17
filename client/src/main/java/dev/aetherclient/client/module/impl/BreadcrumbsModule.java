package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class BreadcrumbsModule extends Module {
    public final ModeSetting style = addSetting(new ModeSetting("style", "Стиль", "Лента", "Лента", "Точки", "Неоновый шлейф", "Светящиеся искры", "Квантовый след").withEn("Style"));
    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Градиент", "Статика", "Радуга", "Тема", "Градиент").withEn("Palette"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет", 0xFF38BDF8).withEn("Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));
    public final NumberSetting width = addSetting(new NumberSetting("width", "Толщина", 2.4, 1.0, 5.0, 0.2).withEn("Width"));
    public final NumberSetting maxPoints = addSetting(new NumberSetting("max_points", "Точек следа", 60.0, 20.0, 200.0, 4.0).withEn("Trail Points"));
    public final NumberSetting fadeSeconds = addSetting(new NumberSetting("fade", "Затухание (с)", 4.0, 2.0, 10.0, 0.5).withEn("Fade (s)"));

    public BreadcrumbsModule() {
        super("breadcrumbs", "Хлебные крошки", "Неоновая 3D-лента следов движения с плавным затуханием", Category.VISUAL);
        setEn("Breadcrumbs", "Neon 3D movement trail ribbon with smooth fading");
    }
}
