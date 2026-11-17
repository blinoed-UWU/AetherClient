package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class CustomTabListModule extends Module {
    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Тема", "Статика", "Радуга", "Тема", "Градиент").withEn("Palette"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет", 0xFF7C5CFF).withEn("Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));
    public final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Плотность стекла", 0.60, 0.25, 0.92, 0.02).withEn("Glass Opacity"));
    public final NumberSetting columns = addSetting(new NumberSetting("columns", "Колонки", 3.0, 2.0, 6.0, 1.0).withEn("Columns"));
    public final BooleanSetting showPing = addSetting(new BooleanSetting("ping", "Пинг", true).withEn("Show Ping"));
    public final BooleanSetting showHeader = addSetting(new BooleanSetting("header", "Заголовок", true).withEn("Show Header"));

    public CustomTabListModule() {
        super("tablist", "Таб-лист", "Стилизованный список игроков в духе Material 3", Category.HUD);
        setEn("Tab List", "Material 3 styled player list");
    }
}
