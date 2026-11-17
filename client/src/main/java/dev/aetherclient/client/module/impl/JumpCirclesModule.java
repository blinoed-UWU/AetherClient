package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class JumpCirclesModule extends Module {
    public final ModeSetting style = addSetting(
            new ModeSetting("style", "Стиль", "Целестиал", "Целестиал", "Неон", "Стекло", "Импульс", "Сетка", "Шлейф рун", "Двойное кольцо", "Шоковая волна").withEn("Style")
    );
    public final ModeSetting palette = addSetting(
            new ModeSetting("palette", "Палитра", "Тема", "Тема", "Статика", "Радуга", "Градиент").withEn("Palette")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет", 0xFF7C5CFF).withEn("Color"));
    public final NumberSetting maxRadius = addSetting(new NumberSetting("max_radius", "Радиус", 1.6, 0.6, 3.5, 0.1).withEn("Radius"));
    public final NumberSetting duration = addSetting(new NumberSetting("duration", "Длительность (с)", 0.85, 0.3, 2.5, 0.05).withEn("Duration (s)"));
    public final NumberSetting thickness = addSetting(new NumberSetting("thickness", "Толщина", 2.2, 0.8, 5.0, 0.2).withEn("Thickness"));
    public final NumberSetting ringsCount = addSetting(new NumberSetting("rings_count", "Количество колец", 2, 1, 4, 1).withEn("Rings Count"));
    public final BooleanSetting burstParticles = addSetting(new BooleanSetting("burst_particles", "Всплеск искр", true).withEn("Particle Burst"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Неоновое свечение", true).withEn("Glow"));
    public final BooleanSetting landingRipple = addSetting(new BooleanSetting("landing", "Волна при приземлении", true).withEn("Landing Wave"));

    public JumpCirclesModule() {
        super("jump_circles", "Круги прыжка", "Светящиеся кольца под ногами при прыжке и приземлении", Category.VISUAL);
        setEn("Jump Circles", "Glowing expanding rings beneath your feet on jumps and landings");
    }
}
