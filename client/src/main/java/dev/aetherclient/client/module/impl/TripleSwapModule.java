package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.audio.SoundEngine;
import dev.aetherclient.client.notify.ToastManager;
import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.KeybindSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 3-Way Radial Swap ("Тройной свап").
 * Upon holding the configured key, reveals a radial pie menu with mouse cursor
 * to choose 1 of 3 combat items (Talisman, Sphere, Totem) into offhand/mainhand.
 * Prioritizes hotbar slots for 0-flag instant swap, with stop-sprint inventory fallback.
 */
public final class TripleSwapModule extends Module {
    public static final String[] ITEM_CHOICES = {
            "Талисман", "Шар / Сфера", "Тотем", "Жемчуг", "Золотое яблоко",
            "Чар. яблоко", "Щит", "Булава", "Ветровой заряд", "Зелье"
    };

    public final KeybindSetting swapKey = new KeybindSetting("key", "Клавиша меню", GLFW.GLFW_KEY_R).withEn("Menu Key");
    public final ModeSetting targetHand = new ModeSetting("hand", "Куда свапать", "Левая рука (Offhand)",
            "Левая рука (Offhand)", "Основная рука (Hotbar)").withEn("Target Hand");
    public final ModeSetting item1 = new ModeSetting("item1", "Сектор 1 (Верх)", "Талисман", ITEM_CHOICES).withEn("Sector 1 (Top)");
    public final ModeSetting item2 = new ModeSetting("item2", "Сектор 2 (Справа)", "Шар / Сфера", ITEM_CHOICES).withEn("Sector 2 (Right)");
    public final ModeSetting item3 = new ModeSetting("item3", "Сектор 3 (Слева)", "Тотем", ITEM_CHOICES).withEn("Sector 3 (Left)");
    public final NumberSetting radius = new NumberSetting("radius", "Радиус меню", 115.0, 70.0, 190.0, 5.0).withEn("Menu Radius");
    public final BooleanSetting blurBackdrop = new BooleanSetting("blur", "Размытие фона", true).withEn("Blur Backdrop");
    public final BooleanSetting instantHoverSwap = new BooleanSetting("instant", "Свап сразу по наведению", false).withEn("Instant on Hover");
    public final BooleanSetting showNotifications = new BooleanSetting("notify", "Уведомление в чат/тост", true).withEn("Notify");
    public final BooleanSetting playSound = new BooleanSetting("sound", "Звуковой эффект", true).withEn("Sound");

    private boolean menuOpen = false;
    private int selectedSector = -1; // -1: none, 0: top, 1: bottom-right, 2: bottom-left
    private float menuProgress = 0f;
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    public TripleSwapModule() {
        super("triple_swap", "Тройной свап", "Круговое меню при зажатии клавиши для выбора 1 из 3 предметов (Талик/Шар/Тотем) в руку без флагов", Category.COMBAT);
        setEn("Triple Swap", "Hold key to open 3-way radial pie menu for 0-flag combat items swap into hand");
        addSetting(swapKey);
        addSetting(targetHand);
        addSetting(item1);
        addSetting(item2);
        addSetting(item3);
        addSetting(radius);
        addSetting(blurBackdrop);
        addSetting(instantHoverSwap);
        addSetting(showNotifications);
        addSetting(playSound);
        setEnabled(true);
    }

    public boolean isMenuOpen() {
        return isEnabled() && menuOpen;
    }

    public int getSelectedSector() {
        return selectedSector;
    }

    public float getMenuProgress() {
        return menuProgress;
    }

    public void updateAnimation(float delta) {
        float target = menuOpen ? 1.0f : 0.0f;
        menuProgress += (target - menuProgress) * Math.min(1.0f, delta * 18.0f);
    }

    public boolean onKey(int key, int scancode, int action, int mods) {
        if (!isEnabled()) return false;
        int trigger = swapKey.getValue();
        if (trigger == -1 || key != trigger) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return false;

        if (action == GLFW.GLFW_PRESS) {
            menuOpen = true;
            selectedSector = -1;
            int winW = mc.getWindow().getWidth();
            int winH = mc.getWindow().getHeight();
            lastMouseX = winW / 2.0;
            lastMouseY = winH / 2.0;
            mc.mouse.unlockCursor();
            return true;
        } else if (action == GLFW.GLFW_RELEASE) {
            if (menuOpen) {
                menuOpen = false;
                mc.mouse.lockCursor();
                int chosen = selectedSector;
                if (chosen >= 0) {
                    executeSwap(chosen);
                }
                return true;
            }
        }
        return false;
    }

    public void onCursorPos(double xpos, double ypos) {
        if (!menuOpen) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        lastMouseX = xpos;
        lastMouseY = ypos;

        double scale = mc.getWindow().getScaleFactor();
        double scaledW = mc.getWindow().getScaledWidth();
        double scaledH = mc.getWindow().getScaledHeight();

        double mx = xpos / scale;
        double my = ypos / scale;
        double cx = scaledW / 2.0;
        double cy = scaledH / 2.0;

        double dx = mx - cx;
        double dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 22.0) {
            selectedSector = -1;
            return;
        }

