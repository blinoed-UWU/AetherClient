package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.discord.DiscordIpcClient;
import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DiscordRpcModule extends Module {
    public final BooleanSetting showServer = addSetting(new BooleanSetting("show_server", "Показывать сервер", true).withEn("Show Server"));
    public final BooleanSetting showName = addSetting(new BooleanSetting("show_name", "Показывать ник", true).withEn("Show Nickname"));

    private DiscordIpcClient client;
    private long lastUpdateMs = 0;
    private final AtomicBoolean updating = new AtomicBoolean(false);
    private ExecutorService rpcExecutor;

    public DiscordRpcModule() {
        super("discord_rpc", "Discord RPC", "Отображение красивого статуса в профиле Discord", Category.CLIENT);
        setEn("Discord RPC", "Rich Presence status in your Discord profile");
    }

    /** Guards against queued tasks re-creating the IPC client after disable(). */
    private volatile boolean disabled = true;

    @Override
    protected void onEnable() {
        disabled = false;
        initExecutor();
        lastUpdateMs = 0;
        rpcExecutor.execute(() -> {
            try {
                if (client == null) {
                    client = new DiscordIpcClient();
                }
                client.connect();
            } catch (Throwable ignored) {}
        });
    }

    @Override
    protected void onDisable() {
        disabled = true;
        if (rpcExecutor != null) {
            java.util.concurrent.ExecutorService exec = rpcExecutor;
            exec.execute(() -> {
                if (client != null) {
                    client.close();
                    client = null;
                }
            });
            // shutdownNow: pending presence ticks must not run after close and
            // recreate the IPC client (they guard on 'disabled' anyway).
            exec.shutdownNow();
            rpcExecutor = null;
        }
    }

    private synchronized void initExecutor() {
        if (rpcExecutor == null || rpcExecutor.isShutdown()) {
            rpcExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Aether-DiscordRPC-Worker");
                t.setDaemon(true);
                return t;
            });
        }
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled() || disabled) return;
        long now = System.currentTimeMillis();
        if (now - lastUpdateMs < 4000) return; // update every 4s
        lastUpdateMs = now;

        if (mc == null) return;

        final String details;
        if (showName.isEnabled() && mc.player != null) {
            details = mc.player.getName().getString() + " • v1.21.11";
        } else {
            details = "AetherClient • v1.21.11";
        }

        String stateStr = "В главном меню";
        String serverIpStr = null;

        if (mc.world != null) {
            if (mc.isInSingleplayer()) {
                stateStr = "Одиночная игра";
            } else {
                ServerInfo sInfo = mc.getCurrentServerEntry();
                if (sInfo != null && showServer.isEnabled()) {
                    stateStr = "Сервер: " + sInfo.address;
                    serverIpStr = sInfo.address;
                } else {
                    stateStr = "Сетевая игра";
                }
            }
        }
        final String state = stateStr;
        final String serverIp = serverIpStr;

        initExecutor();

        // Offload all Discord IPC work to the background daemon executor
        // Game render thread NEVER waits for I/O and NEVER freezes
        if (updating.compareAndSet(false, true)) {
            rpcExecutor.execute(() -> {
                try {
                    if (client == null) {
                        client = new DiscordIpcClient();
                    }
                    client.updatePresence(details, state, serverIp);
                } catch (Throwable ignored) {
                } finally {
                    updating.set(false);
                }
            });
        }
    }
}
