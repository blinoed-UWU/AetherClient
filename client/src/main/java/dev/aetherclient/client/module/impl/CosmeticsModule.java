package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class CosmeticsModule extends Module {
    public final ModeSetting headSlot = addSetting(
            new ModeSetting("head_slot", "Голова", "Нимб",
                    "Нимб", "Рога демона", "Шляпа", "Корона", "Кошачьи ушки", "Маска / Бандана", "Нет")
                    .withEn("Head Slot")
    );
    public final ModeSetting bodySlot = addSetting(
            new ModeSetting("body_slot", "Тело / Спина", "Крылья (Ангельские)",
                    "Крылья (Ангельские)", "Крылья (Демонические)", "Крылья (Дракон)", "Крылья (Фея)", "Хвост дракона", "Плащ", "Нет")
                    .withEn("Body / Back Slot")
    );
    public final ModeSetting armsSlot = addSetting(
            new ModeSetting("arms_slot", "Руки", "Митенки / Рукава",
                    "Митенки / Рукава", "Браслеты", "Когти", "Нет")
                    .withEn("Arms Slot")
    );
    public final ModeSetting legsSlot = addSetting(
            new ModeSetting("legs_slot", "Ноги", "Чулки",
                    "Чулки", "Огненная поступь", "Нет")
                    .withEn("Legs Slot")
    );
    public final ModeSetting auraSlot = addSetting(
            new ModeSetting("aura_slot", "Аура", "Орбитальные сферы",
                    "Орбитальные сферы", "Аура пламени", "Световой вихрь", "Нет")
                    .withEn("Aura Slot")
    );

    public final BooleanSetting firstPerson = addSetting(new BooleanSetting("first_person", "От первого лица", false).withEn("First Person View"));
    public final NumberSetting firstPersonOffsetY = addSetting(
            new NumberSetting("fp_offset_y", "Высота от 1-го лица", 0.05, -0.4, 0.6, 0.05)
                    .<NumberSetting>withEn("1st Person Height Offset").visibleIf(firstPerson::isEnabled)
    );
    public final BooleanSetting showOnFriends = addSetting(new BooleanSetting("show_on_friends", "Отображать на друзьях", true).withEn("Show on Friends"));

    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Тема", "Статика", "Радуга", "Тема", "Градиент").withEn("Palette"));
    public final ColorSetting color = addSetting(new ColorSetting("color", "Акцентный цвет", 0xFF7C5CFF).withEn("Accent Color"));
    public final ColorSetting secondaryColor = addSetting(new ColorSetting("sec_color", "Второй цвет (Основа)", 0xFF38BDF8).withEn("Secondary Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));

    public final NumberSetting size = addSetting(new NumberSetting("size", "Размер", 1.0, 0.4, 2.5, 0.05).withEn("Size"));
    public final NumberSetting flapSpeed = addSetting(new NumberSetting("flap_speed", "Скорость махов", 1.2, 0.3, 3.0, 0.1).withEn("Flap Speed"));
    public final NumberSetting speed = addSetting(new NumberSetting("speed", "Скорость вращения", 1.0, 0.1, 3.0, 0.1).withEn("Spin Speed"));
    public final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Прозрачность", 0.90, 0.2, 1.0, 0.05).withEn("Opacity"));
    public final BooleanSetting glow = addSetting(new BooleanSetting("glow", "Неоновый ореол", true).withEn("Neon Glow"));
    public final BooleanSetting pulse = addSetting(new BooleanSetting("pulse", "Пульсация", true).withEn("Pulse"));

    public CosmeticsModule() {
        super("cosmetics", "Косметика", "Премиум 3D косметика: крылья ангела/демона/дракона, рога, хвост, ушки, нимб, плащ и ауры", Category.VISUAL);
        setEn("Cosmetics", "Premium 3D cosmetics: wings, horns, dragon tail, ears, halo, cape and auras");
    }
}
