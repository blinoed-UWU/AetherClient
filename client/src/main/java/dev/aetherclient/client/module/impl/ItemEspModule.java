package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class ItemEspModule extends Module {
    public final ModeSetting shape = addSetting(new ModeSetting("shape", "Форма", "Свечение", "Коробка", "Углы", "Свечение", "Скобка").withEn("Shape"));
    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Статика", "Статика", "Радуга", "Тема", "Градиент").withEn("Palette"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет", 0xFF10B981).withEn("Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));
    public final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Толщина", 1.5, 0.6, 3.5, 0.1).withEn("Thickness"));
    public final NumberSetting maxDistance = addSetting(new NumberSetting("max_distance", "Дистанция", 48.0, 12.0, 120.0, 4.0).withEn("Max Distance"));
    public final BooleanSetting showName = addSetting(new BooleanSetting("show_name", "Название предмета", true).withEn("Show Name"));
    public final BooleanSetting showCount = addSetting(new BooleanSetting("show_count", "Количество", true).withEn("Show Count"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Свечение", true).withEn("Glow"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", false).withEn("Through Walls"));

    public ItemEspModule() {
        super("item_esp", "ESP предметов", "Подсветка выпавших предметов с названием и количеством", Category.VISUAL);
        setEn("Item ESP", "Highlight dropped items with name, count and glow");
    }
}
