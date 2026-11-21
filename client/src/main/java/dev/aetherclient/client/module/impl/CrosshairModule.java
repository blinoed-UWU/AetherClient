package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class CrosshairModule extends Module {
    public final ModeSetting style = addSetting(
            new ModeSetting("style", "Стиль прицела", "Динамик",
                    "Динамик", "Крест", "Точка", "Кольцо", "Шеврон",
                    "T-образный", "Окружность с перекрестием", "Треугольник")
                    .withEn("Style")
    );
    public final ModeSetting palette = addSetting(
            new ModeSetting("palette", "Палитра", "Тема",
                    "Тема", "Статика", "Радуга", "Градиент")
                    .withEn("Palette")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Основной цвет", 0xFFFFFFFF).withEn("Color"));
    public final ColorSetting targetColor = addSetting(new ColorSetting("col_target", "Цвет на цели", 0xFFFF2255).withEn("Target Color"));
    public final ColorSetting hitmarkerColor = addSetting(new ColorSetting("col_hit", "Цвет хитмаркера", 0xFFFF4444).withEn("Hitmarker Color"));
    public final BooleanSetting centerDot = addSetting(new BooleanSetting("center_dot", "Точка в центре", true).withEn("Center Dot"));
    public final NumberSetting dotRadius = addSetting(new NumberSetting("dot_radius", "Радиус точки", 1.2, 0.5, 3.5, 0.1).withEn("Dot Radius"));
    public final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Толщина линий", 1.5, 0.5, 4.0, 0.1).withEn("Thickness"));
    public final NumberSetting size = addSetting(new NumberSetting("size", "Размер линий", 5.5, 2.5, 14.0, 0.5).withEn("Size"));
    public final NumberSetting gap = addSetting(new NumberSetting("gap", "Зазор от центра", 3.0, 0.5, 10.0, 0.5).withEn("Gap"));
    public final BooleanSetting outline = addSetting(new BooleanSetting("outline", "Черная обводка", true).withEn("Black Outline"));
    public final NumberSetting outlineWidth = addSetting(new NumberSetting("outline_w", "Толщина обводки", 1.0, 0.5, 2.5, 0.25).withEn("Outline Width"));
    public final BooleanSetting dynamicSpread = addSetting(new BooleanSetting("dynamic_spread", "Разброс при движении", true).withEn("Dynamic Spread on Move"));
    public final BooleanSetting zoomOnAttack = addSetting(new BooleanSetting("zoom_on_attack", "Расширение при ударе", true).withEn("Attack Recoil"));
    public final BooleanSetting hitmarker = addSetting(new BooleanSetting("hitmarker", "Хитмаркер при ударе", true).withEn("Hitmarker"));
    public final BooleanSetting cooldownArc = addSetting(new BooleanSetting("cooldown_arc", "Индикатор к/д удара", true).withEn("Cooldown Ring"));
    public final BooleanSetting targetHighlight = addSetting(new BooleanSetting("target_highlight", "Подсветка при наведении", true).withEn("Target Highlight"));
    public final NumberSetting rainbowSpeed = addSetting(new NumberSetting("rainbow_speed", "Скорость радуги", 1.0, 0.2, 4.0, 0.1).withEn("Rainbow Speed"));

    public CrosshairModule() {
        super("crosshair", "Прицел", "Кастомный киберспортивный прицел с хитмаркером, отдачей, обводкой и динамическим разбросом", Category.VISUAL);
        setEn("Crosshair", "Custom esports crosshair with hitmarker, attack recoil, black outline and dynamic spread");
    }
}
