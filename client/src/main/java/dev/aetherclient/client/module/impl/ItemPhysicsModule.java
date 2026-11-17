package dev.aetherclient.client.module.impl;

import dev.aetherclient.bridge.AetherBridge;
import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

public final class ItemPhysicsModule extends Module {
    public final NumberSetting spinSpeed = addSetting(new NumberSetting("spin_speed", "Скорость вращения (об/с)", 1.5, 0.1, 6.0, 0.1).withEn("Spin Speed (rev/s)"));

    public ItemPhysicsModule() {
        super("item_physics", "Физика предметов", "Предметы на земле непрерывно вращаются", Category.VISUAL);
        setEn("Item Physics", "Dropped items spin continuously on the ground");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled()) return;
        // Continuous, wall-clock based spin angle in radians (0 when module is disabled).
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
