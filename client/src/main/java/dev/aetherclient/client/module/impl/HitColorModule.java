package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class HitColorModule extends Module {
    public final ModeSetting targetType = addSetting(new ModeSetting("target_type", "Цели", "Все", "Все", "Только игроки", "Только цель", "Только мобы").withEn("Targets"));
    public final ModeSetting style = addSetting(new ModeSetting("style", "Стиль", "3D Модель", "3D Модель").withEn("Style"));
    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Статика", "Статика", "Радуга", "Тема", "Градиент").withEn("Palette"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет вспышки", 0xFFFF2E4C).withEn("Hit Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));
    public final NumberSetting duration = addSetting(new NumberSetting("duration", "Длительность (с)", 0.35, 0.1, 1.0, 0.05).withEn("Duration (s)"));
    public final NumberSetting intensity = addSetting(new NumberSetting("intensity", "Яркость", 0.80, 0.15, 1.0, 0.05).withEn("Intensity"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", false).withEn("Through Walls"));

    public HitColorModule() {
        super("hit_color", "Цвет урона", "Кастомная вспышка и свечение сущности при получении урона", Category.COMBAT);
        setEn("Hit Color", "Custom colored flash and glow on entities when damaged");
    }
}
