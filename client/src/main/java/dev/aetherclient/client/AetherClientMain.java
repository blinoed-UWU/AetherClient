package dev.aetherclient.client;

import dev.aetherclient.bridge.AetherBridge;
import dev.aetherclient.client.audio.SoundEngine;
import dev.aetherclient.client.config.ConfigManager;
import dev.aetherclient.client.gui.AetherMenuScreen;
import dev.aetherclient.client.gui.ClickGuiController;
import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.hud.impl.ArmorHud;
import dev.aetherclient.client.hud.impl.ArmorHudPro;
import dev.aetherclient.client.hud.impl.ArrayListHud;
import dev.aetherclient.client.hud.impl.CompassHud;
import dev.aetherclient.client.hud.impl.CoordsHud;
import dev.aetherclient.client.hud.impl.FpsPingGraphHud;
import dev.aetherclient.client.hud.impl.InventoryHud;
import dev.aetherclient.client.hud.impl.KeystrokesHud;
import dev.aetherclient.client.hud.impl.PotionsHud;
import dev.aetherclient.client.hud.impl.RadarHud;
import dev.aetherclient.client.hud.impl.SpeedometerHud;
import dev.aetherclient.client.hud.impl.StatsHud;
import dev.aetherclient.client.hud.impl.TargetHudElement;
import dev.aetherclient.client.hud.impl.WatermarkHud;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.ModuleManager;
import dev.aetherclient.client.notify.ToastManager;
import dev.aetherclient.client.render.GlStateGuard;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.render.VisualRenderer;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.util.Window;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.List;

/**
 * Runtime entry point called by AetherAgentMain inside Minecraft's KnotClassLoader hierarchy.
 */
public final class AetherClientMain {
    public static final long CLIENT_START_TIME = System.currentTimeMillis();
    private static volatile AetherClientMain instance;

    private final ConfigManager configManager;
    private final ModuleManager moduleManager;
    private final AetherPalette palette;
    private final NanoVgEngine nanoVg;
    private final GlStateGuard glGuard;
    private final SoundEngine soundEngine;
    private final VisualRenderer visualRenderer;

    private final KeystrokesHud keystrokesHud;
    private final TargetHudElement targetHud;
    private final List<HudElement> hudElements;
    private final ClickGuiController clickGuiController;
    private final dev.aetherclient.client.gui.TripleSwapOverlay tripleSwapOverlay;

    private long lastFrameNs = 0L;
    private long lastThemePollMs = 0L;
    private double rawMouseX = 0.0;
    private double rawMouseY = 0.0;
    private boolean hasCursorPos = false;
    private volatile boolean unloadRequested = false;
    private volatile boolean unloaded = false;
    private boolean loggedBlurError = false;
    private boolean loggedRenderError = false;

    public static void requestUnload() {
        if (instance != null) {
            instance.unloadRequested = true;
        }
    }

    private AetherClientMain() {
        this.configManager = new ConfigManager();
        this.moduleManager = new ModuleManager();
        this.nanoVg = new NanoVgEngine();
        this.glGuard = new GlStateGuard();
        this.soundEngine = new SoundEngine();
        this.visualRenderer = new VisualRenderer();
        this.tripleSwapOverlay = new dev.aetherclient.client.gui.TripleSwapOverlay(moduleManager.tripleSwap);

        WatermarkHud watermark = new WatermarkHud(moduleManager.hud);
        CoordsHud coords = new CoordsHud(moduleManager.hud);
        this.keystrokesHud = new KeystrokesHud(moduleManager.hud);
        ArmorHud armor = new ArmorHud(moduleManager.hud);
        ArrayListHud arrayList = new ArrayListHud(moduleManager, moduleManager.hud);
        this.targetHud = new TargetHudElement(moduleManager.targetHud, moduleManager.hud);
        this.targetHud.setWorldToScreen(this.visualRenderer.getWorldToScreen());
        PotionsHud potions = new PotionsHud(moduleManager.hud);
        RadarHud radar = new RadarHud(moduleManager.hud);
        StatsHud stats = new StatsHud(moduleManager.hud, moduleManager.nameProtect);
        CompassHud compass = new CompassHud(moduleManager.hud);
        SpeedometerHud speedometer = new SpeedometerHud(moduleManager.hud);
        FpsPingGraphHud fpsPing = new FpsPingGraphHud(moduleManager.hud);
        InventoryHud inventory = new InventoryHud(moduleManager.hud);
        ArmorHudPro armorPro = new ArmorHudPro(moduleManager.hud);
        this.hudElements = List.of(
                watermark, coords, keystrokesHud, armor, arrayList, targetHud,
                potions, radar, stats, compass, speedometer, fpsPing, inventory, armorPro
        );

        configManager.loadClientConfig(moduleManager, hudElements);
        if (moduleManager.clickGui.syncTheme.isEnabled() && configManager.getSyncedLanguage() != null) {
            moduleManager.clickGui.language.setValue(configManager.getSyncedLanguage());
        }
        dev.aetherclient.client.i18n.I18n.setLang(moduleManager.clickGui.language.getValue());
        this.palette = new AetherPalette(resolveActiveSeed(), resolveActiveDarkMode());
        new dev.aetherclient.client.friend.FriendsManager(configManager.getRootDir());
        this.clickGuiController = new ClickGuiController(
                moduleManager, configManager, hudElements, soundEngine, () -> unloadRequested = true
        );
    }

