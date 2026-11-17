package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class AspectRatioModule extends Module {
    public final ModeSetting aspect = addSetting(new ModeSetting("aspect", "Соотношение", "21:9", "21:9", "16:9", "4:3", "1:1", "Кастомный").withEn("Aspect Ratio"));
    public final NumberSetting customRatio = addSetting(new NumberSetting("custom_ratio", "Кастомный коэфф.", 2.33, 0.5, 3.5, 0.05).withEn("Custom Ratio"));
    public final BooleanSetting smoothTransition = addSetting(new BooleanSetting("smooth_transition", "Плавный переход", true).withEn("Smooth Transition"));
    public final NumberSetting transitionSpeed = addSetting(new NumberSetting("transition_speed", "Скорость перехода", 8.0, 1.0, 20.0, 1.0).withEn("Transition Speed"));
    public final BooleanSetting cinemaBars = addSetting(new BooleanSetting("cinema_bars", "Кинематографичные полосы", true).withEn("Cinema Letterbox Bars"));
    public final BooleanSetting hudOnTop = addSetting(new BooleanSetting("hud_on_top", "ХУД поверх полос", true).withEn("HUD Over Bars"));
    public final NumberSetting barOpacity = addSetting(new NumberSetting("bar_opacity", "Затемнение полос", 1.0, 0.2, 1.0, 0.05).withEn("Bar Opacity"));
    public final BooleanSetting frostedBorders = addSetting(new BooleanSetting("frosted_borders", "Матовая окантовка", true).withEn("Frosted Borders"));

    public AspectRatioModule() {
        super("aspect_ratio", "Соотношение сторон", "Кинематографический леттербоксинг и кастомное соотношение сторон экрана", Category.VISUAL);
        setEn("Aspect Ratio", "Cinematic letterboxing and custom viewport aspect ratios");
    }

    public float getTargetRatio() {
        String mode = aspect.getValue();
        return switch (mode) {
            case "21:9" -> 21f / 9f;
            case "16:9" -> 16f / 9f;
            case "4:3" -> 4f / 3f;
            case "1:1" -> 1f;
            default -> customRatio.asFloat();
        };
    }
}
