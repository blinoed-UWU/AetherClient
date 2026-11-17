package dev.aetherclient.bridge;

/**
 * Bootstrap-classloader bridge between transformed Minecraft 1.21.11 classes (KnotClassLoader)
 * and the injected AetherClient runtime. Kept strictly dependency-free.
 */
public final class AetherBridge {
    public interface FrameRenderHook {
        void onFrameRender(Object renderTickCounter, boolean tick);
    }

    public interface HudRenderHook {
        void onHudRender(Object drawContext, Object renderTickCounter);
    }

    public interface ClientTickHook {
        void onClientTick(Object minecraftClient);
    }

    public interface KeyHook {
        boolean onKey(long window, int action, Object keyInput);
    }

    public interface MouseButtonHook {
        boolean onMouseButton(long window, Object mouseInput, int action);
    }

    public interface CursorPosHook {
        void onCursorPos(long window, double x, double y);
    }

    public interface MouseScrollHook {
        boolean onMouseScroll(long window, double horizontal, double vertical);
    }

    public interface CharHook {
        boolean onChar(long window, Object charInput);
    }

    public interface SwingOffsetHook {
        boolean onApplySwingOffset(Object matrixStack, Object arm, float swingProgress);
    }

    public interface EquipOffsetHook {
        boolean onApplyEquipOffset(Object matrixStack, Object arm, float equipProgress);
    }

    private static volatile boolean initialized = false;
    private static volatile boolean hooksTransformed = false;
    private static volatile boolean hideScoreboard = false;
    private static volatile boolean hideTabList = false;
    private static volatile boolean hideCrosshair = false;
    private static volatile float itemSpinAngle = 0f;
    private static volatile int customSkyColor = 0;

    private static volatile FrameRenderHook frameRenderHook;
    private static volatile HudRenderHook hudRenderHook;
    private static volatile ClientTickHook clientTickHook;
    private static volatile KeyHook keyHook;
    private static volatile MouseButtonHook mouseButtonHook;
    private static volatile CursorPosHook cursorPosHook;
    private static volatile MouseScrollHook mouseScrollHook;
    private static volatile CharHook charHook;
    private static volatile SwingOffsetHook swingOffsetHook;
    private static volatile EquipOffsetHook equipOffsetHook;
    private static volatile Runnable unloadHandler;

    private AetherBridge() {}

    public static boolean isInitialized() {
        return initialized;
    }

    public static synchronized void markInitialized() {
        initialized = true;
        System.setProperty("aether.client.active", "true");
    }

    public static boolean areHooksTransformed() {
        return hooksTransformed;
    }

    public static synchronized void markHooksTransformed() {
        hooksTransformed = true;
    }

    public static synchronized void markHooksTransformedReset() {
        hooksTransformed = false;
    }

    public static synchronized void setUnloadHandler(Runnable handler) {
        unloadHandler = handler;
    }

    public static synchronized void registerHooks(
            FrameRenderHook frameRender,
            HudRenderHook hudRender,
            ClientTickHook tick,
            KeyHook key,
            MouseButtonHook mouseBtn,
            CursorPosHook cursor,
            MouseScrollHook scroll
    ) {
        registerHooks(frameRender, hudRender, tick, key, mouseBtn, cursor, scroll, null);
    }

    public static synchronized void registerHooks(
            FrameRenderHook frameRender,
            HudRenderHook hudRender,
            ClientTickHook tick,
            KeyHook key,
            MouseButtonHook mouseBtn,
            CursorPosHook cursor,
            MouseScrollHook scroll,
            CharHook charCb
    ) {
        frameRenderHook = frameRender;
        hudRenderHook = hudRender;
        clientTickHook = tick;
        keyHook = key;
        mouseButtonHook = mouseBtn;
        cursorPosHook = cursor;
        mouseScrollHook = scroll;
        charHook = charCb;
    }

    public static synchronized void setCharHook(CharHook charCb) {
        charHook = charCb;
    }

