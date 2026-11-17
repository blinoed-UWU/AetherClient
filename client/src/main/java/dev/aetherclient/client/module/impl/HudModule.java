package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.NumberSetting;

public final class HudModule extends Module {
    public final BooleanSetting watermark = addSetting(new BooleanSetting("watermark", "Водяной знак", true).withEn("Watermark"));
    public final BooleanSetting coords = addSetting(new BooleanSetting("coords", "Координаты", true).withEn("Coordinates"));
    public final BooleanSetting keystrokes = addSetting(new BooleanSetting("keystrokes", "Keystrokes и CPS", true).withEn("Keystrokes & CPS"));
    public final BooleanSetting armor = addSetting(new BooleanSetting("armor", "Броня и руки", true).withEn("Armor & Hands"));
    public final BooleanSetting arrayList = addSetting(new BooleanSetting("arraylist", "Список модулей", true).withEn("Module List"));
    public final BooleanSetting potions = addSetting(new BooleanSetting("potions", "Эффекты зелий", true).withEn("Potions"));
    public final BooleanSetting radar = addSetting(new BooleanSetting("radar", "Мини-радар", false).withEn("Mini Radar"));
    public final BooleanSetting stats = addSetting(new BooleanSetting("stats", "Скорость и статус", true).withEn("Speed & Stats"));
    public final BooleanSetting compass = addSetting(new BooleanSetting("compass", "Компас", false).withEn("Compass"));
    public final BooleanSetting speedometer = addSetting(new BooleanSetting("speedometer", "Спидометр", false).withEn("Speedometer"));
    public final BooleanSetting fpsPing = addSetting(new BooleanSetting("fps_ping", "График FPS/пинг", false).withEn("FPS & Ping Graph"));
    public final BooleanSetting inventory = addSetting(new BooleanSetting("inventory", "Инвентарь", false).withEn("Inventory"));
    public final BooleanSetting armorPro = addSetting(new BooleanSetting("armor_pro", "Броня Pro", false).withEn("Armor Pro"));
    public final BooleanSetting blurHud = addSetting(new BooleanSetting("hud_blur", "Размытие виджетов", true).withEn("Widget Blur"));
    public final NumberSetting glassOpacity = addSetting(new NumberSetting("hud_opacity", "Плотность стекла", 0.64, 0.25, 0.92, 0.02).withEn("Glass Opacity"));
    public final NumberSetting scaleWatermark = addSetting(new NumberSetting("scale_watermark", "Масштаб: Знак", 1.0, 0.5, 3.0, 0.1).withEn("Scale: Watermark"));
    public final NumberSetting scaleCoords = addSetting(new NumberSetting("scale_coords", "Масштаб: Коорд", 1.0, 0.5, 3.0, 0.1).withEn("Scale: Coords"));
    public final NumberSetting scaleKeystrokes = addSetting(new NumberSetting("scale_keystrokes", "Масштаб: Keystrokes", 1.0, 0.5, 3.0, 0.1).withEn("Scale: Keystrokes"));
    public final NumberSetting scaleArmor = addSetting(new NumberSetting("scale_armor", "Масштаб: Броня", 1.0, 0.5, 3.0, 0.1).withEn("Scale: Armor"));
    public final NumberSetting scaleArrayList = addSetting(new NumberSetting("scale_arraylist", "Масштаб: Список", 1.0, 0.5, 3.0, 0.1).withEn("Scale: ArrayList"));
    public final NumberSetting scaleTarget = addSetting(new NumberSetting("scale_target", "Масштаб: Цель", 1.0, 0.5, 3.0, 0.1).withEn("Scale: Target"));
    public final NumberSetting scalePotions = addSetting(new NumberSetting("scale_potions", "Масштаб: Зелья", 1.0, 0.5, 3.0, 0.1).withEn("Scale: Potions"));
    public final NumberSetting scaleRadar = addSetting(new NumberSetting("scale_radar", "Масштаб: Радар", 1.0, 0.5, 3.0, 0.1).withEn("Scale: Radar"));
    public final NumberSetting scaleStats = addSetting(new NumberSetting("scale_stats", "Масштаб: Статус", 1.0, 0.5, 3.0, 0.1).withEn("Scale: Stats"));
    public final NumberSetting scaleCompass = addSetting(new NumberSetting("scale_compass", "Масштаб: Компас", 1.0, 0.5, 3.0, 0.1).withEn("Scale: Compass"));
    public final NumberSetting scaleSpeedometer = addSetting(new NumberSetting("scale_speed", "Масштаб: Спидометр", 1.0, 0.5, 3.0, 0.1).withEn("Scale: Speedometer"));
    public final NumberSetting scaleFpsPing = addSetting(new NumberSetting("scale_fps_ping", "Масштаб: FPS/пинг", 1.0, 0.5, 3.0, 0.1).withEn("Scale: FPS/Ping"));
    public final NumberSetting scaleInventory = addSetting(new NumberSetting("scale_inventory", "Масштаб: Инвентарь", 1.0, 0.5, 3.0, 0.1).withEn("Scale: Inventory"));
    public final NumberSetting scaleArmorPro = addSetting(new NumberSetting("scale_armor_pro", "Масштаб: Броня Pro", 1.0, 0.5, 3.0, 0.1).withEn("Scale: Armor Pro"));
    public final NumberSetting hudScale = addSetting(new NumberSetting("hud_scale", "Масштаб HUD", 1.0, 0.65, 1.80, 0.05).withEn("Global HUD Scale"));

    public HudModule() {
        super("hud", "HUD", "Информационные виджеты на экране", Category.HUD);
        setEn("HUD", "On-screen informational widgets");
        setEnabled(true);
    }
}
