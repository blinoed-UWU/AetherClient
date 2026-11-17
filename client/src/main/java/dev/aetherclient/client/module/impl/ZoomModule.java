package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public final class ZoomModule extends Module {
    public final NumberSetting targetFov = addSetting(new NumberSetting("target_fov", "FOV приближения", 28.0, 12.0, 60.0, 2.0).withEn("Zoom FOV"));
    public final NumberSetting smoothness = addSetting(new NumberSetting("smoothness", "Плавность", 14.0, 4.0, 30.0, 1.0).withEn("Smoothness"));

    private double savedFov = -1.0;
    private double currentFov = -1.0;

    public ZoomModule() {
        super("zoom", "Приближение", "Плавный кинематографический зум камеры", Category.VISUAL, GLFW.GLFW_KEY_C);
        setEn("Zoom", "Smooth cinematic camera zoom");
    }

    public float getEffectiveFov(MinecraftClient mc) {
        if (mc == null || mc.options == null) return 70f;
        if (currentFov > 1.0) return (float) currentFov;
        return mc.options.getFov().getValue().floatValue();
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null && savedFov < 0) {
            savedFov = mc.options.getFov().getValue();
            currentFov = savedFov;
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null && savedFov > 0) {
            mc.options.getFov().setValue((int) Math.round(savedFov));
        }
        savedFov = -1.0;
        currentFov = -1.0;
    }

    @Override
    public void onRender2D(MinecraftClient mc, float screenW, float screenH, float dt) {
        if (mc == null || mc.options == null) return;
        if (isEnabled()) {
            if (savedFov < 0) {
                savedFov = mc.options.getFov().getValue();
                currentFov = savedFov;
            }
            double goal = targetFov.getValue();
            currentFov += (goal - currentFov) * Math.min(1.0, dt * smoothness.getValue());
            int rounded = Math.max(10, Math.min(110, (int) Math.round(currentFov)));
            if (mc.options.getFov().getValue() != rounded) {
                mc.options.getFov().setValue(rounded);
            }
        }
    }
}
