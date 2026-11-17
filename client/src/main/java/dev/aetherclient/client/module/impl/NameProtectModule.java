package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;

import java.util.regex.Pattern;

public final class NameProtectModule extends Module {
    public final ModeSetting alias = addSetting(
            new ModeSetting("alias", "Псевдоним", "AetherUser", "AetherUser", "Protected", "Anonymous", "You", "Ghost", "Legend").withEn("Alias")
    );
    public final BooleanSetting maskInScoreboard = addSetting(new BooleanSetting("mask_scoreboard", "В скорборде", true).withEn("In Scoreboard"));
    public final BooleanSetting maskInTab = addSetting(new BooleanSetting("mask_tab", "В табе", true).withEn("In Tab"));
    public final BooleanSetting maskInNametag = addSetting(new BooleanSetting("mask_nametag", "В именах", true).withEn("In Nametags"));
    public final BooleanSetting maskInTargetHud = addSetting(new BooleanSetting("mask_targethud", "В Target HUD", true).withEn("In Target HUD"));

    public NameProtectModule() {
        super("name_protect", "Скрыть ник", "Заменяет ваш никнейм на выбранный псевдоним во всех элементах интерфейса", Category.CLIENT);
        setEn("Name Protect", "Masks your username with a chosen alias across widgets, chat, tab and scoreboard");
    }

    public String mask(String rawName) {
        if (!isEnabled() || rawName == null || rawName.isEmpty()) return rawName;
        return alias.getValue();
    }

    public String filterText(String text, String clientPlayerName) {
        if (!isEnabled() || text == null || text.isEmpty()) return text;
        if (clientPlayerName == null || clientPlayerName.isEmpty()) return text;
        return text.replaceAll("(?i)" + Pattern.quote(clientPlayerName), alias.getValue());
    }
}
