package dev.aetherclient.client.module.setting;

public final class BooleanSetting extends Setting<Boolean> {
    public float animProgress;

    public BooleanSetting(String id, String name, boolean defaultValue) {
        super(id, name, defaultValue);
        this.animProgress = defaultValue ? 1f : 0f;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(getValue());
    }

    public void toggle() {
        setValue(!isEnabled());
    }

    @Override
    public void resetToDefault() {
        super.resetToDefault();
        animProgress = isEnabled() ? 1f : 0f;
    }
}
