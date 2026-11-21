package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class ArrowsModule extends Module {
    public final ModeSetting arrowStyle = addSetting(
            new ModeSetting("style", "Форма стрелок", "Неоновый треугольник",
                    "Неоновый треугольник", "Шеврон", "Точка с кольцом", "Свечение")
                    .withEn("Arrow Shape")
    );
    public final ModeSetting colorMode = addSetting(
            new ModeSetting("color_mode", "Режим окраски", "По дистанции",
                    "По дистанции", "Статика", "Радуга", "Команда / Друзья")
                    .withEn("Color Mode")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет стрелок", 0xFF7C5CFF).withEn("Arrow Color"));
    public final ColorSetting friendColor = addSetting(new ColorSetting("friend_col", "Цвет стрелок для друзей", 0xFF10B981).withEn("Friend Color"));
    public final BooleanSetting players = addSetting(new BooleanSetting("players", "Игроки", true).withEn("Players"));
    public final BooleanSetting hostiles = addSetting(new BooleanSetting("hostiles", "Враждебные", false).withEn("Hostiles"));
    public final BooleanSetting passives = addSetting(new BooleanSetting("passives", "Мирные", false).withEn("Passives"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", false).withEn("Through Walls"));
    public final NumberSetting radius = addSetting(new NumberSetting("radius", "Радиус круга", 74.0, 36.0, 180.0, 2.0).withEn("Circle Radius"));
    public final NumberSetting size = addSetting(new NumberSetting("size", "Размер стрелок", 9.0, 5.0, 20.0, 0.5).withEn("Arrow Size"));
    public final NumberSetting maxDistance = addSetting(new NumberSetting("max_distance", "Макс. дистанция", 96.0, 16.0, 256.0, 8.0).withEn("Max Distance"));
    public final BooleanSetting pulse = addSetting(new BooleanSetting("pulse", "Пульсация при сближении", true).withEn("Pulse on Approach"));
    public final BooleanSetting showDistanceText = addSetting(new BooleanSetting("show_dist", "Показывать метры", true).withEn("Show Distance"));
    public final BooleanSetting showHealthBar = addSetting(new BooleanSetting("show_hp", "Мини-полоска HP цели", true).withEn("Target Health Bar"));

    public ArrowsModule() {
        super("arrows", "Стрелки", "Тактические круговые 360° индикаторы направления к игрокам вокруг прицела", Category.VISUAL);
        setEn("Arrows", "Tactical off-screen 360 radar arrows around crosshair with distance and health indicators");
    }
}
