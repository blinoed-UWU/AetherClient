package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class CustomTabListModule extends Module {
    public final ModeSetting palette = addSetting(
            new ModeSetting("palette", "Палитра", "Тема",
                    "Тема", "Статика", "Радуга", "Градиент", "Киберпанк", "Стекло")
                    .withEn("Palette")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Акцентный цвет", 0xFF7C5CFF).withEn("Accent Color"));
    public final ColorSetting friendColor = addSetting(new ColorSetting("friend_col", "Цвет друзей", 0xFF10B981).withEn("Friend Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));
    public final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Плотность стекла", 0.60, 0.20, 0.95, 0.02).withEn("Glass Opacity"));
    public final NumberSetting columns = addSetting(new NumberSetting("columns", "Колонки", 3.0, 1.0, 6.0, 1.0).withEn("Columns"));
    public final BooleanSetting showPing = addSetting(new BooleanSetting("ping", "Отображать пинг", true).withEn("Show Ping"));
    public final BooleanSetting pingAsNumber = addSetting(new BooleanSetting("ping_number", "Пинг числом (мс)", true).withEn("Numeric Ping"));
    public final BooleanSetting showHeader = addSetting(new BooleanSetting("header", "Шапка и подвал сервера", true).withEn("Show Header & Footer"));
    public final BooleanSetting showHeads = addSetting(new BooleanSetting("heads", "Аватарки игроков", true).withEn("Show Player Heads"));
    public final BooleanSetting highlightFriends = addSetting(new BooleanSetting("highlight_friends", "Выделять друзей", true).withEn("Highlight Friends"));
    public final NumberSetting cornerRadius = addSetting(new NumberSetting("radius", "Радиус скругления", 8.0, 2.0, 20.0, 1.0).withEn("Corner Radius"));
    public final BooleanSetting blur = addSetting(new BooleanSetting("blur", "Размытие фона (Kawase)", true).withEn("Blur Background"));

    public CustomTabListModule() {
        super("tablist", "Таб-лист", "Стилизованный список игроков в стиле Material 3 Liquid Glass с пингом числом и головами", Category.HUD);
        setEn("Tab List", "Material 3 styled player list with numeric ping, player heads and friend recognition");
    }
}
