package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

/**
 * 3D China Hat rendered above the player's head.
 * Features customizable radius, height, vertical offset, double layers,
 * tip/base gradient, glowing rim, and rainbow animations.
 */
public final class ChinaHatModule extends Module {
    public final NumberSetting radius = addSetting(new NumberSetting("radius", "Радиус шляпы", 0.65, 0.3, 1.4, 0.05).withEn("Hat Radius"));
    public final NumberSetting height = addSetting(new NumberSetting("height", "Высота конуса", 0.28, 0.1, 0.7, 0.02).withEn("Cone Height"));
    public final NumberSetting offsetY = addSetting(new NumberSetting("offset_y", "Высота над головой", 0.0, -0.2, 0.6, 0.05).withEn("Vertical Offset"));
    public final BooleanSetting layers = addSetting(new BooleanSetting("layers", "3D Объём (2 слоя)", true).withEn("Double Layers"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет обода", 0xCC7C5CFF).withEn("Rim Color"));
    public final ColorSetting colorTip = addSetting(new ColorSetting("color_tip", "Цвет вершины", 0xEE38BDF8).withEn("Tip Color"));
    public final BooleanSetting rainbow = addSetting(new BooleanSetting("rainbow", "Радужный режим", false).withEn("Rainbow"));
    public final NumberSetting rainbowSpeed = addSetting(new NumberSetting("rainbow_speed", "Скорость радуги", 1.0, 0.2, 3.0, 0.1).withEn("Rainbow Speed"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Неоновое свечение", true).withEn("Glow"));
    public final NumberSetting segments = addSetting(new NumberSetting("segments", "Детализация", 28.0, 16.0, 48.0, 4.0).withEn("Segments"));
    public final BooleanSetting firstPerson = addSetting(new BooleanSetting("first_person", "Вид от 1-го лица", false).withEn("First Person"));

    public ChinaHatModule() {
        super("china_hat", "Китайская шляпа", "3D коническая шляпка над головой с градиентом, слоями и неоном", Category.VISUAL);
        setEn("China Hat", "3D conical hat above head with gradient, double layers and glow");
    }
}
