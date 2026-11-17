package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

public final class AmbienceModule extends Module {
    public final BooleanSetting customTime = addSetting(new BooleanSetting("custom_time", "Своё время суток", true).withEn("Custom Time"));
    public final ModeSetting timePreset = addSetting(
            new ModeSetting("time_preset", "Время", "Закат", "День", "Закат", "Ночь", "Полночь").<ModeSetting>withEn("Time Preset").visibleIf(customTime::isEnabled)
    );
    public final BooleanSetting vignette = addSetting(new BooleanSetting("vignette", "Виньетка экрана", true).withEn("Screen Vignette"));
    public final NumberSetting vignetteAlpha = addSetting(
            new NumberSetting("vignette_alpha", "Сила виньетки", 0.35, 0.1, 0.85, 0.05).<NumberSetting>withEn("Vignette Strength").visibleIf(vignette::isEnabled)
    );

    public AmbienceModule() {
        super("ambience", "Атмосфера", "Настройка времени суток и мягкой виньетки экрана", Category.VISUAL);
        setEn("Ambience", "Custom world time of day and atmospheric screen vignette");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled() || mc.world == null || !customTime.isEnabled()) return;
        long tod = switch (timePreset.getValue()) {
            case "День" -> 6000L;
            case "Закат" -> 12650L;
            case "Ночь" -> 15000L;
            default -> 18000L;
        };
        mc.world.setTime(mc.world.getTime(), tod, false);
    }
}
