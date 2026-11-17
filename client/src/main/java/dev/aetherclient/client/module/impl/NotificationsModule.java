package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import dev.aetherclient.client.notify.ToastManager;

public final class NotificationsModule extends Module {
    public final ModeSetting position = addSetting(new ModeSetting("position", "Позиция", "Правый верх", "Правый верх", "Левый верх", "Правый низ", "Левый низ").withEn("Position"));
    public final NumberSetting duration = addSetting(new NumberSetting("duration", "Длительность (с)", 3.2, 1.5, 8.0, 0.2).withEn("Duration (s)"));
    public final NumberSetting maxToasts = addSetting(new NumberSetting("max_toasts", "Макс. тостов", 5.0, 3.0, 8.0, 1.0).withEn("Max Toasts"));
    public final NumberSetting opacity = addSetting(new NumberSetting("opacity", "Плотность стекла", 0.72, 0.30, 0.94, 0.02).withEn("Glass Opacity"));
    public final ModeSetting palette = addSetting(new ModeSetting("palette", "Палитра", "Тема", "Статика", "Радуга", "Тема", "Градиент").withEn("Palette"));
    public final ColorSetting accentColor = addSetting(new ColorSetting("accent_color", "Цвет акцента", 0xFF7C5CFF).withEn("Accent Color"));
    public final NumberSetting colorSpeed = addSetting(new NumberSetting("color_speed", "Скорость цвета", 1.0, 0.2, 4.0, 0.1).withEn("Color Speed"));

    public NotificationsModule() {
        super("notifications", "Уведомления", "Стеклянные всплывающие уведомления о событиях и переключении модулей", Category.CLIENT);
        setEn("Notifications", "Glass toast notifications for events and module toggles");
        setEnabled(true);
    }

    @Override
    protected void onDisable() {
        ToastManager.get().clear();
    }

    @Override
    public String getDisplaySuffix() {
        return position.getValue();
    }
}
