package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public final class ZoomModule extends Module {
    public final NumberSetting targetFov = addSetting(new NumberSetting("target_fov", "FOV приближения", 28.0, 10.0, 65.0, 2.0).withEn("Zoom FOV"));
    public final NumberSetting smoothness = addSetting(new NumberSetting("smoothness", "Плавность перехода", 14.0, 4.0, 30.0, 1.0).withEn("Smoothness"));
    public final ModeSetting zoomMode = addSetting(new ModeSetting("mode", "Режим активации", "Удержание", "Удержание", "Переключатель").withEn("Activation Mode"));
    public final BooleanSetting smoothScrollZoom = addSetting(new BooleanSetting("scroll_zoom", "Регулировка колесиком", true).withEn("Mouse Wheel Zoom"));
    public final NumberSetting scrollSpeed = addSetting(new NumberSetting("scroll_spd", "Скорость колесика", 4.0, 1.0, 10.0, 0.5).withEn("Scroll Speed"));
    public final BooleanSetting mouseSensitivityReduction = addSetting(new BooleanSetting("sens_reduct", "Снижение чувств. мыши", true).withEn("Reduce Mouse Sensitivity"));
    public final NumberSetting sensitivityRatio = addSetting(
            new NumberSetting("sens_ratio", "Множитель чувств.", 0.45, 0.1, 1.0, 0.05)
                    .<NumberSetting>withEn("Sensitivity Multiplier").visibleIf(mouseSensitivityReduction::isEnabled)
    );
    public final BooleanSetting vignette = addSetting(new BooleanSetting("vignette", "Кино-виньетка при зуме", true).withEn("Cinematic Vignette"));

    private double savedFov = -1.0;
    private double currentFov = -1.0;

    public ZoomModule() {
        super("zoom", "Приближение", "Плавный кинематографический зум камеры с регулировкой колесиком и кино-виньеткой", Category.VISUAL, GLFW.GLFW_KEY_C);
        setEn("Zoom", "Smooth cinematic camera zoom with mouse wheel adjustments and vignette");
    }

    public float getEffectiveFov(MinecraftClient mc) {
        if (mc == null || mc.options == null) return 70f;
        if (currentFov > 1.0) return (float) currentFov;
        return mc.options.getFov().getValue().floatValue();
    }

    public void onMouseScroll(double vertical) {
        if (!isEnabled() || !smoothScrollZoom.isEnabled()) return;
        double step = vertical * scrollSpeed.getValue();
        double updated = Math.max(8.0, Math.min(85.0, targetFov.getValue() - step));
        targetFov.setValue(updated);
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
