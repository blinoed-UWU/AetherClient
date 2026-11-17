package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class DamageVignetteModule extends Module {
    public final BooleanSetting onHit = addSetting(new BooleanSetting("on_hit", "При уроне", true).withEn("On Damage"));
    public final BooleanSetting lowHp = addSetting(new BooleanSetting("low_hp", "При низком HP", true).withEn("Low Health Pulse"));
    public final NumberSetting intensity = addSetting(new NumberSetting("intensity", "Яркость", 0.65, 0.2, 1.0, 0.05).withEn("Intensity"));

    public DamageVignetteModule() {
        super("damage_vignette", "Виньетка урона", "Мягкая пульсирующая подсветка краёв при уроне и низком HP", Category.COMBAT);
        setEn("Damage Vignette", "Soft edge glow on damage and low health");
    }
}
