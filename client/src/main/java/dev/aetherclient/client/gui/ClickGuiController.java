package dev.aetherclient.client.gui;

import dev.aetherclient.client.audio.SoundEngine;
import dev.aetherclient.client.config.ConfigManager;
import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.ModuleManager;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ColorSetting;
import dev.aetherclient.client.module.setting.KeybindSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import dev.aetherclient.client.module.setting.Setting;
import dev.aetherclient.client.notify.ToastManager;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.render.Render2D.IconType;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Compact Material You + Liquid Glass ClickGUI and HUD Editor with 0.5s fluid
 * water-gather / water-disperse transitions, cascade card entrances, ripple waves,
 * and procedural OpenAL UI sound feedback.
 */
public final class ClickGuiController {
    private static final float OPEN_CLOSE_DURATION_SEC = 0.50f;
    private static final int[] SWATCH_COLORS = {
            0xFF7C5CFF, 0xFF38BDF8, 0xFF10B981, 0xFFF43F5E, 0xFFF59E0B, 0xFFEC4899, 0xFF8B5CF6
    };

    private final ModuleManager moduleManager;
    private final ConfigManager configManager;
    private final List<HudElement> hudElements;
    private final SoundEngine soundEngine;
    private final Runnable unloadCallback;

    private boolean open = false;
    private boolean hudEditMode = false;
    private float rawOpen = 0f;
    private float openAnim = 0f;

    private Category selectedCategory = null; // null = Все
    private float railIndicatorY = -1f;
    private final float[] catHoverAnim = new float[5];
    private int lastHoveredCategory = -1;
    private Module lastHoveredModule = null;

    private String searchQuery = "";
    private boolean searchFocused = false;
    private float searchHoverAnim = 0f;
    public static final int TAB_MODULES = 0;
    public static final int TAB_CONFIGS = 1;
    public static final int TAB_INFO = 2;
    public static final int TAB_FRIENDS = 3;
    private int activeTopTab = TAB_MODULES;

    private float modTabHoverAnim = 0f;
    private float hudTabHoverAnim = 0f;
    private float unloadHoverAnim = 0f;

    private float cfgTabHoverAnim = 0f;
    private float infoTabHoverAnim = 0f;
    private float friendsTabHoverAnim = 0f;
    private float cfgTabX, cfgTabY, cfgTabW, cfgTabH;
    private float infoTabX, infoTabY, infoTabW, infoTabH;
    private float friendsTabX, friendsTabY, friendsTabW, friendsTabH;

    private String newConfigName = "";
    private boolean configNameFocused = false;
    private String activeConfigName = "client";

    private String newFriendName = "";
    private boolean friendNameFocused = false;

    private float targetScroll = 0f;
    private float currentScroll = 0f;
    private float maxScroll = 0f;
    private float scrollActivity = 0f;

    private float mouseX = 0f;
    private float mouseY = 0f;
    private float hudMouseX = 0f;
    private float hudMouseY = 0f;

    private NumberSetting draggingSlider = null;
    private float sliderTrackX = 0f;
    private float sliderTrackW = 1f;

    private KeybindSetting listeningKeybind = null;

    private HudElement draggingHud = null;
    private float hudDragOffsetX = 0f;
    private float hudDragOffsetY = 0f;
    private boolean snapXCenter = false;
    private boolean snapYCenter = false;
    private boolean isChatDragging = false;
    // Smart guide state: which alignment lines are active while dragging
    private final List<Float> snapGuideXs = new ArrayList<>(4);
    private final List<Float> snapGuideYs = new ArrayList<>(4);
    private static final float SNAP_EDGE_MARGIN = 10f;
    private static final float SNAP_TOLERANCE = 6f;
    private static final float SNAP_GRID_PX = 4f;

    // Last computed hit rects for click handling
    private float pX, pY, pW, pH;
    private float modTabX, modTabY, modTabW, modTabH;
    private float hudTabX, hudTabY, hudTabW, hudTabH;
    private float searchX, searchY, searchW, searchH;
    private float unloadX, unloadY, unloadW, unloadH;
    private float doneHudX, doneHudY, doneHudW, doneHudH;
    private float bodyX, bodyY, bodyW, bodyH;

    private Module contextModule = null;
    private float contextMenuX = 0f;
    private float contextMenuY = 0f;
    private float contextMenuW = 138f;
    private float contextMenuH = 0f;
    private final Spring contextSpring = new Spring(220f, 22f);

    // Settings view spring animations
    private float settingsViewAnim = 0f;
    private float settingsViewTarget = 0f;
    private final Spring settingsSpring = new Spring(280f, 26f);
    private Module lastExpandedModule = null;

    private final float[] catBtnX = new float[5];
    private final float[] catBtnY = new float[5];
    private final float[] catBtnW = new float[5];
    private final float[] catBtnH = new float[5];

    private record HitAction(float x, float y, float w, float h, Runnable action) {}
    private record SliderBox(NumberSetting setting, float x, float y, float w, float h, float trackX, float trackW) {}

    private final List<HitAction> clickTargets = new ArrayList<>(32);
    private final List<HitAction> rightClickTargets = new ArrayList<>(16);
    private final List<SliderBox> sliderBoxes = new ArrayList<>(16);
    private final List<Module> filteredScratch = new ArrayList<>(16);
    private final List<Setting<?>> visibleSettingsScratch = new ArrayList<>(16);

