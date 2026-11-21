package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class WorldParticlesModule extends Module {
    public final ModeSetting style = addSetting(
            new ModeSetting("style", "Тип частиц", "Сакура",
                    "Сакура", "Светлячки", "Звёздная пыль", "Угли", "Снежинки", "Руны", "Пузыри")
                    .withEn("Particle Style")
    );
    public final ModeSetting palette = addSetting(
            new ModeSetting("palette", "Палитра", "Тема",
                    "Тема", "Статика", "Радуга", "Градиент", "Огонь/Угли")
                    .withEn("Palette")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Основной цвет", 0xFFFF77A9).withEn("Primary Color"));
    public final ColorSetting secondaryColor = addSetting(new ColorSetting("sec_color", "Второй цвет", 0xFF60A5FA).withEn("Secondary Color"));
    public final NumberSetting count = addSetting(new NumberSetting("count", "Количество частиц", 64.0, 16.0, 180.0, 4.0).withEn("Particle Count"));
    public final NumberSetting radius = addSetting(new NumberSetting("radius", "Радиус спавна", 16.0, 6.0, 32.0, 2.0).withEn("Spawn Radius"));
    public final NumberSetting fallSpeed = addSetting(new NumberSetting("fall_speed", "Скорость падения", 0.65, 0.1, 2.5, 0.05).withEn("Fall Speed"));
    public final NumberSetting windDrift = addSetting(new NumberSetting("wind", "Сила ветра", 0.35, -1.5, 1.5, 0.05).withEn("Wind Drift"));
    public final NumberSetting size = addSetting(new NumberSetting("size", "Размер частиц", 3.2, 1.0, 9.0, 0.2).withEn("Size"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Неоновый ореол", true).withEn("Glow Halo"));
    public final BooleanSetting spin = addSetting(new BooleanSetting("spin", "Вращение частиц", true).withEn("Rotate Particles"));

    public WorldParticlesModule() {
        super("world_particles", "Мировые частицы", "Атмосферные 3D частицы вокруг игрока: лепестки сакуры, светлячки, звёздная пыль", Category.VISUAL);
        setEn("World Particles", "Atmospheric 3D world particles around player: sakura petals, fireflies, stardust, embers");
    }
}
