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
    public final BooleanSetting maskInChat = addSetting(new BooleanSetting("mask_chat", "В чате", true).withEn("In Chat"));
    public final BooleanSetting maskOthers = addSetting(new BooleanSetting("mask_others", "Скрывать чужие ники (Режим стримера)", false).withEn("Mask Other Players"));

    public NameProtectModule() {
        super("name_protect", "Скрыть ник", "Заменяет ваш никнейм и ники игроков на выбранный псевдоним во всех элементах интерфейса (чат, таб, скорборд, ESP)", Category.CLIENT);
        setEn("Name Protect", "Masks your username and opponents' names across widgets, chat, tab, scoreboard and ESP");
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
