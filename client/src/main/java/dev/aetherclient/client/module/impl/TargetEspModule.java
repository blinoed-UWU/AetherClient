package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class TargetEspModule extends Module {
    public final ModeSetting style = addSetting(new ModeSetting("style", "Стиль", "Кольцо", "Кольцо", "Призрак").withEn("Style"));
    public final NumberSetting speed = addSetting(new NumberSetting("speed", "Скорость", 1.4, 0.4, 3.5, 0.1).withEn("Speed"));
    public final NumberSetting radius = addSetting(new NumberSetting("radius", "Радиус", 0.75, 0.4, 1.4, 0.05).withEn("Radius"));

    public TargetEspModule() {
        super("target_esp", "Target ESP", "Анимированный маркер вокруг текущей цели", Category.VISUAL);
        setEn("Target ESP", "Animated marker around the current target");
    }
}
