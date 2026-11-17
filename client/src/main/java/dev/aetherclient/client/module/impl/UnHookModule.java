package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.AetherClientMain;
import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * UnHook Module ("Анхук / Выгрузка").
 * Fully detaches the client from the running game, restores standard window title,
 * wipes chat traces of client commands, and cleans temporary memory structures.
 */
public final class UnHookModule extends Module {
    public final BooleanSetting cleanChat = new BooleanSetting("clean_chat", "Очистить чат от следов чита", true);
    public final BooleanSetting wipeLogs = new BooleanSetting("wipe_logs", "Удалить временные логи", true);

    public UnHookModule() {
        super("unhook", "Анхук (Выгрузка чита)", "Полностью и бесследно выгружает клиент из запущенной игры прямо во время матча", Category.CLIENT);
        addSetting(cleanChat);
        addSetting(wipeLogs);
        setEnabled(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            triggerUnhook();
        }
    }

    public void triggerUnhook() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (cleanChat.isEnabled() && mc != null && mc.inGameHud != null) {
            try {
                // Clear chat hud messages that might contain client prefixes
                mc.inGameHud.getChatHud().clear(false);
            } catch (Throwable ignored) {}
        }

        if (wipeLogs.isEnabled()) {
            try {
                String userHome = System.getProperty("user.home", ".");
                Path clientDir = Paths.get(userHome, ".local", "share", "AetherClient", "logs");
                if (Files.exists(clientDir)) {
                    try (var stream = Files.list(clientDir)) {
                        stream.forEach(f -> {
                            try { Files.deleteIfExists(f); } catch (Throwable ignored) {}
                        });
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (mc != null && mc.getWindow() != null) {
            try {
                mc.getWindow().setTitle("Minecraft 1.21.1");
            } catch (Throwable ignored) {}
        }

        AetherClientMain.requestUnload();
    }
}
