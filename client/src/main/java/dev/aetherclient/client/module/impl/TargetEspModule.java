package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class TargetEspModule extends Module {
    public final ModeSetting style = addSetting(
            new ModeSetting("style", "Стиль маркера", "Цилиндр Jello",
                    "Цилиндр Jello", "Кольцо", "Призма", "Кристалл Sims", "Спираль ДНК", "Маячок", "Призрак")
                    .withEn("Marker Style")
    );
    public final ModeSetting palette = addSetting(
            new ModeSetting("palette", "Палитра", "Тема",
                    "Тема", "Статика", "Радуга", "Градиент")
                    .withEn("Palette")
    );
    public final ColorSetting primaryColor = addSetting(new ColorSetting("color", "Основной цвет", 0xFF6366F1).withEn("Primary Color"));
    public final ColorSetting secondaryColor = addSetting(new ColorSetting("sec_color", "Второй цвет (Градиент)", 0xFFEC4899).withEn("Secondary Color"));
    public final NumberSetting speed = addSetting(new NumberSetting("speed", "Скорость вращения", 1.4, 0.2, 4.0, 0.1).withEn("Spin Speed"));
    public final NumberSetting radius = addSetting(new NumberSetting("radius", "Радиус маркера", 0.85, 0.3, 2.2, 0.05).withEn("Radius"));
    public final NumberSetting height = addSetting(new NumberSetting("height", "Высота маркера", 1.8, 0.3, 3.0, 0.1).withEn("Height"));
    public final NumberSetting ringCount = addSetting(new NumberSetting("rings", "Количество колец", 2.0, 1.0, 5.0, 1.0).withEn("Ring Count"));
    public final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Толщина линий", 2.2, 0.8, 5.0, 0.2).withEn("Thickness"));
    public final NumberSetting points = addSetting(new NumberSetting("points", "Детализация (точек)", 32.0, 16.0, 64.0, 4.0).withEn("Detail Segments"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Неоновое свечение", true).withEn("Neon Glow"));
    public final BooleanSetting pulsing = addSetting(new BooleanSetting("pulsing", "Пульсация радиуса", true).withEn("Pulse Radius"));
    public final NumberSetting pulseSpeed = addSetting(new NumberSetting("pulse_speed", "Скорость пульсации", 2.0, 0.5, 5.0, 0.25).withEn("Pulse Speed"));
    public final BooleanSetting orbitParticles = addSetting(new BooleanSetting("orbit_sparks", "Орбитальные искры", true).withEn("Orbit Sparks"));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting("through_walls", "Сквозь стены", true).withEn("Through Walls"));

    public TargetEspModule() {
        super("target_esp", "Target ESP", "Анимированный 3D маркер цели: цилиндр Jello, призма, кристалл Sims, кольца", Category.VISUAL);
        setEn("Target ESP", "Animated 3D target indicator: Jello cylinder, Prisma diamonds, Sims diamond, DNA helix");
    }
}
