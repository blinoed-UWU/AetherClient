package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class BlockOverlayModule extends Module {
    public final ModeSetting shape = addSetting(new ModeSetting("shape", "Форма", "Углы", "Коробка", "Углы", "Свечение", "Скобка").withEn("Shape"));
    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Статика", "Статика", "Радуга", "Тема", "Градиент").withEn("Palette"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет", 0xFFFF5F6D).withEn("Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));
    public final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Толщина", 1.7, 0.5, 5.0, 0.1).withEn("Thickness"));
    public final BooleanSetting smoothTransition = addSetting(new BooleanSetting("smooth_transition", "Плавный переход", true).withEn("Smooth Transition"));
    public final NumberSetting transitionSpeed = addSetting(new NumberSetting("transition_speed", "Скорость перехода", 10.0, 1.0, 25.0, 1.0).withEn("Transition Speed"));
    public final NumberSetting cornerSize = addSetting(new NumberSetting("corner_size", "Размер углов", 0.35, 0.1, 1.0, 0.05).withEn("Corner Size"));
    public final BooleanSetting fill = addSetting(new BooleanSetting("fill", "Заливка", true).withEn("Fill"));
    public final NumberSetting fillAlpha = addSetting(new NumberSetting("fill_alpha", "Прозрачность заливки", 36.0, 5.0, 150.0, 5.0).withEn("Fill Opacity"));
    public final BooleanSetting showBreak = addSetting(new BooleanSetting("show_break", "Прогресс ломания", true).withEn("Mining Progress"));
    public final NumberSetting breakRadius = addSetting(new NumberSetting("break_radius", "Радиус кольца", 18.0, 8.0, 40.0, 1.0).withEn("Progress Ring Radius"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", false).withEn("Through Walls"));

    public BlockOverlayModule() {
        super("block_overlay", "Блок-оверлей", "Кастомная анимированная рамка целевого блока с индикатором ломания", Category.VISUAL);
        setEn("Block Overlay", "Custom animated targeted-block outline with mining progress indicator");
    }
}

