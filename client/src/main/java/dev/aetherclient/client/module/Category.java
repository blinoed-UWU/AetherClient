package dev.aetherclient.client.module;

import dev.aetherclient.client.i18n.I18n;

public enum Category {
    VISUAL("Визуалы", "Visuals"),
    HUD("Интерфейс", "HUD"),
    COMBAT("Бой", "Combat"),
    CLIENT("Клиент", "Client");

    private final String displayName;
    private final String displayNameEn;

    Category(String displayName, String displayNameEn) {
        this.displayName = displayName;
        this.displayNameEn = displayNameEn;
    }

    public String getDisplayName() {
        return I18n.tr(displayName, displayNameEn);
    }
}
