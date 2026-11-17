package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class HpCounterModule extends Module {
    public final ModeSetting palette = addSetting(
            new ModeSetting("palette", "Палитра", "Тема", "Тема", "Статика", "Радуга", "Градиент").withEn("Palette")
    );
    public final ColorSetting damageColor = addSetting(new ColorSetting("dmg_color", "Цвет урона", 0xFFFF4D4D).withEn("Damage Color"));
    public final ColorSetting healColor = addSetting(new ColorSetting("heal_color", "Цвет лечения", 0xFF4ADE80).withEn("Heal Color"));
    public final NumberSetting duration = addSetting(
            new NumberSetting("duration", "Время жизни (с)", 0.95, 0.4, 2.5, 0.05).withEn("Duration (s)")
    );
    public final NumberSetting scale = addSetting(
            new NumberSetting("scale", "Размер текста", 1.0, 0.6, 2.2, 0.05).withEn("Text Size")
    );
    public final BooleanSetting showPlusMinus = addSetting(
            new BooleanSetting("show_sign", "Знаки + и -", true).withEn("Show Signs")
    );
    public final BooleanSetting throughWalls = addSetting(
            new BooleanSetting("through_walls", "Сквозь стены", false).withEn("Through Walls")
    );

    public HpCounterModule() {
        super("hp_counter", "Счетчик урона", "Всплывающие цифры урона и лечения в 3D пространстве", Category.VISUAL);
        setEn("HP Counter", "Floating damage and healing numbers anchored to entities");
    }
}