    public static void bootstrap(String agentArgs) {
        if (AetherBridge.isInitialized()) {
            return;
        }
        AetherBridge.markInitialized();
        AetherClientMain app = new AetherClientMain();
        instance = app;

        AetherBridge.registerHooks(
                app::onFrameRender,
                app::onHudRender,
                app::onClientTick,
                app::onKey,
                app::onMouseButton,
                app::onCursorPos,
                app::onMouseScroll,
                app::onChar
        );
        AetherBridge.registerSwingHooks(
                app::onApplySwingOffset,
                app::onApplyEquipOffset
        );
    }

    private int resolveActiveSeed() {
        return moduleManager.clickGui.syncTheme.isEnabled()
                ? configManager.getSyncedSeedArgb()
                : moduleManager.clickGui.accentColor.getValue();
    }

    private boolean resolveActiveDarkMode() {
        return moduleManager.clickGui.syncTheme.isEnabled()
                ? configManager.isSyncedDarkMode()
                : moduleManager.clickGui.darkMode.isEnabled();
    }

    private float resolveUiScale(Window win) {
        int fbH = Math.max(1, win.getFramebufferHeight());
        float autoBase = Math.max(1.18f, Math.min(2.15f, (fbH / 1080f) * 1.48f));
        return Math.max(0.75f, autoBase * moduleManager.clickGui.guiScale.asFloat());
    }

    private float resolveHudScale(Window win) {
        int fbH = Math.max(1, win.getFramebufferHeight());
        float autoBase = Math.max(1.12f, Math.min(1.95f, (fbH / 1080f) * 1.38f));
        return Math.max(0.70f, autoBase * moduleManager.hud.hudScale.asFloat());
    }

    private void syncCursorProjection(Window win) {
        if (!hasCursorPos) return;
        int fbW = Math.max(1, win.getFramebufferWidth());
        int fbH = Math.max(1, win.getFramebufferHeight());
        float uiScale = resolveUiScale(win);
        float hudScale = resolveHudScale(win);
        double uiW = fbW / (double) uiScale;
        double uiH = fbH / (double) uiScale;
        double hudW = fbW / (double) hudScale;
        double hudH = fbH / (double) hudScale;
        double winW = Math.max(1.0, win.getWidth());
        double winH = Math.max(1.0, win.getHeight());
        clickGuiController.onCursorMove(
                rawMouseX * uiW / winW,
                rawMouseY * uiH / winH,
                rawMouseX * hudW / winW,
                rawMouseY * hudH / winH,
                (float) hudW,
                (float) hudH
        );
    }

    private void onClientTick(Object mcObj) {
        if (unloadRequested) return;
        MinecraftClient mc = mcObj instanceof MinecraftClient m ? m : MinecraftClient.getInstance();
        if (mc == null) return;

        AetherBridge.setHideScoreboard(moduleManager.scoreboard.isEnabled());
        AetherBridge.setHideTabList(moduleManager.tabList.isEnabled());
        AetherBridge.setHideCrosshair(moduleManager.crosshair.isEnabled());

        for (Module module : moduleManager.getModules()) {
            if (module.isEnabled()) {
                module.onTick(mc);
            }
        }
        moduleManager.autoTool.onTick();
        moduleManager.chestStealer.onTick();
    }

    private void onHudRender(Object drawContext, Object tickCounter) {
        // Primary rendering happens cleanly at Window.swapBuffers via NanoVG
    }

