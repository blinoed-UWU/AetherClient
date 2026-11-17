package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

/**
 * Predicts and renders the 3D trajectory and landing impact location
 * for thrown Ender Pearls (and held pearls) with arc simulation and landing rings.
 */
public final class PearlPredictModule extends Module {
    public final BooleanSetting trajectory = addSetting(new BooleanSetting("trajectory", "Линия траектории", true).withEn("Trajectory Line"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет траектории", 0xFF38BDF8).withEn("Arc Color"));
    public final BooleanSetting gradient = addSetting(new BooleanSetting("gradient", "Плавный градиент", true).withEn("Fade Gradient"));
    public final NumberSetting lineWidth = addSetting(new NumberSetting("width", "Толщина линии", 2.5, 1.0, 5.0, 0.5).withEn("Line Width"));
    public final BooleanSetting landingRing = addSetting(new BooleanSetting("landing_ring", "Круг приземления", true).withEn("Landing Ring"));
    public final NumberSetting ringSize = addSetting(new NumberSetting("ring_size", "Размер круга", 0.8, 0.3, 1.8, 0.1).withEn("Ring Radius"));
    public final BooleanSetting impactBox = addSetting(new BooleanSetting("impact_box", "3D бокс на блоке", true).withEn("Impact Box"));
    public final BooleanSetting showTimer = addSetting(new BooleanSetting("timer", "Таймер до удара", true).withEn("Impact Countdown"));
    public final BooleanSetting predictInHand = addSetting(new BooleanSetting("in_hand", "Прогноз из руки", true).withEn("Predict In Hand"));
    public final BooleanSetting otherPlayers = addSetting(new BooleanSetting("other_players", "Жемчуг врагов", true).withEn("Track Other Players"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Неоновый ореол", true).withEn("Glow"));

    public PearlPredictModule() {
        super("pearl_predict", "Траектория жемчуга", "Физическая баллистическая дуга, круг приземления, 3D бокс и таймер до удара", Category.VISUAL);
        setEn("Pearl Predict", "Ballistic arc prediction, landing circle, 3D impact box and countdown timer");
        setEnabled(true);
    }
}
