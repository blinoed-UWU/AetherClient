package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.audio.SoundEngine;
import dev.aetherclient.client.friend.FriendsManager;
import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import dev.aetherclient.client.notify.ToastManager;
import net.minecraft.client.MinecraftClient;

import java.util.Locale;

/**
 * Auto TpAccept ("Авто-принятие телепорта").
 * Automatically accepts incoming teleport requests, optionally filtering only for friends.
 */
public final class AutoTpAcceptModule extends Module {
    public final BooleanSetting onlyFriends = addSetting(
            new BooleanSetting("only_friends", "Только от друзей", true).withEn("Only Friends")
    );
    public final NumberSetting delayMs = addSetting(
            new NumberSetting("delay", "Задержка принятия (мс)", 500.0, 0.0, 3000.0, 100.0).withEn("Accept Delay")
    );
    public final ModeSetting command = addSetting(
            new ModeSetting("command", "Команда", "/tpaccept", "/tpaccept", "/tpyes", "/call accept").withEn("Command")
    );
    public final BooleanSetting notify = addSetting(
            new BooleanSetting("notify", "Уведомление в тост", true).withEn("Show Toast")
    );
    public final BooleanSetting playSound = addSetting(
            new BooleanSetting("sound", "Звуковой эффект", true).withEn("Play Sound")
    );

    private String lastAcceptedMsg = "";
    private long lastAcceptTime = 0L;

    public AutoTpAcceptModule() {
        super("auto_tp_accept", "Авто-телепорт (TpAccept)", "Автоматически принимает запросы на телепортацию от друзей с настраиваемой задержкой", Category.COMBAT);
        setEn("Auto TpAccept", "Automatically accepts teleport requests from friends with configurable delay");
    }

    public void onChatMessage(String rawMessage) {
        if (!isEnabled() || rawMessage == null || rawMessage.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.getNetworkHandler() == null) return;

        String lower = rawMessage.toLowerCase(Locale.ROOT);
        boolean isTpRequest = lower.contains("телепортироваться")
                || lower.contains("телепортаци")
                || lower.contains("teleport request")
                || lower.contains("has requested to teleport")
                || lower.contains("/tpaccept")
                || lower.contains("/tpyes");

        if (!isTpRequest) return;

        long now = System.currentTimeMillis();
        if (rawMessage.equals(lastAcceptedMsg) && (now - lastAcceptTime) < 2000L) {
            return;
        }

        if (onlyFriends.isEnabled()) {
            boolean isFromFriend = false;
            FriendsManager fm = FriendsManager.get();
            if (fm != null) {
                for (String friend : fm.getFriends()) {
                    if (friend != null && !friend.isBlank() && lower.contains(friend.toLowerCase(Locale.ROOT))) {
                        isFromFriend = true;
                        break;
                    }
                }
            }
            if (!isFromFriend) return;
        }

        lastAcceptedMsg = rawMessage;
        lastAcceptTime = now;

        long delay = (long) delayMs.asFloat();
        String cmd = command.getValue();

        new Thread(() -> {
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
            }
            mc.execute(() -> {
                if (mc.player != null && mc.getNetworkHandler() != null) {
                    String cleanCmd = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                    mc.getNetworkHandler().sendChatCommand(cleanCmd);

                    if (playSound.isEnabled()) {
                        SoundEngine.get().playClick(mc, true, 1.3f);
                    }
                    if (notify.isEnabled()) {
                        ToastManager.get().push("TpAccept", "Запрос телепорта принят (" + cmd + ")", 0xFF10B981);
                    }
                }
            });
        }, "Aether-AutoTpAccept").start();
    }
}
