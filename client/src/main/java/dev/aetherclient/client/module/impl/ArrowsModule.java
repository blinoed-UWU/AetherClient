package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class ArrowsModule extends Module {
    public final BooleanSetting players = addSetting(new BooleanSetting("players", "Игроки", true).withEn("Players"));
    public final BooleanSetting hostiles = addSetting(new BooleanSetting("hostiles", "Враждебные", false).withEn("Hostiles"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", false).withEn("Through Walls"));
    public final NumberSetting radius = addSetting(new NumberSetting("radius", "Радиус круга", 74.0, 36.0, 150.0, 2.0).withEn("Circle Radius"));
    public final NumberSetting size = addSetting(new NumberSetting("size", "Размер", 9.0, 5.0, 16.0, 0.5).withEn("Arrow Size"));
    public final NumberSetting maxDistance = addSetting(new NumberSetting("max_distance", "Дистанция", 80.0, 16.0, 160.0, 4.0).withEn("Max Distance"));

    public ArrowsModule() {
        super("arrows", "Стрелки", "Тактические индикаторы игроков вокруг прицела", Category.VISUAL);
        setEn("Arrows", "Tactical off-screen entity indicators around crosshair");
    }
}
