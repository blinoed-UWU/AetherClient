package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class ParticlesModule extends Module {
    public final ModeSetting style = addSetting(new ModeSetting("style", "Тип частиц", "Светлячки", "Светлячки", "Снег", "Боке").withEn("Style"));
    public final NumberSetting count = addSetting(new NumberSetting("count", "Количество", 36.0, 10.0, 90.0, 2.0).withEn("Count"));
    public final NumberSetting speed = addSetting(new NumberSetting("speed", "Скорость", 1.0, 0.3, 2.5, 0.1).withEn("Speed"));

    public ParticlesModule() {
        super("particles", "Частицы", "Атмосферные парящие частицы и боке на экране", Category.VISUAL);
        setEn("Particles", "Ambient floating fireflies, snow, or bokeh on screen");
    }
}
