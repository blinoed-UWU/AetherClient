package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class KillEffectModule extends Module {
    public final ModeSetting effectType = addSetting(
            new ModeSetting("effect", "Тип эффекта", "Молния",
                    "Молния", "Взрывная волна", "Ангельские крылья", "Кровавый всплеск", "Череп души", "Все сразу")
                    .withEn("Effect Type")
    );
    public final ModeSetting soundEffect = addSetting(
            new ModeSetting("sound", "Звук при убийстве", "Громовой раскат",
                    "Громовой раскат", "Колокол", "Колокольный перезвон", "Взрыв", "Без звука")
                    .withEn("Kill Sound")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет эффекта", 0xFF60A5FA).withEn("Effect Color"));
    public final ColorSetting secondaryColor = addSetting(new ColorSetting("sec_color", "Второй цвет", 0xFFEC4899).withEn("Secondary Color"));
    public final NumberSetting scale = addSetting(new NumberSetting("scale", "Масштаб эффекта", 1.2, 0.5, 3.0, 0.1).withEn("Effect Scale"));
    public final NumberSetting duration = addSetting(new NumberSetting("duration", "Длительность (с)", 1.2, 0.4, 3.0, 0.1).withEn("Duration (s)"));
    public final BooleanSetting screenShake = addSetting(new BooleanSetting("screen_shake", "Тряска экрана", false).withEn("Screen Shake"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Неоновый ореол", true).withEn("Glow Halo"));

    public KillEffectModule() {
        super("kill_effect", "Эффект убийства", "Зрелищный визуальный спецэффект и звук при ликвидации цели", Category.VISUAL);
        setEn("Kill Effect", "Spectacular visual kill effect (lightning, shockwave, wings) and sound on kill");
    }
}
