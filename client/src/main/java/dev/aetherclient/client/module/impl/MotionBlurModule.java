package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class MotionBlurModule extends Module {
    public final NumberSetting strength = addSetting(new NumberSetting("strength", "Сила блюра", 0.38, 0.05, 0.95, 0.05).withEn("Strength"));
    public final NumberSetting sensitivity = addSetting(new NumberSetting("sensitivity", "Чувствительность", 1.0, 0.3, 3.0, 0.1).withEn("Sensitivity"));

    public MotionBlurModule() {
        super("motion_blur", "Motion Blur", "Кинематографичный блюр при резком движении камеры", Category.VISUAL);
        setEn("Motion Blur", "Cinematic directional blur on fast camera movement");
    }
}
