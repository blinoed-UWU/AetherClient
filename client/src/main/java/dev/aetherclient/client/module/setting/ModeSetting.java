package dev.aetherclient.client.module.setting;

import java.util.List;

public final class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String id, String name, String defaultMode, String... modes) {
        super(id, name, defaultMode);
        this.modes = List.of(modes);
    }

    public List<String> getModes() {
        return modes;
    }

    @Override
    public void setValue(String mode) {
        if (mode != null && modes.contains(mode)) {
            super.setValue(mode);
        }
    }

    public void cycle(int dir) {
        if (modes.isEmpty()) return;
        int idx = modes.indexOf(getValue());
        if (idx < 0) idx = 0;
        int next = (idx + dir + modes.size()) % modes.size();
        setValue(modes.get(next));
    }
}
