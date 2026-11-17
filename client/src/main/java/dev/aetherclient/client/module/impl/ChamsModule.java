package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class ChamsModule extends Module {
    public final ModeSetting shape = addSetting(new ModeSetting("shape", "Форма", "Стекло", "Стекло", "Свечение", "Силуэт", "Коробка", "Углы", "Скобка").withEn("Shape"));
    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Статика", "Статика", "Радуга", "Тема", "Градиент").withEn("Palette"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет", 0xFF7C5CFF).withEn("Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));
    public final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Плотность заливки", 0.32, 0.05, 0.85, 0.01).withEn("Fill Opacity"));
    public final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Толщина линии", 1.8, 0.6, 4.0, 0.1).withEn("Line Thickness"));
    public final NumberSetting maxDistance = addSetting(new NumberSetting("max_distance", "Дистанция", 64.0, 16.0, 160.0, 4.0).withEn("Max Distance"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", false).withEn("Through Walls"));
    public final BooleanSetting colorByVis = addSetting(new BooleanSetting("color_vis", "Цвет по видимости", true).withEn("Color by Visibility"));
    public final BooleanSetting players = addSetting(new BooleanSetting("players", "Игроки", true).withEn("Players"));
    public final BooleanSetting hostiles = addSetting(new BooleanSetting("hostiles", "Враждебные", true).withEn("Hostiles"));
    public final BooleanSetting passives = addSetting(new BooleanSetting("passives", "Мирные", false).withEn("Passives"));

    public ChamsModule() {
        super("chams", "Chams", "Эстетичная силуэтная подсветка сущностей в стиле Liquid Glass", Category.VISUAL);
        setEn("Chams", "Aesthetic silhouette entity highlight in Liquid Glass style");
    }
}
