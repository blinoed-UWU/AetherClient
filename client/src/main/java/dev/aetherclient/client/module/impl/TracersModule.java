package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class TracersModule extends Module {
    public final ModeSetting origin = addSetting(new ModeSetting("origin", "Начало", "Центр", "Центр", "Низ").withEn("Origin"));
    public final BooleanSetting players = addSetting(new BooleanSetting("players", "Игроки", true).withEn("Players"));
    public final BooleanSetting hostiles = addSetting(new BooleanSetting("hostiles", "Враждебные", false).withEn("Hostiles"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", false).withEn("Through Walls"));
    public final NumberSetting width = addSetting(new NumberSetting("width", "Толщина", 1.3, 0.5, 3.0, 0.1).withEn("Thickness"));
    public final NumberSetting maxDistance = addSetting(new NumberSetting("max_distance", "Дистанция", 96.0, 16.0, 180.0, 8.0).withEn("Max Distance"));

    public TracersModule() {
        super("tracers", "Линии", "Линии направления к игрокам и мобам", Category.VISUAL);
        setEn("Tracers", "Smooth direction lines to players and mobs");
    }
}
