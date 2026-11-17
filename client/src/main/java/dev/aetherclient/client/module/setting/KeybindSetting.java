package dev.aetherclient.client.module.setting;

import org.lwjgl.glfw.GLFW;

public final class KeybindSetting extends Setting<Integer> {
    public KeybindSetting(String id, String name, int defaultKey) {
        super(id, name, defaultKey);
    }

    public String getKeyLabel() {
        int key = getValue();
        if (key == GLFW.GLFW_KEY_UNKNOWN || key == 0) return "Нет";

        // Mouse buttons encoded as -100 - button
        if (key <= -100 && key >= -115) {
            return switch (key) {
                case -100 -> "ЛКМ";
                case -101 -> "ПКМ";
                case -102 -> "СКМ";
                case -103 -> "M4";
                case -104 -> "M5";
                case -105 -> "M6";
                case -106 -> "M7";
                case -107 -> "M8";
                default -> "M" + (-key - 99);
            };
        }

        if (key < 0) return "Нет";

        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_ESCAPE -> "Esc";
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> "Enter";
            case GLFW.GLFW_KEY_BACKSPACE -> "Bksp";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps";
            case GLFW.GLFW_KEY_INSERT -> "Ins";
            case GLFW.GLFW_KEY_DELETE -> "Del";
            case GLFW.GLFW_KEY_HOME -> "Home";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_PAGE_UP -> "PgUp";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PgDn";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_LEFT -> "Left";
            case GLFW.GLFW_KEY_RIGHT -> "Right";
            default -> {
                if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F12) {
                    yield "F" + (key - GLFW.GLFW_KEY_F1 + 1);
                }
                String glfwName = GLFW.glfwGetKeyName(key, 0);
                yield glfwName != null ? glfwName.toUpperCase(java.util.Locale.ROOT) : "K" + key;
            }
        };
    }
}
