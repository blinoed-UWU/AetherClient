package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class HitParticlesModule extends Module {
    public final ModeSetting shape = addSetting(
            new ModeSetting("shape", "Форма", "Искры", "Искры", "Звездочки", "Пузыри", "Капли", "Огонь").withEn("Shape")
    );
    public final ModeSetting palette = addSetting(
            new ModeSetting("palette", "Палитра", "Тема", "Тема", "Статика", "Радуга", "Огонь/Кровь").withEn("Palette")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Цвет", 0xFFFF4466).withEn("Color"));
    public final NumberSetting count = addSetting(new NumberSetting("count", "Количество", 16.0, 4.0, 48.0, 2.0).withEn("Count"));
    public final NumberSetting size = addSetting(new NumberSetting("size", "Размер", 3.2, 1.2, 7.0, 0.2).withEn("Size"));
    public final NumberSetting speed = addSetting(new NumberSetting("speed", "Скорость разлета", 1.0, 0.3, 2.5, 0.1).withEn("Burst Speed"));
    public final NumberSetting lifetime = addSetting(new NumberSetting("lifetime", "Время жизни (с)", 0.65, 0.2, 1.8, 0.05).withEn("Lifetime (s)"));
    public final BooleanSetting gravity = addSetting(new BooleanSetting("gravity", "Гравитация", true).withEn("Gravity"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Свечение частиц", true).withEn("Glow"));

    public HitParticlesModule() {
        super("hit_particles", "Искры удара", "Вспышка частиц при атаке по цели с физикой", Category.VISUAL);
        setEn("Hit Particles", "Glowing burst of particles when attacking entities with physics");
    }
}
