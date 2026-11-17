package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class EspModule extends Module {
    public final ModeSetting boxStyle = addSetting(new ModeSetting("style", "Стиль бокса", "Стекло", "Стекло", "Углы", "Рамка", "Точки", "Скобка").withEn("Box Style"));
    public final BooleanSetting players = addSetting(new BooleanSetting("players", "Игроки", true).withEn("Players"));
    public final BooleanSetting hostiles = addSetting(new BooleanSetting("hostiles", "Враждебные", true).withEn("Hostiles"));
    public final BooleanSetting passives = addSetting(new BooleanSetting("passives", "Мирные", false).withEn("Passives"));
    public final BooleanSetting items = addSetting(new BooleanSetting("items", "Предметы", true).withEn("Items"));
    public final BooleanSetting healthBar = addSetting(new BooleanSetting("health_bar", "Полоса HP", true).withEn("Health Bar"));
    public final BooleanSetting nametags = addSetting(new BooleanSetting("nametags", "Имена и дистанция", true).withEn("Names & Distance"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", false).withEn("Through Walls"));
    public final BooleanSetting colorByVisibility = addSetting(new BooleanSetting("color_vis", "Цвет по видимости", true).withEn("Color by Visibility"));
    public final NumberSetting maxDistance = addSetting(new NumberSetting("max_distance", "Дистанция", 256.0, 16.0, 512.0, 8.0).withEn("Max Distance"));

    public EspModule() {
        super("esp", "ESP 2D", "Подсветка игроков, мобов и предметов в стиле Liquid Glass", Category.VISUAL);
        setEn("2D ESP", "Liquid Glass entity and item highlights");
    }
}
