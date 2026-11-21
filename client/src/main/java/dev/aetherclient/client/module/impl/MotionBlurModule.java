package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class MotionBlurModule extends Module {
    public final ModeSetting blurMode = addSetting(
            new ModeSetting("mode", "Режим блюра", "Линейный",
                    "Линейный", "Радиальный", "Аккумуляционный")
                    .withEn("Blur Mode")
    );
    public final NumberSetting strength = addSetting(new NumberSetting("strength", "Сила размытия", 0.38, 0.05, 0.95, 0.05).withEn("Strength"));
    public final NumberSetting sensitivity = addSetting(new NumberSetting("sensitivity", "Чувствительность", 1.0, 0.2, 3.5, 0.1).withEn("Sensitivity"));
    public final NumberSetting samples = addSetting(new NumberSetting("samples", "Качество выборки (сэмплы)", 8.0, 4.0, 16.0, 1.0).withEn("Samples Quality"));
    public final BooleanSetting onlyMoving = addSetting(new BooleanSetting("only_moving", "Только при движении", false).withEn("Only While Moving"));
    public final BooleanSetting chromaticAberration = addSetting(new BooleanSetting("chromatic", "Хроматическая аберрация", true).withEn("Chromatic Aberration"));
    public final NumberSetting chromaticIntensity = addSetting(
            new NumberSetting("chrom_intensity", "Сила RGB сдвига", 0.8, 0.1, 3.0, 0.1)
                    .<NumberSetting>withEn("RGB Shift Intensity").visibleIf(chromaticAberration::isEnabled)
    );

    public MotionBlurModule() {
        super("motion_blur", "Motion Blur", "Кинематографичный размытый шлейф и хроматическая аберрация при резком движении камеры", Category.VISUAL);
        setEn("Motion Blur", "Cinematic motion blur and chromatic aberration on fast camera movement");
    }
}
