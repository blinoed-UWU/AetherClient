package dev.aetherclient.client.module.impl;

import dev.aetherclient.client.module.Category;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.setting.BooleanSetting;
import dev.aetherclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

public final class FreecamModule extends Module {
    public final NumberSetting flySpeed = addSetting(new NumberSetting("fly_speed", "Скорость полёта", 1.5, 0.2, 5.0, 0.1).withEn("Fly Speed"));
    public final BooleanSetting noClip = addSetting(new BooleanSetting("noclip", "Проходить сквозь блоки", true).withEn("No-clip"));
    public final BooleanSetting freezePlayer = addSetting(new BooleanSetting("freeze_player", "Заморозить игрока", true).withEn("Freeze Player"));

    private OtherClientPlayerEntity dummy;
    private Vec3d originalPlayerPos;
    private float originalYaw;
    private float originalPitch;

    public FreecamModule() {
        super("freecam", "Свободная камера", "Плавный полет камеры сквозь блоки с полным вращением мышью", Category.VISUAL);
        setEn("Freecam", "Smooth noclip camera flight with full mouse rotation");
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.world == null) return;

        originalPlayerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        originalYaw = mc.player.getYaw();
        originalPitch = mc.player.getPitch();

        dummy = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());
        dummy.copyPositionAndRotation(mc.player);
        dummy.setHeadYaw(mc.player.getHeadYaw());
        dummy.setBodyYaw(mc.player.getBodyYaw());
        dummy.noClip = noClip.isEnabled();
        dummy.getAbilities().flying = true;
        dummy.getAbilities().allowFlying = true;

        mc.world.addEntity(dummy);
        mc.setCameraEntity(dummy);
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            if (mc.player != null) {
                mc.setCameraEntity(mc.player);
                if (originalPlayerPos != null) {
                    mc.player.setPosition(originalPlayerPos.x, originalPlayerPos.y, originalPlayerPos.z);
                    mc.player.setVelocity(0, 0, 0);
                    mc.player.setYaw(originalYaw);
                    mc.player.setPitch(originalPitch);
                }
            }
            if (dummy != null) {
                dummy.discard();
                dummy = null;
            }
        }
        originalPlayerPos = null;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!isEnabled() || mc.player == null || dummy == null) return;

        if (freezePlayer.isEnabled() && originalPlayerPos != null) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
            mc.player.setPosition(originalPlayerPos.x, originalPlayerPos.y, originalPlayerPos.z);
            if (mc.player.input != null) {
                mc.player.input.playerInput = PlayerInput.DEFAULT;
            }
        }

        // Full mouse rotation synchronization:
        dummy.setYaw(mc.player.getYaw());
        dummy.setPitch(mc.player.getPitch());
        dummy.setHeadYaw(mc.player.getYaw());
        dummy.setBodyYaw(mc.player.getYaw());

        dummy.noClip = noClip.isEnabled();
        dummy.getAbilities().flying = true;

        double speed = flySpeed.asFloat() * 0.7;
        double forward = 0;
        double strafe = 0;
        double up = 0;

        if (mc.options.forwardKey.isPressed()) forward += 1;
        if (mc.options.backKey.isPressed()) forward -= 1;
        if (mc.options.leftKey.isPressed()) strafe += 1;
        if (mc.options.rightKey.isPressed()) strafe -= 1;
        if (mc.options.jumpKey.isPressed()) up += 1;
        if (mc.options.sneakKey.isPressed()) up -= 1;

        double yawRad = Math.toRadians(dummy.getYaw());
        double dx = (strafe * Math.cos(yawRad) - forward * Math.sin(yawRad)) * speed;
        double dz = (forward * Math.cos(yawRad) + strafe * Math.sin(yawRad)) * speed;
        double dy = up * speed;

        // Smooth positional updates with proper interpolation anchor
        dummy.lastX = dummy.getX();
        dummy.lastY = dummy.getY();
        dummy.lastZ = dummy.getZ();
        dummy.lastRenderX = dummy.getX();
        dummy.lastRenderY = dummy.getY();
        dummy.lastRenderZ = dummy.getZ();

        dummy.setPosition(dummy.getX() + dx, dummy.getY() + dy, dummy.getZ() + dz);
        dummy.setVelocity(0, 0, 0);
    }

    public void syncCameraRotation(float yaw, float pitch) {
        if (dummy != null) {
            dummy.setYaw(yaw);
            dummy.setPitch(pitch);
            dummy.setHeadYaw(yaw);
            dummy.setBodyYaw(yaw);
        }
    }

    public boolean isFreecamActive() {
        return isEnabled() && dummy != null;
    }

    @Override
    public String getDisplaySuffix() {
        return String.format(java.util.Locale.ROOT, "%.1fx", flySpeed.getValue());
    }
}
