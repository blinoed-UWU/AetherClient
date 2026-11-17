package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class HoleEspModule extends Module {
    public final ModeSetting shape = addSetting(new ModeSetting("shape", "Форма", "Коробка", "Коробка", "Углы", "Свечение", "Скобка").withEn("Shape"));
    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Статика", "Статика", "Радуга", "Тема", "Градиент").withEn("Palette"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет", 0xFF5CE092).withEn("Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));
    public final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Толщина", 1.7, 0.6, 3.5, 0.1).withEn("Thickness"));
    public final NumberSetting radius = addSetting(new NumberSetting("radius", "Радиус поиска", 12.0, 5.0, 32.0, 1.0).withEn("Search Radius"));
    public final BooleanSetting bedrockOnly = addSetting(new BooleanSetting("bedrock", "Только бедрок", false).withEn("Bedrock Only"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", false).withEn("Through Walls"));

    public HoleEspModule() {
        super("hole_esp", "Hole ESP", "Подсветка безопасных 2x2-ям (бедрок/обсидиан)", Category.VISUAL);
        setEn("Hole ESP", "Highlight safe 2x2 bedrock/obsidian holes");
    }
}
