package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class TargetHudModule extends Module {
    public final ModeSetting positionMode = addSetting(
            new ModeSetting("pos_mode", "Позиция", "Экран (HUD)", "Экран (HUD)", "Под ногами", "Над головой", "Сбоку").withEn("Position Mode")
    );
    public final ModeSetting cardStyle = addSetting(
            new ModeSetting("card_style", "Стиль карточки", "Стекло", "Стекло", "Целестиал", "Компакт", "Киберспорт", "Круг").withEn("Card Style")
    );
    public final ModeSetting barStyle = addSetting(new ModeSetting("bar_style", "Стиль полосы", "Градиент", "Градиент", "Акцент").withEn("Bar Style"));
    public final BooleanSetting smoothFollow = addSetting(
            new BooleanSetting("smooth_follow", "Плавное следование", true)
                    .<BooleanSetting>withEn("Smooth Follow").visibleIf(() -> !"Экран (HUD)".equals(positionMode.getValue()))
    );
    public final NumberSetting followSpeed = addSetting(
            new NumberSetting("follow_speed", "Скорость следования", 18.0, 5.0, 35.0, 1.0)
                    .<NumberSetting>withEn("Follow Speed").visibleIf(() -> !"Экран (HUD)".equals(positionMode.getValue()))
    );
    public final BooleanSetting scaleByDistance = addSetting(
            new BooleanSetting("scale_dist", "Масштаб по дистанции", true)
                    .<BooleanSetting>withEn("Scale by Distance").visibleIf(() -> !"Экран (HUD)".equals(positionMode.getValue()))
    );
    public final BooleanSetting showArmor = addSetting(new BooleanSetting("show_armor", "Показ брони", true).withEn("Show Armor"));
    public final BooleanSetting showDistance = addSetting(new BooleanSetting("show_dist", "Показ дистанции", true).withEn("Show Distance"));
    public final BooleanSetting showAbsorption = addSetting(new BooleanSetting("show_absorb", "Золотые сердца", true).withEn("Absorption Hearts"));
    public final BooleanSetting onlyPlayers = addSetting(new BooleanSetting("only_players", "Только игроки", false).withEn("Players Only"));
    public final NumberSetting holdSeconds = addSetting(new NumberSetting("hold_seconds", "Удержание (с)", 2.5, 0.5, 6.0, 0.5).withEn("Hold Time (s)"));

    public TargetHudModule() {
        super("targethud", "Target HUD", "Информативная карточка цели: на экране или над целью", Category.COMBAT);
        setEn("Target HUD", "Informative target card: on-screen or anchored to entity");
        setEnabled(true);
    }
}
