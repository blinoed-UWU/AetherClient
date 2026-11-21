package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.ModeSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public final class FreeLookModule extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("mode", "Режим", "Удержание", "Удержание", "Переключатель").withEn("Mode"));
    public final BooleanSetting thirdPerson = addSetting(new BooleanSetting("third_person", "Вид от третьего лица", true).withEn("Switch to 3rd Person"));
    public final BooleanSetting smoothInertia = addSetting(new BooleanSetting("inertia", "Кинематографичная плавность", true).withEn("Cinematic Smoothness"));
    public final NumberSetting sensitivity = addSetting(new NumberSetting("sensitivity", "Чувствительность", 1.0, 0.2, 3.0, 0.1).withEn("Sensitivity"));
    public final BooleanSetting reverseView = addSetting(new BooleanSetting("reverse", "Разворот на 180°", false).withEn("Look Backwards"));

    private float cameraYaw = 0f;
    private float cameraPitch = 0f;
    private int originalPerspective = 0;
    private boolean active = false;

    public FreeLookModule() {
        super("free_look", "Свободный обзор", "360° обзор камерой без изменения траектории движения и прицела", Category.VISUAL, GLFW.GLFW_KEY_V);
        setEn("Free Look", "360 degree camera orbit without altering movement direction or server aim");
    }

    public boolean isFreeLooking() {
        return isEnabled() && active;
    }

    public float getCameraYaw() {
        return cameraYaw;
    }

    public float getCameraPitch() {
        return cameraPitch;
    }

    public void updateCameraRotation(float deltaYaw, float deltaPitch) {
        float sens = sensitivity.asFloat();
        this.cameraYaw += deltaYaw * sens * 0.15f;
        this.cameraPitch = Math.max(-90f, Math.min(90f, this.cameraPitch + deltaPitch * sens * 0.15f));
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            cameraYaw = mc.player.getYaw() + (reverseView.isEnabled() ? 180f : 0f);
            cameraPitch = mc.player.getPitch();
            if (mc.options != null && thirdPerson.isEnabled()) {
                originalPerspective = mc.options.getPerspective().ordinal();
                mc.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
            }
        }
        active = true;
    }

    @Override
    protected void onDisable() {
        active = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null && thirdPerson.isEnabled()) {
            net.minecraft.client.option.Perspective[] values = net.minecraft.client.option.Perspective.values();
            if (originalPerspective >= 0 && originalPerspective < values.length) {
                mc.options.setPerspective(values[originalPerspective]);
            }
        }
    }
}