        // Angle in degrees from -180 to 180
        // Top: -90 deg, Right: 0 deg, Bottom: 90 deg, Left: 180 / -180 deg
        double angle = Math.toDegrees(Math.atan2(dy, dx));

        // 3 sectors of 120 degrees each:
        // Sector 0 (Top): -150 to -30 degrees
        // Sector 1 (Bottom-Right): -30 to 90 degrees
        // Sector 2 (Bottom-Left): 90 to 180 or -180 to -150 degrees
        if (angle >= -150 && angle < -30) {
            selectedSector = 0;
        } else if (angle >= -30 && angle < 90) {
            selectedSector = 1;
        } else {
            selectedSector = 2;
        }
    }

    public String getItemNameForSector(int sector) {
        return switch (sector) {
            case 0 -> item1.getValue();
            case 1 -> item2.getValue();
            case 2 -> item3.getValue();
            default -> "";
        };
    }

    public void executeSwap(int sector) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.interactionManager == null) return;

        String targetName = getItemNameForSector(sector);
        if (targetName == null || targetName.isEmpty()) return;

        PlayerInventory inv = mc.player.getInventory();
        int targetSlot = -1;
        boolean inHotbar = false;

        // 1. Search hotbar first (0..8) - zero anticheat flag
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (isMatching(stack, targetName)) {
                targetSlot = i;
                inHotbar = true;
                break;
            }
        }

        // 2. Search main inventory (9..35)
        if (targetSlot == -1) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = inv.getStack(i);
                if (isMatching(stack, targetName)) {
                    targetSlot = i;
                    inHotbar = false;
                    break;
                }
            }
        }

        if (targetSlot == -1) {
            if (showNotifications.isEnabled()) {
                ToastManager.get().push("Тройной свап", "Предмет не найден: " + targetName, 0xFFEF4444);
            }
            return;
        }

        boolean toOffhand = targetHand.getValue().contains("Offhand") || targetHand.getValue().contains("Левая");

        if (inHotbar) {
            if (toOffhand) {
                // Click slot 36 + targetSlot with button=40 (SWAP with offhand)
                int screenSlot = 36 + targetSlot;
                mc.interactionManager.clickSlot(0, screenSlot, 40, SlotActionType.SWAP, mc.player);
            } else {
                inv.setSelectedSlot(targetSlot);
            }
        } else {
            // Main inventory slot (screen slot: 9..35)
            // Perform safe stop-sprint swap
            mc.player.setSprinting(false);
            if (toOffhand) {
                mc.interactionManager.clickSlot(0, targetSlot, 40, SlotActionType.SWAP, mc.player);
            } else {
                mc.interactionManager.clickSlot(0, targetSlot, inv.getSelectedSlot(), SlotActionType.SWAP, mc.player);
            }
            if (mc.getNetworkHandler() != null && mc.currentScreen != null) {
                mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
            }
        }

        if (playSound.isEnabled()) {
            SoundEngine.get().playClick(mc, true, 1.25f);
        }

        if (showNotifications.isEnabled()) {
            ToastManager.get().push("Тройной свап", "Взято в руку: " + targetName, 0xFF10B981);
        }
    }

    private boolean isMatching(ItemStack stack, String targetKey) {
        if (stack == null || stack.isEmpty()) return false;
        String lowerKey = targetKey.toLowerCase();

        String displayName = stack.getName().getString().toLowerCase();
        String lore = "";
        LoreComponent loreComp = stack.get(DataComponentTypes.LORE);
        if (loreComp != null) {
            StringBuilder sb = new StringBuilder();
            for (Text line : loreComp.lines()) {
                sb.append(line.getString().toLowerCase()).append(" ");
            }
            lore = sb.toString();
        }

        boolean isCustomSpecial = displayName.contains("талисман") || displayName.contains("талик")
                || displayName.contains("шар") || displayName.contains("сфер")
                || lore.contains("талисман") || lore.contains("талик")
                || lore.contains("шар") || lore.contains("сфер");

        if (lowerKey.contains("талисман")) {
            return displayName.contains("талисман") || displayName.contains("талик")
                    || lore.contains("талисман") || lore.contains("талик");
        } else if (lowerKey.contains("шар") || lowerKey.contains("сфер")) {
            return displayName.contains("шар") || displayName.contains("сфер")
                    || lore.contains("шар") || lore.contains("сфер");
        } else if (lowerKey.contains("тотем")) {
            return stack.isOf(Items.TOTEM_OF_UNDYING) && !isCustomSpecial;
        } else if (lowerKey.contains("жемчуг") || lowerKey.contains("перл")) {
            return stack.isOf(Items.ENDER_PEARL);
        } else if (lowerKey.contains("яблоко") || lowerKey.contains("гэпл") || lowerKey.contains("ябло")) {
            return stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE);
        }

        return displayName.contains(lowerKey) || lore.contains(lowerKey);
    }
}
