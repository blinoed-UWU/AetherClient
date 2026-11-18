package dev.aetherclient.client.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pure Java 21 Discord IPC client.
 * Connects directly to Discord Unix Domain Socket (/run/user/UID/discord-ipc-0) on Linux/macOS
 * or Windows Named Pipe (\\.\pipe\discord-ipc-0) with zero JNI native binaries.
 * Built with strict asynchronous timeouts to prevent any game thread freezes on Windows.
 */
public final class DiscordIpcClient implements AutoCloseable {
    private static final String DEFAULT_CLIENT_ID = "1311091798826942504";

    private final String clientId;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private ByteChannel channel;
    private final long startTimestamp = System.currentTimeMillis();
    private long lastConnectAttempt = 0L;

    private static final ExecutorService IO_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Aether-DiscordIPC-IO");
        t.setDaemon(true);
        return t;
    });

    public static void shutdownExecutor() {
        try {
            IO_EXECUTOR.shutdownNow();
        } catch (Throwable ignored) {}
    }

    public DiscordIpcClient() {
        this(DEFAULT_CLIENT_ID);
    }

    public DiscordIpcClient(String clientId) {
        this.clientId = clientId;
    }

    public synchronized boolean connect() {
        if (connected.get()) return true;

        long now = System.currentTimeMillis();
        // Cooldown: do not retry faster than every 8 seconds if connection failed
        if (now - lastConnectAttempt < 8000) {
            return false;
        }
        lastConnectAttempt = now;

        Future<Boolean> future = IO_EXECUTOR.submit(() -> {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                return connectWindows();
            } else {
                return connectUnix();
            }
        });

        try {
            // Strict 1500ms timeout for connection handshake so game never hangs
            return future.get(1500, TimeUnit.MILLISECONDS);
        } catch (Throwable t) {
            future.cancel(true);
            closeQuietly();
            return false;
        }
    }

    private boolean connectUnix() {
        String uid = System.getenv("XDG_RUNTIME_DIR");
        Path runtimeDir = (uid != null && !uid.isBlank()) ? Paths.get(uid) : Paths.get("/tmp");

        for (int i = 0; i < 10; i++) {
            Path sockPath = runtimeDir.resolve("discord-ipc-" + i);
            if (!Files.exists(sockPath)) {
                sockPath = Paths.get("/tmp/discord-ipc-" + i);
            }
            if (Files.exists(sockPath)) {
                try {
                    UnixDomainSocketAddress address = UnixDomainSocketAddress.of(sockPath);
                    SocketChannel sc = SocketChannel.open(StandardProtocolFamily.UNIX);
                    sc.connect(address);
                    this.channel = sc;

                    JsonObject hs = new JsonObject();
                    hs.addProperty("v", 1);
                    hs.addProperty("client_id", clientId);
                    sendPacket(0, hs.toString());

                    byte[] reply = readPacket();
                    if (reply != null) {
                        connected.set(true);
                        return true;
                    }
                } catch (Throwable t) {
                    closeQuietly();
                }
            }
        }
        return false;
    }

    private boolean connectWindows() {
        for (int i = 0; i < 10; i++) {
            File pipeFile = new File("\\\\.\\pipe\\discord-ipc-" + i);
            if (!pipeFile.exists()) continue; // Skip non-existent pipes immediately without blocking
            try {
                RandomAccessFile raf = new RandomAccessFile(pipeFile, "rw");
                this.channel = raf.getChannel();

                JsonObject hs = new JsonObject();
                hs.addProperty("v", 1);
                hs.addProperty("client_id", clientId);
                sendPacket(0, hs.toString());

                byte[] reply = readPacket();
                if (reply != null) {
                    connected.set(true);
                    return true;
                }
            } catch (Throwable t) {
                closeQuietly();
            }
        }
        return false;
    }

    public synchronized void updatePresence(String details, String state, String serverIp) {
        if (!connected.get()) {
            if (!connect()) return;
        }

        try {
            JsonObject activity = new JsonObject();
            if (details != null && !details.isBlank()) {
                activity.addProperty("details", details);
            }
            if (state != null && !state.isBlank()) {
                activity.addProperty("state", state);
            }

            // Elapsed playtime
            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", startTimestamp);
            activity.add("timestamps", timestamps);

            // Assets (Avatar and logo)
            JsonObject assets = new JsonObject();
            assets.addProperty("large_image", "aether");
            assets.addProperty("large_text", "AetherClient 1.21.11");
            assets.addProperty("small_image", "tg");
            assets.addProperty("small_text", "@shadowwtls");
            activity.add("assets", assets);

            // Telegram channel link button
            JsonArray buttons = new JsonArray();
            JsonObject tgBtn = new JsonObject();
            tgBtn.addProperty("label", "Telegram Канал");
            tgBtn.addProperty("url", "https://t.me/shadowwtls");
            buttons.add(tgBtn);
            activity.add("buttons", buttons);

            JsonObject args = new JsonObject();
            args.addProperty("pid", (int) ProcessHandle.current().pid());
            args.add("activity", activity);

            JsonObject root = new JsonObject();
            root.addProperty("cmd", "SET_ACTIVITY");
            root.add("args", args);
            root.addProperty("nonce", String.valueOf(System.nanoTime()));

            sendPacket(1, root.toString());
        } catch (Throwable t) {
            connected.set(false);
            closeQuietly();
        }
    }

    public synchronized void clearPresence() {
        if (!connected.get() || channel == null) return;
        try {
            JsonObject args = new JsonObject();
            args.addProperty("pid", (int) ProcessHandle.current().pid());
            JsonObject root = new JsonObject();
            root.addProperty("cmd", "SET_ACTIVITY");
            root.add("args", args);
            root.addProperty("nonce", String.valueOf(System.nanoTime()));
            sendPacket(1, root.toString());
        } catch (Throwable ignored) {}
    }

    private void sendPacket(int op, String json) throws IOException {
        if (channel == null || !channel.isOpen()) throw new IOException("Channel closed");
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(op);
        buf.putInt(payload.length);
        buf.put(payload);
        buf.flip();
        while (buf.hasRemaining()) {
            channel.write(buf);
        }
    }

    private byte[] readPacket() throws IOException {
        if (channel == null || !channel.isOpen()) return null;
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        while (header.hasRemaining()) {
            int read = channel.read(header);
            if (read == -1) return null;
        }
        header.flip();
        int op = header.getInt();
        int length = header.getInt();
        if (length < 0 || length > 65536) return null;

        ByteBuffer body = ByteBuffer.allocate(length);
        while (body.hasRemaining()) {
            int read = channel.read(body);
            if (read == -1) return null;
        }
        return body.array();
    }

    public boolean isConnected() {
        return connected.get();
    }

    private void closeQuietly() {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (Throwable ignored) {}
        channel = null;
    }

    @Override
    public synchronized void close() {
        connected.set(false);
        clearPresence();
        closeQuietly();
    }
}
