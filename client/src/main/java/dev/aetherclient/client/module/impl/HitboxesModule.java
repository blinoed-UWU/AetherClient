package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class HitboxesModule extends Module {
    public final ModeSetting shape = addSetting(
            new ModeSetting("shape", "Форма хитбокса", "3D Каркас",
                    "3D Каркас", "Углы", "Цилиндр", "Свечение")
                    .withEn("Box Shape")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет хитбокса", 0xFF7C5CFF).withEn("Box Color"));
    public final BooleanSetting healthColor = addSetting(new BooleanSetting("hp_color", "Цвет по здоровью цели", false).withEn("Color by Health"));
    public final BooleanSetting fill = addSetting(new BooleanSetting("fill", "Заливка граней", true).withEn("Fill Faces"));
    public final NumberSetting fillAlpha = addSetting(
            new NumberSetting("fill_alpha", "Прозрачность заливки", 0.22, 0.05, 0.80, 0.05)
                    .<NumberSetting>withEn("Fill Alpha").visibleIf(fill::isEnabled)
    );
    public final NumberSetting lineWidth = addSetting(new NumberSetting("line_width", "Толщина линий", 1.8, 0.5, 4.5, 0.1).withEn("Line Width"));
    public final NumberSetting expand = addSetting(new NumberSetting("expand", "Расширение хитбокса", 0.0, 0.0, 1.0, 0.05).withEn("Hitbox Padding"));
    public final BooleanSetting eyeVector = addSetting(new BooleanSetting("eye_vector", "Вектор взгляда", true).withEn("Eye Vector"));
    public final NumberSetting eyeVectorLength = addSetting(
            new NumberSetting("eye_len", "Длина вектора взгляда", 3.0, 1.0, 8.0, 0.5)
                    .<NumberSetting>withEn("Eye Vector Length").visibleIf(eyeVector::isEnabled)
    );
    public final ColorSetting eyeColor = addSetting(
            new ColorSetting("eye_color", "Цвет вектора", 0xFFFF3366)
                    .<ColorSetting>withEn("Eye Vector Color").visibleIf(eyeVector::isEnabled)
    );
    public final BooleanSetting facingAngle = addSetting(new BooleanSetting("facing_angle", "Сектор обзора на земле", true).withEn("Floor Look Sector"));
    public final BooleanSetting invisible = addSetting(new BooleanSetting("invisible", "Невидимые сущности", true).withEn("Show Invisible"));
    public final BooleanSetting playersOnly = addSetting(new BooleanSetting("players_only", "Только игроки", false).withEn("Players Only"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", true).withEn("Through Walls"));

    public HitboxesModule() {
        super("hitboxes", "Хитбоксы", "3D визуализация хитбоксов, секторов атаки и векторов взгляда сущностей (включая невидимок)", Category.COMBAT);
        setEn("Hitboxes", "3D entity bounding boxes, floor look sectors and eye vectors with face fills and invisibility support");
    }
}