    private void onFrameRender(Object tickCounter, boolean tick) {
        if (unloaded) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        if (unloadRequested) {
            performUnload(mc);
            return;
        }

        Window win = mc.getWindow();
        int fbW = win.getFramebufferWidth();
        int fbH = win.getFramebufferHeight();
        if (fbW <= 0 || fbH <= 0) return;

        long nowNs = System.nanoTime();
        float dt = lastFrameNs == 0L ? (1f / 60f) : Math.max(0.001f, Math.min(0.1f, (nowNs - lastFrameNs) / 1_000_000_000f));
        lastFrameNs = nowNs;

        long nowMs = System.currentTimeMillis();
        if (nowMs - lastThemePollMs > 750L) {
            lastThemePollMs = nowMs;
            if (configManager.pollLoaderThemeIfChanged() && moduleManager.clickGui.syncTheme.isEnabled()) {
                String syncedLang = configManager.getSyncedLanguage();
                if (syncedLang != null && !syncedLang.isEmpty()) {
                    moduleManager.clickGui.language.setValue(syncedLang);
                }
            }
        }
        dev.aetherclient.client.i18n.I18n.setLang(moduleManager.clickGui.language.getValue());
        int targetSeed = resolveActiveSeed();
        boolean targetDark = resolveActiveDarkMode();
        if (targetSeed != palette.seedArgb || targetDark != palette.darkMode) {
            palette.update(targetSeed, targetDark);
        }

        boolean inWorld = mc.world != null && mc.player != null;
        boolean isChat = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        boolean isGameMenu = mc.currentScreen instanceof net.minecraft.client.gui.screen.GameMenuScreen;
        boolean canDrawOverlay = mc.currentScreen == null || mc.currentScreen instanceof AetherMenuScreen || isChat || isGameMenu;
        boolean showGui = clickGuiController.isVisibleOrAnimating();
        boolean showHud = inWorld && (moduleManager.hud.isEnabled() || moduleManager.targetHud.isEnabled()) && canDrawOverlay;
        boolean showVisuals = inWorld && canDrawOverlay && hasActiveOverlayVisuals();

        if (!showGui && !showHud && !showVisuals) return;

        float uiScale = resolveUiScale(win);
        float screenW = fbW / uiScale;
        float screenH = fbH / uiScale;

        syncCursorProjection(win);

        glGuard.save();
        try {
            glGuard.prepareForNanoVg();
            nanoVg.ensureInit();

            boolean needBlur = (showGui && moduleManager.clickGui.blurRadius.asFloat() > 0.2f && clickGuiController.getBlurFactor() > 0.02f)
                    || (showHud && moduleManager.hud.blurHud.isEnabled())
                    || (inWorld && canDrawOverlay && moduleManager.motionBlur.isEnabled());
            if (needBlur) {
                try {
                    float radius = showGui
                            ? moduleManager.clickGui.blurRadius.asFloat() * clickGuiController.getBlurFactor()
                            : (moduleManager.motionBlur.isEnabled()
                                    ? Math.max(3f, moduleManager.motionBlur.strength.asFloat() * 7f)
                                    : Math.max(4f, moduleManager.clickGui.blurRadius.asFloat() * 0.75f));
                    if (radius > 0.2f) {
                        int sourceFbo = GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
                        nanoVg.updateBlurTexture(sourceFbo, fbW, fbH, radius);
                    } else {
                        nanoVg.clearBlurTexture();
                    }
                } catch (Throwable blurErr) {
                    if (!loggedBlurError) {
                        loggedBlurError = true;
                        blurErr.printStackTrace(System.err);
                    }
                }
            } else {
                nanoVg.clearBlurTexture();
            }

            if (moduleManager.customSky.isEnabled()) {
                AetherBridge.setCustomSkyColor(moduleManager.customSky.getTopColor((float) (nowNs / 1_000_000_000.0)));
            } else {
                AetherBridge.setCustomSkyColor(0);
            }

            dev.aetherclient.client.render.HitColorApplier.update(mc, moduleManager.hitColor, palette, (float) (nowNs / 1_000_000_000.0));

            glGuard.prepareForNanoVg();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL11.glViewport(0, 0, fbW, fbH);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            boolean editMode = clickGuiController.isHudEditMode() || isChat;
            if (showHud || editMode || showVisuals) {
                float hudScale = resolveHudScale(win);
                float hudScreenW = fbW / hudScale;
                float hudScreenH = fbH / hudScale;
                nanoVg.beginFrame(hudScreenW, hudScreenH, hudScale);
                if (isGameMenu) {
                    Render2D.drawRoundedRect(nanoVg.handle(), 0f, 0f, hudScreenW, hudScreenH, 0f, 0x44000000);
                }
                if (inWorld && canDrawOverlay) {
                    visualRenderer.renderWorldVisuals(mc, moduleManager, nanoVg, palette, hudScreenW, hudScreenH, dt);
                }
                if (showHud || editMode) {
                    float pauseDim = isGameMenu ? 0.35f : 1.0f;
                    for (HudElement el : hudElements) {
                        boolean active = el.isActive();
                        float anim = el.updateEnterAnim(dt, active || editMode);
                        if (anim <= 0.002f) continue;
                        el.prepareSize(mc, nanoVg);
                        float ex = el.getPixelX(hudScreenW);
                        float ey = el.getPixelY(hudScreenH);
                        float es = el.getElementScale();
                        // Spring-driven enter/exit: fade + slide-up + soft overshoot scale
                        float eased = HudElement.enterEase(anim);
                        float popAlpha = Math.max(0f, Math.min(1f, eased * 1.15f)) * pauseDim;
                        float popSlide = (1f - Math.max(0f, Math.min(1f, eased))) * 12f;
                        float popScale = 0.94f + 0.06f * eased;
                        org.lwjgl.nanovg.NanoVG.nvgSave(nanoVg.handle());
                        org.lwjgl.nanovg.NanoVG.nvgGlobalAlpha(nanoVg.handle(), popAlpha);
                        if (Math.abs(es - 1f) > 0.005f || Math.abs(popScale - 1f) > 0.0005f || popSlide > 0.05f) {
                            long vgh = nanoVg.handle();
                            float totalScale = es * popScale;
                            float ecx = ex + el.getScaledWidth() * 0.5f;
                            float ecy = ey + el.getScaledHeight() * 0.5f;
                            org.lwjgl.nanovg.NanoVG.nvgTranslate(vgh, ecx, ecy + popSlide);
                            org.lwjgl.nanovg.NanoVG.nvgScale(vgh, totalScale, totalScale);
                            org.lwjgl.nanovg.NanoVG.nvgTranslate(vgh, -ecx, -ecy);
                        }
                        el.render(mc, nanoVg, palette, ex, ey, hudScreenW, hudScreenH, editMode, dt);
                        org.lwjgl.nanovg.NanoVG.nvgRestore(nanoVg.handle());
                    }
                    if (editMode) {
                        clickGuiController.renderHudEditorOutlines(nanoVg, palette, hudScreenW, hudScreenH);
                    }
                    if (moduleManager.aspectRatio.isEnabled() && !moduleManager.aspectRatio.hudOnTop.isEnabled()) {
                        visualRenderer.renderAspectRatioBars(moduleManager, nanoVg.handle(), palette, hudScreenW, hudScreenH, dt);
                    }
                }
                nanoVg.endFrame();
            }
            if (showGui) {
                nanoVg.beginFrame(screenW, screenH, uiScale);
                clickGuiController.render(nanoVg, palette, screenW, screenH, dt);
                nanoVg.endFrame();
            }
            if (moduleManager.tripleSwap.getMenuProgress() > 0.01f) {
                nanoVg.beginFrame(screenW, screenH, uiScale);
                tripleSwapOverlay.render(nanoVg.handle(), screenW, screenH, palette, dt);
                nanoVg.endFrame();
            }
        } catch (Throwable err) {
            if (!loggedRenderError) {
                loggedRenderError = true;
                err.printStackTrace(System.err);
            }
            if (err instanceof LinkageError) {
                unloadRequested = true;
                performUnload(mc);
            }
        } finally {
            glGuard.restore();
        }
    }