    public static synchronized void clearHooks() {
        frameRenderHook = null;
        hudRenderHook = null;
        clientTickHook = null;
        keyHook = null;
        mouseButtonHook = null;
        cursorPosHook = null;
        mouseScrollHook = null;
        charHook = null;
        swingOffsetHook = null;
        equipOffsetHook = null;
        customSkyColor = 0;
        hideScoreboard = false;
        hideTabList = false;
        hideCrosshair = false;
        itemSpinAngle = 0f;
        initialized = false;
        System.clearProperty("aether.client.active");
        Runnable handler = unloadHandler;
        if (handler != null) {
            try {
                handler.run();
            } catch (Throwable ignored) {}
        }
    }

    public static void dispatchFrameRender(Object renderTickCounter, boolean tick) {
        FrameRenderHook hook = frameRenderHook;
        if (hook != null) {
            hook.onFrameRender(renderTickCounter, tick);
        }
    }

    public static void dispatchHudRender(Object drawContext, Object renderTickCounter) {
        HudRenderHook hook = hudRenderHook;
        if (hook != null) {
            hook.onHudRender(drawContext, renderTickCounter);
        }
    }

    public static void dispatchClientTick(Object minecraftClient) {
        ClientTickHook hook = clientTickHook;
        if (hook != null) {
            hook.onClientTick(minecraftClient);
        }
    }

    public static boolean dispatchKey(long window, int action, Object keyInput) {
        KeyHook hook = keyHook;
        return hook != null && hook.onKey(window, action, keyInput);
    }

    public static boolean dispatchMouseButton(long window, Object mouseInput, int action) {
        MouseButtonHook hook = mouseButtonHook;
        return hook != null && hook.onMouseButton(window, mouseInput, action);
    }

    public static void dispatchCursorPos(long window, double x, double y) {
        CursorPosHook hook = cursorPosHook;
        if (hook != null) {
            hook.onCursorPos(window, x, y);
        }
    }

    public static boolean dispatchMouseScroll(long window, double horizontal, double vertical) {
        MouseScrollHook hook = mouseScrollHook;
        return hook != null && hook.onMouseScroll(window, horizontal, vertical);
    }

    public static boolean dispatchChar(long window, Object charInput) {
        CharHook hook = charHook;
        return hook != null && hook.onChar(window, charInput);
    }

    public static void setHideScoreboard(boolean hide) {
        hideScoreboard = hide;
    }

    public static void setHideTabList(boolean hide) {
        hideTabList = hide;
    }

    public static boolean shouldHideScoreboard() {
        return hideScoreboard;
    }

    public static boolean shouldHideTabList() {
        return hideTabList;
    }

    public static void setHideCrosshair(boolean hide) {
        hideCrosshair = hide;
    }

    public static boolean shouldHideCrosshair() {
        return hideCrosshair;
    }

    public static void setItemSpinAngle(float angleRadians) {
        itemSpinAngle = angleRadians;
    }

    public static float getItemSpinAngle() {
        return itemSpinAngle;
    }

    public static synchronized void registerSwingHooks(SwingOffsetHook swing, EquipOffsetHook equip) {
        swingOffsetHook = swing;
        equipOffsetHook = equip;
    }

    public static boolean dispatchSwingOffset(Object matrixStack, Object arm, float swingProgress) {
        SwingOffsetHook hook = swingOffsetHook;
        return hook != null && hook.onApplySwingOffset(matrixStack, arm, swingProgress);
    }

    public static boolean dispatchEquipOffset(Object matrixStack, Object arm, float equipProgress) {
        EquipOffsetHook hook = equipOffsetHook;
        return hook != null && hook.onApplyEquipOffset(matrixStack, arm, equipProgress);
    }

    public static void setCustomSkyColor(int color) {
        customSkyColor = color;
    }

    public static int getCustomSkyColor(int original) {
        int c = customSkyColor;
        return c != 0 ? c : original;
    }
}
