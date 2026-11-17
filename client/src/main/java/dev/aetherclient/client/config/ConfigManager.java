package dev.aetherclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.ModuleManager;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.KeybindSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import dev.aetherclient.client.module.setting.Setting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

/**
 * Syncs theme settings with AetherLoader's loader_settings.json and persists
 * in-game module states, settings, and HUD widget positions to configs/client.json.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path rootDir;
    private final Path loaderStateFile;
    private final Path clientConfigFile;

    private long lastLoaderModified = -1L;
    private int syncedSeedArgb = 0xFF7C5CFF;
    private boolean syncedDarkMode = true;
    private String syncedLanguage = "RU";

    public ConfigManager() {
        this.rootDir = resolveRootDir();
        this.loaderStateFile = rootDir.resolve("loader_settings.json");
        this.clientConfigFile = rootDir.resolve("configs").resolve("client.json");
    }

    public int getSyncedSeedArgb() {
        return syncedSeedArgb;
    }

    public boolean isSyncedDarkMode() {
        return syncedDarkMode;
    }

    public String getSyncedLanguage() {
        return syncedLanguage;
    }

    public boolean pollLoaderThemeIfChanged() {
        try {
            if (!Files.exists(loaderStateFile)) return false;
            long mod = Files.getLastModifiedTime(loaderStateFile).toMillis();
            if (mod == lastLoaderModified) return false;
            String json = Files.readString(loaderStateFile, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            boolean changed = false;
            if (obj.has("seedColorHex")) {
                int newSeed = (int) obj.get("seedColorHex").getAsLong();
                if (newSeed != syncedSeedArgb) {
                    syncedSeedArgb = newSeed;
                    changed = true;
                }
            }
            if (obj.has("darkMode")) {
                boolean newDark = obj.get("darkMode").getAsBoolean();
                if (newDark != syncedDarkMode) {
                    syncedDarkMode = newDark;
                    changed = true;
                }
            }
            if (obj.has("language")) {
                String newLang = obj.get("language").getAsString();
                if (newLang != null && !newLang.equalsIgnoreCase(syncedLanguage)) {
                    syncedLanguage = "EN".equalsIgnoreCase(newLang) ? "EN" : "RU";
                    changed = true;
                }
            }
            lastLoaderModified = mod;
            return changed;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void loadClientConfig(ModuleManager moduleManager, List<HudElement> hudElements) {
        pollLoaderThemeIfChanged();
        loadConfigFromFile(clientConfigFile, moduleManager, hudElements);
    }

    public boolean loadConfigFromFile(Path targetFile, ModuleManager moduleManager, List<HudElement> hudElements) {
        if (!Files.exists(targetFile)) return false;
        try {
            String raw = Files.readString(targetFile, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();

            if (root.has("modules") && root.get("modules").isJsonObject()) {
                JsonObject modsObj = root.getAsJsonObject("modules");
                for (Module module : moduleManager.getModules()) {
                    try {
                        if (!modsObj.has(module.getId()) || !modsObj.get(module.getId()).isJsonObject()) continue;
                        JsonObject modJson = modsObj.getAsJsonObject(module.getId());
                        if (modJson.has("settings") && modJson.get("settings").isJsonObject()) {
                            JsonObject settingsJson = modJson.getAsJsonObject("settings");
                            for (Setting<?> setting : module.getSettings()) {
                                JsonElement el = settingsJson.get(setting.getId());
                                if (el == null || el.isJsonNull()) continue;
                                applySetting(setting, el);
                                if (setting instanceof BooleanSetting bs) {
                                    bs.animProgress = bs.isEnabled() ? 1f : 0f;
                                }
                            }
                        }
                        if (modJson.has("enabled")) {
                            module.setEnabled(modJson.get("enabled").getAsBoolean());
                            module.toggleAnim = module.isEnabled() ? 1f : 0f;
                        }
                        if (modJson.has("expanded") && modJson.get("expanded").isJsonPrimitive()) {
                            module.expanded = modJson.get("expanded").getAsBoolean();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            if (root.has("hud") && root.get("hud").isJsonObject()) {
                JsonObject hudObj = root.getAsJsonObject("hud");
                for (HudElement element : hudElements) {
                    try {
                        if (!hudObj.has(element.getId()) || !hudObj.get(element.getId()).isJsonObject()) continue;
                        JsonObject pos = hudObj.getAsJsonObject(element.getId());
                        float nx = pos.has("x") ? pos.get("x").getAsFloat() : element.getNormX();
                        float ny = pos.has("y") ? pos.get("y").getAsFloat() : element.getNormY();
                        element.setNormPosition(nx, ny);
                    } catch (Exception ignored) {
                    }
                }
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void saveClientConfig(ModuleManager moduleManager, List<HudElement> hudElements) {
        saveConfigToFile(clientConfigFile, moduleManager, hudElements);
    }

    public boolean saveConfigToFile(Path targetFile, ModuleManager moduleManager, List<HudElement> hudElements) {
        try {
            Path parent = targetFile.getParent();
            if (parent != null) Files.createDirectories(parent);
            JsonObject root = new JsonObject();

            JsonObject modsObj = new JsonObject();
            for (Module module : moduleManager.getModules()) {
                JsonObject modJson = new JsonObject();
                modJson.addProperty("enabled", module.isEnabled());
                modJson.addProperty("expanded", module.expanded);
                JsonObject settingsJson = new JsonObject();
                for (Setting<?> s : module.getSettings()) {
                    switch (s) {
                        case BooleanSetting bs -> settingsJson.addProperty(bs.getId(), bs.isEnabled());
                        case NumberSetting ns -> settingsJson.addProperty(ns.getId(), ns.getValue());
                        case ModeSetting ms -> settingsJson.addProperty(ms.getId(), ms.getValue());
                        case ColorSetting cs -> settingsJson.addProperty(cs.getId(), cs.getValue());
                        case KeybindSetting ks -> settingsJson.addProperty(ks.getId(), ks.getValue());
                        default -> {}
                    }
                }
                modJson.add("settings", settingsJson);
                modsObj.add(module.getId(), modJson);
            }
            root.add("modules", modsObj);

            JsonObject hudObj = new JsonObject();
            for (HudElement element : hudElements) {
                JsonObject pos = new JsonObject();
                pos.addProperty("x", element.getNormX());
                pos.addProperty("y", element.getNormY());
                hudObj.add(element.getId(), pos);
            }
            root.add("hud", hudObj);

            Path tmp = targetFile.resolveSibling(targetFile.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, targetFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicUnsupported) {
                Files.move(tmp, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public List<String> listAvailableConfigs() {
        List<String> list = new java.util.ArrayList<>();
        try {
            Path configsDir = rootDir.resolve("configs");
            if (Files.exists(configsDir)) {
                try (var stream = Files.list(configsDir)) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".json") && !p.getFileName().toString().endsWith(".tmp"))
                            .forEach(p -> {
                                String name = p.getFileName().toString();
                                list.add(name.substring(0, name.length() - 5));
                            });
                }
            }
        } catch (Exception ignored) {}
        if (!list.contains("client")) {
            list.add(0, "client");
        }
        return list;
    }

    public boolean saveNamedConfig(String name, ModuleManager moduleManager, List<HudElement> hudElements) {
        if (name == null || name.isBlank()) return false;
        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
        Path target = rootDir.resolve("configs").resolve(sanitized + ".json");
        return saveConfigToFile(target, moduleManager, hudElements);
    }

    public boolean loadNamedConfig(String name, ModuleManager moduleManager, List<HudElement> hudElements) {
        if (name == null || name.isBlank()) return false;
        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
        Path target = rootDir.resolve("configs").resolve(sanitized + ".json");
        return loadConfigFromFile(target, moduleManager, hudElements);
    }

    public boolean deleteNamedConfig(String name) {
        if (name == null || name.isBlank() || name.equals("client")) return false;
        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
        Path target = rootDir.resolve("configs").resolve(sanitized + ".json");
        try {
            return Files.deleteIfExists(target);
        } catch (Exception e) {
            return false;
        }
    }

    private static void applySetting(Setting<?> setting, JsonElement el) {
        try {
            switch (setting) {
                case BooleanSetting bs -> bs.setValue(el.getAsBoolean());
                case NumberSetting ns -> ns.setValue(el.getAsDouble());
                case ModeSetting ms -> ms.setValue(el.getAsString());
                case ColorSetting cs -> cs.setValue(el.getAsInt());
                case KeybindSetting ks -> ks.setValue(el.getAsInt());
                default -> {}
            }
        } catch (Exception ignored) {
        }
    }

    private static Path resolveRootDir() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Paths.get(appData, "AetherClient");
            }
            return Paths.get(home, "AppData", "Roaming", "AetherClient");
        }
        if (os.contains("mac")) {
            return Paths.get(home, "Library", "Application Support", "AetherClient");
        }
        String xdg = System.getenv("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Paths.get(xdg, "AetherClient");
        }
        return Paths.get(home, ".local", "share", "AetherClient");
    }

    public java.io.File getRootDir() {
        return rootDir.toFile();
    }
}
