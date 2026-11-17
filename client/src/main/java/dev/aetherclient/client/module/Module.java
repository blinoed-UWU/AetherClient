package dev.aetherclient.client.module;

import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.setting.KeybindSetting;
import dev.aetherclient.client.module.setting.Setting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all Aether Client modules.
 */
public abstract class Module {
    private final String id;
    private final String name;
    private String nameEn;
    private final String description;
    private String descriptionEn;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final KeybindSetting keybind;

    private boolean enabled;
    public float toggleAnim;
    public float hoverAnim;
    public float expandAnim;
    public float pressAnim;
    public float rippleAnim;
    public float appearAnim = 1f;
    public boolean expanded;
    public final dev.aetherclient.client.gui.Spring pressSpring = new dev.aetherclient.client.gui.Spring(150f, 16f);

    protected Module(String id, String name, String description, Category category, int defaultKey) {
        this.id = id;
        this.name = name;
        this.nameEn = name;
        this.description = description;
        this.descriptionEn = description;
        this.category = category;
        this.keybind = new KeybindSetting("keybind", "Клавиша", defaultKey).withEn("Keybind");
        this.settings.add(keybind);
    }

    protected Module(String id, String name, String description, Category category) {
        this(id, name, description, category, GLFW.GLFW_KEY_UNKNOWN);
    }

    protected void setEn(String nameEn, String descriptionEn) {
        this.nameEn = nameEn;
        this.descriptionEn = descriptionEn;
    }

    protected <S extends Setting<?>> S addSetting(S setting) {
        settings.add(setting);
        return setting;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return I18n.tr(name, nameEn);
    }

    public String getDescription() {
        return I18n.tr(description, descriptionEn);
    }

    public Category getCategory() {
        return category;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public KeybindSetting getKeybind() {
        return keybind;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    protected void onEnable() {}

    protected void onDisable() {}

    public void onTick(MinecraftClient mc) {}

    public void onRender2D(MinecraftClient mc, float screenW, float screenH, float dt) {}

    public String getDisplaySuffix() {
        return "";
    }

    public void resetSettings() {
        for (Setting<?> s : settings) {
            s.resetToDefault();
        }
    }
}
