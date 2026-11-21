package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class ParticlesModule extends Module {
    public final ModeSetting style = addSetting(
            new ModeSetting("style", "Тип частиц", "Светлячки",
                    "Светлячки", "Снег", "Боке", "Сакура", "Звёздная пыль", "Угли", "Электричество", "Пузыри")
                    .withEn("Style")
    );
    public final NumberSetting count = addSetting(new NumberSetting("count", "Количество", 42.0, 10.0, 120.0, 2.0).withEn("Count"));
    public final NumberSetting speed = addSetting(new NumberSetting("speed", "Скорость движения", 1.0, 0.2, 3.0, 0.1).withEn("Speed"));
    public final NumberSetting size = addSetting(new NumberSetting("size", "Размер частиц", 3.5, 1.0, 12.0, 0.5).withEn("Size"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет частиц", 0xFFFFFFFF).withEn("Particle Color"));
    public final BooleanSetting randomColors = addSetting(new BooleanSetting("random_colors", "Разноцветные частицы", true).withEn("Multi-color"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Неоновое свечение", true).withEn("Glow Halo"));
    public final NumberSetting windForce = addSetting(new NumberSetting("wind", "Боковой ветер", 0.2, -2.0, 2.0, 0.1).withEn("Wind Force"));
    public final BooleanSetting worldSpace = addSetting(new BooleanSetting("world_space", "В 3D пространстве", false).withEn("Render in 3D"));

    public ParticlesModule() {
        super("particles", "Частицы", "Атмосферные парящие частицы, светлячки, снег, боке и сакура на экране и в мире", Category.VISUAL);
        setEn("Particles", "Ambient floating fireflies, snow, bokeh, sakura and embers on screen and in 3D");
    }
}
