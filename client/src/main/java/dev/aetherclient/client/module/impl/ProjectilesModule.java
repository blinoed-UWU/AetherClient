package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class ProjectilesModule extends Module {
    public final ModeSetting type = addSetting(new ModeSetting("type", "Тип снаряда", "Все", "Все", "Лук", "Жемчуг", "Снежки").withEn("Projectile Type"));
    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Статика", "Статика", "Радуга", "Тема", "Градиент").withEn("Palette"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет", 0xFFF59E0B).withEn("Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));
    public final NumberSetting dots = addSetting(new NumberSetting("dots", "Точек траектории", 48.0, 16.0, 96.0, 4.0).withEn("Trajectory Dots"));
    public final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Толщина", 1.6, 0.6, 3.5, 0.1).withEn("Thickness"));
    public final NumberSetting gravityScale = addSetting(new NumberSetting("gravity", "Гравитация", 1.0, 0.5, 1.6, 0.05).withEn("Gravity Scale"));
    public final BooleanSetting onlyHeld = addSetting(new BooleanSetting("only_held", "Только когда в руке", true).withEn("Only When Held"));
    public final BooleanSetting trackInFlight = addSetting(new BooleanSetting("track_inflight", "Отслеживать летящие снаряды", true).withEn("Track In-Flight Projectiles"));
    public final BooleanSetting showDistance = addSetting(new BooleanSetting("show_distance", "Показ дистанции", true).withEn("Show Trajectory Distance"));
    public final BooleanSetting pearlParticles = addSetting(new BooleanSetting("pearl_particles", "След за летящим жемчугом", true).withEn("Pearl Flight Particles"));
    public final BooleanSetting hitLandingCircle = addSetting(new BooleanSetting("landing_circle", "Круг в точке падения", true).withEn("Landing Target Ring"));
    public final NumberSetting landingRadius = addSetting(new NumberSetting("landing_radius", "Радиус точки падения", 1.2, 0.5, 3.0, 0.1).withEn("Landing Radius"));

    public ProjectilesModule() {
        super("projectiles", "Траектории", "Расчёт и отрисовка траектории стрел, жемчуга и снежков со следами частиц", Category.VISUAL);
        setEn("Projectiles", "Predicted ballistic trajectories for arrows, pearls and snowballs with particle trails");
    }
}

