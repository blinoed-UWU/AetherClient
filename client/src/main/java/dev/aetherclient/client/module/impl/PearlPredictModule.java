package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

/**
 * Predicts and renders the 3D trajectory and landing impact location
 * for thrown Ender Pearls (and held pearls).
 */
public final class PearlPredictModule extends Module {
    public final BooleanSetting trajectory = new BooleanSetting("trajectory", "Линия траектории", true);
    public final BooleanSetting landingRing = new BooleanSetting("landing_ring", "Круг приземления", true);
    public final BooleanSetting showTimer = new BooleanSetting("timer", "Таймер до удара", true);
    public final BooleanSetting predictInHand = new BooleanSetting("in_hand", "Прогноз из руки", true);
    public final ColorSetting color = new ColorSetting("color", "Цвет траектории", 0xFF38BDF8);
    public final NumberSetting lineWidth = new NumberSetting("width", "Толщина линии", 2.5, 1.0, 5.0, 0.5);

    public PearlPredictModule() {
        super("pearl_predict", "Траектория жемчуга", "Показывает точную физическую дугу полета эндер-жемчуга и место приземления", Category.VISUAL);
        addSetting(trajectory);
        addSetting(landingRing);
        addSetting(showTimer);
        addSetting(predictInHand);
        addSetting(color);
        addSetting(lineWidth);
        setEnabled(true);
    }
}
