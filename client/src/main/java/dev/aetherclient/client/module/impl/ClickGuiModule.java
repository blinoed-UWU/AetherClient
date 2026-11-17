package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public final class ClickGuiModule extends Module {
    public final ModeSetting language = addSetting(new ModeSetting("language", "Язык", "RU", "RU", "EN").withEn("Language"));
    public final BooleanSetting syncTheme = addSetting(new BooleanSetting("sync_theme", "Тема из лаунчера", true).withEn("Sync Launcher Theme"));
    public final BooleanSetting darkMode = addSetting(
            new BooleanSetting("dark_mode", "Тёмная тема", true).<BooleanSetting>withEn("Dark Mode").visibleIf(() -> !syncTheme.isEnabled())
    );
    public final ColorSetting accentColor = addSetting(
            new ColorSetting("accent_color", "Акцентный цвет", 0xFF7C5CFF).<ColorSetting>withEn("Accent Color").visibleIf(() -> !syncTheme.isEnabled())
    );
    public final NumberSetting guiScale = addSetting(new NumberSetting("gui_scale", "Масштаб меню", 1.0, 0.65, 1.80, 0.05).withEn("Menu Scale"));
    public final NumberSetting blurRadius = addSetting(new NumberSetting("blur_radius", "Размытие фона", 10.0, 0.0, 18.0, 0.5).withEn("Blur Radius"));
    public final NumberSetting glassOpacity = addSetting(new NumberSetting("glass_opacity", "Плотность стекла", 0.72, 0.30, 0.94, 0.02).withEn("Glass Opacity"));
    public final NumberSetting cornerRadius = addSetting(new NumberSetting("corner_radius", "Скругление", 15.0, 8.0, 22.0, 1.0).withEn("Corner Radius"));
    public final BooleanSetting uiSounds = addSetting(new BooleanSetting("ui_sounds", "Звуки интерфейса", false).withEn("UI Sounds"));
    public final NumberSetting soundVolume = addSetting(
            new NumberSetting("sound_volume", "Громкость звуков", 0.55, 0.05, 1.0, 0.05).<NumberSetting>withEn("Sound Volume").visibleIf(uiSounds::isEnabled)
    );
    public final NumberSetting panicKey = addSetting(
            new NumberSetting("panic_key", "Клавиша анхука (паника)", 0.0, 0.0, 350.0, 1.0).withEn("Panic / Unhook Key")
    );

    public ClickGuiModule() {
        super("clickgui", "Меню", "Главное окно настроек и редактор HUD", Category.CLIENT, GLFW.GLFW_KEY_RIGHT_SHIFT);
        setEn("ClickGUI", "Main settings window and HUD editor");
    }
}
