package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class DamageVignetteModule extends Module {
    public final BooleanSetting onHit = addSetting(new BooleanSetting("on_hit", "Вспышка при уроне", true).withEn("On Damage"));
    public final BooleanSetting lowHp = addSetting(new BooleanSetting("low_hp", "Пульсация при низком HP", true).withEn("Low Health Pulse"));
    public final NumberSetting intensity = addSetting(new NumberSetting("intensity", "Яркость виньетки", 0.65, 0.2, 1.0, 0.05).withEn("Intensity"));
    public final NumberSetting lowHpThreshold = addSetting(new NumberSetting("threshold", "Порог низкого HP", 6.0, 2.0, 14.0, 1.0).withEn("Low HP Threshold"));
    public final ColorSetting hitColor = addSetting(new ColorSetting("hit_col", "Цвет урона", 0xFFF43F5E).withEn("Damage Color"));
    public final ColorSetting lowHpColor = addSetting(new ColorSetting("low_col", "Цвет критического HP", 0xFF990000).withEn("Critical HP Color"));
    public final NumberSetting pulseSpeed = addSetting(new NumberSetting("pulse_speed", "Скорость пульсации", 2.0, 0.5, 5.0, 0.25).withEn("Pulse Speed"));
    public final ModeSetting blendMode = addSetting(
            new ModeSetting("blend", "Стиль наложения", "Мягкий",
                    "Мягкий", "Кинематографичный", "Кровавый", "Неон")
                    .withEn("Blend Mode")
    );
    public final BooleanSetting heartbeatSound = addSetting(new BooleanSetting("heartbeat", "Звук сердцебиения", true).withEn("Heartbeat Sound"));

    public DamageVignetteModule() {
        super("damage_vignette", "Виньетка урона", "Мягкая пульсирующая кинематографическая виньетка краёв экрана при уроне и критическом HP", Category.COMBAT);
        setEn("Damage Vignette", "Soft pulsing cinematic edge glow on damage and critical health with custom colors");
    }
}