    public ClickGuiController(
            ModuleManager moduleManager,
            ConfigManager configManager,
            List<HudElement> hudElements,
            SoundEngine soundEngine,
            Runnable unloadCallback
    ) {
        this.moduleManager = moduleManager;
        this.configManager = configManager;
        this.hudElements = hudElements;
        this.soundEngine = soundEngine;
        this.unloadCallback = unloadCallback;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isHudEditMode() {
        return open && hudEditMode;
    }

    public boolean isVisibleOrAnimating() {
        return open || rawOpen > 0.005f;
    }

    public float getBlurFactor() {
        return openAnim;
    }

    private boolean soundsEnabled() {
        return moduleManager.clickGui.uiSounds.isEnabled();
    }

    private float soundVolume() {
        return moduleManager.clickGui.soundVolume.asFloat();
    }

    private void resetCardCascade() {
        List<Module> all = moduleManager.getModules();
        for (int i = 0; i < all.size(); i++) {
            all.get(i).appearAnim = -i * 0.10f;
        }
    }

    private void clearInteractionState() {
        this.draggingSlider = null;
        this.draggingHud = null;
        this.snapXCenter = false;
        this.snapYCenter = false;
        this.listeningKeybind = null;
        this.searchFocused = false;
        this.lastHoveredCategory = -1;
        this.lastHoveredModule = null;
        this.contextModule = null;
    }

    public void setOpen(MinecraftClient mc, boolean shouldOpen) {
        if (this.open == shouldOpen) return;
        this.open = shouldOpen;
        clearInteractionState();

        if (shouldOpen) {
            this.hudEditMode = false;
            resetCardCascade();
            soundEngine.playOpen(mc, soundsEnabled(), soundVolume());
            mc.execute(() -> {
                if (!(mc.currentScreen instanceof AetherMenuScreen)) {
                    mc.setScreen(new AetherMenuScreen(() -> {
                        if (this.open) {
                            this.open = false;
                            this.hudEditMode = false;
                            clearInteractionState();
                            soundEngine.playClose(mc, soundsEnabled(), soundVolume());
                            configManager.saveClientConfig(moduleManager, hudElements);
                        }
                    }));
                }
            });
        } else {
            this.hudEditMode = false;
            soundEngine.playClose(mc, soundsEnabled(), soundVolume());
            configManager.saveClientConfig(moduleManager, hudElements);
            mc.execute(() -> {
                if (mc.currentScreen instanceof AetherMenuScreen) {
                    mc.setScreen(null);
                }
            });
        }
    }

    public void forceCloseImmediate(MinecraftClient mc) {
        this.open = false;
        this.hudEditMode = false;
        this.rawOpen = 0f;
        this.openAnim = 0f;
        clearInteractionState();
        if (mc != null) {
            try {
                if (mc.isOnThread()) {
                    if (mc.currentScreen instanceof AetherMenuScreen) {
                        mc.setScreen(null);
                    }
                } else {
                    mc.execute(() -> {
                        if (mc.currentScreen instanceof AetherMenuScreen) {
                            mc.setScreen(null);
                        }
                    });
                }
            } catch (Throwable ignored) {}
        }
    }

    public void onCursorMove(double uiX, double uiY, double hudX, double hudY, float hudScreenW, float hudScreenH) {
        this.mouseX = (float) uiX;
        this.mouseY = (float) uiY;
        this.hudMouseX = (float) hudX;
        this.hudMouseY = (float) hudY;

        if (draggingSlider != null) {
            double ratio = Math.max(0.0, Math.min(1.0, (mouseX - sliderTrackX) / Math.max(1f, sliderTrackW)));
            double val = draggingSlider.getMin() + ratio * (draggingSlider.getMax() - draggingSlider.getMin());
            draggingSlider.setValue(val);
        }

        if (draggingHud != null && (hudEditMode || isChatDragging)) {
            float rawX = hudMouseX - hudDragOffsetX;
            float rawY = hudMouseY - hudDragOffsetY;
            float dw = draggingHud.getScaledWidth();
            float dh = draggingHud.getScaledHeight();

            snapXCenter = false;
            snapYCenter = false;
            snapGuideXs.clear();
            snapGuideYs.clear();

            // Candidate anchor points of the dragged widget (left, center, right / top, center, bottom)
            float[] candX = {rawX, rawX + dw * 0.5f, rawX + dw};
            float[] candY = {rawY, rawY + dh * 0.5f, rawY + dh};

            // 1. Screen edges with margin (magnetic pull)
            // Anchor candidates: [left, center, right] with matching snap targets
            float bestXDist = SNAP_TOLERANCE + 1f;
            int bestXAnchor = -1;
            float[] snapTargetsX = {SNAP_EDGE_MARGIN, hudScreenW * 0.5f, hudScreenW - SNAP_EDGE_MARGIN};
            for (int i = 0; i < 3; i++) {
                float d = Math.abs(candX[i] - snapTargetsX[i]);
                if (d < SNAP_TOLERANCE && d < bestXDist) {
                    bestXDist = d;
                    bestXAnchor = i;
                }
            }
            if (bestXAnchor >= 0) {
                rawX = snapTargetsX[bestXAnchor] - (bestXAnchor == 1 ? dw * 0.5f : bestXAnchor == 0 ? 0f : dw);
                snapXCenter = bestXAnchor == 1;
                snapGuideXs.add(snapTargetsX[bestXAnchor]);
            }

            float bestYDist = SNAP_TOLERANCE + 1f;
            int bestYAnchor = -1;
            float[] snapTargetsY = {SNAP_EDGE_MARGIN, hudScreenH * 0.5f, hudScreenH - SNAP_EDGE_MARGIN};
            for (int i = 0; i < 3; i++) {
                float d = Math.abs(candY[i] - snapTargetsY[i]);
                if (d < SNAP_TOLERANCE && d < bestYDist) {
                    bestYDist = d;
                    bestYAnchor = i;
                }
            }
            if (bestYAnchor >= 0) {
                rawY = snapTargetsY[bestYAnchor] - (bestYAnchor == 1 ? dh * 0.5f : bestYAnchor == 0 ? 0f : dh);
                snapYCenter = bestYAnchor == 1;
                snapGuideYs.add(snapTargetsY[bestYAnchor]);
            }

            // 2. Smart guides: align with other widgets' edges and centers (closest match wins)
            float bestSmartXDist = SNAP_TOLERANCE + 1f;
            float bestSmartX = Float.NaN;
            int bestSmartXCand = -1;
            float bestSmartYDist = SNAP_TOLERANCE + 1f;
            float bestSmartY = Float.NaN;
            int bestSmartYCand = -1;
            for (HudElement other : hudElements) {
                if (other == draggingHud || !other.isActive()) continue;
                float ox = other.getPixelX(hudScreenW);
                float oy = other.getPixelY(hudScreenH);
                float ow = other.getScaledWidth();
                float oh = other.getScaledHeight();
                float[] otherXs = {ox, ox + ow * 0.5f, ox + ow};
                float[] otherYs = {oy, oy + oh * 0.5f, oy + oh};
                for (float gx : otherXs) {
                    for (int ci = 0; ci < 3; ci++) {
                        float d = Math.abs(candX[ci] - gx);
                        if (d < bestSmartXDist) {
                            bestSmartXDist = d;
                            bestSmartX = gx;
                            bestSmartXCand = ci;
                        }
                    }
                }
                for (float gy : otherYs) {
                    for (int ci = 0; ci < 3; ci++) {
                        float d = Math.abs(candY[ci] - gy);
                        if (d < bestSmartYDist) {
                            bestSmartYDist = d;
                            bestSmartY = gy;
                            bestSmartYCand = ci;
                        }
                    }
                }
            }
            if (!Float.isNaN(bestSmartX)) {
                rawX = bestSmartX - (bestSmartXCand == 1 ? dw * 0.5f : bestSmartXCand == 0 ? 0f : dw);
                snapGuideXs.add(bestSmartX);
            }
            if (!Float.isNaN(bestSmartY)) {
                rawY = bestSmartY - (bestSmartYCand == 1 ? dh * 0.5f : bestSmartYCand == 0 ? 0f : dh);
                snapGuideYs.add(bestSmartY);
            }

            // 3. Fine grid fallback (4px) when nothing else snapped
            if (snapGuideXs.isEmpty()) {
                float gx = Math.round(rawX / SNAP_GRID_PX) * SNAP_GRID_PX;
                if (Math.abs(gx - rawX) > 0.01f) rawX = gx;
            }
            if (snapGuideYs.isEmpty()) {
                float gy = Math.round(rawY / SNAP_GRID_PX) * SNAP_GRID_PX;
                if (Math.abs(gy - rawY) > 0.01f) rawY = gy;
            }

            draggingHud.setPixelPosition(rawX, rawY, hudScreenW, hudScreenH);
        }
    }

    public boolean onChatHudMouseButton(
            MinecraftClient mc, int button, int action,
            float hudScreenW, float hudScreenH
    ) {
        if (action == GLFW.GLFW_RELEASE) {
            if (draggingHud != null) {
                draggingHud = null;
                isChatDragging = false;
                snapXCenter = false;
                snapYCenter = false;
                snapGuideXs.clear();
                snapGuideYs.clear();
                configManager.saveClientConfig(moduleManager, hudElements);
                return true;
            }
            return false;
        }

        if (action != GLFW.GLFW_PRESS) return false;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int i = hudElements.size() - 1; i >= 0; i--) {
                HudElement el = hudElements.get(i);
                if (!el.isActive()) continue;
                float ex = el.getPixelX(hudScreenW);
                float ey = el.getPixelY(hudScreenH);
                if (Render2D.isHovered(hudMouseX, hudMouseY, ex - 4f, ey - 4f, el.getScaledWidth() + 8f, el.getScaledHeight() + 8f)) {
                    draggingHud = el;
                    isChatDragging = true;
                    hudDragOffsetX = hudMouseX - ex;
                    hudDragOffsetY = hudMouseY - ey;
                    soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                    return true;
                }
            }
        }
        return false;
    }

    public boolean onMouseButton(
            MinecraftClient mc, int button, int action,
            float uiScreenW, float uiScreenH,
            float hudScreenW, float hudScreenH
    ) {
        if (!open) return false;

        if (action == GLFW.GLFW_RELEASE) {
            boolean changed = false;
            if (draggingSlider != null) {
                draggingSlider = null;
                changed = true;
            }
            if (draggingHud != null) {
                draggingHud = null;
                snapXCenter = false;
                snapYCenter = false;
                snapGuideXs.clear();
                snapGuideYs.clear();
                changed = true;
            }
            if (changed) {
                configManager.saveClientConfig(moduleManager, hudElements);
            }
            return true;
        }

        if (action != GLFW.GLFW_PRESS) return true;

        if (listeningKeybind != null) {
            listeningKeybind.setValue(-100 - button);
            listeningKeybind = null;
            soundEngine.playClick(mc, soundsEnabled(), soundVolume());
            configManager.saveClientConfig(moduleManager, hudElements);
            return true;
        }

        if (contextModule != null) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                int idx = contextMenuItemAt(mouseX, mouseY);
                if (idx >= 0) {
                    runContextAction(mc, idx);
                } else {
                    contextModule = null;
                }
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                contextModule = null;
                return true;
            }
        }

        if (hudEditMode) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (Render2D.isHovered(mouseX, mouseY, doneHudX, doneHudY, doneHudW, doneHudH)) {
                    hudEditMode = false;
                    draggingHud = null;
                    snapXCenter = false;
                    snapYCenter = false;
                    soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                    configManager.saveClientConfig(moduleManager, hudElements);
                    return true;
                }
                for (int i = hudElements.size() - 1; i >= 0; i--) {
                    HudElement el = hudElements.get(i);
                    float ex = el.getPixelX(hudScreenW);
                    float ey = el.getPixelY(hudScreenH);
                    if (Render2D.isHovered(hudMouseX, hudMouseY, ex - 4f, ey - 4f, el.getScaledWidth() + 8f, el.getScaledHeight() + 8f)) {
                        draggingHud = el;
                        hudDragOffsetX = hudMouseX - ex;
                        hudDragOffsetY = hudMouseY - ey;
                        soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                        return true;
                    }
                }
            }
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (Render2D.isHovered(mouseX, mouseY, unloadX, unloadY, unloadW, unloadH)) {
                soundEngine.playClose(mc, soundsEnabled(), soundVolume());
                forceCloseImmediate(mc);
                unloadCallback.run();
                return true;
            }
            if (Render2D.isHovered(mouseX, mouseY, modTabX, modTabY, modTabW, modTabH)) {
                activeTopTab = TAB_MODULES;
                hudEditMode = false;
                configNameFocused = false;
                friendNameFocused = false;
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                return true;
            }
            if (Render2D.isHovered(mouseX, mouseY, hudTabX, hudTabY, hudTabW, hudTabH)) {
                hudEditMode = true;
                searchFocused = false;
                configNameFocused = false;
                friendNameFocused = false;
                draggingSlider = null;
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                return true;
            }
            if (Render2D.isHovered(mouseX, mouseY, cfgTabX, cfgTabY, cfgTabW, cfgTabH)) {
                activeTopTab = TAB_CONFIGS;
                hudEditMode = false;
                searchFocused = false;
                configNameFocused = false;
                friendNameFocused = false;
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                return true;
            }
            if (Render2D.isHovered(mouseX, mouseY, infoTabX, infoTabY, infoTabW, infoTabH)) {
                activeTopTab = TAB_INFO;
                hudEditMode = false;
                searchFocused = false;
                configNameFocused = false;
                friendNameFocused = false;
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                return true;
            }
            if (Render2D.isHovered(mouseX, mouseY, friendsTabX, friendsTabY, friendsTabW, friendsTabH)) {
                activeTopTab = TAB_FRIENDS;
                hudEditMode = false;
                searchFocused = false;
                configNameFocused = false;
                friendNameFocused = false;
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                return true;
            }
            boolean prevSearch = searchFocused;
            searchFocused = Render2D.isHovered(mouseX, mouseY, searchX, searchY, searchW, searchH);
            if (searchFocused && !prevSearch) {
                configNameFocused = false;
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
            }
            if (searchFocused && !searchQuery.isEmpty() && mouseX >= searchX + searchW - 22f) {
                searchQuery = "";
                targetScroll = 0f;
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                return true;
            }

            if (Render2D.isHovered(mouseX, mouseY, catBtnX[0], catBtnY[0], catBtnW[0], catBtnH[0])) {
                activeTopTab = TAB_MODULES;
                if (selectedCategory != null) {
                    selectedCategory = null;
                    targetScroll = 0f;
                    resetCardCascade();
                    soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                }
                return true;
            }
            Category[] cats = Category.values();
            for (int i = 0; i < cats.length; i++) {
                int idx = i + 1;
                if (Render2D.isHovered(mouseX, mouseY, catBtnX[idx], catBtnY[idx], catBtnW[idx], catBtnH[idx])) {
                    activeTopTab = TAB_MODULES;
                    if (selectedCategory != cats[i]) {
                        selectedCategory = cats[i];
                        targetScroll = 0f;
                        resetCardCascade();
                        soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                    }
                    return true;
                }
            }

            if (Render2D.isHovered(mouseX, mouseY, bodyX, bodyY, bodyW, bodyH)) {
                for (SliderBox sb : sliderBoxes) {
                    if (Render2D.isHovered(mouseX, mouseY, sb.x, sb.y, sb.w, sb.h)) {
                        draggingSlider = sb.setting;
                        sliderTrackX = sb.trackX;
                        sliderTrackW = sb.trackW;
                        double ratio = Math.max(0.0, Math.min(1.0, (mouseX - sb.trackX) / Math.max(1f, sb.trackW)));
                        sb.setting.setValue(sb.setting.getMin() + ratio * (sb.setting.getMax() - sb.setting.getMin()));
                        soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                        return true;
                    }
                }
                for (HitAction hit : clickTargets) {
                    if (Render2D.isHovered(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) {
                        hit.action.run();
                        configManager.saveClientConfig(moduleManager, hudElements);
                        return true;
                    }
                }
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (Render2D.isHovered(mouseX, mouseY, bodyX, bodyY, bodyW, bodyH)) {
                for (HitAction hit : rightClickTargets) {
                    if (Render2D.isHovered(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) {
                        hit.action.run();
                        return true;
                    }
                }
            }
        }

        return true;
    }

    public boolean onScroll(double vertical) {
        if (!open || hudEditMode) return open;
        if (Render2D.isHovered(mouseX, mouseY, bodyX, bodyY, bodyW, bodyH)) {
            targetScroll = Math.max(0f, Math.min(maxScroll, targetScroll - (float) vertical * 26f));
            scrollActivity = 1f;
        }
        return true;
    }

    public boolean onChar(int codepoint, String text) {
        if (!open || hudEditMode) return false;
        if (configNameFocused && activeTopTab == TAB_CONFIGS) {
            if (text != null && !text.isEmpty() && !Character.isISOControl(codepoint) && newConfigName.length() < 24) {
                newConfigName += text;
            }
            return true;
        }
        if (friendNameFocused && activeTopTab == TAB_FRIENDS) {
            if (text != null && !text.isEmpty() && !Character.isISOControl(codepoint) && newFriendName.length() < 24) {
                newFriendName += text;
            }
            return true;
        }
        if (!searchFocused) return false;
        if (text == null || text.isEmpty() || Character.isISOControl(codepoint)) return true;
        if (searchQuery.length() < 24) {
            searchQuery += text;
            if (searchQuery.length() > 24) {
                searchQuery = searchQuery.substring(0, 24);
            }
            targetScroll = 0f;
        }
        return true;
    }

    public boolean onKey(MinecraftClient mc, int key, int scancode, int modifiers, int action) {
        if (listeningKeybind != null && open) {
            if (action == GLFW.GLFW_PRESS) {
                if (key == GLFW.GLFW_KEY_ESCAPE) {
                    listeningKeybind = null;
                    soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                    return true;
                }
                if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
                    if (listeningKeybind != moduleManager.clickGui.getKeybind()) {
                        listeningKeybind.setValue(GLFW.GLFW_KEY_UNKNOWN);
                    }
                } else if (key != GLFW.GLFW_KEY_UNKNOWN) {
                    listeningKeybind.setValue(key);
                }
                listeningKeybind = null;
                soundEngine.playToggle(mc, true, soundsEnabled(), soundVolume());
                configManager.saveClientConfig(moduleManager, hudElements);
            }
            return true;
        }

        if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_ESCAPE && open) {
            for (Module m : moduleManager.getModules()) {
                if (m.expanded) {
                    m.expanded = false;
                    targetScroll = 0f;
                    currentScroll = 0f;
                    soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                    return true;
                }
            }
        }

        int guiKey = moduleManager.clickGui.getKeybind().getValue();
        boolean nonPrintableBind = guiKey >= GLFW.GLFW_KEY_ESCAPE || GLFW.glfwGetKeyName(guiKey, scancode) == null;
        if (action == GLFW.GLFW_PRESS && key == guiKey && guiKey != GLFW.GLFW_KEY_UNKNOWN) {
            if (open) {
                if (!searchFocused || nonPrintableBind) {
                    setOpen(mc, false);
                    return true;
                }
            } else if (mc.currentScreen == null) {
                setOpen(mc, true);
                return true;
            }
        }

        if (open && configNameFocused && activeTopTab == TAB_CONFIGS) {
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                if (key == GLFW.GLFW_KEY_BACKSPACE && !newConfigName.isEmpty()) {
                    newConfigName = newConfigName.substring(0, newConfigName.length() - 1);
                    return true;
                } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                    if (!newConfigName.isBlank()) {
                        String name = newConfigName.trim();
                        boolean ok = configManager.saveNamedConfig(name, moduleManager, hudElements);
                        if (ok) {
                            activeConfigName = name;
                            ToastManager.get().push("Конфиги", "Конфиг сохранен: " + name, 0xFF10B981);
                        }
                        soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                        newConfigName = "";
                        configNameFocused = false;
                    }
                    return true;
                } else if (key == GLFW.GLFW_KEY_ESCAPE) {
                    configNameFocused = false;
                    return true;
                }
            }
            return true;
        }

        if (open && friendNameFocused && activeTopTab == TAB_FRIENDS) {
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                if (key == GLFW.GLFW_KEY_BACKSPACE && !newFriendName.isEmpty()) {
                    newFriendName = newFriendName.substring(0, newFriendName.length() - 1);
                    return true;
                } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                    if (!newFriendName.isBlank()) {
                        String name = newFriendName.trim();
                        boolean added = dev.aetherclient.client.friend.FriendsManager.get().addFriend(name);
                        if (added) {
                            ToastManager.get().push("Друзья", "Добавлен в друзья: " + name, 0xFF10B981);
                        } else {
                            ToastManager.get().push("Друзья", "Игрок уже в друзьях: " + name, 0xFFF59E0B);
                        }
                        soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                        newFriendName = "";
                        friendNameFocused = false;
                    }
                    return true;
                } else if (key == GLFW.GLFW_KEY_ESCAPE) {
                    friendNameFocused = false;
                    return true;
                }
            }
            return true;
        }

        if (open && searchFocused && !hudEditMode) {
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                if (key == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    targetScroll = 0f;
                } else if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                    searchFocused = false;
                }
            }
            return true;
        }

        if (action == GLFW.GLFW_PRESS && open && key == GLFW.GLFW_KEY_ESCAPE) {
            if (contextModule != null) {
                contextModule = null;
                return true;
            }
            if (hudEditMode) {
                hudEditMode = false;
                draggingHud = null;
                snapXCenter = false;
                snapYCenter = false;
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
            } else {
                setOpen(mc, false);
            }
            return true;
        }
        return open;
    }

    public void renderHudEditorOutlines(NanoVgEngine engine, AetherPalette palette, float hudScreenW, float hudScreenH) {
        if (!open || !hudEditMode) return;
        long vg = engine.handle();
        // Smart guides (alignment lines active while dragging)
        for (float gx : snapGuideXs) {
            Render2D.drawRoundedRect(vg, gx - 0.5f, 0f, 1f, hudScreenH, 0f, AetherPalette.withAlpha(palette.primary, 190));
        }
        for (float gy : snapGuideYs) {
            Render2D.drawRoundedRect(vg, 0f, gy - 0.5f, hudScreenW, 1f, 0f, AetherPalette.withAlpha(palette.primary, 190));
        }
        // Screen-center guides when center-snapped (kept for the classic center line look)
        if (snapXCenter && snapGuideXs.isEmpty()) {
            Render2D.drawRoundedRect(vg, hudScreenW * 0.5f - 0.5f, 0f, 1f, hudScreenH, 0f, AetherPalette.withAlpha(palette.primary, 190));
        }
        if (snapYCenter && snapGuideYs.isEmpty()) {
            Render2D.drawRoundedRect(vg, 0f, hudScreenH * 0.5f - 0.5f, hudScreenW, 1f, 0f, AetherPalette.withAlpha(palette.primary, 190));
        }

        for (HudElement el : hudElements) {
            float ex = el.getPixelX(hudScreenW);
            float ey = el.getPixelY(hudScreenH);
            boolean hov = Render2D.isHovered(hudMouseX, hudMouseY, ex - 3f, ey - 3f, el.getScaledWidth() + 6f, el.getScaledHeight() + 6f);
            int outlineCol = (draggingHud == el) ? palette.primary : (hov ? palette.tertiary : AetherPalette.withAlpha(palette.onSurfaceVariant, 125));
            Render2D.drawRoundedOutline(vg, ex - 2.5f, ey - 2.5f, el.getScaledWidth() + 5f, el.getScaledHeight() + 5f, 9f, 1.35f, outlineCol);
            Render2D.drawTextBold(vg, el.getTitle(), ex, Math.max(12f, ey - 5f), 9.6f, outlineCol, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_BOTTOM);
        }
    }

    public void render(NanoVgEngine engine, AetherPalette palette, float screenW, float screenH, float dt) {
        float step = dt / OPEN_CLOSE_DURATION_SEC;
        if (open) {
            rawOpen = Math.min(1f, rawOpen + step);
        } else {
            rawOpen = Math.max(0f, rawOpen - step);
        }

        if (!open && rawOpen <= 0.002f) {
            rawOpen = 0f;
            openAnim = 0f;
            return;
        }

        float eased = open ? easeOutCubic(rawOpen) : easeInOutQuad(rawOpen);
        openAnim = eased;
        float spreadPhase = 1f - eased;
        scrollActivity = Math.max(0f, scrollActivity - dt * 1.6f);

        long vg = engine.handle();
        clickTargets.clear();
        rightClickTargets.clear();
        sliderBoxes.clear();

        if (hudEditMode && open) {
            renderHudEditorToolbar(engine, palette, screenW, screenH);
            return;
        }

        int scrim = AetherPalette.withAlpha(0xFF000000, Math.round(eased * 80f));
        Render2D.drawRoundedRect(vg, 0f, 0f, screenW, screenH, 0f, scrim);

        // Generous modern Material 3 + Liquid Glass window (564x378 logical px)
        pW = Math.min(564f, Math.max(320f, screenW - 36f));
        pH = Math.min(378f, Math.max(240f, screenH - 36f));
        pX = (screenW - pW) * 0.5f;
        pY = (screenH - pH) * 0.5f;

        float scale = 1.0f + spreadPhase * 0.10f;
        float cx = pX + pW * 0.5f;
        float cy = pY + pH * 0.5f;
        float radius = Math.max(0f, Math.min(Math.min(pW, pH) * 0.5f, moduleManager.clickGui.cornerRadius.asFloat()));

        if (rawOpen > 0.01f && rawOpen < 0.99f) {
            float rippleExpand1 = spreadPhase * 30f;
            float rippleExpand2 = spreadPhase * 50f;
            int rAlpha1 = Math.round((1f - Math.abs(spreadPhase - 0.35f)) * 95f * eased);
            int rAlpha2 = Math.round((1f - spreadPhase) * 65f * eased);
            if (rAlpha1 > 4) {
                Render2D.drawRoundedOutline(
                        vg,
                        pX - rippleExpand1 * 0.5f, pY - rippleExpand1 * 0.5f,
                        pW + rippleExpand1, pH + rippleExpand1,
                        radius + rippleExpand1 * 0.28f,
                        1.4f,
                        AetherPalette.withAlpha(palette.primary, Math.max(0, Math.min(110, rAlpha1)))
                );
            }
            if (rAlpha2 > 4) {
                Render2D.drawRoundedOutline(
                        vg,
                        pX - rippleExpand2 * 0.5f, pY - rippleExpand2 * 0.5f,
                        pW + rippleExpand2, pH + rippleExpand2,
                        radius + rippleExpand2 * 0.30f,
                        1.0f,
                        AetherPalette.withAlpha(0xFFFFFFFF, Math.max(0, Math.min(80, rAlpha2)))
                );
            }
        }

        float visualAlpha = (float) Math.pow(eased, open ? 0.85 : 1.25);

        NanoVG.nvgSave(vg);
        NanoVG.nvgGlobalAlpha(vg, visualAlpha);
        NanoVG.nvgTranslate(vg, cx, cy);
        NanoVG.nvgScale(vg, scale, scale);
        NanoVG.nvgTranslate(vg, -cx, -cy);

        float opacity = moduleManager.clickGui.glassOpacity.asFloat();
        Render2D.drawLiquidGlassPanel(
                engine, pX, pY, pW, pH, radius, screenW, screenH, palette,
                opacity, mouseX, mouseY, 0.85f * eased
        );

        renderTopBar(engine, palette, dt);
        if (activeTopTab == TAB_MODULES) {
            renderCategoryRail(engine, palette, screenW, screenH, dt);
            renderModulesBody(engine, palette, screenW, screenH, dt);
        } else if (activeTopTab == TAB_CONFIGS) {
            renderConfigsBody(engine, palette, screenW, screenH, dt);
        } else if (activeTopTab == TAB_INFO) {
            renderInfoBody(engine, palette, screenW, screenH, dt);
        } else if (activeTopTab == TAB_FRIENDS) {
            renderFriendsBody(engine, palette, screenW, screenH, dt);
        }
        renderFooterBar(vg, palette);
        renderContextMenu(engine, palette, dt);

        NanoVG.nvgRestore(vg);
    }

    private void renderHudEditorToolbar(NanoVgEngine engine, AetherPalette palette, float screenW, float screenH) {
        long vg = engine.handle();
        float barW = 282f;
        float barH = 34f;
        float barX = (screenW - barW) * 0.5f;
        float barY = 12f;
        Render2D.drawLiquidGlassPanel(
                engine, barX, barY, barW, barH, 11f, screenW, screenH, palette,
                0.78f, mouseX, mouseY, 0.7f
        );
        Render2D.drawIcon(vg, IconType.EDIT_HUD, barX + 15f, barY + barH * 0.5f, 12f, palette.primary);
        Render2D.drawText(vg, I18n.tr("Перетаскивайте виджеты HUD", "Drag HUD widgets"), barX + 28f, barY + barH * 0.5f + 0.4f, 10.4f,
                palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        doneHudW = 66f;
        doneHudH = 22f;
        doneHudX = barX + barW - doneHudW - 6f;
        doneHudY = barY + 6f;
        boolean doneHov = Render2D.isHovered(mouseX, mouseY, doneHudX, doneHudY, doneHudW, doneHudH);
        Render2D.drawRoundedRect(vg, doneHudX, doneHudY, doneHudW, doneHudH, 7.5f,
                doneHov ? palette.tertiary : palette.primary);
        Render2D.drawTextBold(vg, I18n.tr("Готово", "Done"), doneHudX + doneHudW * 0.5f, doneHudY + doneHudH * 0.5f + 0.4f, 10.0f,
                palette.onPrimary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
    }

    private void renderTopBar(NanoVgEngine engine, AetherPalette palette, float dt) {
        long vg = engine.handle();
        float topH = 41f;
        float hoverSpeed = Math.min(1f, dt * 16f);

        Render2D.drawLogo(engine, pX + 12f, pY + 9.5f, 22f, 6.5f);
        Render2D.drawTextBold(vg, "Aether", pX + 39f, pY + 20.5f, 13.4f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        // Segmented pills: Модули / HUD / Конфиги / Инфо
        modTabX = pX + 96f;
        modTabY = pY + 9f;
        modTabW = 66f;
        modTabH = 23f;
        boolean modActive = !hudEditMode && activeTopTab == TAB_MODULES;
        boolean modHov = Render2D.isHovered(mouseX, mouseY, modTabX, modTabY, modTabW, modTabH);
        modTabHoverAnim += ((modHov ? 1f : 0f) - modTabHoverAnim) * hoverSpeed;

        int modBg = modActive
                ? palette.primary
                : AetherPalette.withAlpha(palette.onSurface, 18 + Math.round(modTabHoverAnim * 18f));
        Render2D.drawRoundedRect(vg, modTabX, modTabY, modTabW, modTabH, 7.5f, modBg);
        int modFg = modActive ? palette.onPrimary : palette.onSurface;
        Render2D.drawIcon(vg, IconType.MODULES, modTabX + 11.5f, modTabY + 11.5f, 9.8f, modFg);
        Render2D.drawTextBold(vg, I18n.tr("Модули", "Modules"), modTabX + 20.5f, modTabY + 12f, 9.6f,
                modFg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        hudTabX = modTabX + modTabW + 4f;
        hudTabY = pY + 9f;
        hudTabW = 50f;
        hudTabH = 23f;
        boolean hudHov = Render2D.isHovered(mouseX, mouseY, hudTabX, hudTabY, hudTabW, hudTabH);
        hudTabHoverAnim += ((hudHov ? 1f : 0f) - hudTabHoverAnim) * hoverSpeed;
        int hudBg = hudEditMode
                ? palette.primary
                : AetherPalette.withAlpha(palette.onSurface, 18 + Math.round(hudTabHoverAnim * 18f));
        Render2D.drawRoundedRect(vg, hudTabX, hudTabY, hudTabW, hudTabH, 7.5f, hudBg);
        int hudFg = hudEditMode ? palette.onPrimary : palette.onSurface;
        Render2D.drawIcon(vg, IconType.EDIT_HUD, hudTabX + 11.5f, hudTabY + 11.5f, 9.8f, hudFg);
        Render2D.drawTextBold(vg, "HUD", hudTabX + 20.5f, hudTabY + 12f, 9.6f,
                hudFg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        cfgTabX = hudTabX + hudTabW + 4f;
        cfgTabY = pY + 9f;
        cfgTabW = 68f;
        cfgTabH = 23f;
        boolean cfgActive = !hudEditMode && activeTopTab == TAB_CONFIGS;
        boolean cfgHov = Render2D.isHovered(mouseX, mouseY, cfgTabX, cfgTabY, cfgTabW, cfgTabH);
        cfgTabHoverAnim += ((cfgHov ? 1f : 0f) - cfgTabHoverAnim) * hoverSpeed;
        int cfgBg = cfgActive
                ? palette.primary
                : AetherPalette.withAlpha(palette.onSurface, 18 + Math.round(cfgTabHoverAnim * 18f));
        Render2D.drawRoundedRect(vg, cfgTabX, cfgTabY, cfgTabW, cfgTabH, 7.5f, cfgBg);
        int cfgFg = cfgActive ? palette.onPrimary : palette.onSurface;
        Render2D.drawIcon(vg, IconType.SLIDERS, cfgTabX + 11.5f, cfgTabY + 11.5f, 9.8f, cfgFg);
        Render2D.drawTextBold(vg, I18n.tr("Конфиги", "Configs"), cfgTabX + 20.5f, cfgTabY + 12f, 9.6f,
                cfgFg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        infoTabX = cfgTabX + cfgTabW + 4f;
        infoTabY = pY + 9f;
        infoTabW = 54f;
        infoTabH = 23f;
        boolean infoActive = !hudEditMode && activeTopTab == TAB_INFO;
        boolean infoHov = Render2D.isHovered(mouseX, mouseY, infoTabX, infoTabY, infoTabW, infoTabH);
        infoTabHoverAnim += ((infoHov ? 1f : 0f) - infoTabHoverAnim) * hoverSpeed;
        int infoBg = infoActive
                ? palette.primary
                : AetherPalette.withAlpha(palette.onSurface, 18 + Math.round(infoTabHoverAnim * 18f));
        Render2D.drawRoundedRect(vg, infoTabX, infoTabY, infoTabW, infoTabH, 7.5f, infoBg);
        int infoFg = infoActive ? palette.onPrimary : palette.onSurface;
        Render2D.drawIcon(vg, IconType.USER, infoTabX + 11.5f, infoTabY + 11.5f, 9.8f, infoFg);
        Render2D.drawTextBold(vg, I18n.tr("Инфо", "Info"), infoTabX + 20.5f, infoTabY + 12f, 9.6f,
                infoFg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        friendsTabX = infoTabX + infoTabW + 4f;
        friendsTabY = pY + 9f;
        friendsTabW = 68f;
        friendsTabH = 23f;
        boolean friendsActive = !hudEditMode && activeTopTab == TAB_FRIENDS;
        boolean friendsHov = Render2D.isHovered(mouseX, mouseY, friendsTabX, friendsTabY, friendsTabW, friendsTabH);
        friendsTabHoverAnim += ((friendsHov ? 1f : 0f) - friendsTabHoverAnim) * hoverSpeed;
        int friendsBg = friendsActive
                ? palette.primary
                : AetherPalette.withAlpha(palette.onSurface, 18 + Math.round(friendsTabHoverAnim * 18f));
        Render2D.drawRoundedRect(vg, friendsTabX, friendsTabY, friendsTabW, friendsTabH, 7.5f, friendsBg);
        int friendsFg = friendsActive ? palette.onPrimary : palette.onSurface;
        Render2D.drawIcon(vg, IconType.HEART, friendsTabX + 11.5f, friendsTabY + 11.5f, 9.8f, friendsFg);
        Render2D.drawTextBold(vg, I18n.tr("Друзья", "Friends"), friendsTabX + 20.5f, friendsTabY + 12f, 9.6f,
                friendsFg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        // Unload button on right
        unloadW = 84f;
        unloadH = 23f;
        unloadX = pX + pW - unloadW - 10f;
        unloadY = pY + 9f;
        boolean unHov = Render2D.isHovered(mouseX, mouseY, unloadX, unloadY, unloadW, unloadH);
        unloadHoverAnim += ((unHov ? 1f : 0f) - unloadHoverAnim) * hoverSpeed;
        Render2D.drawRoundedRect(vg, unloadX, unloadY, unloadW, unloadH, 7.5f,
                AetherPalette.withAlpha(0xFFFF5F6D, 145 + Math.round(unloadHoverAnim * 75f)));
        Render2D.drawIcon(vg, IconType.UNLOAD, unloadX + 12f, unloadY + 11.5f, 9.6f, 0xFFFFFFFF);
        Render2D.drawTextBold(vg, I18n.tr("Выгрузить", "Unload"), unloadX + 20.5f, unloadY + 12f, 9.6f,
                0xFFFFFFFF, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        // Search box between tabs & unload button
        searchX = friendsTabX + friendsTabW + 6f;
        searchY = pY + 9f;
        searchW = Math.max(50f, unloadX - searchX - 6f);
        searchH = 23f;
        boolean sHov = Render2D.isHovered(mouseX, mouseY, searchX, searchY, searchW, searchH);
        searchHoverAnim += ((sHov ? 1f : 0f) - searchHoverAnim) * hoverSpeed;
        Render2D.drawRoundedRect(vg, searchX, searchY, searchW, searchH, 7.5f,
                AetherPalette.withAlpha(palette.surface, 120 + Math.round(searchHoverAnim * 35f)));
        Render2D.drawRoundedOutline(vg, searchX, searchY, searchW, searchH, 7.5f, 0.95f,
                searchFocused ? palette.primary : AetherPalette.withAlpha(palette.outline, 85 + Math.round(searchHoverAnim * 60f)));
        Render2D.drawIcon(vg, IconType.SEARCH, searchX + 11.5f, searchY + 11.5f, 10f,
                searchFocused ? palette.primary : palette.onSurfaceVariant);

        String shown = searchQuery.isEmpty() ? (searchFocused ? "|" : I18n.tr("Поиск...", "Search...")) : (searchQuery + (searchFocused ? "|" : ""));
        int searchColor = searchQuery.isEmpty() && !searchFocused ? palette.onSurfaceVariant : palette.onSurface;
        float searchTextReserveRight = searchQuery.isEmpty() ? 8f : 20f;
        Render2D.pushScissor(vg, searchX + 20f, searchY + 2f, Math.max(8f, searchW - 20f - searchTextReserveRight), searchH - 4f);
        Render2D.drawText(vg, shown, searchX + 21.5f, searchY + 12f, 9.8f, searchColor, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        Render2D.popScissor(vg);
        if (!searchQuery.isEmpty()) {
            Render2D.drawTextBold(vg, "x", searchX + searchW - 10f, searchY + 11.5f, 9.6f, palette.onSurfaceVariant, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        }

        Render2D.drawRoundedRect(vg, pX + 10f, pY + topH, pW - 20f, 1f, 0f, AetherPalette.withAlpha(palette.outline, 75));
    }

    private void renderCategoryRail(NanoVgEngine engine, AetherPalette palette, float screenW, float screenH, float dt) {
        long vg = engine.handle();
        float railX = pX + 12f;
        float railY = pY + 48f;
        float railW = 114f;
        float btnH = 27.5f;
        float gap = 4.5f;

        // Frosted glass container for the Material 3 navigation rail
        float railBoxH = Math.max(40f, pH - 70f);
        Render2D.drawFrostedGlassCard(
                engine, railX - 4f, railY - 4f, railW + 8f, railBoxH, 12f,
                screenW, screenH, palette, 0.08f, 0f, mouseX, mouseY
        );

        int selectedIndex = 0;
        Category[] cats = Category.values();
        if (selectedCategory != null) {
            for (int i = 0; i < cats.length; i++) {
                if (cats[i] == selectedCategory) {
                    selectedIndex = i + 1;
                    break;
                }
            }
        }
        float targetIndY = railY + selectedIndex * (btnH + gap);
        if (railIndicatorY < 0f || Math.abs(railIndicatorY - targetIndY) > 250f) {
            railIndicatorY = targetIndY;
        } else {
            railIndicatorY += (targetIndY - railIndicatorY) * Math.min(1f, dt * 18f);
        }

        // Sliding Material 3 Active Indicator Pill with Liquid Glass sheen
        Render2D.drawRoundedGradient(vg, railX, railIndicatorY, railW, btnH, 8.8f,
                AetherPalette.withAlpha(palette.primary, 232),
                AetherPalette.withAlpha(palette.tertiary, 210), false);
        Render2D.drawRoundedOutline(vg, railX + 0.5f, railIndicatorY + 0.5f, railW - 1f, btnH - 1f, 8.3f, 0.9f,
                0x4CFFFFFF);

        int hoveredCatSlot = -1;
        for (int i = 0; i < 5; i++) {
            float by = railY + i * (btnH + gap);
            if (Render2D.isHovered(mouseX, mouseY, railX, by, railW, btnH)) {
                hoveredCatSlot = i;
                break;
            }
        }
        if (hoveredCatSlot != -1 && hoveredCatSlot != lastHoveredCategory) {
            soundEngine.playHover(soundsEnabled(), soundVolume());
        }
        lastHoveredCategory = hoveredCatSlot;

        drawCategoryButton(vg, palette, 0, IconType.ALL, I18n.tr("Все модули", "All"), moduleManager.getModules().size(),
                selectedCategory == null, railX, railY, railW, btnH, dt);

        for (int i = 0; i < cats.length; i++) {
            float by = railY + (i + 1) * (btnH + gap);
            int count = countCategory(cats[i]);
            drawCategoryButton(vg, palette, i + 1, iconForCategory(cats[i]), cats[i].getDisplayName(), count,
                    selectedCategory == cats[i], railX, by, railW, btnH, dt);
        }
    }

    private int countCategory(Category cat) {
        int count = 0;
        for (Module m : moduleManager.getModules()) {
            if (m.getCategory() == cat) count++;
        }
        return count;
    }

    private void drawCategoryButton(
            long vg, AetherPalette palette, int slot, IconType icon, String label, int count, boolean selected,
            float x, float y, float w, float h, float dt
    ) {
        catBtnX[slot] = x;
        catBtnY[slot] = y;
        catBtnW[slot] = w;
        catBtnH[slot] = h;
        boolean hov = Render2D.isHovered(mouseX, mouseY, x, y, w, h);
        catHoverAnim[slot] += ((hov ? 1f : 0f) - catHoverAnim[slot]) * Math.min(1f, dt * 16f);

        if (!selected && catHoverAnim[slot] > 0.01f) {
            Render2D.drawRoundedRect(vg, x, y, w, h, 8.2f,
                    AetherPalette.withAlpha(palette.onSurface, Math.round(catHoverAnim[slot] * 24f)));
        }

        int textCol = selected ? palette.onPrimary : palette.onSurface;
        int iconCol = selected ? palette.onPrimary : (hov ? palette.primary : palette.onSurfaceVariant);
        float shift = selected ? 1.8f : catHoverAnim[slot] * 2.2f;

        Render2D.drawIcon(vg, icon, x + 12.5f + shift, y + h * 0.5f, 11f, iconCol);
        Render2D.drawTextFace(vg, selected ? NanoVgEngine.FONT_BOLD : NanoVgEngine.FONT_REGULAR,
                label, x + 24f + shift, y + h * 0.5f + 0.4f, 10.2f, textCol,
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        String cnt = Integer.toString(count);
        int cntBg = selected ? AetherPalette.withAlpha(palette.onPrimary, 40) : AetherPalette.withAlpha(palette.onSurface, 18);
        Render2D.drawRoundedRect(vg, x + w - 21f, y + h * 0.5f - 7.5f, 16.5f, 15f, 5f, cntBg);
        Render2D.drawTextBold(vg, cnt, x + w - 12.75f, y + h * 0.5f + 0.4f, 9.0f,
                selected ? palette.onPrimary : palette.onSurfaceVariant,
                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
    }

    private void addClippedClick(List<HitAction> list, float x, float y, float w, float h, float minY, float maxY, Runnable action) {
        float cy1 = Math.max(y, minY);
        float cy2 = Math.min(y + h, maxY);
        if (cy2 > cy1 + 1f && w > 1f) {
            list.add(new HitAction(x, cy1, w, cy2 - cy1, action));
        }
    }

    private void addClippedSlider(NumberSetting ns, float hitX, float hitY, float hitW, float hitH, float trX, float trW, float minY, float maxY) {
        float cy1 = Math.max(hitY, minY);
        float cy2 = Math.min(hitY + hitH, maxY);
        if (cy2 > cy1 + 1f && hitW > 1f) {
            sliderBoxes.add(new SliderBox(ns, hitX, cy1, hitW, cy2 - cy1, trX, trW));
        }
    }

    private void renderModulesBody(NanoVgEngine engine, AetherPalette palette, float screenW, float screenH, float dt) {
        long vg = engine.handle();
        bodyX = pX + 138f;
        bodyY = pY + 48f;
        bodyW = pW - 148f;
        bodyH = Math.max(80f, pH - 72f);

        currentScroll += (targetScroll - currentScroll) * Math.min(1f, dt * 16f);

        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        filteredScratch.clear();
        for (Module m : moduleManager.getModules()) {
            if (selectedCategory != null && m.getCategory() != selectedCategory) continue;
            if (!q.isEmpty()) {
                boolean match = m.getName().toLowerCase(Locale.ROOT).contains(q)
                        || m.getDescription().toLowerCase(Locale.ROOT).contains(q);
                if (!match) continue;
            }
            filteredScratch.add(m);
        }

        float speed = Math.min(1f, dt * 15f);
        float gapX = 6f;
        float gapY = 6f;
        int cols = Math.max(2, (int) ((bodyW + gapX) / (112f + gapX)));
        float boxW = (bodyW - (cols - 1) * gapX) / cols;
        float boxH = 40f;

        // Find expanded module (shows dedicated settings view instead of grid)
        Module expandedModule = null;
        for (Module m : moduleManager.getModules()) {
            if (m.expanded && !m.getSettings().isEmpty()) {
                expandedModule = m;
                break;
            }
        }

        // Animate settings view entrance/exit with spring physics
        settingsSpring.setTarget(settingsViewTarget);
        settingsSpring.update(dt);
        if (expandedModule != null) {
            settingsViewTarget = 1f;
            if (lastExpandedModule != expandedModule) {
                settingsViewAnim = 0f;
                settingsSpring.snap(0f);
                lastExpandedModule = expandedModule;
            }
            settingsViewAnim = settingsSpring.getPos();
            renderModuleSettingsView(engine, palette, expandedModule, screenW, screenH, dt, settingsViewAnim);
            return;
        } else {
            settingsViewTarget = 0f;
            if (lastExpandedModule != null) {
                // Animate out
                settingsViewAnim = settingsSpring.getPos();
                if (settingsViewAnim > 0.001f) {
                    // Render fading out settings view
                    renderModuleSettingsView(engine, palette, lastExpandedModule, screenW, screenH, dt, settingsViewAnim);
                    return;
                }
                lastExpandedModule = null;
            }
        }

        // Compute layout heights
        int rows = (filteredScratch.size() + cols - 1) / cols;
        float totalContentH = rows > 0 ? rows * (boxH + gapY) - gapY : 0f;

        maxScroll = Math.max(0f, totalContentH - bodyH);
        if (targetScroll > maxScroll) targetScroll = maxScroll;

        Render2D.pushScissor(vg, bodyX - 2f, bodyY - 2f, bodyW + 4f, bodyH + 4f);

        Module hoveredNow = null;
        Module tooltipModule = null;

        for (int i = 0; i < filteredScratch.size(); i++) {
            Module m = filteredScratch.get(i);
            int col = i % cols;
            int row = i / cols;
            float bx = bodyX + col * (boxW + gapX);
            float by = bodyY + row * (boxH + gapY) - currentScroll;

            // appear animation
            if (m.appearAnim < 1f) m.appearAnim = Math.min(1f, m.appearAnim + dt * 3.8f);
            float ap = easeOutCubic(Math.max(0f, m.appearAnim));

            m.toggleAnim += ((m.isEnabled() ? 1f : 0f) - m.toggleAnim) * speed;
            m.pressAnim = Math.max(0f, m.pressAnim - dt * 5.0f);

            boolean inView = by + boxH >= bodyY - 6f && by <= bodyY + bodyH + 6f;
            boolean boxHov = inView
                    && Render2D.isHovered(mouseX, mouseY, bx, by, boxW, boxH)
                    && Render2D.isHovered(mouseX, mouseY, bodyX, bodyY, bodyW, bodyH);
            if (boxHov) { hoveredNow = m; tooltipModule = m; }
            m.hoverAnim += ((boxHov ? 1f : 0f) - m.hoverAnim) * speed;

            if (!inView || ap < 0.01f) continue;

            m.pressSpring.update(dt);
            float pressDrop = (float) Math.sin(m.pressAnim * Math.PI) * 0.02f;
            float boxScale = 1f + m.hoverAnim * 0.014f + m.pressSpring.getPos() - pressDrop;
            float bcx = bx + boxW * 0.5f;
            float bcy = by + boxH * 0.5f;

            NanoVG.nvgSave(vg);
            if (ap < 0.995f) NanoVG.nvgGlobalAlpha(vg, ap);
            if (Math.abs(boxScale - 1f) > 0.0005f) {
                NanoVG.nvgTranslate(vg, bcx, bcy);
                NanoVG.nvgScale(vg, boxScale, boxScale);
                NanoVG.nvgTranslate(vg, -bcx, -bcy);
            }

            // Liquid Glass + Frosted Glass + Material 3 card
            float activeState = (m == moduleManager.clickGui) ? 0.85f : m.toggleAnim;
            Render2D.drawFrostedGlassCard(
                    engine, bx, by, boxW, boxH, 10.5f,
                    screenW, screenH, palette, activeState, m.hoverAnim, mouseX, mouseY
            );
            if (m.expanded) {
                Render2D.drawRoundedOutline(vg, bx, by, boxW, boxH, 10.5f, 1.4f,
                        AetherPalette.withAlpha(palette.primary, 215));
            }

            // Icon badge (left inside box)
            float iconS = 22f;
            float iconX = bx + 7.5f;
            float iconY = by + (boxH - iconS) * 0.5f;
            float activeL = (m == moduleManager.clickGui) ? 1f : m.toggleAnim;
            int ibg = AetherPalette.lerpArgb(AetherPalette.withAlpha(palette.onSurface, 18), AetherPalette.withAlpha(palette.primary, 210), activeL);
            int ifg = AetherPalette.lerpArgb(palette.onSurfaceVariant, palette.onPrimary, activeL);
            Render2D.drawRoundedRect(vg, iconX, iconY, iconS, iconS, 6.5f, ibg);
            Render2D.drawIcon(vg, iconForModule(m), iconX + iconS * 0.5f, iconY + iconS * 0.5f, 11f, ifg);

            // Module name (bold, clipped to remaining width minus toggle dot space)
            float textX = iconX + iconS + 6.5f;
            float textMaxW = boxW - (textX - bx) - 14f;
            Render2D.pushScissor(vg, textX, by + 2f, Math.max(10f, textMaxW), boxH - 4f);
            Render2D.drawTextBold(vg, m.getName(), textX, by + boxH * 0.5f + 0.3f, 10.2f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            Render2D.popScissor(vg);

            // Toggle dot (top-right corner) or settings indicator for ClickGUI
            float dotX = bx + boxW - 8.5f;
            float dotY = by + 8.5f;
            int dotCol = (m == moduleManager.clickGui || m.isEnabled())
                    ? palette.primary
                    : AetherPalette.withAlpha(palette.onSurfaceVariant, 90);
            Render2D.drawCircle(vg, dotX, dotY, 3.6f, dotCol);

            // Keybind badge on card (if bound or listening)
            String kbBadge = (listeningKeybind == m.getKeybind())
                    ? "..."
                    : (m.getKeybind().getValue() != GLFW.GLFW_KEY_UNKNOWN ? m.getKeybind().getKeyLabel() : null);
            if (kbBadge != null) {
                float badgeW = Render2D.measureTextBold(vg, kbBadge, 8.5f) + 8f;
                float badgeX = dotX - badgeW - 4f;
                float badgeY = by + 3.5f;
                Render2D.drawRoundedRect(vg, badgeX, badgeY, badgeW, 11f, 3.5f,
                        listeningKeybind == m.getKeybind() ? palette.primary : AetherPalette.withAlpha(palette.onSurface, 24));
                Render2D.drawTextBold(vg, kbBadge, badgeX + badgeW * 0.5f, badgeY + 5.5f + 0.3f, 8.5f,
                        listeningKeybind == m.getKeybind() ? palette.onPrimary : palette.onSurfaceVariant,
                        NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            }

            NanoVG.nvgRestore(vg);

            // Click targets
            if (m == moduleManager.clickGui) {
                addClippedClick(clickTargets, bx, by, boxW, boxH, bodyY, bodyY + bodyH, () -> {
                    for (Module other : moduleManager.getModules()) other.expanded = false;
                    m.expanded = true;
                    targetScroll = 0f;
                    currentScroll = 0f;
                    m.pressAnim = 1f;
                    m.pressSpring.impulse(-0.16f);
                    soundEngine.playClick(MinecraftClient.getInstance(), soundsEnabled(), soundVolume());
                });
                addClippedClick(rightClickTargets, bx, by, boxW, boxH, bodyY, bodyY + bodyH, () -> openContextMenu(m, mouseX, mouseY));
            } else {
                addClippedClick(clickTargets, bx, by, boxW, boxH, bodyY, bodyY + bodyH, () -> {
                    m.toggle();
                    m.pressAnim = 1f;
                    m.pressSpring.impulse(-0.16f);
                    if (moduleManager.notifications.isEnabled()) {
                        ToastManager.get().pushModuleToggle(m, moduleManager.notifications);
                    }
                    soundEngine.playToggle(MinecraftClient.getInstance(), m.isEnabled(), soundsEnabled(), soundVolume());
                    configManager.saveClientConfig(moduleManager, hudElements);
                });
                addClippedClick(rightClickTargets, bx, by, boxW, boxH, bodyY, bodyY + bodyH, () -> openContextMenu(m, mouseX, mouseY));
            }
        }

        // Tooltip for hovered module
        if (tooltipModule != null && tooltipModule.hoverAnim > 0.35f) {
            String desc = tooltipModule.getDescription();
            if (desc != null && !desc.isEmpty()) {
                float tTipW = Math.min(bodyW - 8f, Render2D.measureText(vg, desc, 9.2f) + 16f);
                float tTipH = 20f;
                float tTipX = Math.max(bodyX, Math.min(mouseX + 10f, bodyX + bodyW - tTipW - 2f));
                float tTipY = mouseY - tTipH - 5f;
                if (tTipY < bodyY) tTipY = mouseY + 12f;
                NanoVG.nvgSave(vg);
                NanoVG.nvgGlobalAlpha(vg, Math.min(1f, (tooltipModule.hoverAnim - 0.35f) * 1.55f));
                Render2D.drawRoundedRect(vg, tTipX, tTipY, tTipW, tTipH, 6f,
                        AetherPalette.withAlpha(palette.surface, 235));
                Render2D.drawRoundedOutline(vg, tTipX, tTipY, tTipW, tTipH, 6f, 0.8f,
                        AetherPalette.withAlpha(palette.outline, 130));
                Render2D.pushScissor(vg, tTipX + 4f, tTipY, tTipW - 8f, tTipH);
                Render2D.drawText(vg, desc, tTipX + tTipW * 0.5f, tTipY + tTipH * 0.5f, 9.2f,
                        palette.onSurfaceVariant, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
                Render2D.popScissor(vg);
                NanoVG.nvgRestore(vg);
            }
        }

        // Hover sound
        if (hoveredNow != null && hoveredNow != lastHoveredModule) {
            soundEngine.playHover(soundsEnabled(), soundVolume());
        }
        lastHoveredModule = hoveredNow;

        // Scrollbar
        if (maxScroll > 4f) {
            float barX2 = bodyX + bodyW + 2f;
            float thumbRatio = Math.max(0.18f, bodyH / Math.max(bodyH + 1f, totalContentH));
            float thumbH = Math.max(22f, bodyH * thumbRatio);
            float scrollProg = currentScroll / maxScroll;
            float thumbY = bodyY + (bodyH - thumbH) * scrollProg;
            float barW2 = 2.5f + scrollActivity * 1.1f;
            Render2D.drawRoundedRect(vg, barX2, thumbY, barW2, thumbH, barW2 * 0.5f,
                    AetherPalette.withAlpha(palette.primary, 95 + Math.round(scrollActivity * 95f)));
        }

        Render2D.popScissor(vg);

        if (filteredScratch.isEmpty()) {
            Render2D.drawText(vg, I18n.tr("Ничего не найдено", "Nothing found"), bodyX + bodyW * 0.5f, bodyY + bodyH * 0.5f, 11.2f,
                    palette.onSurfaceVariant, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        }
    }

    private void renderModuleSettingsView(
            NanoVgEngine engine, AetherPalette palette, Module m,
            float screenW, float screenH, float dt, float anim
    ) {
        long vg = engine.handle();
        currentScroll += (targetScroll - currentScroll) * Math.min(1f, dt * 16f);

        // Spring-driven entrance: slide up + fade + scale
        float eased = easeOutCubic(Math.max(0f, Math.min(1f, anim)));
        float slideY = (1f - eased) * 14f;
        float viewAlpha = Math.max(0f, Math.min(1f, anim * 1.35f));
        float viewScale = 0.975f + 0.025f * eased;

        NanoVG.nvgSave(vg);
        NanoVG.nvgGlobalAlpha(vg, viewAlpha);
        if (Math.abs(viewScale - 1f) > 0.0005f) {
            float cx = bodyX + bodyW * 0.5f;
            float cy = bodyY + bodyH * 0.5f;
            NanoVG.nvgTranslate(vg, cx, cy + slideY);
            NanoVG.nvgScale(vg, viewScale, viewScale);
            NanoVG.nvgTranslate(vg, -cx, -cy);
        } else if (Math.abs(slideY) > 0.05f) {
            NanoVG.nvgTranslate(vg, 0f, slideY);
        }

        visibleSettingsScratch.clear();
        for (Setting<?> s : m.getSettings()) {
            if (s.isVisible()) visibleSettingsScratch.add(s);
        }

        float topHeaderH = 46f;
        float settingsTotalH = visibleSettingsScratch.size() * 28f + 14f;
        float availH = bodyH - topHeaderH;
        maxScroll = Math.max(0f, settingsTotalH - availH);
        if (targetScroll > maxScroll) targetScroll = maxScroll;

        // Header Card (Frosted Glass + Liquid Glass)
        float cardH = 38f;
        Render2D.drawFrostedGlassCard(
                engine, bodyX, bodyY, bodyW, cardH, 10f,
                screenW, screenH, palette, 0.28f, 0f, mouseX, mouseY
        );

        // Back button
        float backW = 68f;
        float backH = 24f;
        float backX = bodyX + 7f;
        float backY = bodyY + 7f;
        boolean backHov = Render2D.isHovered(mouseX, mouseY, backX, backY, backW, backH);
        if (backHov) {
            Render2D.drawRoundedRect(vg, backX, backY, backW, backH, 6f, AetherPalette.withAlpha(palette.primary, 36));
        }
        Render2D.drawIcon(vg, IconType.ARROW, backX + 9f, backY + backH * 0.5f, 9.5f, backHov ? palette.primary : palette.onSurfaceVariant);
        Render2D.drawTextBold(vg, I18n.tr("Назад", "Back"), backX + 22f, backY + backH * 0.5f + 0.3f, 10.0f,
                backHov ? palette.primary : palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        addClippedClick(clickTargets, backX, backY, backW, backH, bodyY, bodyY + bodyH, () -> {
            m.expanded = false;
            targetScroll = 0f;
            currentScroll = 0f;
            soundEngine.playClick(MinecraftClient.getInstance(), soundsEnabled(), soundVolume());
        });

        // Module icon badge + Title
        float infoX = backX + backW + 10f;
        Render2D.drawIcon(vg, iconForModule(m), infoX + 6f, bodyY + cardH * 0.5f, 11f, palette.primary);
        Render2D.drawTextBold(vg, m.getName(), infoX + 17f, bodyY + cardH * 0.5f + 0.3f, 11.2f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        // Active toggle switch on the right side of header card
        float swW = (m != moduleManager.clickGui) ? 28f : 0f;
        float swX = bodyX + bodyW - swW - 10f;
        if (m != moduleManager.clickGui) {
            m.toggleAnim += ((m.isEnabled() ? 1f : 0f) - m.toggleAnim) * Math.min(1f, dt * 16f);
            float swH = 15.5f;
            float swY = bodyY + (cardH - swH) * 0.5f;
            drawMaterialSwitch(vg, palette, swX, swY, swW, swH, m.toggleAnim);
            addClippedClick(clickTargets, swX - 4f, swY - 4f, swW + 8f, swH + 8f, bodyY, bodyY + bodyH, () -> {
                m.toggle();
                if (moduleManager.notifications.isEnabled()) {
                    ToastManager.get().pushModuleToggle(m, moduleManager.notifications);
                }
                soundEngine.playToggle(MinecraftClient.getInstance(), m.isEnabled(), soundsEnabled(), soundVolume());
                configManager.saveClientConfig(moduleManager, hudElements);
            });
        }

        // Module keybind button (supports keyboard and mouse buttons)
        KeybindSetting modKb = m.getKeybind();
        boolean kbListening = listeningKeybind == modKb;
        String kbLabel = kbListening ? I18n.tr("Нажмите...", "Press...") : (I18n.tr("Бинд: ", "Bind: ") + modKb.getKeyLabel());
        float kbPillW = Math.max(62f, Render2D.measureTextBold(vg, kbLabel, 9.4f) + 24f);
        float kbPillX = (m != moduleManager.clickGui) ? (swX - kbPillW - 8f) : (bodyX + bodyW - kbPillW - 10f);
        float kbPillY = bodyY + (cardH - 20f) * 0.5f;
        boolean kbHov = Render2D.isHovered(mouseX, mouseY, kbPillX, kbPillY, kbPillW, 20f);
        Render2D.drawRoundedRect(vg, kbPillX, kbPillY, kbPillW, 20f, 6f,
                kbListening ? palette.primary : (kbHov ? AetherPalette.withAlpha(palette.primary, 40) : AetherPalette.withAlpha(palette.onSurface, 24)));
        int kbFg = kbListening ? palette.onPrimary : palette.onSurface;
        Render2D.drawIcon(vg, IconType.KEYBIND, kbPillX + 10f, kbPillY + 10f, 9.4f, kbFg);
        Render2D.drawTextBold(vg, kbLabel, kbPillX + 18.5f, kbPillY + 10.2f, 9.4f, kbFg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        addClippedClick(clickTargets, kbPillX, kbPillY, kbPillW, 20f, bodyY, bodyY + cardH, () -> {
            listeningKeybind = modKb;
            soundEngine.playClick(MinecraftClient.getInstance(), soundsEnabled(), soundVolume());
        });

        // Settings list in a dedicated frosted glass card
        float listY = bodyY + topHeaderH;
        float listH = bodyH - topHeaderH;
        Render2D.drawFrostedGlassCard(
                engine, bodyX, listY, bodyW, listH, 11f,
                screenW, screenH, palette, 0.15f, 0f, mouseX, mouseY
        );

        Render2D.pushScissor(vg, bodyX - 2f, listY + 2f, bodyW + 4f, listH - 4f);

        float sy = listY + 8f - currentScroll;
        float clipMin = listY + 2f;
        float clipMax = listY + listH - 2f;
        for (Setting<?> s : visibleSettingsScratch) {
            renderSettingRow(vg, palette, s, bodyX + 12f, sy, bodyW - 24f, clipMin, clipMax, dt);
            sy += 28f;
        }

        // Scrollbar
        if (maxScroll > 4f) {
            float barX2 = bodyX + bodyW - 5f;
            float thumbRatio = Math.max(0.18f, listH / Math.max(listH + 1f, settingsTotalH));
            float thumbH = Math.max(22f, listH * thumbRatio);
            float scrollProg = currentScroll / maxScroll;
            float thumbY = listY + 4f + (listH - 8f - thumbH) * scrollProg;
            Render2D.drawRoundedRect(vg, barX2, thumbY, 3f, thumbH, 1.5f,
                    AetherPalette.withAlpha(palette.primary, 130));
        }

        Render2D.popScissor(vg);

        NanoVG.nvgRestore(vg);
    }

    private void renderInfoBody(NanoVgEngine engine, AetherPalette palette, float screenW, float screenH, float dt) {
        long vg = engine.handle();
        bodyX = pX + 16f;
        bodyY = pY + 44f;
        bodyW = pW - 32f;
        bodyH = Math.max(80f, pH - 66f);

        // Top Banner with Liquid Glass
        float bannerH = 46f;
        Render2D.drawFrostedGlassCard(
                engine, bodyX, bodyY, bodyW, bannerH, 10f,
                screenW, screenH, palette, 0.12f, 0f, mouseX, mouseY
        );
        Render2D.drawLogo(engine, bodyX + 12f, bodyY + 11f, 22f, 6f);
        Render2D.drawTextBold(vg, "AETHER CLIENT", bodyX + 40f, bodyY + 18f, 13.5f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        Render2D.drawText(vg, "Material 3 • Liquid Glass • Fabric 1.21 • Pure Visuals", bodyX + 40f, bodyY + 32f, 9.5f, palette.onSurfaceVariant, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        // Version Badge
        float badgeW = 96f;
        float badgeH = 20f;
        float badgeX = bodyX + bodyW - badgeW - 12f;
        float badgeY = bodyY + 13f;
        Render2D.drawRoundedGradient(vg, badgeX, badgeY, badgeW, badgeH, 6f, palette.primary, palette.tertiary, false);
        Render2D.drawTextBold(vg, "v1.2.0-RELEASE", badgeX + badgeW * 0.5f, badgeY + badgeH * 0.5f + 0.4f, 9.6f, palette.onPrimary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        // Two Cards Side-by-Side
        float cardY = bodyY + bannerH + 8f;
        float cardH = bodyH - bannerH - 8f;
        float colGap = 8f;
        float colW = (bodyW - colGap) * 0.5f;

        // Left Card: Profile & Session Info
        float c1X = bodyX;
        Render2D.drawFrostedGlassCard(
                engine, c1X, cardY, colW, cardH, 10f,
                screenW, screenH, palette, 0.08f, 0f, mouseX, mouseY
        );
        Render2D.drawIcon(vg, IconType.USER, c1X + 14f, cardY + 16f, 12f, palette.primary);
        Render2D.drawTextBold(vg, I18n.tr("Сессия и Профиль", "Session & Profile"), c1X + 30f, cardY + 16.5f, 11.2f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        Render2D.drawRoundedRect(vg, c1X + 12f, cardY + 28f, colW - 24f, 1f, 0f, AetherPalette.withAlpha(palette.outline, 45));

        MinecraftClient mc = MinecraftClient.getInstance();
        String player = (mc != null && mc.getSession() != null) ? mc.getSession().getUsername() : "Player";
        long elapsedSec = (System.currentTimeMillis() - dev.aetherclient.client.AetherClientMain.CLIENT_START_TIME) / 1000L;
        long h = elapsedSec / 3600;
        long m = (elapsedSec % 3600) / 60;
        long s = elapsedSec % 60;
        String playtime = String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s);
        int fps = mc != null ? mc.getCurrentFps() : 0;

        float py = cardY + 40f;
        float lineH = 18f;
        Render2D.drawText(vg, I18n.tr("Игрок: ", "Player: ") + player, c1X + 14f, py, 10.0f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        py += lineH;
        Render2D.drawText(vg, I18n.tr("Время сессии: ", "Session Playtime: ") + playtime, c1X + 14f, py, 10.0f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        py += lineH;
        Render2D.drawText(vg, I18n.tr("Частота кадров: ", "Framerate: ") + fps + " FPS", c1X + 14f, py, 10.0f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        py += lineH;
        Render2D.drawText(vg, I18n.tr("Защита: ", "Protection: ") + I18n.tr("Zero-Flag (Client-Side)", "Zero-Flag (Client-Side)"), c1X + 14f, py, 10.0f, 0xFF10B981, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        py += lineH;
        Render2D.drawText(vg, I18n.tr("Выгрузка: HotSpot retransformation", "Unload: HotSpot retransformation"), c1X + 14f, py, 9.2f, palette.onSurfaceVariant, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        // Right Card: Contacts & Support
        float c2X = bodyX + colW + colGap;
        Render2D.drawFrostedGlassCard(
                engine, c2X, cardY, colW, cardH, 10f,
                screenW, screenH, palette, 0.08f, 0f, mouseX, mouseY
        );
        Render2D.drawIcon(vg, IconType.CLIENT, c2X + 14f, cardY + 16f, 12f, palette.tertiary);
        Render2D.drawTextBold(vg, I18n.tr("Контакты и Поддержка", "Contacts & Support"), c2X + 30f, cardY + 16.5f, 11.2f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        Render2D.drawRoundedRect(vg, c2X + 12f, cardY + 28f, colW - 24f, 1f, 0f, AetherPalette.withAlpha(palette.outline, 45));

        drawContactRow(vg, palette, c2X + 14f, cardY + 38f, colW - 28f, "Discord", "nelttan.gg", () -> {
            if (mc != null && mc.keyboard != null) {
                mc.keyboard.setClipboard("nelttan.gg");
                ToastManager.get().push("Контакты", "Discord nelttan.gg скопирован в буфер", palette.primary);
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
            }
        });

        drawContactRow(vg, palette, c2X + 14f, cardY + 70f, colW - 28f, "Telegram", "@shadowwtls", () -> {
            if (mc != null && mc.keyboard != null) {
                mc.keyboard.setClipboard("@shadowwtls");
                ToastManager.get().push("Контакты", "Telegram @shadowwtls скопирован в буфер", palette.primary);
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
            }
        });

        drawContactRow(vg, palette, c2X + 14f, cardY + 102f, colW - 28f, "Telegram", "@Phant0mNight", () -> {
            if (mc != null && mc.keyboard != null) {
                mc.keyboard.setClipboard("@Phant0mNight");
                ToastManager.get().push("Контакты", "Telegram @Phant0mNight скопирован в буфер", palette.primary);
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
            }
        });

        Render2D.drawText(vg, I18n.tr("Нажмите кнопку чтобы скопировать контакт", "Click button to copy contact to clipboard"), c2X + 14f, cardY + cardH - 14f, 8.8f, palette.onSurfaceVariant, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
    }

    private void drawContactRow(long vg, AetherPalette palette, float x, float y, float w, String label, String handle, Runnable onCopy) {
        Render2D.drawRoundedRect(vg, x, y, w, 26f, 6.5f, AetherPalette.withAlpha(palette.surface, 120));
        Render2D.drawTextBold(vg, label + ":", x + 8f, y + 13f, 9.8f, palette.primary, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        Render2D.drawText(vg, handle, x + 58f, y + 13f, 9.8f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        float btnW = 56f;
        float btnH = 19f;
        float btnX = x + w - btnW - 4f;
        float btnY = y + 3.5f;
        boolean hov = Render2D.isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH);
        Render2D.drawRoundedRect(vg, btnX, btnY, btnW, btnH, 5f, hov ? palette.primary : AetherPalette.withAlpha(palette.primary, 175));
        Render2D.drawTextBold(vg, I18n.tr("Копия", "Copy"), btnX + btnW * 0.5f, btnY + btnH * 0.5f + 0.3f, 8.8f, palette.onPrimary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        addClippedClick(clickTargets, btnX, btnY, btnW, btnH, bodyY, bodyY + bodyH, onCopy);
    }

    private void renderFriendsBody(NanoVgEngine engine, AetherPalette palette, float screenW, float screenH, float dt) {
        long vg = engine.handle();
        bodyX = pX + 16f;
        bodyY = pY + 44f;
        bodyW = pW - 32f;
        bodyH = Math.max(80f, pH - 66f);
        MinecraftClient mc = MinecraftClient.getInstance();

        // Top Row: New friend input and Add button
        float inputW = bodyW - 110f;
        float inputH = 28f;
        float inputX = bodyX;
        float inputY = bodyY;
        boolean inHov = Render2D.isHovered(mouseX, mouseY, inputX, inputY, inputW, inputH);
        Render2D.drawRoundedRect(vg, inputX, inputY, inputW, inputH, 7f,
                AetherPalette.withAlpha(palette.surface, 140));
        Render2D.drawRoundedOutline(vg, inputX, inputY, inputW, inputH, 7f, 0.9f,
                friendNameFocused ? palette.primary : AetherPalette.withAlpha(palette.outline, 70));

        String friendShown = newFriendName.isEmpty()
                ? (friendNameFocused ? "|" : I18n.tr("Никнейм игрока...", "Player username..."))
                : (newFriendName + (friendNameFocused ? "|" : ""));
        int friendColor = newFriendName.isEmpty() && !friendNameFocused ? palette.onSurfaceVariant : palette.onSurface;
        Render2D.drawText(vg, friendShown, inputX + 10f, inputY + inputH * 0.5f + 0.4f, 10.0f, friendColor,
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        addClippedClick(clickTargets, inputX, inputY, inputW, inputH, bodyY, bodyY + bodyH, () -> {
            friendNameFocused = true;
            configNameFocused = false;
            searchFocused = false;
        });

        float addBtnX = inputX + inputW + 6f;
        float addBtnW = 104f;
        boolean addHov = Render2D.isHovered(mouseX, mouseY, addBtnX, inputY, addBtnW, inputH);
        Render2D.drawRoundedRect(vg, addBtnX, inputY, addBtnW, inputH, 7f,
                addHov ? palette.tertiary : palette.primary);
        Render2D.drawTextBold(vg, I18n.tr("+ Добавить", "+ Add Friend"), addBtnX + addBtnW * 0.5f, inputY + inputH * 0.5f + 0.4f, 10.0f,
                palette.onPrimary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        addClippedClick(clickTargets, addBtnX, inputY, addBtnW, inputH, bodyY, bodyY + bodyH, () -> {
            if (!newFriendName.isBlank()) {
                String name = newFriendName.trim();
                boolean added = dev.aetherclient.client.friend.FriendsManager.get().addFriend(name);
                if (added) {
                    ToastManager.get().push("Друзья", "Добавлен в друзья: " + name, 0xFF10B981);
                } else {
                    ToastManager.get().push("Друзья", "Игрок уже в друзьях: " + name, 0xFFF59E0B);
                }
                newFriendName = "";
                friendNameFocused = false;
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
            }
        });

        // Friends list container
        float listY = inputY + inputH + 8f;
        float listH = bodyH - inputH - 34f;
        Render2D.drawFrostedGlassCard(
                engine, bodyX, listY, bodyW, listH, 10f,
                screenW, screenH, palette, 0.08f, 0f, mouseX, mouseY
        );

        java.util.Set<String> friends = dev.aetherclient.client.friend.FriendsManager.get().getFriends();
        float itemH = 28f;
        float itemGap = 4f;
        float curY = listY + 6f;

        if (friends.isEmpty()) {
            Render2D.drawIcon(vg, IconType.HEART, bodyX + bodyW * 0.5f, listY + listH * 0.40f, 24f, AetherPalette.withAlpha(palette.primary, 140));
            Render2D.drawTextBold(vg, I18n.tr("Список друзей пуст", "Friends list is empty"),
                    bodyX + bodyW * 0.5f, listY + listH * 0.40f + 24f, 11.2f, palette.onSurface,
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            Render2D.drawText(vg, I18n.tr("Добавляйте друзей через поле выше, кликом СКМ по игроку или .friend add <ник>",
                    "Add friends via the box above, middle-click on a player or .friend add <name>"),
                    bodyX + bodyW * 0.5f, listY + listH * 0.40f + 40f, 9.2f, palette.onSurfaceVariant,
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        } else {
            Render2D.pushScissor(vg, bodyX, listY + 4f, bodyW, listH - 8f);
            for (String friendName : friends) {
                if (curY + itemH > listY + listH - 4f) break;
                boolean itemHov = Render2D.isHovered(mouseX, mouseY, bodyX + 6f, curY, bodyW - 12f, itemH);
                Render2D.drawRoundedRect(vg, bodyX + 6f, curY, bodyW - 12f, itemH, 6.5f,
                        itemHov ? AetherPalette.withAlpha(palette.onSurface, 14) : AetherPalette.withAlpha(palette.surface, 80));

                Render2D.drawIcon(vg, IconType.HEART, bodyX + 18f, curY + itemH * 0.5f, 10.5f, 0xFF10B981);
                Render2D.drawTextBold(vg, friendName, bodyX + 32f, curY + itemH * 0.5f + 0.4f, 10.2f,
                        palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

                // Delete button: [Удалить]
                float btnH2 = 19f;
                float btnY2 = curY + (itemH - btnH2) * 0.5f;
                float delW = 58f;
                float delX = bodyX + bodyW - 12f - delW;
                boolean delHov = Render2D.isHovered(mouseX, mouseY, delX, btnY2, delW, btnH2);
                Render2D.drawRoundedRect(vg, delX, btnY2, delW, btnH2, 5.5f,
                        AetherPalette.withAlpha(0xFFFF5F6D, delHov ? 200 : 130));
                Render2D.drawTextBold(vg, I18n.tr("Удалить", "Delete"), delX + delW * 0.5f, btnY2 + btnH2 * 0.5f + 0.4f, 8.8f,
                        0xFFFFFFFF, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

                addClippedClick(clickTargets, delX, btnY2, delW, btnH2, bodyY, bodyY + bodyH, () -> {
                    boolean removed = dev.aetherclient.client.friend.FriendsManager.get().removeFriend(friendName);
                    if (removed) {
                        ToastManager.get().push("Друзья", "Удален из друзей: " + friendName, 0xFFF43F5E);
                    }
                    soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                });

                curY += itemH + itemGap;
            }
            Render2D.popScissor(vg);
        }

        // Footer info: friend count & quick instructions
        float footY = bodyY + bodyH - 18f;
        String countStr = I18n.tr("Всего друзей: ", "Total friends: ") + friends.size()
                + "  •  " + I18n.tr("СКМ по игроку — быстрое добавление", "MMB on player — quick toggle");
        Render2D.drawText(vg, countStr, bodyX + 8f, footY + 4f, 9.0f, palette.onSurfaceVariant,
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
    }

    private void renderConfigsBody(NanoVgEngine engine, AetherPalette palette, float screenW, float screenH, float dt) {
        long vg = engine.handle();
        bodyX = pX + 16f;
        bodyY = pY + 44f;
        bodyW = pW - 32f;
        bodyH = Math.max(80f, pH - 66f);
        MinecraftClient mc = MinecraftClient.getInstance();

        // Top Row: New config input and create button
        float inputW = bodyW - 110f;
        float inputH = 28f;
        float inputX = bodyX;
        float inputY = bodyY;
        boolean inHov = Render2D.isHovered(mouseX, mouseY, inputX, inputY, inputW, inputH);
        Render2D.drawRoundedRect(vg, inputX, inputY, inputW, inputH, 7f,
                AetherPalette.withAlpha(palette.surface, 140));
        Render2D.drawRoundedOutline(vg, inputX, inputY, inputW, inputH, 7f, 0.9f,
                configNameFocused ? palette.primary : AetherPalette.withAlpha(palette.outline, 70));

        String cfgShown = newConfigName.isEmpty()
                ? (configNameFocused ? "|" : I18n.tr("Имя нового конфига...", "New config name..."))
                : (newConfigName + (configNameFocused ? "|" : ""));
        int cfgColor = newConfigName.isEmpty() && !configNameFocused ? palette.onSurfaceVariant : palette.onSurface;
        Render2D.drawText(vg, cfgShown, inputX + 10f, inputY + inputH * 0.5f + 0.4f, 10.0f, cfgColor,
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        addClippedClick(clickTargets, inputX, inputY, inputW, inputH, bodyY, bodyY + bodyH, () -> {
            configNameFocused = true;
            searchFocused = false;
        });

        float createBtnX = inputX + inputW + 6f;
        float createBtnW = 104f;
        boolean createHov = Render2D.isHovered(mouseX, mouseY, createBtnX, inputY, createBtnW, inputH);
        Render2D.drawRoundedRect(vg, createBtnX, inputY, createBtnW, inputH, 7f,
                createHov ? palette.tertiary : palette.primary);
        Render2D.drawTextBold(vg, I18n.tr("Сохранить", "Save New"), createBtnX + createBtnW * 0.5f, inputY + inputH * 0.5f + 0.4f, 10.0f,
                palette.onPrimary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        addClippedClick(clickTargets, createBtnX, inputY, createBtnW, inputH, bodyY, bodyY + bodyH, () -> {
            if (!newConfigName.isBlank()) {
                String name = newConfigName.trim();
                boolean ok = configManager.saveNamedConfig(name, moduleManager, hudElements);
                if (ok) {
                    activeConfigName = name;
                    ToastManager.get().push("Конфиги", "Конфиг сохранен: " + name, 0xFF10B981);
                } else {
                    ToastManager.get().push("Конфиги", "Ошибка сохранения: " + name, 0xFFF43F5E);
                }
                newConfigName = "";
                configNameFocused = false;
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
            }
        });

        // Configs list container
        float listY = inputY + inputH + 8f;
        float listH = bodyH - inputH - 34f;
        Render2D.drawFrostedGlassCard(
                engine, bodyX, listY, bodyW, listH, 10f,
                screenW, screenH, palette, 0.08f, 0f, mouseX, mouseY
        );

        List<String> configs = configManager.listAvailableConfigs();
        float itemH = 28f;
        float itemGap = 4f;
        float curY = listY + 6f;

        Render2D.pushScissor(vg, bodyX, listY + 4f, bodyW, listH - 8f);
        for (String cfg : configs) {
            if (curY + itemH > listY + listH - 4f) break;
            boolean isActive = cfg.equals(activeConfigName);
            boolean itemHov = Render2D.isHovered(mouseX, mouseY, bodyX + 6f, curY, bodyW - 12f, itemH);
            Render2D.drawRoundedRect(vg, bodyX + 6f, curY, bodyW - 12f, itemH, 6.5f,
                    isActive ? AetherPalette.withAlpha(palette.primary, 35) : (itemHov ? AetherPalette.withAlpha(palette.onSurface, 14) : AetherPalette.withAlpha(palette.surface, 80)));

            Render2D.drawIcon(vg, IconType.SLIDERS, bodyX + 18f, curY + itemH * 0.5f, 10.5f, isActive ? palette.primary : palette.onSurfaceVariant);
            Render2D.drawTextBold(vg, cfg, bodyX + 32f, curY + itemH * 0.5f + 0.4f, 10.2f,
                    isActive ? palette.primary : palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

            if (isActive) {
                Render2D.drawText(vg, I18n.tr("[АКТИВЕН]", "[ACTIVE]"), bodyX + 36f + Render2D.measureText(vg, cfg, 10.2f), curY + itemH * 0.5f + 0.4f, 8.5f, 0xFF10B981, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            }

            // Buttons: [Загрузить] [Перезаписать] [Удалить]
            float btnH2 = 19f;
            float btnY2 = curY + (itemH - btnH2) * 0.5f;
            float rightX = bodyX + bodyW - 12f;

            if (!cfg.equals("client")) {
                float delW = 54f;
                rightX -= delW + 4f;
                boolean delHov = Render2D.isHovered(mouseX, mouseY, rightX, btnY2, delW, btnH2);
                Render2D.drawRoundedRect(vg, rightX, btnY2, delW, btnH2, 5f,
                        delHov ? 0xFFF43F5E : AetherPalette.withAlpha(0xFFFF5F6D, 160));
                Render2D.drawTextBold(vg, I18n.tr("Удалить", "Delete"), rightX + delW * 0.5f, btnY2 + btnH2 * 0.5f + 0.3f, 8.8f, 0xFFFFFFFF, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

                addClippedClick(clickTargets, rightX, btnY2, delW, btnH2, listY, listY + listH, () -> {
                    boolean ok = configManager.deleteNamedConfig(cfg);
                    if (ok) {
                        ToastManager.get().push("Конфиги", "Конфиг удален: " + cfg, 0xFFFF5F6D);
                    }
                    soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                });
            }

            float saveW = 66f;
            rightX -= saveW + 4f;
            boolean saveHov = Render2D.isHovered(mouseX, mouseY, rightX, btnY2, saveW, btnH2);
            Render2D.drawRoundedRect(vg, rightX, btnY2, saveW, btnH2, 5f,
                    saveHov ? palette.primaryContainer : AetherPalette.withAlpha(palette.primaryContainer, 180));
            Render2D.drawTextBold(vg, I18n.tr("Записать", "Overwrite"), rightX + saveW * 0.5f, btnY2 + btnH2 * 0.5f + 0.3f, 8.8f, palette.onPrimaryContainer, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            addClippedClick(clickTargets, rightX, btnY2, saveW, btnH2, listY, listY + listH, () -> {
                boolean ok = configManager.saveNamedConfig(cfg, moduleManager, hudElements);
                if (ok) {
                    activeConfigName = cfg;
                    ToastManager.get().push("Конфиги", "Конфиг перезаписан: " + cfg, 0xFF10B981);
                }
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
            });

            float loadW = 60f;
            rightX -= loadW + 4f;
            boolean loadHov = Render2D.isHovered(mouseX, mouseY, rightX, btnY2, loadW, btnH2);
            Render2D.drawRoundedRect(vg, rightX, btnY2, loadW, btnH2, 5f,
                    loadHov ? palette.primary : AetherPalette.withAlpha(palette.primary, 180));
            Render2D.drawTextBold(vg, I18n.tr("Загрузить", "Load"), rightX + loadW * 0.5f, btnY2 + btnH2 * 0.5f + 0.3f, 8.8f, palette.onPrimary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            addClippedClick(clickTargets, rightX, btnY2, loadW, btnH2, listY, listY + listH, () -> {
                boolean ok = configManager.loadNamedConfig(cfg, moduleManager, hudElements);
                if (ok) {
                    activeConfigName = cfg;
                    ToastManager.get().push("Конфиги", "Загружен конфиг: " + cfg, 0xFF38BDF8);
                }
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
            });

            curY += itemH + itemGap;
        }
        Render2D.popScissor(vg);

        // Bottom Action Buttons: Code Share & Reset
        float resetY = listY + listH + 4f;
        float btnRowH = 20f;
        float btnGap = 8f;
        float codeBtnW = (bodyW - btnGap * 2f) / 3f;

        // Button 1: Copy Code to Clipboard
        float b1X = bodyX;
        boolean b1Hov = Render2D.isHovered(mouseX, mouseY, b1X, resetY, codeBtnW, btnRowH);
        Render2D.drawRoundedRect(vg, b1X, resetY, codeBtnW, btnRowH, 5f,
                b1Hov ? palette.primary : AetherPalette.withAlpha(palette.primary, 175));
        Render2D.drawTextBold(vg, I18n.tr("Копировать код", "Copy Code"), b1X + codeBtnW * 0.5f, resetY + btnRowH * 0.5f + 0.3f, 8.8f,
                palette.onPrimary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        addClippedClick(clickTargets, b1X, resetY, codeBtnW, btnRowH, bodyY, bodyY + bodyH, () -> {
            String code = configManager.exportConfigCode(moduleManager, hudElements);
            if (mc != null && mc.keyboard != null) {
                mc.keyboard.setClipboard(code);
                ToastManager.get().push("Конфиги", "Код скопирован в буфер! (Без файлов на диске)", 0xFF10B981);
            }
            soundEngine.playClick(mc, soundsEnabled(), soundVolume());
        });

        // Button 2: Import Code from Clipboard
        float b2X = b1X + codeBtnW + btnGap;
        boolean b2Hov = Render2D.isHovered(mouseX, mouseY, b2X, resetY, codeBtnW, btnRowH);
        Render2D.drawRoundedRect(vg, b2X, resetY, codeBtnW, btnRowH, 5f,
                b2Hov ? palette.tertiary : AetherPalette.withAlpha(palette.tertiary, 175));
        Render2D.drawTextBold(vg, I18n.tr("Вставить код", "Paste Code"), b2X + codeBtnW * 0.5f, resetY + btnRowH * 0.5f + 0.3f, 8.8f,
                palette.onPrimary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        addClippedClick(clickTargets, b2X, resetY, codeBtnW, btnRowH, bodyY, bodyY + bodyH, () -> {
            if (mc != null && mc.keyboard != null) {
                String clip = mc.keyboard.getClipboard();
                boolean ok = configManager.importConfigCode(clip, moduleManager, hudElements);
                if (ok) {
                    ToastManager.get().push("Конфиги", "Конфиг успешно применён из кода!", 0xFF10B981);
                } else {
                    ToastManager.get().push("Конфиги", "Некорректный код конфига в буфере!", 0xFFEF4444);
                }
            }
            soundEngine.playClick(mc, soundsEnabled(), soundVolume());
        });

        // Button 3: Reset Settings
        float b3X = b2X + codeBtnW + btnGap;
        boolean rHov = Render2D.isHovered(mouseX, mouseY, b3X, resetY, codeBtnW, btnRowH);
        Render2D.drawRoundedRect(vg, b3X, resetY, codeBtnW, btnRowH, 5f,
                rHov ? AetherPalette.withAlpha(palette.onSurface, 35) : AetherPalette.withAlpha(palette.onSurface, 18));
        Render2D.drawText(vg, I18n.tr("Сбросить всё", "Reset All"), b3X + codeBtnW * 0.5f, resetY + btnRowH * 0.5f + 0.3f, 8.8f,
                palette.onSurfaceVariant, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

        addClippedClick(clickTargets, b3X, resetY, codeBtnW, btnRowH, bodyY, bodyY + bodyH, () -> {
            for (Module m : moduleManager.getModules()) {
                if (m.isEnabled()) m.setEnabled(false);
            }
            configManager.saveClientConfig(moduleManager, hudElements);
            ToastManager.get().push("Конфиги", "Настройки сброшены", 0xFFF59E0B);
            soundEngine.playClick(mc, soundsEnabled(), soundVolume());
        });
    }

    private void renderFooterBar(long vg, AetherPalette palette) {
        float footY = pY + pH - 19f;
        Render2D.drawRoundedRect(vg, pX + 10f, footY - 3.5f, pW - 20f, 1f, 0f, AetherPalette.withAlpha(palette.outline, 45));

        int enabledCount = 0;
        int totalToggleable = 0;
        for (Module m : moduleManager.getModules()) {
            if (m == moduleManager.clickGui) continue;
            totalToggleable++;
            if (m.isEnabled()) enabledCount++;
        }

        String leftStatus = I18n.tr("Активно: ", "Active: ") + enabledCount + "/" + totalToggleable;
        Render2D.drawText(vg, leftStatus, pX + 12f, footY + 6f, 9.2f,
                palette.onSurfaceVariant, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        String bindName = moduleManager.clickGui.getKeybind().getKeyLabel();
        String hint = I18n.tr("ЛКМ — вкл/выкл  •  ПКМ — настройки  •  ", "LMB — toggle  •  RMB — settings  •  ") + bindName;
        Render2D.drawText(vg, hint, pX + pW - 12f, footY + 6f, 9.2f,
                palette.onSurfaceVariant, NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);
    }

    private void renderSettingRow(
            long vg, AetherPalette palette, Setting<?> s,
            float x, float y, float w, float clipMinY, float clipMaxY, float dt
    ) {
        boolean rowHov = Render2D.isHovered(mouseX, mouseY, x - 3f, y, w + 6f, 24f)
                && mouseY >= clipMinY && mouseY <= clipMaxY;
        if (rowHov) {
            Render2D.drawRoundedRect(vg, x - 3f, y + 0.5f, w + 6f, 23f, 6f,
                    AetherPalette.withAlpha(palette.onSurface, 12));
        }

        Render2D.drawText(vg, s.getName(), x + 2f, y + 12f, 10.2f,
                rowHov ? palette.onSurface : palette.onSurfaceVariant,
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        switch (s) {
            case BooleanSetting bs -> {
                bs.animProgress += ((bs.isEnabled() ? 1f : 0f) - bs.animProgress) * Math.min(1f, dt * 16f);
                float swW = 28f;
                float swH = 15.5f;
                float swX = x + w - swW;
                float swY = y + 4.25f;
                drawMaterialSwitch(vg, palette, swX, swY, swW, swH, bs.animProgress);
                addClippedClick(clickTargets, swX - 4f, swY - 4f, swW + 8f, swH + 8f, clipMinY, clipMaxY, () -> {
                    bs.toggle();
                    soundEngine.playToggle(MinecraftClient.getInstance(), bs.isEnabled(), soundsEnabled(), soundVolume());
                });
            }
            case NumberSetting ns -> {
                String valStr = ns.formatted();
                float badgeW = Math.max(34f, Render2D.measureTextBold(vg, valStr, 9.4f) + 10f);
                Render2D.drawRoundedRect(vg, x + w - badgeW, y + 3.5f, badgeW, 17f, 5f,
                        AetherPalette.withAlpha(palette.primary, draggingSlider == ns ? 55 : 28));
                Render2D.drawTextBold(vg, valStr, x + w - badgeW * 0.5f, y + 12.2f, 9.4f,
                        palette.primary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

                float trW = Math.min(144f, w * 0.42f);
                float trX = x + w - badgeW - trW - 9f;
                float trY = y + 10.2f;
                if (draggingSlider == ns) {
                    sliderTrackX = trX;
                    sliderTrackW = trW;
                }
                float targetRatio = (float) ((ns.getValue() - ns.getMin()) / Math.max(1e-6, ns.getMax() - ns.getMin()));
                if (ns.visualRatio < 0f) {
                    ns.visualRatio = targetRatio;
                } else {
                    ns.visualRatio += (targetRatio - ns.visualRatio) * Math.min(1f, dt * 24f);
                }
                float knobX = trX + trW * ns.visualRatio;

                Render2D.drawRoundedRect(vg, trX, trY, trW, 3.8f, 1.9f, AetherPalette.withAlpha(palette.onSurface, 42));
                Render2D.drawRoundedRect(vg, trX, trY, Math.max(3.8f, trW * ns.visualRatio), 3.8f, 1.9f, palette.primary);
                if (draggingSlider == ns || rowHov) {
                    Render2D.drawCircle(vg, knobX, trY + 1.9f, draggingSlider == ns ? 7.8f : 6.4f,
                            AetherPalette.withAlpha(palette.primary, 55));
                }
                Render2D.drawCircle(vg, knobX, trY + 1.9f, 4.8f, palette.onPrimary);
                Render2D.drawCircle(vg, knobX, trY + 1.9f, 3.3f, palette.primary);

                addClippedSlider(ns, trX - 6f, y + 1f, trW + 12f, 22f, trX, trW, clipMinY, clipMaxY);
            }
            case ModeSetting ms -> {
                String val = localizeModeValue(ms.getValue());
                float pillW = Math.max(60f, Render2D.measureTextBold(vg, val, 9.5f) + 16f);
                float pillX = x + w - pillW;
                float pillY = y + 3f;
                Render2D.drawRoundedRect(vg, pillX, pillY, pillW, 18f, 6f, AetherPalette.withAlpha(palette.primaryContainer, 215));
                Render2D.drawTextBold(vg, val, pillX + pillW * 0.5f, pillY + 9.2f, 9.5f, palette.onSurface, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
                addClippedClick(clickTargets, pillX, pillY, pillW, 18f, clipMinY, clipMaxY, () -> {
                    ms.cycle(1);
                    soundEngine.playClick(MinecraftClient.getInstance(), soundsEnabled(), soundVolume());
                });
            }
            case ColorSetting cs -> {
                float sx = x + w - SWATCH_COLORS.length * 18f + 2f;
                float sy = y + 4.5f;
                for (int col : SWATCH_COLORS) {
                    boolean sel = cs.getValue() == col;
                    boolean cHov = Render2D.isHovered(mouseX, mouseY, sx - 1f, sy - 1f, 16f, 16f);
                    Render2D.drawCircle(vg, sx + 7f, sy + 7f, sel ? 6.6f : (cHov ? 6.1f : 5.4f), col);
                    if (sel) {
                        Render2D.drawCircleOutline(vg, sx + 7f, sy + 7f, 7.7f, 1.15f, palette.onSurface);
                    }
                    final int chosen = col;
                    addClippedClick(clickTargets, sx - 1f, sy - 1f, 16f, 16f, clipMinY, clipMaxY, () -> {
                        cs.setValue(chosen);
                        soundEngine.playClick(MinecraftClient.getInstance(), soundsEnabled(), soundVolume());
                    });
                    sx += 18f;
                }
            }
            case KeybindSetting ks -> {
                boolean listening = listeningKeybind == ks;
                String label = listening ? I18n.tr("Нажмите...", "Press key...") : ks.getKeyLabel();
                float pillW = Math.max(58f, Render2D.measureTextBold(vg, label, 9.5f) + 24f);
                float pillX = x + w - pillW;
                float pillY = y + 3f;
                Render2D.drawRoundedRect(vg, pillX, pillY, pillW, 18f, 6f,
                        listening ? palette.primary : AetherPalette.withAlpha(palette.onSurface, 26));
                int fg = listening ? palette.onPrimary : palette.onSurface;
                Render2D.drawIcon(vg, IconType.KEYBIND, pillX + 10.5f, pillY + 9f, 9.4f, fg);
                Render2D.drawTextBold(vg, label, pillX + 18.5f, pillY + 9.2f, 9.5f, fg, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
                addClippedClick(clickTargets, pillX, pillY, pillW, 18f, clipMinY, clipMaxY, () -> {
                    listeningKeybind = ks;
                    soundEngine.playClick(MinecraftClient.getInstance(), soundsEnabled(), soundVolume());
                });
            }
            default -> {}
        }
    }

    private static void drawMaterialSwitch(long vg, AetherPalette palette, float x, float y, float w, float h, float progress) {
        int trackOff = AetherPalette.withAlpha(palette.onSurface, 36);
        int track = AetherPalette.lerpArgb(trackOff, palette.primary, progress);
        Render2D.drawRoundedRect(vg, x, y, w, h, h * 0.5f, track);

        if (progress < 0.95f) {
            Render2D.drawRoundedOutline(vg, x + 0.5f, y + 0.5f, w - 1f, h - 1f, h * 0.5f, 1f,
                    AetherPalette.withAlpha(palette.outline, Math.round((1f - progress) * 140f)));
        }

        float thumbR = (h - 6.5f) * 0.5f + progress * 1.2f;
        float startCx = x + h * 0.5f;
        float endCx = x + w - h * 0.5f;
        float cx = startCx + (endCx - startCx) * progress;
        float cy = y + h * 0.5f;
        int thumbCol = AetherPalette.lerpArgb(palette.onSurfaceVariant, palette.onPrimary, progress);
        Render2D.drawCircle(vg, cx, cy, thumbR, thumbCol);
    }

    private static IconType iconForCategory(Category cat) {
        return switch (cat) {
            case VISUAL -> IconType.VISUAL;
            case HUD -> IconType.HUD;
            case COMBAT -> IconType.COMBAT;
            case CLIENT -> IconType.CLIENT;
        };
    }

    private IconType iconForModule(Module m) {
        if (m == moduleManager.clickGui) return IconType.SLIDERS;
        if (m == moduleManager.hud) return IconType.HUD;
        if (m == moduleManager.targetHud) return IconType.TARGET;
        if (m == moduleManager.fullbright) return IconType.SUN;
        if (m == moduleManager.esp) return IconType.EYE;
        if (m == moduleManager.tracers) return IconType.LINES;
        if (m == moduleManager.targetEsp) return IconType.RING;
        if (m == moduleManager.arrows) return IconType.ARROW;
        if (m == moduleManager.jumpCircles) return IconType.RING;
        if (m == moduleManager.trails) return IconType.SPARKLES;
        if (m == moduleManager.hitParticles) return IconType.SPARKLES;
        if (m == moduleManager.crosshair) return IconType.TARGET;
        if (m == moduleManager.antiBlind) return IconType.SHIELD;
        if (m == moduleManager.ambience) return IconType.CLOCK;
        if (m == moduleManager.zoom) return IconType.SEARCH;
        if (m == moduleManager.particles) return IconType.SPARKLES;
        if (m == moduleManager.itemEsp) return IconType.POTION;
        if (m == moduleManager.itemPhysics) return IconType.SPARKLES;
        if (m == moduleManager.damageVignette) return IconType.SHIELD;
        if (m == moduleManager.nameProtect) return IconType.USER;
        if (m == moduleManager.hpCounter) return IconType.POTION;
        if (m == moduleManager.swingAnimation) return IconType.SPARKLES;
        if (m == moduleManager.chinaHat) return IconType.SPARKLES;
        if (m == moduleManager.pearlPredict) return IconType.TARGET;
        if (m == moduleManager.tripleSwap) return IconType.COMBAT;
        if (m == moduleManager.autoTool) return IconType.SPARKLES;
        if (m == moduleManager.chestStealer) return IconType.POTION;
        if (m == moduleManager.invArranger) return IconType.SHIELD;
        if (m == moduleManager.maceHelper) return IconType.COMBAT;
        if (m == moduleManager.autoTpAccept) return IconType.USER;
        if (m == moduleManager.itemScroller) return IconType.SPARKLES;
        if (m == moduleManager.unHook) return IconType.SHIELD;
        return iconForCategory(m.getCategory());
    }

    private void openContextMenu(Module m, float clickX, float clickY) {
        contextModule = m;
        contextMenuW = 142f;
        contextMenuH = 4f * 24f + 10f;
        contextMenuX = Math.max(pX + 8f, Math.min(clickX, pX + pW - contextMenuW - 8f));
        contextMenuY = Math.max(pY + 8f, Math.min(clickY, pY + pH - contextMenuH - 8f));
        contextSpring.snap(0.25f);
        contextSpring.setTarget(1f);
        contextSpring.impulse(0.5f);
    }

    private int contextMenuItemAt(float mx, float my) {
        if (contextModule == null) return -1;
        float h = 24f;
        for (int i = 0; i < 4; i++) {
            float iy = contextMenuY + 5f + i * h;
            if (Render2D.isHovered(mx, my, contextMenuX, iy, contextMenuW, h)) return i;
        }
        return -1;
    }

    private void runContextAction(MinecraftClient mc, int idx) {
        if (contextModule == null) return;
        Module m = contextModule;
        contextModule = null;
        switch (idx) {
            case 0 -> {
                for (Module other : moduleManager.getModules()) other.expanded = false;
                m.expanded = true;
                targetScroll = 0f;
                currentScroll = 0f;
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
            }
            case 1 -> {
                listeningKeybind = m.getKeybind();
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
            }
            case 2 -> {
                m.resetSettings();
                soundEngine.playClick(mc, soundsEnabled(), soundVolume());
                configManager.saveClientConfig(moduleManager, hudElements);
            }
            case 3 -> {
                m.toggle();
                m.pressAnim = 1f;
                m.pressSpring.impulse(-0.16f);
                if (moduleManager.notifications.isEnabled()) {
                    ToastManager.get().pushModuleToggle(m, moduleManager.notifications);
                }
                soundEngine.playToggle(mc, m.isEnabled(), soundsEnabled(), soundVolume());
                configManager.saveClientConfig(moduleManager, hudElements);
            }
            default -> {
            }
        }
    }

    private void renderContextMenu(NanoVgEngine engine, AetherPalette palette, float dt) {
        if (contextModule == null) return;
        long vg = engine.handle();
        contextSpring.update(dt);
        float s = contextSpring.getPos();
        if (s < 0.05f) return;
        contextMenuW = 142f;
        contextMenuH = 4f * 24f + 10f;

        NanoVG.nvgSave(vg);
        float ox = contextMenuX;
        float oy = contextMenuY;
        NanoVG.nvgTranslate(vg, ox, oy);
        NanoVG.nvgScale(vg, s, s);
        NanoVG.nvgTranslate(vg, -ox, -oy);

        Render2D.drawShadow(vg, ox, oy, contextMenuW, contextMenuH, 10f, 18f, palette.darkMode ? 0x60000000 : 0x34000000);
        Render2D.drawRoundedRect(vg, ox, oy, contextMenuW, contextMenuH, 10f, AetherPalette.withAlpha(palette.surfaceContainerHigh, 250));
        Render2D.drawRoundedOutline(vg, ox, oy, contextMenuW, contextMenuH, 10f, 1f, AetherPalette.withAlpha(palette.outline, 150));

        String[] labels = {
                I18n.tr("Настройки", "Settings"),
                I18n.tr("Сменить клавишу", "Change Keybind"),
                I18n.tr("Сбросить", "Reset"),
                I18n.tr("Переключить", "Toggle")
        };
        for (int i = 0; i < labels.length; i++) {
            float iy = oy + 5f + i * 24f;
            boolean hov = Render2D.isHovered(mouseX, mouseY, ox, iy, contextMenuW, 24f);
            if (hov) {
                Render2D.drawRoundedRect(vg, ox + 3f, iy, contextMenuW - 6f, 24f, 6f, AetherPalette.withAlpha(palette.primary, 42));
            }
            int fg = hov ? palette.primary : palette.onSurface;
            Render2D.drawText(vg, labels[i], ox + 11f, iy + 12f, 9.8f, fg,
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
        }
        NanoVG.nvgRestore(vg);
    }

    private static float easeOutCubic(float t) {
        float inv = 1f - Math.max(0f, Math.min(1f, t));
        return 1f - inv * inv * inv;
    }

    private static String localizeModeValue(String val) {
        if (val == null) return "";
        return switch (val) {
            case "Ночное зрение" -> I18n.tr("Ночное зрение", "Night Vision");
            case "Гамма" -> I18n.tr("Гамма", "Gamma");
            case "Градиент" -> I18n.tr("Градиент", "Gradient");
            case "Акцент" -> I18n.tr("Акцент", "Accent");
            case "Статика" -> I18n.tr("Статика", "Static");
            case "Радуга" -> I18n.tr("Радуга", "Rainbow");
            case "Тема" -> I18n.tr("Тема", "Theme");
            case "Коробка" -> I18n.tr("Коробка", "Box");
            case "Углы" -> I18n.tr("Углы", "Corners");
            case "Свечение" -> I18n.tr("Свечение", "Glow");
            case "Скобка" -> I18n.tr("Скобка", "Bracket");
            case "Рамка" -> I18n.tr("Рамка", "Frame");
            case "Стекло" -> I18n.tr("Стекло", "Glass");
            case "Лента" -> I18n.tr("Лента", "Ribbon");
            case "Точки" -> I18n.tr("Точки", "Dots");
            case "Дождь" -> I18n.tr("Дождь", "Rain");
            case "Снег" -> I18n.tr("Снег", "Snow");
            case "Гроза" -> I18n.tr("Гроза", "Thunder");
            case "Чисто" -> I18n.tr("Чисто", "Clear");
            case "Шляпа" -> I18n.tr("Шляпа", "Hat");
            case "Нимб" -> I18n.tr("Нимб", "Halo");
            case "Крылья" -> I18n.tr("Крылья", "Wings");
            case "Тёплый" -> I18n.tr("Тёплый", "Warm");
            case "Холодный" -> I18n.tr("Холодный", "Cool");
            case "Сепия" -> I18n.tr("Сепия", "Sepia");
            case "Моно" -> I18n.tr("Моно", "Mono");
            case "Лук" -> I18n.tr("Лук", "Bow");
            case "Жемчуг" -> I18n.tr("Жемчуг", "Pearl");
            case "Снежки" -> I18n.tr("Снежки", "Snowballs");
            case "Все" -> I18n.tr("Все", "All");
            case "Правый верх" -> I18n.tr("Правый верх", "Top Right");
            case "Левый верх" -> I18n.tr("Левый верх", "Top Left");
            case "Правый низ" -> I18n.tr("Правый низ", "Bottom Right");
            case "Левый низ" -> I18n.tr("Левый низ", "Bottom Left");
            case "Правый" -> I18n.tr("Правый", "Right");
            case "Левый" -> I18n.tr("Левый", "Left");
            case "День" -> I18n.tr("День", "Day");
            case "Закат" -> I18n.tr("Закат", "Sunset");
            case "Ночь" -> I18n.tr("Ночь", "Night");
            case "Полночь" -> I18n.tr("Полночь", "Midnight");
            case "Призрак" -> I18n.tr("Призрак", "Ghost");
            case "Кольцо" -> I18n.tr("Кольцо", "Ring");
            case "Динамик" -> I18n.tr("Динамик", "Dynamic");
            case "Крест" -> I18n.tr("Крест", "Cross");
            case "Точка" -> I18n.tr("Точка", "Dot");
            case "Центр" -> I18n.tr("Центр", "Center");
            case "Низ" -> I18n.tr("Низ", "Bottom");
            case "Светлячки" -> I18n.tr("Светлячки", "Fireflies");
            case "Боке" -> I18n.tr("Боке", "Bokeh");
            case "Под ногами" -> I18n.tr("Под ногами", "At Feet");
            case "Над головой" -> I18n.tr("Над головой", "Above Head");
            case "Сбоку" -> I18n.tr("Сбоку", "Beside Target");
            case "Экран (HUD)" -> I18n.tr("Экран (HUD)", "Screen (HUD)");
            case "Корона" -> I18n.tr("Корона", "Crown");
            case "Орбитальные сферы" -> I18n.tr("Орбитальные сферы", "Orbiting Spheres");
            case "Аура пламени" -> I18n.tr("Аура пламени", "Flame Aura");
            case "Кошачьи ушки" -> I18n.tr("Кошачьи ушки", "Cat Ears");
            case "Неоновый ореол" -> I18n.tr("Неоновый ореол", "Neon Halo");
            case "Вспышка" -> I18n.tr("Вспышка", "Flash");
            case "Силуэт" -> I18n.tr("Силуэт", "Silhouette");
            case "1.7 Vanilla" -> I18n.tr("1.7 Vanilla", "1.7 Vanilla");
            case "Плавный" -> I18n.tr("Плавный", "Smooth");
            case "Размашистый" -> I18n.tr("Размашистый", "Wide Swipe");
            case "Вращение" -> I18n.tr("Вращение", "Spin");
            case "Компактный" -> I18n.tr("Компактный", "Compact");
            case "Только игроки" -> I18n.tr("Только игроки", "Players Only");
            case "Только цель" -> I18n.tr("Только цель", "Target Only");
            case "Только мобы" -> I18n.tr("Только мобы", "Mobs Only");
            case "В темноте" -> I18n.tr("В темноте", "In Darkness");
            case "Под землей" -> I18n.tr("Под землей", "Underground");
            case "Ангельские" -> I18n.tr("Ангельские", "Angelic");
            case "Демонические" -> I18n.tr("Демонические", "Demonic");
            case "Фея" -> I18n.tr("Фея", "Fairy");
            case "Искры" -> I18n.tr("Искры", "Sparks");
            case "Звездочки" -> I18n.tr("Звездочки", "Stars");
            case "Пузыри" -> I18n.tr("Пузыри", "Bubbles");
            case "Капли" -> I18n.tr("Капли", "Drops");
            case "Огонь" -> I18n.tr("Огонь", "Fire");
            case "Огонь/Кровь" -> I18n.tr("Огонь/Кровь", "Fire/Blood");
            case "Неон" -> I18n.tr("Неон", "Neon");
            case "Импульс" -> I18n.tr("Импульс", "Pulse");
            case "Сетка" -> I18n.tr("Сетка", "Grid");
            default -> val;
        };
    }

    private static float easeInOutQuad(float t) {
        float c = Math.max(0f, Math.min(1f, t));
        return c < 0.5f ? 2f * c * c : 1f - (float) Math.pow(-2f * c + 2f, 2) * 0.5f;
    }
}
