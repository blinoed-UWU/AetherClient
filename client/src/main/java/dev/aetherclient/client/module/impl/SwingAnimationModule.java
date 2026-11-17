package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class SwingAnimationModule extends Module {
    public final ModeSetting mode = addSetting(
            new ModeSetting("mode", "Стиль взмаха",
                    "Chop",
                    "Chop", "Swipe", "Down", "Smooth", "Smooth 2", "Power", "Feast", "Twist", "1.7 Vanilla", "Вращение", "Компактный")
                    .withEn("Swing Style")
    );
    public final NumberSetting strength = addSetting(
            new NumberSetting("strength", "Сила взмаха", 1.0, 0.3, 3.0, 0.1).withEn("Swing Strength")
    );
    public final NumberSetting swingSpeed = addSetting(
            new NumberSetting("speed", "Скорость взмаха", 1.0, 0.4, 3.0, 0.05).withEn("Swing Speed")
    );
    public final NumberSetting angle = addSetting(
            new NumberSetting("angle", "Угол наклона", 0.0, -60.0, 60.0, 1.0).withEn("Pitch Angle")
    );
    public final BooleanSetting ignoreMining = addSetting(
            new BooleanSetting("ignore_mining", "Не менять при копании", true).withEn("Ignore Mining")
    );

    public SwingAnimationModule() {
        super("swing_anim", "Анимация удара", "Пресеты взмаха из топ-клиентов (Chop, Twist, Swipe, Power, Feast, Smooth, 1.7)", Category.VISUAL);
        setEn("Swing Animation", "Custom sword swing animations (Chop, Twist, Swipe, Power, Feast, Smooth, 1.7)");
    }
}
