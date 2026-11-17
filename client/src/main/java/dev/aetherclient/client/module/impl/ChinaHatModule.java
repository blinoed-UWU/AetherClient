package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

/**
 * 3D China Hat rendered above the player's head.
 */
public final class ChinaHatModule extends Module {
    public final NumberSetting radius = new NumberSetting("radius", "Радиус шляпы", 0.65, 0.3, 1.2, 0.05);
    public final NumberSetting height = new NumberSetting("height", "Высота конуса", 0.28, 0.1, 0.6, 0.02);
    public final ColorSetting color = new ColorSetting("color", "Цвет шляпы", 0xCC7C5CFF);
    public final BooleanSetting rainbow = new BooleanSetting("rainbow", "Радужный градиент", false);
    public final BooleanSetting firstPerson = new BooleanSetting("first_person", "Отображать от 1-го лица", false);

    public ChinaHatModule() {
        super("china_hat", "Китайская шляпа", "3D коническая шляпка над головой персонажа", Category.VISUAL);
        addSetting(radius);
        addSetting(height);
        addSetting(color);
        addSetting(rainbow);
        addSetting(firstPerson);
    }
}
