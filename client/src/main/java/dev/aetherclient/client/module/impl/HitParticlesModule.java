package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class HitParticlesModule extends Module {
    public final ModeSetting shape = addSetting(
            new ModeSetting("shape", "Форма частиц", "Искры",
                    "Искры", "Звездочки", "Пузыри", "Капли", "Огонь",
                    "Молния", "Сердечки", "Кровь / Редстоун", "Магические руны")
                    .withEn("Particle Shape")
    );
    public final ModeSetting palette = addSetting(
            new ModeSetting("palette", "Палитра", "Тема",
                    "Тема", "Статика", "Радуга", "Огонь/Кровь", "Градиент", "Киберпанк")
                    .withEn("Palette")
    );
    public final ColorSetting color = addSetting(new ColorSetting("color", "Основной цвет", 0xFFFF4466).withEn("Primary Color"));
    public final ColorSetting secondaryColor = addSetting(new ColorSetting("sec_color", "Второй цвет (Градиент)", 0xFFFBBF24).withEn("Secondary Color"));
    public final NumberSetting count = addSetting(new NumberSetting("count", "Количество", 18.0, 4.0, 60.0, 2.0).withEn("Count"));
    public final NumberSetting size = addSetting(new NumberSetting("size", "Размер частиц", 3.5, 1.0, 10.0, 0.2).withEn("Size"));
    public final NumberSetting speed = addSetting(new NumberSetting("speed", "Скорость разлета", 1.2, 0.3, 3.5, 0.1).withEn("Burst Speed"));
    public final NumberSetting lifetime = addSetting(new NumberSetting("lifetime", "Время жизни (с)", 0.75, 0.2, 2.5, 0.05).withEn("Lifetime (s)"));
    public final BooleanSetting gravity = addSetting(new BooleanSetting("gravity", "Гравитация", true).withEn("Gravity"));
    public final NumberSetting gravityScale = addSetting(new NumberSetting("gravity_scale", "Сила гравитации", 1.0, 0.2, 3.0, 0.1).withEn("Gravity Scale"));
    public final BooleanSetting bounce = addSetting(new BooleanSetting("bounce", "Отскок от земли", true).withEn("Bounce on Ground"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Свечение частиц", true).withEn("Glow"));
    public final BooleanSetting fadeScale = addSetting(new BooleanSetting("fade_scale", "Уменьшение при затухании", true).withEn("Fade Scale"));

    public HitParticlesModule() {
        super("hit_particles", "Искры удара", "Вспышка стилизованных частиц при атаке по цели с физикой, гравитацией и отскоком", Category.VISUAL);
        setEn("Hit Particles", "Burst of stylized particles when attacking entities with physics, gravity and bounce");
    }
}
