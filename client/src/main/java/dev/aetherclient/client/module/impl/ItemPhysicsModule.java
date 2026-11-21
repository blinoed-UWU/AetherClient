package dev.aetherclient.client.module.impl;

import dev.aetherclient.bridge.AetherBridge;
import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

public final class ItemPhysicsModule extends Module {
    public final ModeSetting axis = addSetting(
            new ModeSetting("axis", "Ось вращения", "Горизонтальная Y",
                    "Горизонтальная Y", "Кувырок X", "Все 3D оси")
                    .withEn("Rotation Axis")
    );
    public final NumberSetting spinSpeed = addSetting(new NumberSetting("spin_speed", "Скорость вращения (об/с)", 1.5, 0.1, 6.0, 0.1).withEn("Spin Speed (rev/s)"));
    public final BooleanSetting layFlat = addSetting(new BooleanSetting("lay_flat", "Лежать плашмя на земле", false).withEn("Lay Flat on Ground"));
    public final NumberSetting scale = addSetting(new NumberSetting("scale", "Масштаб предметов", 1.0, 0.4, 2.5, 0.05).withEn("Item Scale"));
    public final BooleanSetting bobbing = addSetting(new BooleanSetting("bobbing", "Левитация (покачивание)", true).withEn("Floating Bobbing"));
    public final NumberSetting bobHeight = addSetting(new NumberSetting("bob_height", "Амплитуда покачивания", 0.12, 0.02, 0.4, 0.02).withEn("Bob Amplitude"));
    public final BooleanSetting glowRare = addSetting(new BooleanSetting("glow_rare", "Свечение редких предметов", true).withEn("Glow Rare Items"));
    public final ColorSetting rareColor = addSetting(new ColorSetting("rare_col", "Цвет подсветки ценного лута", 0xFFFFD700).withEn("Rare Item Color"));

    public ItemPhysicsModule() {
        super("item_physics", "Физика предметов", "Реалистичное вращение, кувырок, лежание на земле и свечение редкого лута", Category.VISUAL);
        setEn("Item Physics", "Realistic dropped item spinning, tumbling, flat floor alignment and rare loot glow");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled()) return;
        AetherBridge.setItemSpinAngle((System.currentTimeMillis() / 1000f) * spinSpeed.asFloat() * 6.2831855f);
    }

    @Override
    protected void onDisable() {
        AetherBridge.setItemSpinAngle(0f);
    }

    @Override
    public String getDisplaySuffix() {
        return String.format(java.util.Locale.ROOT, "%.1f %s", spinSpeed.getValue(),
                dev.aetherclient.client.i18n.I18n.tr("об/с", "rev/s"));
    }
}
