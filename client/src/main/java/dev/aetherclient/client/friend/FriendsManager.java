package dev.aetherclient.client.friend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public final class FriendsManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static FriendsManager instance;

    private final File file;
    private final Set<String> friends = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    public FriendsManager(File rootDir) {
        this.file = new File(rootDir, "friends.json");
        instance = this;
        load();
    }

    public static FriendsManager get() {
        return instance;
    }

    public synchronized boolean isFriend(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        return friends.contains(name.trim());
    }

    public synchronized boolean addFriend(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        boolean added = friends.add(name.trim());
        if (added) save();
        return added;
    }

    public synchronized boolean removeFriend(String name) {
        if (name == null) return false;
        boolean removed = friends.remove(name.trim());
        if (removed) save();
        return removed;
    }

    public synchronized void clear() {
        friends.clear();
        save();
    }

    public synchronized Set<String> getFriends() {
        return Collections.unmodifiableSet(new TreeSet<>(friends));
    }

    public synchronized void load() {
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Set<String> loaded = GSON.fromJson(reader, new TypeToken<Set<String>>(){}.getType());
            if (loaded != null) {
                friends.clear();
                for (String s : loaded) {
                    if (s != null && !s.trim().isEmpty()) {
                        friends.add(s.trim());
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[AetherClient] Failed to load friends: " + t.getMessage());
        }
    }

    public synchronized void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(friends, writer);
            }
        } catch (Throwable t) {
            System.err.println("[AetherClient] Failed to save friends: " + t.getMessage());
        }
    }
}
