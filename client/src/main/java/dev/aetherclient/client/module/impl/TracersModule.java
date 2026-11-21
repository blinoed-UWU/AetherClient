package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class TracersModule extends Module {
    public final ModeSetting origin = addSetting(
            new ModeSetting("origin", "Начало линий", "Центр",
                    "Центр", "Низ", "Прицел", "Голова")
                    .withEn("Origin")
    );
    public final ModeSetting colorMode = addSetting(
            new ModeSetting("color_mode", "Режим окраски", "По дистанции",
                    "По дистанции", "Статика", "Радуга", "По здоровью", "Команда / Друзья")
                    .withEn("Color Mode")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Статический цвет", 0xFFFF3366).withEn("Static Color"));
    public final ColorSetting closeColor = addSetting(new ColorSetting("close_col", "Цвет близких целей", 0xFFFF2255).withEn("Close Color"));
    public final ColorSetting farColor = addSetting(new ColorSetting("far_col", "Цвет дальних целей", 0xFF10B981).withEn("Far Color"));
    public final ColorSetting friendColor = addSetting(new ColorSetting("friend_col", "Цвет друзей", 0xFF38BDF8).withEn("Friend Color"));
    public final BooleanSetting players = addSetting(new BooleanSetting("players", "Игроки", true).withEn("Players"));
    public final BooleanSetting hostiles = addSetting(new BooleanSetting("hostiles", "Враждебные", false).withEn("Hostiles"));
    public final BooleanSetting passives = addSetting(new BooleanSetting("passives", "Мирные", false).withEn("Passives"));
    public final BooleanSetting items = addSetting(new BooleanSetting("items", "Выпавшие предметы", false).withEn("Dropped Items"));
    public final BooleanSetting stemHead = addSetting(new BooleanSetting("stem_head", "Наводить в голову цели", false).withEn("Target Head"));
    public final BooleanSetting distanceText = addSetting(new BooleanSetting("dist_text", "Показывать метры", true).withEn("Show Distance"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", false).withEn("Through Walls"));
    public final NumberSetting width = addSetting(new NumberSetting("width", "Толщина линий", 1.4, 0.5, 4.0, 0.1).withEn("Thickness"));
    public final NumberSetting maxDistance = addSetting(new NumberSetting("max_distance", "Макс. дистанция", 120.0, 16.0, 256.0, 8.0).withEn("Max Distance"));

    public TracersModule() {
        super("tracers", "Линии", "Трассировочные линии направления к игрокам, мобам и предметам с цветовым градиентом", Category.VISUAL);
        setEn("Tracers", "Direction tracer lines to players, mobs and items with distance color gradient");
    }
}
