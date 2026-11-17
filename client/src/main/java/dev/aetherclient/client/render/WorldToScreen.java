package dev.aetherclient.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Fast 3D-to-2D projection helper using Minecraft's live Camera and projection matrix.
 * Allows rendering crisp NanoVG ESP boxes, tracers, target rings, trails, and particles
 * without interfering with Minecraft's 3D render pipeline or shaders.
 */
public final class WorldToScreen {
    private final Matrix4f viewProj = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final Quaternionf invRot = new Quaternionf();
    private final Vector4f clip = new Vector4f();
    private Vec3d camPos = Vec3d.ZERO;
    private float screenW;
    private float screenH;
    private boolean valid;

    private static java.lang.invoke.MethodHandle GET_FOV_MH;
    static {
        try {
            for (java.lang.reflect.Method m : net.minecraft.client.render.GameRenderer.class.getDeclaredMethods()) {
                if (m.getReturnType() == float.class) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 3 && p[0] == Camera.class && p[1] == float.class && p[2] == boolean.class) {
                        m.setAccessible(true);
                        GET_FOV_MH = java.lang.invoke.MethodHandles.lookup().unreflect(m);
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public void prepare(MinecraftClient mc, float screenW, float screenH, float fovOverride) {
        prepare(mc, screenW, screenH, fovOverride, 1f);
    }

    public void prepare(MinecraftClient mc, float screenW, float screenH, float fovOverride, float tickDelta) {
        this.screenW = screenW;
        this.screenH = screenH;
        this.valid = false;
        if (mc == null || mc.gameRenderer == null || mc.world == null || screenW <= 0f || screenH <= 0f) return;
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null) return;

        this.camPos = cam.getCameraPos();
        Quaternionf rot = cam.getRotation();
        if (rot == null) return;

        float fov;
        if (fovOverride > 1f) {
            fov = fovOverride;
        } else {
            float resolved = -1f;
            if (GET_FOV_MH != null) {
                try {
                    resolved = (float) GET_FOV_MH.invokeExact(mc.gameRenderer, cam, tickDelta, true);
                } catch (Throwable ignored) {}
            }
            if (resolved > 1f) {
                fov = resolved;
            } else {
                float base = mc.options != null ? mc.options.getFov().getValue().floatValue() : 70f;
                if (mc.player instanceof net.minecraft.client.network.AbstractClientPlayerEntity p && mc.options != null) {
                    boolean fp = mc.options.getPerspective().isFirstPerson();
                    float scale = mc.options.getFovEffectScale().getValue().floatValue();
                    base *= p.getFovMultiplier(fp, scale);
                }
                fov = base;
            }
        }
        Matrix4f proj = mc.gameRenderer.getBasicProjectionMatrix(fov);
        invRot.set(rot).conjugate();
        view.identity().rotate(invRot);
        viewProj.set(proj).mul(view);
        this.valid = true;
    }

    public boolean isValid() {
        return valid;
    }

    public Vec3d getCamPos() {
        return camPos;
    }

    /**
     * Projects a 3D world point to 2D NanoVG coordinates.
     *
     * @param out float[3] = {screenX, screenY, clipW}
     * @return true if in front of the camera
     */
    public boolean project(double wx, double wy, double wz, float[] out) {
        if (!valid) return false;
        clip.set((float) (wx - camPos.x), (float) (wy - camPos.y), (float) (wz - camPos.z), 1.0f);
        clip.mul(viewProj);
        if (clip.w <= 0.001f) return false;
        float invW = 1.0f / clip.w;
        float ndcX = clip.x * invW;
        float ndcY = clip.y * invW;
        out[0] = (ndcX * 0.5f + 0.5f) * screenW;
        out[1] = (0.5f - ndcY * 0.5f) * screenH;
        out[2] = clip.w;
        return Float.isFinite(out[0]) && Float.isFinite(out[1]);
    }

    /**
     * Projects an entity's 3D bounding box into a 2D screen-space rectangle {minX, minY, maxX, maxY}.
     */
    public boolean projectBox(Box box, float[] outRect, float[] scratch) {
        if (!valid || box == null) return false;
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        int visibleCorners = 0;

        for (int i = 0; i < 8; i++) {
            double cx = (i & 1) == 0 ? box.minX : box.maxX;
            double cy = (i & 2) == 0 ? box.minY : box.maxY;
            double cz = (i & 4) == 0 ? box.minZ : box.maxZ;
            if (project(cx, cy, cz, scratch)) {
                visibleCorners++;
                if (scratch[0] < minX) minX = scratch[0];
                if (scratch[1] < minY) minY = scratch[1];
                if (scratch[0] > maxX) maxX = scratch[0];
                if (scratch[1] > maxY) maxY = scratch[1];
            }
        }
        if (visibleCorners < 1) return false;

        // Resolution-independent margins: 10% of screen dimension so players at screen edge stay tracked
        float marginX = screenW * 0.10f;
        float marginY = screenH * 0.10f;
        if (maxX < -marginX || minX > screenW + marginX || maxY < -marginY || minY > screenH + marginY) return false;

        float bw = maxX - minX;
        float bh = maxY - minY;
        if (bw < 3f) {
            float midX = (minX + maxX) * 0.5f;
            minX = midX - 2f;
            maxX = midX + 2f;
        }
        if (bh < 6f) {
            float midY = (minY + maxY) * 0.5f;
            minY = midY - 4f;
            maxY = midY + 4f;
        }
        if (bw > screenW * 2.5f || bh > screenH * 2.5f) return false;

        outRect[0] = minX;
        outRect[1] = minY;
        outRect[2] = maxX;
        outRect[3] = maxY;
        return true;
    }

    /**
     * Projects a 3D world point even if behind the camera (useful for off-screen arrows).
     */
    public void projectDirection(double wx, double wy, double wz, Vector3f outView) {
        outView.set((float) (wx - camPos.x), (float) (wy - camPos.y), (float) (wz - camPos.z));
        outView.rotate(invRot);
    }
}
