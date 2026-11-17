package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class ItemRadiusModule extends Module {
    public final ModeSetting target = addSetting(new ModeSetting("target", "Цель", "Оба", "Игрок", "Предметы", "Оба").withEn("Target"));
    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Тема", "Тема", "Статика", "Радуга", "Градиент").withEn("Palette"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет", 0xFF64B5F6).withEn("Color"));
    public final NumberSetting radius = addSetting(new NumberSetting("radius", "Радиус", 3.0, 1.0, 8.0, 0.25).withEn("Radius"));
    public final BooleanSetting pulsate = addSetting(new BooleanSetting("pulsate", "Пульсация", true).withEn("Pulsate"));
    public final NumberSetting pulseSpeed = addSetting(new NumberSetting("pulse_speed", "Скорость пульсации", 2.0, 0.5, 5.0, 0.25).withEn("Pulse Speed"));
    public final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Толщина кольца", 1.8, 0.8, 4.0, 0.2).withEn("Ring Thickness"));
    public final BooleanSetting fill = addSetting(new BooleanSetting("fill", "Заливка круга", true).withEn("Circle Fill"));
    public final NumberSetting fillAlpha = addSetting(new NumberSetting("fill_alpha", "Прозрачность заливки", 25.0, 5.0, 80.0, 5.0).withEn("Fill Opacity"));
    public final BooleanSetting showItemCount = addSetting(new BooleanSetting("show_count", "Бейдж с количеством", true).withEn("Item Count Badge"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", false).withEn("Through Walls"));

    public ItemRadiusModule() {
        super("item_radius", "Радиус подбора", "Голографический круг радиуса подбора предметов и подсветка стаков", Category.VISUAL);
        setEn("Item Radius", "Holographic pickup radius circle and dropped item stack visualizer");
    }
}
