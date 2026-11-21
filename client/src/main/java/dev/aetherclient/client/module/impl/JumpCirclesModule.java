package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class JumpCirclesModule extends Module {
    public final ModeSetting style = addSetting(
            new ModeSetting("style", "Стиль кругов", "Целестиал",
                    "Целестиал", "Неон", "Стекло", "Импульс", "Сетка", "Шлейф рун",
                    "Двойное кольцо", "Шоковая волна", "Звезда / Всплеск", "Шестигранник")
                    .withEn("Circle Style")
    );
    public final ModeSetting palette = addSetting(
            new ModeSetting("palette", "Палитра", "Тема",
                    "Тема", "Статика", "Радуга", "Градиент", "Киберпанк", "Закат")
                    .withEn("Palette")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Основной цвет", 0xFF7C5CFF).withEn("Primary Color"));
    public final ColorSetting secondaryColor = addSetting(new ColorSetting("sec_color", "Второй цвет (Градиент)", 0xFF38BDF8).withEn("Secondary Color"));
    public final NumberSetting maxRadius = addSetting(new NumberSetting("max_radius", "Радиус", 1.8, 0.5, 4.5, 0.1).withEn("Radius"));
    public final NumberSetting duration = addSetting(new NumberSetting("duration", "Длительность (с)", 0.85, 0.2, 3.0, 0.05).withEn("Duration (s)"));
    public final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Толщина линий", 2.4, 0.5, 6.0, 0.2).withEn("Thickness"));
    public final NumberSetting ringsCount = addSetting(new NumberSetting("rings_count", "Количество колец", 2.0, 1.0, 5.0, 1.0).withEn("Rings Count"));
    public final BooleanSetting burstParticles = addSetting(new BooleanSetting("burst_particles", "Всплеск искр", true).withEn("Particle Burst"));
    public final NumberSetting burstCount = addSetting(new NumberSetting("burst_count", "Искр во всплеске", 16.0, 4.0, 36.0, 2.0).withEn("Burst Particles Count"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Неоновое свечение", true).withEn("Glow"));
    public final BooleanSetting landingRipple = addSetting(new BooleanSetting("landing", "Волна при приземлении", true).withEn("Landing Wave"));
    public final BooleanSetting soundEffect = addSetting(new BooleanSetting("sound", "Звуковой эффект", true).withEn("Sound Effect"));
    public final NumberSetting soundVolume = addSetting(new NumberSetting("sound_vol", "Громкость звука", 0.6, 0.1, 1.0, 0.05).withEn("Sound Volume"));

    public JumpCirclesModule() {
        super("jump_circles", "Круги прыжка", "Светящиеся кольца и ударные волны под ногами при прыжке и приземлении", Category.VISUAL);
        setEn("Jump Circles", "Glowing expanding rings, starbursts and shockwaves on jumps and landings");
    }
}