    private boolean onKey(long window, int action, Object keyInputObj) {
        if (unloadRequested || unloaded) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || !(keyInputObj instanceof KeyInput ki)) return false;

        int key = ki.key();
        int panic = (int) Math.round(moduleManager.clickGui.panicKey.getValue());
        if (action == GLFW.GLFW_PRESS) {
            int mods = ki.modifiers();
            if ((key == GLFW.GLFW_KEY_DELETE && (mods & GLFW.GLFW_MOD_CONTROL) != 0 && (mods & GLFW.GLFW_MOD_SHIFT) != 0)
                    || (panic > 0 && key == panic)) {
                unloadRequested = true;
                performUnload(mc);
                return true;
            }
        }

        if (clickGuiController.onKey(mc, key, ki.scancode(), ki.modifiers(), action)) {
            return true;
        }

        if (moduleManager.tripleSwap.onKey(key, ki.scancode(), action, ki.modifiers())) {
            return true;
        }

        if (moduleManager.invArranger.onKey(key, action)) {
            return true;
        }

        if (action == GLFW.GLFW_PRESS && mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen chatScreen) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (handleChatCommand(mc, chatScreen)) {
                    return true;
                }
            }
        }

        if (action == GLFW.GLFW_PRESS && mc.currentScreen == null && key != GLFW.GLFW_KEY_UNKNOWN) {
            for (Module m : moduleManager.getModules()) {
                if (m == moduleManager.clickGui) continue;
                if (m.getKeybind().getValue() == key) {
                    m.toggle();
                    soundEngine.playToggle(
                            mc,
                            m.isEnabled(),
                            moduleManager.clickGui.uiSounds.isEnabled(),
                            moduleManager.clickGui.soundVolume.asFloat()
                    );
                    if (moduleManager.notifications.isEnabled()) {
                        ToastManager.get().pushModuleToggle(m, moduleManager.notifications);
                    }
                    configManager.saveClientConfig(moduleManager, hudElements);
                }
            }
            if (moduleManager.clickPearl.key.getValue() == key) {
                moduleManager.clickPearl.triggerThrow(mc);
                return true;
            }
            if (moduleManager.fastSwap.key.getValue() == key) {
                moduleManager.fastSwap.triggerSwap(mc);
                return true;
            }
        }
        return false;
    }

    private boolean onChar(long window, Object charInputObj) {
        if (unloadRequested || unloaded) return false;
        if (charInputObj instanceof CharInput ci && ci.isValidChar()) {
            return clickGuiController.onChar(ci.codepoint(), ci.asString());
        }
        return false;
    }

    private boolean onMouseButton(long window, Object mouseInputObj, int action) {
        if (unloadRequested || unloaded) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null || !(mouseInputObj instanceof MouseInput mi)) return false;

        int button = mi.button();
        if (action == GLFW.GLFW_PRESS && mc.currentScreen == null && !clickGuiController.isOpen()) {
            keystrokesHud.recordMouseClick(button);
            int mouseKeycode = -100 - button;
            for (Module m : moduleManager.getModules()) {
                if (m == moduleManager.clickGui) continue;
                if (m.getKeybind().getValue() == mouseKeycode) {
                    m.toggle();
                    soundEngine.playToggle(
                            mc,
                            m.isEnabled(),
                            moduleManager.clickGui.uiSounds.isEnabled(),
                            moduleManager.clickGui.soundVolume.asFloat()
                    );
                    if (moduleManager.notifications.isEnabled()) {
                        ToastManager.get().pushModuleToggle(m, moduleManager.notifications);
                    }
                    configManager.saveClientConfig(moduleManager, hudElements);
                }
            }
            if (moduleManager.clickPearl.key.getValue() == mouseKeycode) {
                moduleManager.clickPearl.triggerThrow(mc);
            }
            if (moduleManager.fastSwap.key.getValue() == mouseKeycode) {
                moduleManager.fastSwap.triggerSwap(mc);
            }
            if (button == 0 && mc.crosshairTarget instanceof EntityHitResult ehr) {
                targetHud.onAttackEntity(ehr.getEntity());
                visualRenderer.onAttackEntity(ehr.getEntity());
                if (moduleManager.hitSound.isEnabled()) {
                    soundEngine.playHitSound(mc, moduleManager.hitSound.sound.getValue(),
                            moduleManager.hitSound.volume.asFloat(), moduleManager.hitSound.pitch.asFloat());
                }
            }
            if (button == 2) {
                if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof net.minecraft.entity.player.PlayerEntity pe) {
                    String name = pe.getName().getString();
                    boolean isFriend = dev.aetherclient.client.friend.FriendsManager.get().isFriend(name);
                    if (isFriend) {
                        dev.aetherclient.client.friend.FriendsManager.get().removeFriend(name);
                        ToastManager.get().push("Друзья", name + " удален из друзей", 0xFFF43F5E);
                    } else {
                        dev.aetherclient.client.friend.FriendsManager.get().addFriend(name);
                        ToastManager.get().push("Друзья", name + " добавлен в друзья", 0xFF10B981);
                    }
                    soundEngine.playClick(mc, true, 1.0f);
                } else if (moduleManager.clickPearl.middleClick.isEnabled()) {
                    moduleManager.clickPearl.triggerThrow(mc);
                }
            }
        }

        boolean inChat = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        if (inChat) {
            Window win = mc.getWindow();
            syncCursorProjection(win);
            float hudScale = resolveHudScale(win);
            float hudW = Math.max(1, win.getFramebufferWidth()) / hudScale;
            float hudH = Math.max(1, win.getFramebufferHeight()) / hudScale;
            if (clickGuiController.onChatHudMouseButton(mc, button, action, hudW, hudH)) {
                return true;
            }
        }

        if (clickGuiController.isOpen()) {
            Window win = mc.getWindow();
            syncCursorProjection(win);
            float uiScale = resolveUiScale(win);
            float hudScale = resolveHudScale(win);
            float uiW = Math.max(1, win.getFramebufferWidth()) / uiScale;
            float uiH = Math.max(1, win.getFramebufferHeight()) / uiScale;
            float hudW = Math.max(1, win.getFramebufferWidth()) / hudScale;
            float hudH = Math.max(1, win.getFramebufferHeight()) / hudScale;
            return clickGuiController.onMouseButton(mc, button, action, uiW, uiH, hudW, hudH);
        }
        return false;
    }

    private boolean hasActiveOverlayVisuals() {
        return moduleManager.esp.isEnabled()
                || moduleManager.tracers.isEnabled()
                || moduleManager.targetEsp.isEnabled()
                || moduleManager.arrows.isEnabled()
                || moduleManager.jumpCircles.isEnabled()
                || moduleManager.trails.isEnabled()
                || moduleManager.hitParticles.isEnabled()
                || moduleManager.crosshair.isEnabled()
                || moduleManager.ambience.isEnabled()
                || moduleManager.particles.isEnabled()
                || moduleManager.damageVignette.isEnabled()
                || moduleManager.chams.isEnabled()
                || moduleManager.motionBlur.isEnabled()
                || moduleManager.breadcrumbs.isEnabled()
                || moduleManager.projectiles.isEnabled()
                || moduleManager.itemEsp.isEnabled()
                || moduleManager.blockOverlay.isEnabled()
                || moduleManager.holeEsp.isEnabled()
                || moduleManager.hitColor.isEnabled()
                || moduleManager.timeChanger.isEnabled()
                || moduleManager.scoreboard.isEnabled()
                || moduleManager.tabList.isEnabled()
                || moduleManager.cosmetics.isEnabled()
                || moduleManager.colorFilter.isEnabled()
                || moduleManager.notifications.isEnabled()
                || moduleManager.aspectRatio.isEnabled()
                || moduleManager.chinaHat.isEnabled()
                || moduleManager.pearlPredict.isEnabled()
                || moduleManager.tripleSwap.isMenuOpen()
                || moduleManager.itemRadius.isEnabled();
    }


    private void onCursorPos(long window, double rawX, double rawY) {
        if (unloadRequested || unloaded) return;
        this.rawMouseX = rawX;
        this.rawMouseY = rawY;
        this.hasCursorPos = true;
        moduleManager.tripleSwap.onCursorPos(rawX, rawY);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;
        syncCursorProjection(mc.getWindow());
        if (moduleManager.freecam.isFreecamActive() && mc.player != null) {
            moduleManager.freecam.syncCameraRotation(mc.player.getYaw(), mc.player.getPitch());
        }
    }

    private boolean handleChatCommand(MinecraftClient mc, net.minecraft.client.gui.screen.ChatScreen chatScreen) {
        try {
            java.lang.reflect.Field field = null;
            for (java.lang.reflect.Field f : net.minecraft.client.gui.screen.ChatScreen.class.getDeclaredFields()) {
                if (net.minecraft.client.gui.widget.TextFieldWidget.class.isAssignableFrom(f.getType())) {
                    field = f;
                    break;
                }
            }
            if (field == null) return false;
            field.setAccessible(true);
            net.minecraft.client.gui.widget.TextFieldWidget tf = (net.minecraft.client.gui.widget.TextFieldWidget) field.get(chatScreen);
            if (tf == null) return false;
            String text = tf.getText();
            if (text == null) return false;
            text = text.trim();
            if (text.startsWith(".friend") || text.startsWith(".f ")) {
                String[] parts = text.split("\\s+");
                if (parts.length == 1 || (parts.length == 2 && parts[1].equalsIgnoreCase("list"))) {
                    java.util.Set<String> friends = dev.aetherclient.client.friend.FriendsManager.get().getFriends();
                    if (friends.isEmpty()) {
                        mc.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("§7[§bAether§7] Список друзей пуст."));
                    } else {
                        mc.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("§7[§bAether§7] Друзья: §a" + String.join(", ", friends)));
                    }
                } else if (parts.length >= 2 && parts[1].equalsIgnoreCase("clear")) {
                    dev.aetherclient.client.friend.FriendsManager.get().clear();
                    mc.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("§7[§bAether§7] Список друзей очищен."));
                    ToastManager.get().push("Друзья", "Список друзей очищен", 0xFFF43F5E);
                } else if (parts.length >= 3 && (parts[1].equalsIgnoreCase("add") || parts[1].equalsIgnoreCase("+"))) {
                    String name = parts[2];
                    boolean added = dev.aetherclient.client.friend.FriendsManager.get().addFriend(name);
                    if (added) {
                        mc.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("§7[§bAether§7] §a" + name + "§7 добавлен в друзья!"));
                        ToastManager.get().push("Друзья", name + " добавлен в друзья", 0xFF10B981);
                    } else {
                        mc.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("§7[§bAether§7] §e" + name + "§7 уже есть в списке друзей!"));
                    }
                } else if (parts.length >= 3 && (parts[1].equalsIgnoreCase("remove") || parts[1].equalsIgnoreCase("del") || parts[1].equalsIgnoreCase("-"))) {
                    String name = parts[2];
                    boolean removed = dev.aetherclient.client.friend.FriendsManager.get().removeFriend(name);
                    if (removed) {
                        mc.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("§7[§bAether§7] §c" + name + "§7 удален из друзей!"));
                        ToastManager.get().push("Друзья", name + " удален из друзей", 0xFFF43F5E);
                    } else {
                        mc.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("§7[§bAether§7] §e" + name + "§7 не найден в списке друзей!"));
                    }
                } else {
                    mc.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("§7[§bAether§7] Использование: §f.friend add <ник> §7| §f.friend remove <ник> §7| §f.friend list §7| §f.friend clear"));
                }
                mc.setScreen(null);
                soundEngine.playClick(mc, true, 1.0f);
                return true;
            }
        } catch (Throwable t) {
            System.err.println("[AetherClient] Error handling chat command: " + t.getMessage());
        }
        return false;
    }

    private boolean onMouseScroll(long window, double horizontal, double vertical) {
        if (unloadRequested || unloaded) return false;
        if (clickGuiController.isOpen()) {
            return clickGuiController.onScroll(vertical);
        }
        return false;
    }

    private void performUnload(MinecraftClient mc) {
        if (unloaded) return;
        unloaded = true;
        try {
            try {
                configManager.saveClientConfig(moduleManager, hudElements);
            } catch (Throwable ignored) {}
            try {
                for (Module m : moduleManager.getModules()) {
                    if (m.isEnabled()) {
                        m.setEnabled(false);
                    }
                }
            } catch (Throwable ignored) {}
            try {
                visualRenderer.reset();
            } catch (Throwable ignored) {}
            try {
                clickGuiController.forceCloseImmediate(mc);
            } catch (Throwable ignored) {}
            try {
                dev.aetherclient.client.render.HitColorApplier.restore(mc);
            } catch (Throwable ignored) {}
            try {
                soundEngine.destroy();
            } catch (Throwable ignored) {}
            try {
                glGuard.save();
                try {
                    glGuard.prepareForNanoVg();
                    nanoVg.destroy();
                } finally {
                    glGuard.restore();
                }
            } catch (Throwable ignored) {}
        } finally {
            AetherBridge.clearHooks();
            try {
                System.clearProperty("aether.client.active");
            } catch (Throwable ignored) {}
            instance = null;
        }
    }

    public boolean onApplySwingOffset(Object matrixStackObj, Object armObj, float swingProgress) {
        if (!moduleManager.swingAnimation.isEnabled()) return false;
        if (!(matrixStackObj instanceof net.minecraft.client.util.math.MatrixStack matrices)) return false;
        if (!(armObj instanceof net.minecraft.util.Arm arm)) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return false;
        if (moduleManager.swingAnimation.ignoreMining.isEnabled() && mc.interactionManager != null && mc.interactionManager.isBreakingBlock()) {
            return false;
        }

        int side = arm == net.minecraft.util.Arm.RIGHT ? 1 : -1;
        float progress = Math.max(0f, Math.min(1f, swingProgress * moduleManager.swingAnimation.swingSpeed.asFloat()));
        float sin1 = net.minecraft.util.math.MathHelper.sin(progress * progress * (float) Math.PI);
        float sin2 = net.minecraft.util.math.MathHelper.sin(net.minecraft.util.math.MathHelper.sqrt(progress) * (float) Math.PI);
        float pitchAngle = moduleManager.swingAnimation.angle.asFloat();

        float strength = moduleManager.swingAnimation.strength.asFloat();
        String style = moduleManager.swingAnimation.mode.getValue();
        float sinSmooth = (float) (Math.sin(progress * Math.PI) * 0.5f);

        switch (style) {
            case "Chop" -> {
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * -20.0F * side * strength));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength + pitchAngle));
                matrices.translate(0.4F * side, 0.2F, 0.2F);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(20.0F * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            }
            case "Twist" -> {
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(80.0F * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -90.0F * strength + pitchAngle));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees((sin1 - sin2) * 60.0F * side * strength));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-30.0F));
            }
            case "Swipe" -> {
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(70.0F * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5.0F * strength * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -120.0F * strength + pitchAngle));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
            }
            case "Down" -> {
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(76.0F * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0F * strength * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0F * strength + pitchAngle));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
            }
            case "Smooth" -> {
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float) side * (45.0F + sin1 * -20.0F * strength)));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees((float) side * sin2 * -20.0F * strength));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength + pitchAngle));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float) side * -45.0F));
                matrices.translate(0, -0.1f * sin2, 0);
            }
            case "Smooth 2" -> {
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength + pitchAngle));
                matrices.translate(0, -0.1f * sin2, 0);
            }
            case "Power" -> {
                matrices.translate((-sinSmooth * sinSmooth * sin1) * side * strength, 0, 0);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(61.0F * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * strength * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5.0F * strength * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -30.0F * strength + pitchAngle));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60.0F * strength));
            }
            case "Feast" -> {
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75.0F * side * strength));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45.0F * strength + pitchAngle));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * side));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            }
            case "Вращение" -> {
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float) side * (45.0F + sin2 * 80.0F * strength)));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees((float) side * (sin1 - sin2) * 50.0F * strength));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -70.0F * strength + pitchAngle));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float) side * -45.0F));
            }
            case "Компактный" -> {
                matrices.scale(0.85f, 0.85f, 0.85f);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float) side * (35.0F + sin2 * -15.0F * strength)));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees((float) side * sin2 * -12.0F * strength));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -50.0F * strength + pitchAngle));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float) side * -35.0F));
            }
            case "1.7 Vanilla" -> {
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float) side * (45.0F + sin1 * -20.0F * strength)));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees((float) side * sin2 * -20.0F * strength));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength + pitchAngle));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees((float) side * -45.0F));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    public boolean onApplyEquipOffset(Object matrixStackObj, Object armObj, float equipProgress) {
        if (!moduleManager.viewmodel.isEnabled()) return false;
        if (!(matrixStackObj instanceof net.minecraft.client.util.math.MatrixStack matrices)) return false;
        if (!(armObj instanceof net.minecraft.util.Arm arm)) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            if (moduleManager.viewmodel.mainHandOnly.isEnabled() && arm != mc.player.getMainArm()) {
                return false;
            }
        }

        int side = arm == net.minecraft.util.Arm.RIGHT ? 1 : -1;
        float offX = moduleManager.viewmodel.posX.asFloat() * (float) side;
        float offY = moduleManager.viewmodel.posY.asFloat();
        float offZ = moduleManager.viewmodel.posZ.asFloat();
        matrices.translate((float) side * 0.56F + offX, -0.52F + equipProgress * -0.6F + offY, -0.72F + offZ);

        float sc = moduleManager.viewmodel.scale.asFloat();
        if (sc != 1.0f && sc > 0.01f) {
            matrices.scale(sc, sc, sc);
        }

        float pitch = moduleManager.viewmodel.rotPitch.asFloat();
        float yaw = moduleManager.viewmodel.rotYaw.asFloat() * (float) side;
        float roll = moduleManager.viewmodel.rotRoll.asFloat() * (float) side;
        if (pitch != 0f) matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        if (yaw != 0f) matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        if (roll != 0f) matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(roll));

        return true;
    }

    public static AetherClientMain getInstance() {
        return instance;
    }
}
