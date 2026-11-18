package dev.aetherclient.client.render;

import dev.aetherclient.client.module.ModuleManager;
import dev.aetherclient.client.notify.ToastManager;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Unified, zero-clutter renderer for all world-projected and screen-space visual modules.
 */
public final class VisualRenderer {
    /** Minimal lock-free object pool to avoid per-frame allocation churn in hot render paths. */
    private static final class Pool<T> {
        private final ArrayDeque<T> free = new ArrayDeque<>();
        private final Supplier<T> factory;
        private final java.util.function.Consumer<T> reset;

        Pool(Supplier<T> factory) {
            this(factory, null);
        }

        Pool(Supplier<T> factory, java.util.function.Consumer<T> reset) {
            this.factory = factory;
            this.reset = reset;
        }

        T acquire() {
            T v = free.pollLast();
            return v != null ? v : factory.get();
        }

        void release(T v) {
            if (reset != null) reset.accept(v);
            if (free.size() < 512) free.addLast(v);
        }
    }

    private static final class JumpCircle {
        double x, y, z;
        float age;
        float maxAge;
        float maxRadius;
    }

    private static final class TrailPoint {
        double x, y, z;
        long timeMs;
    }

    private static final class BreadcrumbPoint {
        double x, y, z;
        long timeMs;
    }

    private static final class HitSpark {
        double x, y, z;
        double vx, vy, vz;
        float age;
        float maxAge;
    }

    private static final class HitTint {
        Entity entity;
        long timeMs;
    }

    private static final class AmbientParticle {
        double x, y, z;
        float vx, vy, vz, size, phase;
    }

    private static final class FloatingDamage {
        LivingEntity target;
        double offsetX, offsetZ;
        double baseOffsetY;
        double riseY;
        double lastX, lastY, lastZ;
        float amount;
        boolean isHeal;
        float age;
        float maxAge;
        float vy;
    }

    private final WorldToScreen wts = new WorldToScreen();
    private final float[] ptScratch = new float[3];
    private final float[] ptScratch2 = new float[3];
    private final float[] ptScratch3 = new float[3];
    private final float[] ptScratch4 = new float[3];
    private final float[] boxRect = new float[4];
    private final Vector3f dirVec = new Vector3f();

    private static final int[][] CUBE_EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    private static final int[][] CUBE_FACES = {
            {0, 1, 2, 3}, // Bottom
            {4, 5, 6, 7}, // Top
            {0, 1, 5, 4}, // North
            {3, 2, 6, 7}, // South
            {0, 3, 7, 4}, // West
            {1, 2, 6, 5}  // East
    };
    private final float[][] boxCornersScratch = new float[8][3];
    private final boolean[] boxCornersVis = new boolean[8];
    private final float[] polyScratchX = new float[4];
    private final float[] polyScratchY = new float[4];

    private final Pool<JumpCircle> jumpCirclePool = new Pool<>(JumpCircle::new, c -> {
        c.x = c.y = c.z = 0;
        c.age = c.maxAge = c.maxRadius = 0f;
    });
    private final Pool<TrailPoint> trailPointPool = new Pool<>(TrailPoint::new, t -> {
        t.x = t.y = t.z = 0;
        t.timeMs = 0L;
    });
    private final Pool<BreadcrumbPoint> breadcrumbPool = new Pool<>(BreadcrumbPoint::new, b -> {
        b.x = b.y = b.z = 0;
        b.timeMs = 0L;
    });
    private final Pool<HitSpark> hitSparkPool = new Pool<>(HitSpark::new, s -> {
        s.x = s.y = s.z = 0;
        s.vx = s.vy = s.vz = 0;
        s.age = s.maxAge = 0f;
    });
    private final Pool<AmbientParticle> particlePool = new Pool<>(AmbientParticle::new, p -> {
        p.x = p.y = p.z = 0;
        p.vx = p.vy = p.vz = p.size = p.phase = 0f;
    });
    private final Pool<FloatingDamage> floatingDamagePool = new Pool<>(FloatingDamage::new, f -> {
        f.target = null;
        f.offsetX = f.offsetZ = f.baseOffsetY = f.riseY = 0;
        f.lastX = f.lastY = f.lastZ = 0;
        f.amount = 0f;
        f.isHeal = false;
        f.age = f.maxAge = f.vy = 0f;
    });

    private final List<JumpCircle> jumpCircles = new ArrayList<>();
    private final Deque<TrailPoint> trailPoints = new ArrayDeque<>();
    private final Deque<BreadcrumbPoint> breadcrumbPoints = new ArrayDeque<>();
    private final List<HitSpark> hitSparks = new ArrayList<>();
    private final List<HitTint> hitTints = new ArrayList<>();
    private final List<AmbientParticle> ambientParticles = new ArrayList<>();
    private final List<FloatingDamage> floatingDamages = new ArrayList<>();
    private final java.util.Map<Integer, Float> trackedHealth = new java.util.HashMap<>();
    private final Random rng = new Random(0xAE7E4L);

    private boolean wasOnGround = true;
    private double prevYVelocity = 0.0;
    private ModuleManager lastModuleManager = null;
    private double lastTrailX, lastTrailY, lastTrailZ;
    private boolean hasLastTrail = false;
    private double lastCrumbX, lastCrumbY, lastCrumbZ;
    private boolean hasLastCrumb = false;
    private float prevPlayerHealth = -1f;
    private float damageFlash = 0f;
    private LivingEntity activeTarget = null;
    private long activeTargetMs = 0L;
    private float targetSpinAngle = 0f;
    private float globalTime = 0f;
    private Object lastWorldRef = null;
    private float prevYaw = Float.NaN;
    private float prevPitch = Float.NaN;
    private double prevCamX = 0.0;
    private double prevCamY = 0.0;
    private double prevCamZ = 0.0;
    // Motion blur exponential smoothing state
    private float mbSmoothOffX = Float.NaN;
    private float mbSmoothOffY = 0f;
    private float mbSmoothStrength = 0f;
    // Crosshair hitmarker and smooth block overlay state
    private long hitmarkerTimeMs = 0L;
    private double smoothBlockX = Double.NaN;
    private double smoothBlockY = 0.0;
    private double smoothBlockZ = 0.0;
    private double smoothBlockW = 1.0;
    private double smoothBlockH = 1.0;
    private double smoothBlockD = 1.0;
    private float smoothBlockAlpha = 0f;
    private float smoothAspect = Float.NaN;

    public WorldToScreen getWorldToScreen() {
        return wts;
    }

    public void onAttackEntity(Entity target) {
        if (target instanceof LivingEntity living) {
            activeTarget = living;
            activeTargetMs = System.currentTimeMillis();
            hitmarkerTimeMs = System.currentTimeMillis();
            spawnHitSparks(living);
            HitTint tint = new HitTint();
            tint.entity = living;
            tint.timeMs = System.currentTimeMillis();
            hitTints.add(tint);
            while (hitTints.size() > 16) {
                hitTints.remove(0);
            }
        }
    }


    private void spawnHitSparks(LivingEntity target) {
        double cx = target.getX();
        double cy = target.getY() + target.getHeight() * 0.55;
        double cz = target.getZ();
        int sparkCount = 24;
        float baseSpeed = 2.0f;
        float life = 0.55f;
        if (lastModuleManager != null && lastModuleManager.hitParticles != null) {
            sparkCount = (int) Math.round(lastModuleManager.hitParticles.count.getValue());
            baseSpeed = lastModuleManager.hitParticles.speed.asFloat() * 2.0f;
            life = lastModuleManager.hitParticles.lifetime.asFloat();
        }
        for (int i = 0; i < sparkCount; i++) {
            double ang = rng.nextDouble() * Math.PI * 2.0;
            double spd = (0.7 + rng.nextDouble() * 1.3) * baseSpeed;
            double vy = 0.8 + rng.nextDouble() * 2.2;
            HitSpark s = hitSparkPool.acquire();
            s.x = cx;
            s.y = cy;
            s.z = cz;
            s.vx = Math.cos(ang) * spd;
            s.vy = vy;
            s.vz = Math.sin(ang) * spd;
            s.age = 0f;
            s.maxAge = life * (0.8f + rng.nextFloat() * 0.4f);
            hitSparks.add(s);
        }
        while (hitSparks.size() > 160) {
            hitSparkPool.release(hitSparks.remove(0));
        }
    }

    public void reset() {
        for (JumpCircle c : jumpCircles) jumpCirclePool.release(c);
        for (TrailPoint t : trailPoints) trailPointPool.release(t);
        for (BreadcrumbPoint b : breadcrumbPoints) breadcrumbPool.release(b);
        for (HitSpark s : hitSparks) hitSparkPool.release(s);
        for (AmbientParticle p : ambientParticles) particlePool.release(p);
        for (FloatingDamage fd : floatingDamages) floatingDamagePool.release(fd);
        jumpCircles.clear();
        trailPoints.clear();
        breadcrumbPoints.clear();
        hitSparks.clear();
        hitTints.clear();
        ambientParticles.clear();
        floatingDamages.clear();
        trackedHealth.clear();
        activeTarget = null;
        prevPlayerHealth = -1f;
        damageFlash = 0f;
        hasLastTrail = false;
        hasLastCrumb = false;
        prevYaw = Float.NaN;
        prevPitch = Float.NaN;
        mbSmoothOffX = Float.NaN;
        mbSmoothStrength = 0f;
        prevYVelocity = 0.0;
        smoothBlockX = Double.NaN;
        smoothBlockAlpha = 0f;
        smoothAspect = Float.NaN;
    }


    public void renderWorldVisuals(
            MinecraftClient mc,
            ModuleManager mm,
            NanoVgEngine engine,
            AetherPalette palette,
            float screenW,
            float screenH,
            float dt
    ) {
        if (mc == null || mc.world == null || mc.player == null) return;

        if (mc.world != lastWorldRef) {
            lastWorldRef = mc.world;
            for (JumpCircle c : jumpCircles) jumpCirclePool.release(c);
            for (TrailPoint t : trailPoints) trailPointPool.release(t);
            for (BreadcrumbPoint b : breadcrumbPoints) breadcrumbPool.release(b);
            for (HitSpark s : hitSparks) hitSparkPool.release(s);
            for (FloatingDamage fd : floatingDamages) floatingDamagePool.release(fd);
            for (AmbientParticle p : ambientParticles) particlePool.release(p);
            jumpCircles.clear();
            trailPoints.clear();
            breadcrumbPoints.clear();
            hitSparks.clear();
            hitTints.clear();
            floatingDamages.clear();
            ambientParticles.clear();
            trackedHealth.clear();
            activeTarget = null;
            hasLastTrail = false;
            hasLastCrumb = false;
            prevYaw = Float.NaN;
            prevPitch = Float.NaN;
            mbSmoothOffX = Float.NaN;
            mbSmoothStrength = 0f;
            prevYVelocity = 0.0;
            prevPlayerHealth = mc.player.getHealth();
        }

        this.lastModuleManager = mm;
        if (mm.hpCounter.isEnabled()) {
            updateHpTracking(mc, mm);
        }

        globalTime += dt;
        float tickDelta = mc.getRenderTickCounter() != null ? mc.getRenderTickCounter().getTickProgress(false) : 1f;
        float fovOverride = mm.zoom.isEnabled() ? mm.zoom.getEffectiveFov(mc) : -1f;
        wts.prepare(mc, screenW, screenH, fovOverride, tickDelta);

        long vg = engine.handle();

        boolean needEntityLoop = mm.esp.isEnabled() || mm.tracers.isEnabled() || mm.arrows.isEnabled();
        if (needEntityLoop && wts.isValid()) {
            renderEntities(mc, mm, engine, palette, screenW, screenH, tickDelta);
        }

        if (mm.targetEsp.isEnabled() && wts.isValid()) {
            renderTargetEsp(mc, mm, vg, palette, tickDelta, dt);
        }

        if (mm.jumpCircles.isEnabled() && wts.isValid()) {
            renderJumpCircles(mc, mm, vg, palette, dt);
        } else if (!jumpCircles.isEmpty()) {
            jumpCircles.clear();
        }

        if (mm.trails.isEnabled() && wts.isValid()) {
            renderTrails(mc, mm, vg, palette, tickDelta);
        } else if (!trailPoints.isEmpty()) {
            trailPoints.clear();
            hasLastTrail = false;
        }

        if (mm.hitParticles.isEnabled() && wts.isValid()) {
            renderHitSparks(mm, vg, palette, dt);
        } else if (!hitSparks.isEmpty()) {
            hitSparks.clear();
        }

        if (mm.crosshair.isEnabled() && mc.currentScreen == null) {
            renderCustomCrosshair(mc, mm, vg, palette, screenW, screenH);
        }

        if (mm.ambience.isEnabled() && mm.ambience.vignette.isEnabled()) {
            drawEdgeVignette(vg, screenW, screenH, AetherPalette.withAlpha(0xFF06060D, Math.round(mm.ambience.vignetteAlpha.asFloat() * 220f)));
        }

        if (mm.damageVignette.isEnabled()) {
            renderDamageVignette(mc, mm, vg, dt, screenW, screenH);
        }

        boolean weatherSnow = mm.weather.isEnabled() && "Снег".equals(mm.weather.mode.getValue());
        if ((mm.particles.isEnabled() || weatherSnow) && wts.isValid()) {
            renderAmbientParticles(mc, mm, vg, palette, screenW, screenH, dt, weatherSnow);
        } else if (!ambientParticles.isEmpty()) {
            ambientParticles.clear();
        }

        if (mm.chams.isEnabled() && wts.isValid()) {
            renderChams(mc, mm, vg, palette, tickDelta);
        }

        if (mm.breadcrumbs.isEnabled() && wts.isValid()) {
            renderBreadcrumbs(mc, mm, vg, palette, tickDelta);
        } else if (!breadcrumbPoints.isEmpty()) {
            for (BreadcrumbPoint b : breadcrumbPoints) breadcrumbPool.release(b);
            breadcrumbPoints.clear();
            hasLastCrumb = false;
        }

        if (mm.projectiles.isEnabled() && wts.isValid()) {
            renderProjectiles(mc, mm, vg, palette);
        }

        if (mm.itemEsp.isEnabled() && wts.isValid()) {
            renderItemEsp(mc, mm, vg, palette, tickDelta);
        }

        if (mm.blockOverlay.isEnabled() && wts.isValid()) {
            renderBlockOverlay(mc, mm, vg, palette, screenW, screenH, dt);
        }

        if (mm.holeEsp.isEnabled() && wts.isValid()) {
            renderHoleEsp(mc, mm, vg, palette, tickDelta);
        }

        if (mm.hitColor.isEnabled()) {
            renderHitTints(mc, mm, vg, palette, tickDelta);
        } else if (!hitTints.isEmpty()) {
            hitTints.clear();
        }

        if (mm.hpCounter.isEnabled() && wts.isValid()) {
            renderHpCounter(mc, mm, vg, palette, dt);
        } else if (!floatingDamages.isEmpty()) {
            for (FloatingDamage fd : floatingDamages) floatingDamagePool.release(fd);
            floatingDamages.clear();
        }

        if (mm.cosmetics.isEnabled() && wts.isValid()) {
            renderCosmetics(mc, mm, vg, palette, tickDelta);
        }

        if (mm.hitboxes.isEnabled() && wts.isValid()) {
            renderHitboxes(mc, mm, vg, tickDelta);
        }

        if (mm.chinaHat.isEnabled() && wts.isValid()) {
            renderChinaHat(mc, mm, vg, palette, tickDelta);
        }

        if (mm.pearlPredict.isEnabled() && wts.isValid()) {
            renderPearlPredict(mc, mm, vg, palette, tickDelta);
        }

        if (mm.customSky.isEnabled()) {
            renderCustomSky(mc, mm, vg, palette, screenW, screenH, dt);
        } else if (mm.timeChanger.isEnabled() && mm.timeChanger.skyboxTint.isEnabled()) {
            renderSkyTint(mm, vg, palette, screenW, screenH);
        }

        if (mm.colorFilter.isEnabled()) {
            renderColorFilter(mm, vg, screenW, screenH);
        }

        if (mm.motionBlur.isEnabled()) {
            renderMotionBlur(mc, mm, engine, screenW, screenH, dt);
        }

        if (mm.scoreboard.isEnabled()) {
            renderCustomScoreboard(mc, mm, vg, palette, screenW, screenH);
        }

        if (mm.tabList.isEnabled()) {
            renderCustomTabList(mc, mm, vg, palette, screenW, screenH);
        }

        if (mm.notifications.isEnabled()) {
            ToastManager.get().render(engine, palette, screenW, screenH, dt, mm.notifications);
        }

        if (mm.itemRadius.isEnabled() && wts.isValid()) {
            renderItemRadius(mc, mm, vg, palette, tickDelta);
        }

        if (mm.aspectRatio.isEnabled() && mm.aspectRatio.hudOnTop.isEnabled()) {
            renderAspectRatio(mm, vg, palette, screenW, screenH, dt);
        }
    }

    private void renderEntities(
            MinecraftClient mc,
            ModuleManager mm,
            NanoVgEngine engine,
            AetherPalette palette,
            float screenW,
            float screenH,
            float tickDelta
    ) {
        long vg = engine.handle();
        boolean espOn = mm.esp.isEnabled();
        boolean tracersOn = mm.tracers.isEnabled();
        boolean arrowsOn = mm.arrows.isEnabled();

        float espMaxDistSq = (float) (mm.esp.maxDistance.getValue() * mm.esp.maxDistance.getValue());
        float trMaxDistSq = (float) (mm.tracers.maxDistance.getValue() * mm.tracers.maxDistance.getValue());
        float arrMaxDistSq = (float) (mm.arrows.maxDistance.getValue() * mm.arrows.maxDistance.getValue());

        float trStartX = screenW * 0.5f;
        float trStartY = "Низ".equals(mm.tracers.origin.getValue()) ? screenH - 4f : screenH * 0.5f;

        Vec3d camPos = wts.getCamPos();

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !e.isAlive()) continue;
            boolean isPlayer = e instanceof PlayerEntity;
            boolean isHostile = e instanceof HostileEntity;
            boolean isPassive = e instanceof PassiveEntity;
            boolean isItem = e instanceof ItemEntity;
            if (!isPlayer && !isHostile && !isPassive && !isItem) continue;

            double dx = e.getX() - camPos.x;
            double dy = e.getY() - camPos.y;
            double dz = e.getZ() - camPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            boolean isFriend = isPlayer && dev.aetherclient.client.friend.FriendsManager.get() != null
                    && dev.aetherclient.client.friend.FriendsManager.get().isFriend(e.getName().getString());
            boolean isFriendHurt = isFriend && e instanceof LivingEntity le && le.hurtTime > 0;

            int entityCol = isFriendHurt ? ((globalTime * 8 % 2 < 1) ? 0xFFFF1744 : 0xFFFF5252)
                    : (isFriend ? 0xFF34D399
                    : (isPlayer ? palette.primary
                    : (isHostile ? 0xFFFF6B7A : (isPassive ? 0xFF6CE5A8 : palette.tertiary))));

            Vec3d lerped = e.getLerpedPos(tickDelta);
            double ox = lerped.x - e.getX();
            double oy = lerped.y - e.getY();
            double oz = lerped.z - e.getZ();

            boolean canSee = canSeeEntity(mc, e);

            // 1. Tracers (tapered with soft glow for depth)
            if (tracersOn && distSq <= trMaxDistSq && (mm.tracers.throughWalls.isEnabled() || canSee)
                    && ((isPlayer && mm.tracers.players.isEnabled()) || (isHostile && mm.tracers.hostiles.isEnabled()))) {
                if (wts.project(lerped.x, lerped.y + e.getHeight() * 0.5, lerped.z, ptScratch)) {
                    float dist = (float) Math.sqrt(distSq);
                    float alpha = Math.max(0.18f, Math.min(0.88f, 1.0f - dist / (mm.tracers.maxDistance.asFloat() * 1.1f)));
                    float width = mm.tracers.width.asFloat();
                    Render2D.drawTaperedLine(vg, trStartX, trStartY, ptScratch[0], ptScratch[1],
                            width, width * 0.32f,
                            AetherPalette.withAlpha(entityCol, Math.round(alpha * 255f)),
                            AetherPalette.withAlpha(entityCol, Math.round(alpha * 48f)));
                }
            }

            // 2. Arrows (off-screen indicators)
            if (arrowsOn && distSq <= arrMaxDistSq && (mm.arrows.throughWalls.isEnabled() || canSee)
                    && ((isPlayer && mm.arrows.players.isEnabled()) || (isHostile && mm.arrows.hostiles.isEnabled()))) {
                boolean onScreen = wts.project(lerped.x, lerped.y + e.getHeight() * 0.5, lerped.z, ptScratch)
                        && ptScratch[0] >= 12f && ptScratch[0] <= screenW - 12f
                        && ptScratch[1] >= 12f && ptScratch[1] <= screenH - 12f;
                if (!onScreen) {
                    wts.projectDirection(lerped.x, lerped.y + e.getHeight() * 0.5, lerped.z, dirVec);
                    float angle = (float) Math.atan2(-dirVec.y, dirVec.x);
                    float r = mm.arrows.radius.asFloat();
                    float sz = mm.arrows.size.asFloat();
                    float ax = screenW * 0.5f + (float) Math.cos(angle) * r;
                    float ay = screenH * 0.5f + (float) Math.sin(angle) * r;

                    NanoVG.nvgSave(vg);
                    NanoVG.nvgTranslate(vg, ax, ay);
                    NanoVG.nvgRotate(vg, angle + (float) (Math.PI * 0.5));
                    drawFilledArrow(vg, sz, AetherPalette.withAlpha(entityCol, 220));
                    NanoVG.nvgRestore(vg);

                    float dist = (float) Math.sqrt(distSq);
                    Render2D.drawTextBold(vg, Math.round(dist) + "m", ax, ay + sz + 5f, 8.2f,
                            AetherPalette.withAlpha(palette.onSurface, 200), NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
                }
            }

            // 3. 2D ESP
            if (!espOn || distSq > espMaxDistSq) continue;
            boolean allowEsp = (isPlayer && mm.esp.players.isEnabled())
                    || (isHostile && mm.esp.hostiles.isEnabled())
                    || (isPassive && mm.esp.passives.isEnabled())
                    || (isItem && mm.esp.items.isEnabled());
            if (!allowEsp) continue;

            if (!mm.esp.throughWalls.isEnabled() && !canSee) continue;

            int drawCol = isFriend ? mm.esp.friendColor.getValue() : (isPlayer || isHostile ? mm.esp.enemyColor.getValue() : entityCol);
            if (mm.esp.colorByVisibility.isEnabled() && !canSee) {
                drawCol = isFriend ? 0xFF229E74 : (isPlayer ? 0xFF9E86E8 : (isHostile ? 0xFFC95361 : 0xFF589E7E));
            }

            Box bb = e.getBoundingBox().offset(ox, oy, oz).expand(0.06, 0.08, 0.06);
            if (!wts.projectBox(bb, boxRect, ptScratch)) continue;

            float bx = boxRect[0];
            float by = boxRect[1];
            float bw = Math.max(8f, boxRect[2] - bx);
            float bh = Math.max(12f, boxRect[3] - by);

            if (mm.esp.filled.isEnabled()) {
                int fillA = (int) (mm.esp.fillAlpha.asFloat() * 255);
                Render2D.drawRoundedRect(vg, bx, by, bw, bh, 3f, AetherPalette.withAlpha(drawCol, fillA));
            }

            String style = mm.esp.boxStyle.getValue();
            if ("Стекло".equals(style)) {
                // Liquid Glass box: soft inner wash + glow rim + crisp outline
                Render2D.drawGlowBox(vg, bx, by, bw, bh, 4f, 2.6f, AetherPalette.withAlpha(drawCol, 60));
                Render2D.drawRoundedRect(vg, bx, by, bw, bh, 4f, AetherPalette.withAlpha(drawCol, 26));
                Render2D.drawRoundedOutline(vg, bx, by, bw, bh, 4f, 1.35f, AetherPalette.withAlpha(drawCol, 225));
                // Top specular highlight for a glass reflection feel
                Render2D.drawRoundedRect(vg, bx + 1.5f, by + 1.5f, bw - 3f, Math.min(6f, bh * 0.16f), 3f,
                        AetherPalette.withAlpha(0xFFFFFFFF, 26));
            } else if ("Рамка".equals(style)) {
                Render2D.drawRoundedOutline(vg, bx, by, bw, bh, 2.5f, 1.35f, AetherPalette.withAlpha(drawCol, 230));
            } else if ("Точки".equals(style)) {
                float dotR = 2.4f;
                Render2D.drawCircle(vg, bx, by, dotR, drawCol);
                Render2D.drawCircle(vg, bx + bw, by, dotR, drawCol);
                Render2D.drawCircle(vg, bx, by + bh, dotR, drawCol);
                Render2D.drawCircle(vg, bx + bw, by + bh, dotR, drawCol);
            } else if ("Скобка".equals(style)) {
                float bracketLen = Math.max(4f, Math.min(12f, Math.min(bw, bh) * 0.28f));
                Render2D.drawBracketRect(vg, bx, by, bw, bh, bracketLen, 1.6f, AetherPalette.withAlpha(drawCol, 240));
            } else {
                float cornerLen = Math.max(4f, Math.min(12f, Math.min(bw, bh) * 0.28f));
                Render2D.drawCornerRect(vg, bx, by, bw, bh, cornerLen, 1.6f, AetherPalette.withAlpha(drawCol, 240));
            }

            if (mm.esp.healthBar.isEnabled() && e instanceof LivingEntity living) {
                float maxHp = Math.max(1f, living.getMaxHealth());
                float currentHp = living.getHealth();
                float ratio = Math.max(0f, Math.min(1f, currentHp / maxHp));
                float barX = bx - 4.5f;
                float fillH = Math.max(2f, bh * ratio);
                Render2D.drawRoundedRect(vg, barX, by, 2.2f, bh, 1.1f, 0x88000000);
                int hpCol = ratio > 0.55f ? 0xFF5CE092 : (ratio > 0.25f ? 0xFFFFC857 : 0xFFFF5F6D);
                Render2D.drawRoundedRect(vg, barX, by + (bh - fillH), 2.2f, fillH, 1.1f, hpCol);

                if (mm.esp.showHpText.isEnabled()) {
                    String hpStr = String.valueOf((int) Math.ceil(currentHp));
                    float hpY = by + (bh - fillH);
                    Render2D.drawTextBold(vg, hpStr, barX - 3f, hpY, 7.5f, hpCol, NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);
                }
            }

            if (isPlayer && e instanceof net.minecraft.entity.player.PlayerEntity pe) {
                float itemY = by + bh + 4f;
                if (mm.esp.showHeldItem.isEnabled()) {
                    ItemStack mainStack = pe.getMainHandStack();
                    if (!mainStack.isEmpty()) {
                        String itemName = mainStack.getName().getString();
                        int count = mainStack.getCount();
                        String itemText = count > 1 ? itemName + " x" + count : itemName;
                        Render2D.drawTextBold(vg, itemText, bx + bw * 0.5f, itemY, 8.0f, 0xFFE2E8F0, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_TOP);
                        itemY += 10f;
                    }
                }
                if (mm.esp.showArmor.isEnabled()) {
                    float armX = bx + bw + 4f;
                    float armY = by;
                    net.minecraft.entity.EquipmentSlot[] eqSlots = {
                            net.minecraft.entity.EquipmentSlot.HEAD,
                            net.minecraft.entity.EquipmentSlot.CHEST,
                            net.minecraft.entity.EquipmentSlot.LEGS,
                            net.minecraft.entity.EquipmentSlot.FEET
                    };
                    for (net.minecraft.entity.EquipmentSlot eq : eqSlots) {
                        ItemStack armStack = pe.getEquippedStack(eq);
                        if (!armStack.isEmpty()) {
                            String aName = armStack.getName().getString();
                            Render2D.drawTextBold(vg, aName, armX, armY, 7.0f, 0xFF94A3B8, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
                            armY += 8.5f;
                        }
                    }
                }
            }

            if (mm.esp.nametags.isEnabled()) {
                String label = e.getName().getString();
                if (isPlayer && e == mc.player) label = mm.nameProtect.mask(label);
                if (isFriend) label = "[F] " + label;
                float dist = (float) Math.sqrt(distSq);
                String tag = label + " " + Math.round(dist) + "m";
                float tw = Render2D.measureTextBold(vg, tag, 9.0f) + 10f;
                float tx = bx + bw * 0.5f - tw * 0.5f;
                float ty = by - 16f;
                int bgCol = isFriend ? 0xD0064E3B : AetherPalette.withAlpha(palette.surface, 175);
                Render2D.drawRoundedRect(vg, tx, ty, tw, 13f, 4.5f, bgCol);
                Render2D.drawTextBold(vg, tag, bx + bw * 0.5f, ty + 6.5f, 9.0f,
                        isFriend ? (isFriendHurt ? 0xFFFF5252 : 0xFF34D399) : palette.onSurface, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            }

            if (isFriendHurt) {
                Render2D.drawGlowBox(vg, bx - 3f, by - 3f, bw + 6f, bh + 6f, 4f, 14f, 0x88FF1744);
                Render2D.drawRoundedOutline(vg, bx - 1.5f, by - 1.5f, bw + 3f, bh + 3f, 3.5f, 1.8f, 0xFFFF1744);
                String alert = "⚠ ТИМЕЙТ АТАКОВАН!";
                float aw = Render2D.measureTextBold(vg, alert, 8.5f) + 10f;
                float ax = bx + bw * 0.5f - aw * 0.5f;
                float ay = by - (mm.esp.nametags.isEnabled() ? 32f : 16f);
                Render2D.drawRoundedRect(vg, ax, ay, aw, 13f, 4f, 0xEECC1133);
                Render2D.drawTextBold(vg, alert, bx + bw * 0.5f, ay + 6.5f, 8.5f, 0xFFFFFFFF, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            }
        }
    }

    private void renderTargetEsp(
            MinecraftClient mc,
            ModuleManager mm,
            long vg,
            AetherPalette palette,
            float tickDelta,
            float dt
    ) {
        long now = System.currentTimeMillis();
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living && living.isAlive()) {
            activeTarget = living;
            activeTargetMs = now;
        }
        if (activeTarget == null || !activeTarget.isAlive() || now - activeTargetMs > 3000L) return;

        targetSpinAngle += dt * mm.targetEsp.speed.asFloat() * 2.2f;
        Vec3d pos = activeTarget.getLerpedPos(tickDelta);
        double rad = mm.targetEsp.radius.getValue();
        double yBob = pos.y + activeTarget.getHeight() * (0.5 + 0.35 * Math.sin(targetSpinAngle * 1.15));

        if ("Призрак".equals(mm.targetEsp.style.getValue())) {
            int orbs = 10;
            for (int i = 0; i < orbs; i++) {
                double a = targetSpinAngle + (i * Math.PI * 2.0 / orbs);
                double ox = pos.x + Math.cos(a) * rad;
                double oz = pos.z + Math.sin(a) * rad;
                double oy = pos.y + activeTarget.getHeight() * 0.5 + Math.sin(a * 2.0 + targetSpinAngle) * 0.25;
                if (wts.project(ox, oy, oz, ptScratch)) {
                    float r = 4.2f - (i % 3) * 0.7f;
                    Render2D.drawCircle(vg, ptScratch[0], ptScratch[1], r + 2.5f, AetherPalette.withAlpha(palette.primary, 65));
                    Render2D.drawCircle(vg, ptScratch[0], ptScratch[1], r, i % 2 == 0 ? palette.primary : palette.tertiary);
                }
            }
        } else {
            int segs = 28;
            boolean hasPrev = false;
            float px = 0f, py = 0f;
            for (int i = 0; i <= segs; i++) {
                double a = targetSpinAngle + (i * Math.PI * 2.0 / segs);
                double wx = pos.x + Math.cos(a) * rad;
                double wz = pos.z + Math.sin(a) * rad;
                if (wts.project(wx, yBob, wz, ptScratch)) {
                    if (hasPrev) {
                        int col = AetherPalette.lerpArgb(palette.primary, palette.tertiary, (float) i / segs);
                        Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], 2.2f, AetherPalette.withAlpha(col, 230));
                    }
                    px = ptScratch[0];
                    py = ptScratch[1];
                    hasPrev = true;
                } else {
                    hasPrev = false;
                }
            }
        }
    }

    private void renderJumpCircles(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float dt) {
        boolean onGround = mc.player.isOnGround();
        double vy = mc.player.getVelocity().y;
        if (wasOnGround && !onGround && vy > 0.08) {
            JumpCircle jc = jumpCirclePool.acquire();
            jc.x = mc.player.getX();
            jc.y = mc.player.getY() + 0.04;
            jc.z = mc.player.getZ();
            jc.age = 0f;
            jc.maxAge = mm.jumpCircles.duration.asFloat();
            jc.maxRadius = mm.jumpCircles.maxRadius.asFloat();
            jumpCircles.add(jc);
            if (jumpCircles.size() > 16) jumpCirclePool.release(jumpCircles.remove(0));
        } else if (!wasOnGround && onGround && mm.jumpCircles.landingRipple.isEnabled() && prevYVelocity < -0.12) {
            JumpCircle jc = jumpCirclePool.acquire();
            jc.x = mc.player.getX();
            jc.y = mc.player.getY() + 0.04;
            jc.z = mc.player.getZ();
            jc.age = 0f;
            jc.maxAge = mm.jumpCircles.duration.asFloat() * 1.15f;
            jc.maxRadius = mm.jumpCircles.maxRadius.asFloat() * 1.25f;
            jumpCircles.add(jc);
            if (jumpCircles.size() > 16) jumpCirclePool.release(jumpCircles.remove(0));
        }
        wasOnGround = onGround;
        prevYVelocity = vy;

        String style = mm.jumpCircles.style.getValue();
        int baseCol = palette.resolvePalette(mm.jumpCircles.palette.getValue(), mm.jumpCircles.color.getValue(), globalTime, 1.0f);
        float th = mm.jumpCircles.thickness.asFloat();
        boolean glow = mm.jumpCircles.glow.isEnabled();

        Iterator<JumpCircle> it = jumpCircles.iterator();
        while (it.hasNext()) {
            JumpCircle c = it.next();
            c.age += dt;
            if (c.age >= c.maxAge) {
                it.remove();
                jumpCirclePool.release(c);
                continue;
            }
            float p = c.age / c.maxAge;
            float r = c.maxRadius * (0.22f + 0.78f * (1f - (1f - p) * (1f - p)));
            int alpha = Math.round((1f - p) * 230f);
            int segs = 30;
            boolean hasPrev = false;
            float px = 0f, py = 0f;
            for (int i = 0; i <= segs; i++) {
                double a = i * Math.PI * 2.0 / segs;
                if (wts.project(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, ptScratch)) {
                    if (hasPrev) {
                        int col = "Тема".equals(mm.jumpCircles.palette.getValue())
                                ? AetherPalette.lerpArgb(palette.primary, palette.tertiary, (float) i / segs)
                                : baseCol;
                        if ("Целестиал".equals(style)) {
                            int celCol = AetherPalette.lerpArgb(0xFF818CF8, 0xFF38BDF8, (float) i / segs);
                            if (glow) {
                                Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], th * 2.8f, AetherPalette.withAlpha(celCol, Math.round(alpha * 0.40f)));
                            }
                            Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], th * (1f - p * 0.3f), AetherPalette.withAlpha(celCol, alpha));
                        } else if ("Стекло".equals(style)) {
                            Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], th * (1f - p * 0.3f), AetherPalette.withAlpha(col, alpha));
                            Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], th * 2.4f, AetherPalette.withAlpha(0xFFFFFFFF, Math.round(alpha * 0.22f)));
                        } else if ("Импульс".equals(style)) {
                            float pulseTh = th * (1.2f + 0.8f * (float) Math.sin(p * Math.PI));
                            Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], pulseTh, AetherPalette.withAlpha(col, alpha));
                        } else if ("Сетка".equals(style)) {
                            if (i % 2 == 0) {
                                Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], th, AetherPalette.withAlpha(col, alpha));
                            }
                        } else { // "Неон"
                            if (glow) {
                                Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], th * 2.6f, AetherPalette.withAlpha(col, Math.round(alpha * 0.35f)));
                            }
                            Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], th * (1f - p * 0.35f), AetherPalette.withAlpha(col, alpha));
                        }
                    }
                    px = ptScratch[0];
                    py = ptScratch[1];
                    hasPrev = true;
                } else {
                    hasPrev = false;
                }
            }

            // Celestial Inner Ring and AETHER Lettering
            if ("Целестиал".equals(style)) {
                float rInner = r * 0.65f;
                hasPrev = false;
                for (int i = 0; i <= segs; i++) {
                    double a = i * Math.PI * 2.0 / segs;
                    if (wts.project(c.x + Math.cos(a) * rInner, c.y, c.z + Math.sin(a) * rInner, ptScratch)) {
                        if (hasPrev) {
                            int innerCol = AetherPalette.lerpArgb(0xFF38BDF8, 0xFF818CF8, (float) i / segs);
                            Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], th * 0.75f, AetherPalette.withAlpha(innerCol, Math.round(alpha * 0.75f)));
                        }
                        px = ptScratch[0];
                        py = ptScratch[1];
                        hasPrev = true;
                    } else {
                        hasPrev = false;
                    }
                }

                // Glowing expanding AETHER letters along the arc
                String[] letters = {"A", "E", "T", "H", "E", "R"};
                float rMid = r * 0.825f;
                double rotOffset = globalTime * 0.5 + c.age * 0.8;
                for (int k = 0; k < 6; k++) {
                    double la = rotOffset + (k * Math.PI * 2.0 / 6.0);
                    if (wts.project(c.x + Math.cos(la) * rMid, c.y + 0.02, c.z + Math.sin(la) * rMid, ptScratch)) {
                        float lsz = Math.max(7f, Math.min(12.5f, 9.5f * (1f - p * 0.25f)));
                        Render2D.drawCircle(vg, ptScratch[0], ptScratch[1], 2.0f, AetherPalette.withAlpha(0xFF38BDF8, Math.round(alpha * 0.8f)));
                        Render2D.drawTextBold(vg, letters[k], ptScratch[0], ptScratch[1], lsz,
                                AetherPalette.withAlpha(0xFFE0E7FF, Math.round(alpha * 0.95f)),
                                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
                    }
                }
            }
        }
    }

    private void renderTrails(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float tickDelta) {
        Vec3d p = mc.player.getLerpedPos(tickDelta);
        double ty = p.y + 0.15;
        if (!hasLastTrail || Math.hypot(p.x - lastTrailX, p.z - lastTrailZ) > 0.12 || Math.abs(ty - lastTrailY) > 0.12) {
            TrailPoint tp = trailPointPool.acquire();
            tp.x = p.x;
            tp.y = ty;
            tp.z = p.z;
            tp.timeMs = System.currentTimeMillis();
            trailPoints.addLast(tp);
            lastTrailX = p.x;
            lastTrailY = ty;
            lastTrailZ = p.z;
            hasLastTrail = true;
        }
        int maxLen = Math.max(8, (int) Math.round(mm.trails.length.getValue()));
        while (trailPoints.size() > maxLen) {
            trailPointPool.release(trailPoints.removeFirst());
        }
        long now = System.currentTimeMillis();
        long fadeMs = (long) (mm.trails.fadeTime.asFloat() * 1000f);
        while (!trailPoints.isEmpty() && now - trailPoints.peekFirst().timeMs > fadeMs) {
            trailPointPool.release(trailPoints.removeFirst());
        }

        if (trailPoints.size() < 2) return;
        float w = mm.trails.width.asFloat();
        String style = mm.trails.style.getValue();
        int baseCol = palette.resolvePalette(mm.trails.palette.getValue(), mm.trails.color.getValue(), globalTime, 1.0f);
        boolean glow = mm.trails.glow.isEnabled();
        int idx = 0;
        int total = trailPoints.size();
        boolean hasPrev = false;
        float px = 0f, py = 0f;
        for (TrailPoint tp : trailPoints) {
            float t = (float) idx / Math.max(1, total - 1);
            if (wts.project(tp.x, tp.y, tp.z, ptScratch)) {
                int col = "Тема".equals(mm.trails.palette.getValue())
                        ? AetherPalette.lerpArgb(palette.tertiary, palette.primary, t)
                        : baseCol;
                int alpha = Math.round(t * 235f);
                if (hasPrev) {
                    if ("Лента".equals(style)) {
                        Render2D.drawTaperedLine(vg, px, py, ptScratch[0], ptScratch[1],
                                w * (0.35f + 0.65f * t), w * (0.35f + 0.65f * Math.max(0f, t - 1f / Math.max(1, total - 1))),
                                AetherPalette.withAlpha(col, alpha), AetherPalette.withAlpha(col, Math.round(alpha * 0.25f)));
                    } else if ("Неон".equals(style)) {
                        if (glow) {
                            Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], w * 2.5f, AetherPalette.withAlpha(col, Math.round(alpha * 0.35f)));
                        }
                        Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], w * (0.4f + 0.6f * t), AetherPalette.withAlpha(col, alpha));
                    } else if ("Точки".equals(style)) {
                        Render2D.drawCircle(vg, ptScratch[0], ptScratch[1], w * 0.75f, AetherPalette.withAlpha(col, alpha));
                    } else { // "Стекло"
                        Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], w * (0.4f + 0.6f * t), AetherPalette.withAlpha(col, alpha));
                        Render2D.drawLine(vg, px, py, ptScratch[0], ptScratch[1], w * 0.5f, AetherPalette.withAlpha(0xFFFFFFFF, Math.round(alpha * 0.35f)));
                    }
                }
                px = ptScratch[0];
                py = ptScratch[1];
                hasPrev = true;
            } else {
                hasPrev = false;
            }
            idx++;
        }
    }

    private void renderHitSparks(ModuleManager mm, long vg, AetherPalette palette, float dt) {
        float sz = mm.hitParticles.size.asFloat();
        String shape = mm.hitParticles.shape.getValue();
        boolean gravity = mm.hitParticles.gravity.isEnabled();
        boolean glow = mm.hitParticles.glow.isEnabled();
        int baseCol = palette.resolvePalette(mm.hitParticles.palette.getValue(), mm.hitParticles.color.getValue(), globalTime, 1.0f);

        Iterator<HitSpark> it = hitSparks.iterator();
        while (it.hasNext()) {
            HitSpark s = it.next();
            s.age += dt;
            if (s.age >= s.maxAge) {
                it.remove();
                hitSparkPool.release(s);
                continue;
            }
            if (gravity) {
                s.vy -= 5.0 * dt;
            }
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.z += s.vz * dt;
            if (wts.project(s.x, s.y, s.z, ptScratch)) {
                float alpha = 1f - (s.age / s.maxAge);
                int col = "Тема".equals(mm.hitParticles.palette.getValue())
                        ? AetherPalette.lerpArgb(palette.tertiary, palette.primary, alpha)
                        : baseCol;
                float currentSz = sz * (0.6f + 0.4f * alpha);
                int colAlpha = AetherPalette.withAlpha(col, Math.round(alpha * 240f));
                float sx = ptScratch[0];
                float sy = ptScratch[1];

                if ("Звездочки".equals(shape)) {
                    Render2D.drawLine(vg, sx - currentSz, sy, sx + currentSz, sy, 1.5f, colAlpha);
                    Render2D.drawLine(vg, sx, sy - currentSz, sx, sy + currentSz, 1.5f, colAlpha);
                } else if ("Пузыри".equals(shape)) {
                    Render2D.drawRing(vg, sx, sy, currentSz, 1.2f, colAlpha);
                    Render2D.drawCircle(vg, sx - currentSz * 0.3f, sy - currentSz * 0.3f, currentSz * 0.3f, 0xFFFFFFFF);
                } else if ("Капли".equals(shape)) {
                    Render2D.drawLine(vg, sx, sy, sx, sy + currentSz * 1.6f, 1.8f, colAlpha);
                } else if ("Огонь".equals(shape)) {
                    int fireCol = AetherPalette.lerpArgb(col, 0xFFFFE57F, alpha);
                    Render2D.drawCircle(vg, sx, sy, currentSz * 1.5f, AetherPalette.withAlpha(fireCol, Math.round(alpha * 80f)));
                    Render2D.drawCircle(vg, sx, sy, currentSz, AetherPalette.withAlpha(fireCol, Math.round(alpha * 230f)));
                } else { // "Искры"
                    if (glow) {
                        Render2D.drawCircle(vg, sx, sy, currentSz * 2.2f, AetherPalette.withAlpha(col, Math.round(alpha * 70f)));
                    }
                    Render2D.drawCircle(vg, sx, sy, currentSz, colAlpha);
                }
            }
        }
    }

    private void renderCustomCrosshair(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float screenW, float screenH) {
        float cx = screenW * 0.5f;
        float cy = screenH * 0.5f;
        boolean onTarget = mm.crosshair.targetHighlight.isEnabled()
                && mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity;
        int themeCol = palette.resolvePalette(mm.crosshair.palette.getValue(), mm.crosshair.color.getValue(), globalTime, 1.0f);
        int col = onTarget ? 0xFFFF5F6D : themeCol;
        int accent = onTarget ? 0xFFFF5F6D : palette.primary;

        float sz = mm.crosshair.size.asFloat();
        float g = mm.crosshair.gap.asFloat();
        float thick = mm.crosshair.thickness.asFloat();
        String style = mm.crosshair.style.getValue();

        // Dynamic movement spread
        if ("Динамик".equals(style) && mc.player != null) {
            double spd = Math.hypot(mc.player.getX() - mc.player.lastRenderX, mc.player.getZ() - mc.player.lastRenderZ);
            g += (float) Math.min(8.0, spd * 18.0);
        }

        // Attack recoil expansion
        if (mm.crosshair.zoomOnAttack.isEnabled() && mc.player != null) {
            float cooldownProg = mc.player.getAttackCooldownProgress(0f);
            if (cooldownProg < 0.99f) {
                g += (1f - cooldownProg) * 6.5f;
            }
        }

        switch (style) {
            case "Точка" -> {
                float dotR = mm.crosshair.dotRadius.asFloat();
                Render2D.drawCircle(vg, cx, cy, dotR, accent);
                Render2D.drawRing(vg, cx, cy, dotR + 1.8f, thick, AetherPalette.withAlpha(col, 160));
            }
            case "Кольцо" -> {
                Render2D.drawRing(vg, cx, cy, sz + g * 0.5f, thick, AetherPalette.withAlpha(col, 225));
                if (mm.crosshair.centerDot.isEnabled()) {
                    Render2D.drawCircle(vg, cx, cy, mm.crosshair.dotRadius.asFloat(), accent);
                }
            }
            case "Шеврон" -> {
                Render2D.drawLine(vg, cx - sz, cy + sz * 0.6f, cx, cy, thick, col);
                Render2D.drawLine(vg, cx, cy, cx + sz, cy + sz * 0.6f, thick, col);
                if (mm.crosshair.centerDot.isEnabled()) {
                    Render2D.drawCircle(vg, cx, cy, mm.crosshair.dotRadius.asFloat(), accent);
                }
            }
            default -> {
                Render2D.drawLine(vg, cx - g - sz, cy, cx - g, cy, thick, col);
                Render2D.drawLine(vg, cx + g, cy, cx + g + sz, cy, thick, col);
                Render2D.drawLine(vg, cx, cy - g - sz, cx, cy - g, thick, col);
                Render2D.drawLine(vg, cx, cy + g, cx, cy + g + sz, thick, col);
                if (mm.crosshair.centerDot.isEnabled()) {
                    Render2D.drawCircle(vg, cx, cy, mm.crosshair.dotRadius.asFloat(), accent);
                }
            }
        }

        // Cooldown arc
        if (mm.crosshair.cooldownArc.isEnabled() && mc.player != null) {
            float prog = mc.player.getAttackCooldownProgress(0.5f);
            if (prog < 0.99f) {
                float r = sz + g + 3.5f;
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    NVGColor c = NanoVgEngine.argbToNvg(accent, NVGColor.malloc(stack));
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgArc(vg, cx, cy, r, (float) (-Math.PI * 0.5), (float) (-Math.PI * 0.5 + prog * Math.PI * 2.0), NanoVG.NVG_CW);
                    NanoVG.nvgStrokeWidth(vg, 1.6f);
                    NanoVG.nvgStrokeColor(vg, c);
                    NanoVG.nvgStroke(vg);
                }
            }
        }

        // Hitmarker animation
        if (mm.crosshair.hitmarker.isEnabled()) {
            long elapsed = System.currentTimeMillis() - hitmarkerTimeMs;
            if (elapsed < 240L) {
                float hitProg = 1f - (float) elapsed / 240f;
                float hitLen = 4.5f + (1f - hitProg) * 3f;
                float hitGap = 3.5f + (1f - hitProg) * 2.5f;
                int hitCol = AetherPalette.withAlpha(0xFFFF5F6D, Math.round(255f * hitProg));
                Render2D.drawLine(vg, cx - hitGap - hitLen, cy - hitGap - hitLen, cx - hitGap, cy - hitGap, 1.8f, hitCol);
                Render2D.drawLine(vg, cx + hitGap, cy - hitGap, cx + hitGap + hitLen, cy - hitGap - hitLen, 1.8f, hitCol);
                Render2D.drawLine(vg, cx - hitGap - hitLen, cy + hitGap + hitLen, cx - hitGap, cy + hitGap, 1.8f, hitCol);
                Render2D.drawLine(vg, cx + hitGap, cy + hitGap, cx + hitGap + hitLen, cy + hitGap + hitLen, 1.8f, hitCol);
            }
        }
    }

    private void renderDamageVignette(MinecraftClient mc, ModuleManager mm, long vg, float dt, float screenW, float screenH) {
        float hp = mc.player.getHealth();
        if (prevPlayerHealth >= 0f && hp < prevPlayerHealth - 0.1f && mm.damageVignette.onHit.isEnabled()) {
            damageFlash = 1.0f;
        }
        prevPlayerHealth = hp;
        damageFlash = Math.max(0f, damageFlash - dt * 2.4f);

        float lowRatio = 0f;
        if (mm.damageVignette.lowHp.isEnabled() && hp < 8.0f) {
            lowRatio = (8.0f - hp) / 8.0f * (0.55f + 0.45f * (float) Math.sin(globalTime * 4.5));
        }

        boolean friendHurt = false;
        if (mc.world != null && dev.aetherclient.client.friend.FriendsManager.get() != null) {
            for (PlayerEntity pl : mc.world.getPlayers()) {
                if (pl != mc.player && dev.aetherclient.client.friend.FriendsManager.get().isFriend(pl.getName().getString()) && pl.hurtTime > 0) {
                    friendHurt = true;
                    break;
                }
            }
        }

        float strength = Math.max(damageFlash, lowRatio);
        if (friendHurt) strength = Math.max(strength, 0.5f);
        strength *= mm.damageVignette.intensity.asFloat();
        if (strength > 0.02f) {
            drawEdgeVignette(vg, screenW, screenH, AetherPalette.withAlpha(friendHurt && damageFlash < 0.2f ? 0xFFFF1744 : 0xFFFF2E4C, Math.round(strength * 155f)));
        }
    }

    private void renderAmbientParticles(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float screenW, float screenH, float dt, boolean weatherSnow) {
        if (mc.player == null || !wts.isValid()) return;
        int targetCount = weatherSnow ? 180 : Math.max(6, (int) Math.round(mm.particles.count.getValue()));
        String style = mm.particles.style.getValue();
        float spd = weatherSnow ? 1.2f : mm.particles.speed.asFloat();
        boolean isSnow = weatherSnow || "Снег".equals(style);
        boolean isBokeh = !weatherSnow && "Боке".equals(style);

        while (ambientParticles.size() < targetCount) {
            AmbientParticle p = particlePool.acquire();
            p.x = mc.player.getX() + (rng.nextFloat() - 0.5f) * 26.0;
            p.y = mc.player.getY() + rng.nextFloat() * 8.0 - 1.0;
            p.z = mc.player.getZ() + (rng.nextFloat() - 0.5f) * 26.0;
            p.vx = (rng.nextFloat() - 0.5f) * 0.9f;
            p.vy = isSnow ? -(0.9f + rng.nextFloat() * 1.4f) : (0.4f + rng.nextFloat() * 0.8f);
            p.vz = (rng.nextFloat() - 0.5f) * 0.9f;
            p.size = 1.8f + rng.nextFloat() * 3.2f;
            p.phase = rng.nextFloat() * 6.28f;
            ambientParticles.add(p);
        }
        while (ambientParticles.size() > targetCount) {
            particlePool.release(ambientParticles.remove(ambientParticles.size() - 1));
        }

        for (AmbientParticle p : ambientParticles) {
            p.phase += dt * 1.6f;
            p.x += (p.vx + Math.sin(p.phase) * 0.25) * spd * dt;
            p.y += p.vy * spd * dt;
            p.z += (p.vz + Math.cos(p.phase) * 0.25) * spd * dt;

            double dx = p.x - mc.player.getX();
            double dy = p.y - mc.player.getY();
            double dz = p.z - mc.player.getZ();
            if (Math.abs(dx) > 16.0 || Math.abs(dz) > 16.0 || dy > 9.0 || dy < -3.0) {
                p.x = mc.player.getX() + (rng.nextFloat() - 0.5f) * 26.0;
                p.y = mc.player.getY() + (isSnow ? 8.5 : -1.0) + rng.nextFloat() * 2.0;
                p.z = mc.player.getZ() + (rng.nextFloat() - 0.5f) * 26.0;
            }

            if (wts.project(p.x, p.y, p.z, ptScratch)) {
                float depth = Math.max(0.8f, ptScratch[2]);
                float screenSz = Math.max(1.2f, Math.min(24f, p.size * 10f / depth));
                float pulse = 0.55f + 0.45f * (float) Math.sin(p.phase * 1.3f);
                int baseCol = isSnow ? 0xFFFFFFFF : AetherPalette.lerpArgb(palette.primary, palette.tertiary, 0.5f + 0.5f * (float) Math.sin(p.phase));
                float px = ptScratch[0];
                float py = ptScratch[1];
                if (isBokeh) {
                    Render2D.drawCircle(vg, px, py, screenSz * 2.5f, AetherPalette.withAlpha(baseCol, Math.round(pulse * 36f)));
                    Render2D.drawRing(vg, px, py, screenSz * 2.5f, 0.9f, AetherPalette.withAlpha(baseCol, Math.round(pulse * 68f)));
                } else {
                    Render2D.drawCircle(vg, px, py, screenSz * 1.6f, AetherPalette.withAlpha(baseCol, Math.round(pulse * 42f)));
                    Render2D.drawCircle(vg, px, py, screenSz, AetherPalette.withAlpha(baseCol, Math.round(pulse * 200f)));
                }
            }
        }
    }

    private void renderChams(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float tickDelta) {
        Vec3d cam = wts.getCamPos();
        float maxDistSq = mm.chams.maxDistance.asFloat() * mm.chams.maxDistance.asFloat();
        int col = palette.resolvePalette(mm.chams.palette.getValue(), mm.chams.color.getValue(), globalTime, mm.chams.colorSpeed.asFloat());
        String shape = mm.chams.shape.getValue();
        float thickness = mm.chams.thickness.asFloat();
        int fillAlpha = Math.round(mm.chams.opacity.asFloat() * 255f);
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !e.isAlive()) continue;
            boolean isPlayer = e instanceof PlayerEntity;
            boolean isHostile = e instanceof HostileEntity;
            boolean isPassive = e instanceof PassiveEntity;
            boolean allow = (isPlayer && mm.chams.players.isEnabled())
                    || (isHostile && mm.chams.hostiles.isEnabled())
                    || (isPassive && mm.chams.passives.isEnabled());
            if (!allow) continue;

            boolean canSee = canSeeEntity(mc, e);
            if (!mm.chams.throughWalls.isEnabled() && !canSee) continue;

            int drawCol = col;
            if (mm.chams.colorByVis.isEnabled() && !canSee) {
                drawCol = isPlayer ? 0xFF9E86E8 : (isHostile ? 0xFFC95361 : 0xFF589E7E);
            }

            double dx = e.getX() - cam.x;
            double dy = e.getY() - cam.y;
            double dz = e.getZ() - cam.z;
            if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;
            Vec3d lerped = e.getLerpedPos(tickDelta);
            double ox = lerped.x - e.getX();
            double oy = lerped.y - e.getY();
            double oz = lerped.z - e.getZ();
            Box bb = e.getBoundingBox().offset(ox, oy, oz).expand(0.05, 0.06, 0.05);
            if (!wts.projectBox(bb, boxRect, ptScratch)) continue;
            float bx = boxRect[0];
            float by = boxRect[1];
            float bw = Math.max(8f, boxRect[2] - bx);
            float bh = Math.max(12f, boxRect[3] - by);

            // Fresnel-like depth treatment: farther entities get a softer fill and
            // a brighter rim, simulating view-angle rim lighting.
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float depthT = Math.min(1f, dist / Math.max(1f, mm.chams.maxDistance.asFloat()));
            float fillA = fillAlpha * (1f - 0.45f * depthT);
            float rimA = 200f + 55f * depthT;

            if (fillA > 1f) {
                Render2D.drawRoundedRect(vg, bx, by, bw, bh, 4f, AetherPalette.withAlpha(drawCol, Math.round(fillA)));
            }
            // Soft glow halo behind the rim
            Render2D.drawGlowBox(vg, bx, by, bw, bh, 4f, thickness * 1.6f, AetherPalette.withAlpha(drawCol, Math.round(42f * (1f - depthT * 0.5f))));
            drawShapeOutline(vg, shape, bx, by, bw, bh, thickness, AetherPalette.withAlpha(drawCol, Math.round(rimA)));
            // Specular top edge
            Render2D.drawRoundedRect(vg, bx + 1.5f, by + 1.5f, bw - 3f, Math.min(5f, bh * 0.12f), 2.5f,
                    AetherPalette.withAlpha(0xFFFFFFFF, Math.round(22f * (1f - depthT * 0.4f))));
        }
    }

    private void renderBreadcrumbs(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float tickDelta) {
        Vec3d p = mc.player.getLerpedPos(tickDelta);
        double ty = p.y + 0.08;
        if (!hasLastCrumb || Math.hypot(p.x - lastCrumbX, p.z - lastCrumbZ) > 0.35) {
            BreadcrumbPoint b = breadcrumbPool.acquire();
            b.x = p.x;
            b.y = ty;
            b.z = p.z;
            b.timeMs = System.currentTimeMillis();
            breadcrumbPoints.addLast(b);
            lastCrumbX = p.x;
            lastCrumbY = ty;
            lastCrumbZ = p.z;
            hasLastCrumb = true;
        }
        int maxPts = Math.max(20, (int) Math.round(mm.breadcrumbs.maxPoints.getValue()));
        while (breadcrumbPoints.size() > maxPts) {
            breadcrumbPool.release(breadcrumbPoints.removeFirst());
        }
        long now = System.currentTimeMillis();
        long fadeMs = (long) (mm.breadcrumbs.fadeSeconds.asFloat() * 1000f);
        while (!breadcrumbPoints.isEmpty() && now - breadcrumbPoints.peekFirst().timeMs > fadeMs) {
            breadcrumbPool.release(breadcrumbPoints.removeFirst());
        }
        if (breadcrumbPoints.size() < 2) return;
        int col = palette.resolvePalette(mm.breadcrumbs.palette.getValue(), mm.breadcrumbs.color.getValue(), globalTime, mm.breadcrumbs.colorSpeed.asFloat());
        float w = mm.breadcrumbs.width.asFloat();
        boolean ribbon = "Лента".equals(mm.breadcrumbs.style.getValue());
        int idx = 0;
        int total = breadcrumbPoints.size();
        boolean prev = false;
        float px0 = 0f;
        float py0 = 0f;
        for (BreadcrumbPoint b : breadcrumbPoints) {
            float t = (float) idx / Math.max(1, total - 1);
            if (wts.project(b.x, b.y, b.z, ptScratch)) {
                int a = Math.round((1f - t * 0.55f) * 230f);
                if (ribbon) {
                    if (prev) {
                        // Liquid-glass ribbon: tapered core + soft under-glow
                        float wHead = w * (0.4f + 0.6f * t);
                        float wTail = w * (0.4f + 0.6f * Math.max(0f, t - 1f / Math.max(1, total - 1)));
                        Render2D.drawTaperedLine(vg, px0, py0, ptScratch[0], ptScratch[1], wHead, wTail,
                                AetherPalette.withAlpha(col, a), AetherPalette.withAlpha(col, Math.round(a * 0.22f)));
                    }
                } else {
                    float r = w * (0.7f + 0.8f * t);
                    Render2D.drawCircle(vg, ptScratch[0], ptScratch[1], r * 2.1f, AetherPalette.withAlpha(col, Math.round(a * 0.18f)));
                    Render2D.drawCircle(vg, ptScratch[0], ptScratch[1], r, AetherPalette.withAlpha(col, a));
                }
                px0 = ptScratch[0];
                py0 = ptScratch[1];
                prev = true;
            } else {
                prev = false;
            }
            idx++;
        }
    }

    private void renderProjectiles(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette) {
        if (mc.player == null || mc.world == null) return;

        ItemStack main = mc.player.getMainHandStack();
        ItemStack off = mc.player.getOffHandStack();
        ItemStack held = !main.isEmpty() ? main : off;
        Item item = held.getItem();
        boolean isBow = item instanceof BowItem;
        boolean isCrossbow = item instanceof CrossbowItem;
        boolean isPearl = item == Items.ENDER_PEARL;
        boolean isSnowball = item == Items.SNOWBALL;
        boolean isEgg = item == Items.EGG;
        boolean isTrident = item instanceof TridentItem;
        boolean isPotion = item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
        boolean hasProjectile = isBow || isCrossbow || isPearl || isSnowball || isEgg || isTrident || isPotion;

        int col = palette.resolvePalette(mm.projectiles.palette.getValue(), mm.projectiles.color.getValue(), globalTime, mm.projectiles.colorSpeed.asFloat());
        float thickness = mm.projectiles.thickness.asFloat();

        if (!mm.projectiles.onlyHeld.isEnabled() || hasProjectile) {
            double initialSpeed;
            double gravity;
            if (isBow) {
                int useTicks = mc.player.getItemUseTime();
                float pull = BowItem.getPullProgress(useTicks);
                if (pull < 0.15f) pull = 1.0f;
                initialSpeed = pull * 3.0;
                gravity = 0.05000000074505806;
            } else if (isCrossbow) {
                initialSpeed = 3.15;
                gravity = 0.05;
            } else if (isPearl || isSnowball || isEgg) {
                initialSpeed = 1.5;
                gravity = 0.03;
            } else if (isTrident) {
                initialSpeed = 2.5;
                gravity = 0.05;
            } else if (isPotion) {
                initialSpeed = 0.55;
                gravity = 0.05;
            } else {
                initialSpeed = 2.0;
                gravity = 0.04;
            }

            final double drag = 0.9900000095367432;
            gravity *= mm.projectiles.gravityScale.asFloat();

            double yawRad = Math.toRadians(mc.player.getYaw());
            double pitchRad = Math.toRadians(mc.player.getPitch());
            double cosP = Math.cos(pitchRad);
            double dirX = -Math.sin(yawRad) * cosP;
            double dirY = -Math.sin(pitchRad);
            double dirZ = Math.cos(yawRad) * cosP;

            Vec3d eye = mc.player.getEyePos();
            double px = eye.x;
            double py = eye.y;
            double pz = eye.z;
            double vx = dirX * initialSpeed;
            double vy = dirY * initialSpeed;
            double vz = dirZ * initialSpeed;

            int dots = Math.max(16, (int) Math.round(mm.projectiles.dots.getValue()));
            boolean prev = false;
            float sx0 = 0f;
            float sy0 = 0f;
            float lastX = 0f;
            float lastY = 0f;
            boolean hasImpact = false;

            for (int i = 0; i < dots; i++) {
                double nextX = px + vx;
                double nextY = py + vy;
                double nextZ = pz + vz;

                Vec3d from = new Vec3d(px, py, pz);
                Vec3d to = new Vec3d(nextX, nextY, nextZ);
                RaycastContext ctx = new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
                BlockHitResult hit = mc.world.raycast(ctx);
                boolean collided = hit.getType() != net.minecraft.util.hit.HitResult.Type.MISS;
                if (collided) {
                    Vec3d hitPos = hit.getPos();
                    nextX = hitPos.x;
                    nextY = hitPos.y;
                    nextZ = hitPos.z;
                }

                px = nextX;
                py = nextY;
                pz = nextZ;
                vy -= gravity;
                vx *= drag;
                vy *= drag;
                vz *= drag;

                if (wts.project(px, py, pz, ptScratch)) {
                    float t = (float) i / dots;
                    float alpha = 255f * (1f - t * 0.5f);
                    if (prev) {
                        Render2D.drawTaperedLine(vg, sx0, sy0, ptScratch[0], ptScratch[1],
                                thickness * (1f - t * 0.5f), thickness * (1f - (t + 1f / dots) * 0.5f),
                                AetherPalette.withAlpha(col, Math.round(alpha)),
                                AetherPalette.withAlpha(col, Math.round(alpha * 0.2f)));
                    }
                    Render2D.drawCircle(vg, ptScratch[0], ptScratch[1], 1.6f + (1f - t) * 0.8f, AetherPalette.withAlpha(col, Math.round(alpha)));
                    sx0 = ptScratch[0];
                    sy0 = ptScratch[1];
                    lastX = ptScratch[0];
                    lastY = ptScratch[1];
                    prev = true;
                    hasImpact = true;
                } else {
                    prev = false;
                }

                if (collided) break;
            }

            if (hasImpact) {
                float pulse = 0.75f + 0.25f * (float) Math.sin(globalTime * 5.0);
                Render2D.drawRing(vg, lastX, lastY, 5.5f * pulse, 1.3f, AetherPalette.withAlpha(col, 150));
                Render2D.drawCircle(vg, lastX, lastY, 2.2f, AetherPalette.withAlpha(col, 210));
                if (mm.projectiles.hitLandingCircle.isEnabled()) {
                    float landR = mm.projectiles.landingRadius.asFloat();
                    drawHolographicGroundDisc(vg, px, py, pz, landR, 1.8f, col, true, 30);
                }
                if (mm.projectiles.showDistance.isEnabled()) {
                    String distStr = String.format(java.util.Locale.ROOT, "%.1fm", eye.distanceTo(new Vec3d(px, py, pz)));
                    Render2D.drawTextBold(vg, distStr, lastX, lastY - 9f, 9.2f, AetherPalette.withAlpha(0xFFFFFFFF, 220), NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
                }
            }
        }

        if (mm.projectiles.trackInFlight.isEnabled()) {
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof ProjectileEntity proj) || !proj.isAlive()) continue;
                Vec3d vel = proj.getVelocity();
                if (vel.lengthSquared() < 0.01) continue;

                Vec3d pos = new Vec3d(proj.getX(), proj.getY(), proj.getZ());
                double tpx = pos.x;
                double tpy = pos.y;
                double tpz = pos.z;
                double tvx = vel.x;
                double tvy = vel.y;
                double tvz = vel.z;

                int pCol = (proj.getOwner() == mc.player) ? 0xFF38BDF8 : 0xFFF43F5E;
                boolean pPrev = false;
                float psx0 = 0f, psy0 = 0f;
                float plastX = 0f, plastY = 0f;
                boolean pImpact = false;

                for (int s = 0; s < 24; s++) {
                    tpx += tvx;
                    tpy += tvy;
                    tpz += tvz;
                    tvy -= 0.035;
                    tvx *= 0.99;
                    tvy *= 0.99;
                    tvz *= 0.99;
                    if (wts.project(tpx, tpy, tpz, ptScratch)) {
                        if (pPrev) {
                            Render2D.drawLine(vg, psx0, psy0, ptScratch[0], ptScratch[1], 1.5f, AetherPalette.withAlpha(pCol, 180));
                        }
                        psx0 = ptScratch[0];
                        psy0 = ptScratch[1];
                        plastX = ptScratch[0];
                        plastY = ptScratch[1];
                        pPrev = true;
                        pImpact = true;
                    } else {
                        pPrev = false;
                    }
                }
                if (pImpact) {
                    Render2D.drawRing(vg, plastX, plastY, 4.0f, 1.2f, AetherPalette.withAlpha(pCol, 200));
                    Render2D.drawCircle(vg, plastX, plastY, 1.8f, pCol);
                    if (mm.projectiles.showDistance.isEnabled()) {
                        String name = (proj.getOwner() == mc.player) ? "Свой" : "Враг";
                        Render2D.drawTextBold(vg, name, plastX, plastY - 8f, 8.5f, AetherPalette.withAlpha(pCol, 240), NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
                    }
                }
            }
        }
    }

    private void renderItemEsp(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float tickDelta) {
        Vec3d cam = wts.getCamPos();
        float maxDistSq = mm.itemEsp.maxDistance.asFloat() * mm.itemEsp.maxDistance.asFloat();
        int col = palette.resolvePalette(mm.itemEsp.palette.getValue(), mm.itemEsp.color.getValue(), globalTime, mm.itemEsp.colorSpeed.asFloat());
        String shape = mm.itemEsp.shape.getValue();
        float thickness = mm.itemEsp.thickness.asFloat();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ItemEntity item)) continue;
            if (!mm.itemEsp.throughWalls.isEnabled() && !canSeeEntity(mc, item)) continue;
            double dx = item.getX() - cam.x;
            double dy = item.getY() - cam.y;
            double dz = item.getZ() - cam.z;
            if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;
            Vec3d lerped = item.getLerpedPos(tickDelta);
            Box bb = new Box(lerped.x - 0.24, lerped.y, lerped.z - 0.24, lerped.x + 0.24, lerped.y + 0.48, lerped.z + 0.24);
            if (!wts.projectBox(bb, boxRect, ptScratch)) continue;
            float bx = boxRect[0];
            float by = boxRect[1];
            float bw = Math.max(6f, boxRect[2] - bx);
            float bh = Math.max(6f, boxRect[3] - by);
            if (mm.itemEsp.glow.isEnabled()) {
                // Liquid-glass glow: soft halo + wash + specular top edge
                Render2D.drawGlowBox(vg, bx, by, bw, bh, 3f, thickness * 1.5f, AetherPalette.withAlpha(col, 55));
                Render2D.drawRoundedRect(vg, bx, by, bw, bh, 3f, AetherPalette.withAlpha(col, 22));
                Render2D.drawRoundedRect(vg, bx + 1f, by + 1f, bw - 2f, Math.min(4f, bh * 0.18f), 2f,
                        AetherPalette.withAlpha(0xFFFFFFFF, 22));
            }
            drawShapeOutline(vg, shape, bx, by, bw, bh, thickness, AetherPalette.withAlpha(col, 225));
            if (mm.itemEsp.showName.isEnabled() || mm.itemEsp.showCount.isEnabled()) {
                ItemStack stack = item.getStack();
                String label = "";
                if (mm.itemEsp.showName.isEnabled()) label = stack.getName().getString();
                if (mm.itemEsp.showCount.isEnabled() && stack.getCount() > 1) label += " x" + stack.getCount();
                if (!label.isEmpty()) {
                    float tw = Render2D.measureTextBold(vg, label, 8.6f) + 10f;
                    float tx = bx + bw * 0.5f - tw * 0.5f;
                    float ty = by - 13f;
                    Render2D.drawRoundedRect(vg, tx, ty, tw, 12f, 4f, AetherPalette.withAlpha(palette.surface, 185));
                    Render2D.drawTextBold(vg, label, bx + bw * 0.5f, ty + 6f, 8.6f, palette.onSurface,
                            NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
                }
            }
        }
    }

    private void render3DBox(
            long vg,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            int lineCol, float thickness,
            int fillCol, boolean isCorners, float cornerRatio
    ) {
        double[] xs = {minX, maxX, maxX, minX, minX, maxX, maxX, minX};
        double[] ys = {minY, minY, minY, minY, maxY, maxY, maxY, maxY};
        double[] zs = {minZ, minZ, maxZ, maxZ, minZ, minZ, maxZ, maxZ};

        int visibleCount = 0;
        for (int i = 0; i < 8; i++) {
            boxCornersVis[i] = wts.project(xs[i], ys[i], zs[i], boxCornersScratch[i]);
            if (boxCornersVis[i]) visibleCount++;
        }
        if (visibleCount < 2) return;

        int fillAlpha = (fillCol >>> 24) & 0xFF;
        if (fillAlpha > 2) {
            for (int[] face : CUBE_FACES) {
                if (boxCornersVis[face[0]] && boxCornersVis[face[1]] && boxCornersVis[face[2]] && boxCornersVis[face[3]]) {
                    for (int fi = 0; fi < 4; fi++) {
                        polyScratchX[fi] = boxCornersScratch[face[fi]][0];
                        polyScratchY[fi] = boxCornersScratch[face[fi]][1];
                    }
                    Render2D.drawPolygon(vg, polyScratchX, polyScratchY, 4, fillCol);
                }
            }
        }

        int lineAlpha = (lineCol >>> 24) & 0xFF;
        if (lineAlpha > 2) {
            for (int[] edge : CUBE_EDGES) {
                int i0 = edge[0];
                int i1 = edge[1];
                if (boxCornersVis[i0] && boxCornersVis[i1]) {
                    float x0 = boxCornersScratch[i0][0];
                    float y0 = boxCornersScratch[i0][1];
                    float x1 = boxCornersScratch[i1][0];
                    float y1 = boxCornersScratch[i1][1];
                    if (isCorners) {
                        float cr = Math.max(0.12f, Math.min(0.48f, cornerRatio));
                        float mx0 = x0 + (x1 - x0) * cr;
                        float my0 = y0 + (y1 - y0) * cr;
                        float mx1 = x1 + (x0 - x1) * cr;
                        float my1 = y1 + (y0 - y1) * cr;
                        Render2D.drawLine(vg, x0, y0, mx0, my0, thickness, lineCol);
                        Render2D.drawLine(vg, x1, y1, mx1, my1, thickness, lineCol);
                    } else {
                        Render2D.drawLine(vg, x0, y0, x1, y1, thickness, lineCol);
                    }
                }
            }
        }
    }

    private void renderBlockOverlay(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float screenW, float screenH, float dt) {
        net.minecraft.util.hit.HitResult target = mc.crosshairTarget;
        boolean hasBlock = target instanceof BlockHitResult hit && !mc.world.getBlockState(hit.getBlockPos()).isAir();
        BlockHitResult blockHit = hasBlock ? (BlockHitResult) target : null;
        BlockPos currentPos = blockHit != null ? blockHit.getBlockPos() : null;

        boolean canSee = true;
        if (blockHit != null && !mm.blockOverlay.throughWalls.isEnabled()) {
            Vec3d hitPos = blockHit.getPos();
            canSee = canSeePos(mc, hitPos.x, hitPos.y, hitPos.z);
        }

        if (hasBlock && canSee) {
            smoothBlockAlpha = Math.min(1f, smoothBlockAlpha + dt * 6.5f);
            Box targetBox = new Box(currentPos.getX(), currentPos.getY(), currentPos.getZ(),
                    currentPos.getX() + 1.0, currentPos.getY() + 1.0, currentPos.getZ() + 1.0);
            double targetCx = (targetBox.minX + targetBox.maxX) * 0.5;
            double targetCy = (targetBox.minY + targetBox.maxY) * 0.5;
            double targetCz = (targetBox.minZ + targetBox.maxZ) * 0.5;
            double targetW = targetBox.maxX - targetBox.minX;
            double targetH = targetBox.maxY - targetBox.minY;
            double targetD = targetBox.maxZ - targetBox.minZ;

            if (Double.isNaN(smoothBlockX) || !mm.blockOverlay.smoothTransition.isEnabled()) {
                smoothBlockX = targetCx;
                smoothBlockY = targetCy;
                smoothBlockZ = targetCz;
                smoothBlockW = targetW;
                smoothBlockH = targetH;
                smoothBlockD = targetD;
            } else {
                float speed = mm.blockOverlay.transitionSpeed.asFloat();
                float factor = Math.min(1f, dt * speed);
                smoothBlockX += (targetCx - smoothBlockX) * factor;
                smoothBlockY += (targetCy - smoothBlockY) * factor;
                smoothBlockZ += (targetCz - smoothBlockZ) * factor;
                smoothBlockW += (targetW - smoothBlockW) * factor;
                smoothBlockH += (targetH - smoothBlockH) * factor;
                smoothBlockD += (targetD - smoothBlockD) * factor;
            }
        } else {
            smoothBlockAlpha = Math.max(0f, smoothBlockAlpha - dt * 6.5f);
            if (smoothBlockAlpha <= 0.001f) {
                smoothBlockX = Double.NaN;
            }
        }

        if (smoothBlockAlpha > 0.01f && !Double.isNaN(smoothBlockX)) {
            double bMinX = smoothBlockX - smoothBlockW * 0.5;
            double bMinY = smoothBlockY - smoothBlockH * 0.5;
            double bMinZ = smoothBlockZ - smoothBlockD * 0.5;
            double bMaxX = smoothBlockX + smoothBlockW * 0.5;
            double bMaxY = smoothBlockY + smoothBlockH * 0.5;
            double bMaxZ = smoothBlockZ + smoothBlockD * 0.5;

            int baseCol = palette.resolvePalette(mm.blockOverlay.palette.getValue(), mm.blockOverlay.color.getValue(), globalTime, mm.blockOverlay.colorSpeed.asFloat());
            int alphaCol = AetherPalette.withAlpha(baseCol, Math.round(255f * smoothBlockAlpha));
            int fillCol = 0;
            if (mm.blockOverlay.fill.isEnabled()) {
                float fillA = mm.blockOverlay.fillAlpha.asFloat() * smoothBlockAlpha;
                fillCol = AetherPalette.withAlpha(baseCol, Math.round(fillA));
            }

            boolean isCorners = "Углы".equals(mm.blockOverlay.shape.getValue());
            float cornerRatio = mm.blockOverlay.cornerSize.asFloat();
            float thickness = mm.blockOverlay.thickness.asFloat();

            render3DBox(vg, bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ, alphaCol, thickness, fillCol, isCorners, cornerRatio);
        }

        if (mm.blockOverlay.showBreak.isEnabled()) {
            float progress = readBreakProgress(mc);
            if (progress > 0.001f) {
                int baseCol = palette.resolvePalette(mm.blockOverlay.palette.getValue(), mm.blockOverlay.color.getValue(), globalTime, mm.blockOverlay.colorSpeed.asFloat());
                float rad = mm.blockOverlay.breakRadius.asFloat();
                Render2D.drawArc(vg, screenW * 0.5f, screenH * 0.5f, rad, 4.2f,
                        (float) (-Math.PI * 0.5), (float) (Math.PI * 2.0), AetherPalette.withAlpha(0xFF000000, 90));
                Render2D.drawArc(vg, screenW * 0.5f, screenH * 0.5f, rad, 3f,
                        (float) (-Math.PI * 0.5), (float) (progress * Math.PI * 2.0), baseCol);
                float endA = (float) (-Math.PI * 0.5 + progress * Math.PI * 2.0);
                Render2D.drawCircle(vg, screenW * 0.5f + (float) Math.cos(endA) * rad,
                        screenH * 0.5f + (float) Math.sin(endA) * rad, 3.4f, baseCol);
            }
        }
    }

    public void renderAspectRatioBars(ModuleManager mm, long vg, AetherPalette palette, float screenW, float screenH, float dt) {
        renderAspectRatio(mm, vg, palette, screenW, screenH, dt);
    }

    private void renderAspectRatio(ModuleManager mm, long vg, AetherPalette palette, float screenW, float screenH, float dt) {
        float currentRatio = screenW / Math.max(1f, screenH);
        float targetRatio = mm.aspectRatio.getTargetRatio();
        if (Float.isNaN(smoothAspect) || !mm.aspectRatio.smoothTransition.isEnabled()) {
            smoothAspect = targetRatio;
        } else {
            float speed = mm.aspectRatio.transitionSpeed.asFloat();
            smoothAspect += (targetRatio - smoothAspect) * Math.min(1f, dt * speed);
        }

        if (Math.abs(currentRatio - smoothAspect) < 0.01f) return;

        float barAlpha = mm.aspectRatio.barOpacity.asFloat();
        int barCol = AetherPalette.withAlpha(0xFF000000, Math.round(255f * barAlpha));
        int rimCol = AetherPalette.withAlpha(palette.primary, Math.round(180f * barAlpha));

        if (smoothAspect > currentRatio) {
            float visibleH = screenW / smoothAspect;
            float barH = (screenH - visibleH) * 0.5f;
            if (barH > 0.5f) {
                Render2D.drawRoundedRect(vg, 0f, 0f, screenW, barH, 0f, barCol);
                if (mm.aspectRatio.frostedBorders.isEnabled()) {
                    Render2D.drawLine(vg, 0, barH, screenW, barH, 1.5f, rimCol);
                    Render2D.drawGlowBox(vg, 0, barH - 2f, screenW, 4f, 1f, 3f, AetherPalette.withAlpha(palette.primary, Math.round(40f * barAlpha)));
                }
                Render2D.drawRoundedRect(vg, 0f, screenH - barH, screenW, barH, 0f, barCol);
                if (mm.aspectRatio.frostedBorders.isEnabled()) {
                    Render2D.drawLine(vg, 0, screenH - barH, screenW, screenH - barH, 1.5f, rimCol);
                    Render2D.drawGlowBox(vg, 0, screenH - barH - 2f, screenW, 4f, 1f, 3f, AetherPalette.withAlpha(palette.primary, Math.round(40f * barAlpha)));
                }
            }
        } else {
            float visibleW = screenH * smoothAspect;
            float barW = (screenW - visibleW) * 0.5f;
            if (barW > 0.5f) {
                Render2D.drawRoundedRect(vg, 0f, 0f, barW, screenH, 0f, barCol);
                if (mm.aspectRatio.frostedBorders.isEnabled()) {
                    Render2D.drawLine(vg, barW, 0, barW, screenH, 1.5f, rimCol);
                    Render2D.drawGlowBox(vg, barW - 2f, 0, 4f, screenH, 1f, 3f, AetherPalette.withAlpha(palette.primary, Math.round(40f * barAlpha)));
                }
                Render2D.drawRoundedRect(vg, screenW - barW, 0f, barW, screenH, 0f, barCol);
                if (mm.aspectRatio.frostedBorders.isEnabled()) {
                    Render2D.drawLine(vg, screenW - barW, 0, screenW - barW, screenH, 1.5f, rimCol);
                    Render2D.drawGlowBox(vg, screenW - barW - 2f, 0, 4f, screenH, 1f, 3f, AetherPalette.withAlpha(palette.primary, Math.round(40f * barAlpha)));
                }
            }
        }
    }

    private void renderItemRadius(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float tickDelta) {
        if (mc.player == null || mc.world == null) return;
        String mode = mm.itemRadius.target.getValue();
        float baseR = mm.itemRadius.radius.asFloat();
        float pulse = mm.itemRadius.pulsate.isEnabled()
                ? 1f + 0.08f * (float) Math.sin(globalTime * mm.itemRadius.pulseSpeed.asFloat() * 3.0)
                : 1f;
        float r = baseR * pulse;
        int col = palette.resolvePalette(mm.itemRadius.palette.getValue(), mm.itemRadius.color.getValue(), globalTime, 1.0f);
        float thick = mm.itemRadius.thickness.asFloat();
        boolean fill = mm.itemRadius.fill.isEnabled();
        int fillA = Math.round(mm.itemRadius.fillAlpha.asFloat());

        if ("Игрок".equals(mode) || "Оба".equals(mode)) {
            Vec3d pp = mc.player.getLerpedPos(tickDelta);
            drawHolographicGroundDisc(vg, pp.x, pp.y + 0.02, pp.z, r, thick, col, fill, fillA);
        }

        if ("Предметы".equals(mode) || "Оба".equals(mode)) {
            boolean throughWalls = mm.itemRadius.throughWalls.isEnabled();
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof ItemEntity item)) continue;
                if (!throughWalls && !mc.player.canSee(item)) continue;
                if (mc.player.squaredDistanceTo(item) > 256.0) continue;

                Vec3d ip = item.getLerpedPos(tickDelta);
                float itemR = 0.85f * pulse;
                drawHolographicGroundDisc(vg, ip.x, ip.y + 0.02, ip.z, itemR, thick * 0.85f, col, fill, Math.round(fillA * 0.7f));

                if (mm.itemRadius.showItemCount.isEnabled() && wts.project(ip.x, ip.y + 0.55, ip.z, ptScratch)) {
                    ItemStack stack = item.getStack();
                    String badge = stack.getName().getString();
                    if (stack.getCount() > 1) badge += " ×" + stack.getCount();
                    float bw = Render2D.measureText(vg, badge, 8.5f) + 12f;
                    float bh = 15f;
                    float bx = ptScratch[0] - bw * 0.5f;
                    float by = ptScratch[1] - bh * 0.5f;
                    Render2D.drawGlowBox(vg, bx, by, bw, bh, 4f, 2f, AetherPalette.withAlpha(col, 50));
                    Render2D.drawRoundedRect(vg, bx, by, bw, bh, 4f, AetherPalette.withAlpha(palette.surface, 215));
                    Render2D.drawRoundedOutline(vg, bx, by, bw, bh, 4f, 1.2f, AetherPalette.withAlpha(col, 190));
                    Render2D.drawText(vg, badge, ptScratch[0], by + bh * 0.5f, 8.5f, palette.onSurface,
                            NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
                }
            }
        }
    }

    private void drawHolographicGroundDisc(long vg, double cx, double cy, double cz, float radius, float thickness, int col, boolean fill, int fillAlpha) {
        int segments = 32;
        boolean hasPrev = false;
        float prevX = 0f, prevY = 0f;
        float firstX = 0f, firstY = 0f;

        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0) * i / segments;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            if (!wts.project(px, cy, pz, ptScratch)) {
                hasPrev = false;
                continue;
            }
            if (hasPrev) {
                Render2D.drawLine(vg, prevX, prevY, ptScratch[0], ptScratch[1], thickness, col);
            } else if (i == 0) {
                firstX = ptScratch[0];
                firstY = ptScratch[1];
            }
            prevX = ptScratch[0];
            prevY = ptScratch[1];
            hasPrev = true;
        }
        if (hasPrev && firstX != 0f) {
            Render2D.drawLine(vg, prevX, prevY, firstX, firstY, thickness, col);
        }

        if (fill && fillAlpha > 2 && wts.project(cx, cy, cz, ptScratch)) {
            Render2D.drawCircle(vg, ptScratch[0], ptScratch[1], radius * 5.0f, AetherPalette.withAlpha(col, fillAlpha));
        }
    }

    private void renderHoleEsp(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float tickDelta) {
        int r = (int) Math.round(mm.holeEsp.radius.getValue());
        BlockPos p = mc.player.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();
        int col = palette.resolvePalette(mm.holeEsp.palette.getValue(), mm.holeEsp.color.getValue(), globalTime, mm.holeEsp.colorSpeed.asFloat());
        float thickness = mm.holeEsp.thickness.asFloat();
        String shape = mm.holeEsp.shape.getValue();
        boolean bedrock = mm.holeEsp.bedrockOnly.isEnabled();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -3; dy <= 1; dy++) {
                    int bx = p.getX() + dx;
                    int by = p.getY() + dy;
                    int bz = p.getZ() + dz;
                    if (!isSafeHole(mc, m, bx, by, bz, bedrock)) continue;
                    if (!mm.holeEsp.throughWalls.isEnabled() && !canSeePos(mc, bx + 1.0, by + 1.5, bz + 1.0)) continue;
                    int lineCol = AetherPalette.withAlpha(col, 220);
                    int fillCol = AetherPalette.withAlpha(col, 36);
                    boolean isCorners = "Углы".equals(shape);
                    render3DBox(vg, bx, by, bz, bx + 1.0, by + 1.0, bz + 1.0, lineCol, thickness, fillCol, isCorners, 0.35f);
                }
            }
        }
    }

    private boolean canSeeEntity(MinecraftClient mc, Entity e) {
        if (mc.player == null || mc.world == null || e == null) return false;
        Vec3d cam = wts != null && wts.isValid() ? wts.getCamPos() : mc.player.getEyePos();
        // 1. Eye position
        Vec3d eye = e.getEyePos();
        if (hasLineOfSight(mc, cam, eye)) return true;
        // 2. Center/chest position
        Vec3d center = new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ());
        if (hasLineOfSight(mc, cam, center)) return true;
        // 3. Feet position
        Vec3d feet = new Vec3d(e.getX(), e.getY() + 0.15, e.getZ());
        return hasLineOfSight(mc, cam, feet);
    }

    private boolean hasLineOfSight(MinecraftClient mc, Vec3d from, Vec3d to) {
        if (from.squaredDistanceTo(to) < 0.1) return true;
        net.minecraft.world.RaycastContext ctx = new net.minecraft.world.RaycastContext(
                from, to,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        );
        BlockHitResult hit = mc.world.raycast(ctx);
        if (hit.getType() == net.minecraft.util.hit.HitResult.Type.MISS) return true;
        return hit.getPos().squaredDistanceTo(to) <= 0.36;
    }

    private boolean canSeePos(MinecraftClient mc, double x, double y, double z) {
        if (mc.player == null || mc.world == null) return false;
        Vec3d eye = wts != null && wts.isValid() ? wts.getCamPos() : mc.player.getEyePos();
        Vec3d target = new Vec3d(x, y, z);
        return hasLineOfSight(mc, eye, target);
    }

    private boolean isSafeHole(MinecraftClient mc, BlockPos.Mutable m, int x, int y, int z, boolean bedrockOnly) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                m.set(x + dx, y, z + dz);
                BlockState floor = mc.world.getBlockState(m);
                boolean solid = bedrockOnly
                        ? (floor.getBlock() == Blocks.BEDROCK || floor.getBlock() == Blocks.OBSIDIAN)
                        : floor.isFullCube(mc.world, m);
                if (!solid) return false;
                for (int dy = 1; dy <= 2; dy++) {
                    m.set(x + dx, y + dy, z + dz);
                    if (!mc.world.getBlockState(m).isAir()) return false;
                }
            }
        }
        return true;
    }

    private void renderHitTints(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float tickDelta) {
        // Hit color is applied directly to the 3D entity models via HitColorApplier (NativeImage OverlayTexture).
        // 2D bounding boxes and frames are intentionally omitted to only recolor the entity model.
    }

    private void renderCosmetics(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float tickDelta) {
        if (mc.player == null || mc.world == null) return;

        // 1. Render on local player
        renderPlayerCosmetics(mc.player, mc, mm, vg, palette, tickDelta, true);

        // 2. Render on all friends if enabled
        if (mm.cosmetics.showOnFriends.isEnabled() && dev.aetherclient.client.friend.FriendsManager.get() != null) {
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p != mc.player && dev.aetherclient.client.friend.FriendsManager.get().isFriend(p.getName().getString())) {
                    renderPlayerCosmetics(p, mc, mm, vg, palette, tickDelta, false);
                }
            }
        }
    }

    private void renderPlayerCosmetics(PlayerEntity player, MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float tickDelta, boolean isLocal) {
        boolean isFirstPerson = isLocal && mc.options.getPerspective().isFirstPerson();
        if (isFirstPerson && !mm.cosmetics.firstPerson.isEnabled()) return;

        Vec3d lp = player.getLerpedPos(tickDelta);
        double ox = lp.x - player.getX();
        double oy = lp.y - player.getY();
        double oz = lp.z - player.getZ();

        float headYaw = net.minecraft.util.math.MathHelper.lerp(tickDelta, player.lastHeadYaw, player.headYaw);
        float bodyYaw = net.minecraft.util.math.MathHelper.lerp(tickDelta, player.lastBodyYaw, player.bodyYaw);
        float pitch = net.minecraft.util.math.MathHelper.lerp(tickDelta, player.lastPitch, player.getPitch());

        double headX, headY, headZ;
        double bodyX, bodyY, bodyZ;

        if (isFirstPerson) {
            Vec3d rot = player.getRotationVector();
            headX = player.getX() + rot.x * 0.35;
            headY = player.getEyeY() + mm.cosmetics.firstPersonOffsetY.asFloat() + rot.y * 0.35;
            headZ = player.getZ() + rot.z * 0.35;
            bodyX = headX;
            bodyY = headY - 0.35;
            bodyZ = headZ;
        } else {
            headX = player.getX() + ox;
            headY = player.getEyeY() + oy;
            headZ = player.getZ() + oz;
            bodyX = player.getX() + ox;
            bodyY = player.getY() + player.getHeight() * 0.65 + oy;
            bodyZ = player.getZ() + oz;
        }

        float size = mm.cosmetics.size.asFloat();
        float speed = mm.cosmetics.speed.asFloat();
        double spin = globalTime * speed * 2.5;
        int col = palette.resolvePalette(mm.cosmetics.palette.getValue(), mm.cosmetics.color.getValue(), globalTime, mm.cosmetics.colorSpeed.asFloat());
        float opacity = mm.cosmetics.opacity.asFloat();
        if (mm.cosmetics.pulse.isEnabled()) {
            opacity *= 0.8f + 0.2f * (float) Math.sin(globalTime * 3.5);
        }
        int alpha = Math.round(opacity * 255f);
        col = AetherPalette.withAlpha(col, alpha);
        int secCol = AetherPalette.withAlpha(mm.cosmetics.secondaryColor.getValue(), alpha);
        boolean glow = mm.cosmetics.glow.isEnabled();

        // 1. Head Slot (Rotates with headYaw + pitch)
        String head = mm.cosmetics.headSlot.getValue();
        switch (head) {
            case "Нимб" -> renderHalo(vg, headX, headY, headZ, headYaw, pitch, size, spin, col, secCol, alpha, glow);
            case "Рога демона" -> renderDemonHorns(vg, headX, headY, headZ, headYaw, pitch, size, col, secCol, alpha, glow);
            case "Шляпа" -> renderHat(vg, headX, headY, headZ, headYaw, pitch, size, spin, col, secCol, alpha, glow);
            case "Корона" -> renderCrown(vg, headX, headY, headZ, headYaw, pitch, size, spin, col, secCol, alpha, glow);
            case "Кошачьи ушки" -> renderCatEars(vg, headX, headY, headZ, headYaw, pitch, size, col, secCol, alpha);
            case "Маска / Бандана" -> renderMask(vg, headX, headY, headZ, headYaw, pitch, size, col, alpha);
        }

        // 2. Body / Back Slot (Sticks to bodyYaw)
        String body = mm.cosmetics.bodySlot.getValue();
        if (body.startsWith("Крылья")) {
            boolean demon = body.contains("Демонические");
            boolean dragon = body.contains("Дракон");
            boolean fairy = body.contains("Фея");
            renderWings(vg, bodyX, bodyY, bodyZ, bodyYaw, size, col, secCol, alpha, mm.cosmetics.flapSpeed.asFloat(), demon, dragon, fairy, glow);
        } else if ("Хвост дракона".equals(body)) {
            renderDragonTail(vg, bodyX, bodyY - 0.45, bodyZ, bodyYaw, size, col, secCol, alpha, glow);
        } else if ("Плащ".equals(body)) {
            renderCape(vg, bodyX, bodyY, bodyZ, bodyYaw, size, col, secCol, alpha, glow);
        }

        // 3. Arms Slot
        String arms = mm.cosmetics.armsSlot.getValue();
        if (!"Нет".equals(arms)) {
            renderArmsCosmetic(vg, bodyX, bodyY, bodyZ, bodyYaw, size, col, secCol, alpha, glow, arms);
        }

        // 4. Legs Slot
        String legs = mm.cosmetics.legsSlot.getValue();
        if (!"Нет".equals(legs)) {
            renderLegsCosmetic(vg, bodyX, bodyY - 0.7, bodyZ, bodyYaw, size, col, secCol, alpha, glow, legs);
        }

        // 5. Aura Slot
        String aura = mm.cosmetics.auraSlot.getValue();
        if ("Орбитальные сферы".equals(aura)) {
            renderOrbitingSpheres(vg, bodyX, bodyY, bodyZ, size, spin, col, secCol, alpha, glow);
        } else if ("Аура пламени".equals(aura)) {
            renderFlameAura(vg, bodyX, bodyY, bodyZ, size, spin, col, alpha);
        } else if ("Световой вихрь".equals(aura)) {
            renderVortexAura(vg, bodyX, bodyY, bodyZ, size, spin, col, alpha, glow);
        }
    }

    private boolean projectHead(double rx, double ry, double rz, double cx, double cy, double cz, float headYaw, float pitch, float[] out) {
        double cosP = Math.cos(Math.toRadians(pitch));
        double sinP = Math.sin(Math.toRadians(pitch));
        double y1 = ry * cosP - rz * sinP;
        double z1 = ry * sinP + rz * cosP;

        double cosY = Math.cos(Math.toRadians(-headYaw));
        double sinY = Math.sin(Math.toRadians(-headYaw));
        double x2 = rx * cosY + z1 * sinY;
        double z2 = -rx * sinY + z1 * cosY;

        return wts.project(cx + x2, cy + y1, cz + z2, out);
    }

    private boolean projectBody(double rx, double ry, double rz, double cx, double cy, double cz, float bodyYaw, float[] out) {
        double cosY = Math.cos(Math.toRadians(-bodyYaw));
        double sinY = Math.sin(Math.toRadians(-bodyYaw));
        double x2 = rx * cosY + rz * sinY;
        double z2 = -rx * sinY + rz * cosY;

        return wts.project(cx + x2, cy + ry, cz + z2, out);
    }

    /** 3D Solid Volumetric Toroidal Halo with dual glowing face layers */
    private void renderHalo(long vg, double cx, double cy, double cz, float headYaw, float pitch, float size, double spin, int col, int secCol, int alpha, boolean glow) {
        double rIn = 0.28 * size;
        double rOut = 0.42 * size;
        double hTop = 0.34 * size;
        double hBot = 0.29 * size;
        int segs = 16;
        int fillCol = AetherPalette.withAlpha(col, Math.round(alpha * 0.45f));
        int outCol = col;

        for (int i = 0; i < segs; i++) {
            double a0 = i * Math.PI * 2.0 / segs + spin * 0.4;
            double a1 = (i + 1) * Math.PI * 2.0 / segs + spin * 0.4;

            double cos0 = Math.cos(a0), sin0 = Math.sin(a0);
            double cos1 = Math.cos(a1), sin1 = Math.sin(a1);

            // Top quad face
            if (projectHead(rIn * cos0, hTop, rIn * sin0, cx, cy, cz, headYaw, pitch, ptScratch)
                    && projectHead(rOut * cos0, hTop, rOut * sin0, cx, cy, cz, headYaw, pitch, ptScratch2)
                    && projectHead(rOut * cos1, hTop, rOut * sin1, cx, cy, cz, headYaw, pitch, ptScratch3)
                    && projectHead(rIn * cos1, hTop, rIn * sin1, cx, cy, cz, headYaw, pitch, ptScratch4)) {
                Render2D.drawFilledPoly4(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                        ptScratch3[0], ptScratch3[1], ptScratch4[0], ptScratch4[1], fillCol, outCol, 1.2f);
            }

            // Outer rim vertical face
            if (projectHead(rOut * cos0, hTop, rOut * sin0, cx, cy, cz, headYaw, pitch, ptScratch)
                    && projectHead(rOut * cos0, hBot, rOut * sin0, cx, cy, cz, headYaw, pitch, ptScratch2)
                    && projectHead(rOut * cos1, hBot, rOut * sin1, cx, cy, cz, headYaw, pitch, ptScratch3)
                    && projectHead(rOut * cos1, hTop, rOut * sin1, cx, cy, cz, headYaw, pitch, ptScratch4)) {
                int rimCol = AetherPalette.withAlpha(secCol, Math.round(alpha * 0.55f));
                Render2D.drawFilledPoly4(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                        ptScratch3[0], ptScratch3[1], ptScratch4[0], ptScratch4[1], rimCol, outCol, 1.0f);
            }
        }
    }

    /** 3D Solid Faceted Curved Demon Horns */
    private void renderDemonHorns(long vg, double cx, double cy, double cz, float headYaw, float pitch, float size, int col, int secCol, int alpha, boolean glow) {
        int baseFill = AetherPalette.withAlpha(col, Math.round(alpha * 0.75f));
        int midFill = AetherPalette.withAlpha(secCol, Math.round(alpha * 0.85f));

        for (int side = -1; side <= 1; side += 2) {
            // Base ring (4 vertices)
            double bx = side * 0.20 * size, by = 0.18 * size, bz = 0.10 * size;
            double bw = 0.045 * size;

            // Mid ring (4 vertices) curving back and up
            double mx = side * 0.33 * size, my = 0.38 * size, mz = -0.06 * size;
            double mw = 0.030 * size;

            // Tip
            double tx = side * 0.40 * size, ty = 0.54 * size, tz = -0.22 * size;

            double[][] bPts = {
                    {bx - bw * side, by, bz - bw},
                    {bx + bw * side, by, bz - bw},
                    {bx + bw * side, by, bz + bw},
                    {bx - bw * side, by, bz + bw}
            };

            double[][] mPts = {
                    {mx - mw * side, my, mz - mw},
                    {mx + mw * side, my, mz - mw},
                    {mx + mw * side, my, mz + mw},
                    {mx - mw * side, my, mz + mw}
            };

            // Base to Mid quad faces
            for (int k = 0; k < 4; k++) {
                int next = (k + 1) % 4;
                if (projectHead(bPts[k][0], bPts[k][1], bPts[k][2], cx, cy, cz, headYaw, pitch, ptScratch)
                        && projectHead(bPts[next][0], bPts[next][1], bPts[next][2], cx, cy, cz, headYaw, pitch, ptScratch2)
                        && projectHead(mPts[next][0], mPts[next][1], mPts[next][2], cx, cy, cz, headYaw, pitch, ptScratch3)
                        && projectHead(mPts[k][0], mPts[k][1], mPts[k][2], cx, cy, cz, headYaw, pitch, ptScratch4)) {
                    Render2D.drawFilledPoly4(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                            ptScratch3[0], ptScratch3[1], ptScratch4[0], ptScratch4[1], baseFill, col, 1.4f);
                }
            }

            // Mid to Tip triangular faces
            if (projectHead(tx, ty, tz, cx, cy, cz, headYaw, pitch, ptScratch)) {
                float tipX = ptScratch[0], tipY = ptScratch[1];
                for (int k = 0; k < 4; k++) {
                    int next = (k + 1) % 4;
                    if (projectHead(mPts[k][0], mPts[k][1], mPts[k][2], cx, cy, cz, headYaw, pitch, ptScratch2)
                            && projectHead(mPts[next][0], mPts[next][1], mPts[next][2], cx, cy, cz, headYaw, pitch, ptScratch3)) {
                        Render2D.drawFilledPoly3(vg, ptScratch2[0], ptScratch2[1], ptScratch3[0], ptScratch3[1],
                                tipX, tipY, midFill, secCol, 1.4f);
                    }
                }
                Render2D.drawCircle(vg, tipX, tipY, 2.5f * size, 0xFFFFFFFF);
            }
        }
    }

    /** 3D Solid Wizard / Fedora Hat with solid brim and cone faces */
    private void renderHat(long vg, double cx, double cy, double cz, float headYaw, float pitch, float size, double spin, int col, int secCol, int alpha, boolean glow) {
        double apexY = 0.65 * size;
        double baseR = 0.44 * size;
        double brimY = 0.18 * size;
        int segs = 12;
        int fillCol = AetherPalette.withAlpha(col, Math.round(alpha * 0.70f));

        if (projectHead(0, apexY, -0.10 * size, cx, cy, cz, headYaw, pitch, ptScratch)) {
            float ax = ptScratch[0], ay = ptScratch[1];
            for (int i = 0; i < segs; i++) {
                double a0 = i * Math.PI * 2.0 / segs;
                double a1 = (i + 1) * Math.PI * 2.0 / segs;

                if (projectHead(Math.cos(a0) * baseR, brimY, Math.sin(a0) * baseR, cx, cy, cz, headYaw, pitch, ptScratch2)
                        && projectHead(Math.cos(a1) * baseR, brimY, Math.sin(a1) * baseR, cx, cy, cz, headYaw, pitch, ptScratch3)) {
                    Render2D.drawFilledPoly3(vg, ptScratch2[0], ptScratch2[1], ptScratch3[0], ptScratch3[1],
                            ax, ay, i % 2 == 0 ? fillCol : AetherPalette.withAlpha(secCol, Math.round(alpha * 0.60f)), col, 1.2f);
                }
            }
        }
    }

    /** 3D Solid Imperial Crown with faceted cylinder base, peaks and jewels */
    private void renderCrown(long vg, double cx, double cy, double cz, float headYaw, float pitch, float size, double spin, int col, int secCol, int alpha, boolean glow) {
        double baseR = 0.33 * size;
        double botY = 0.16 * size;
        double topY = 0.25 * size;
        int segs = 10;
        int goldFill = AetherPalette.withAlpha(0xFFFFD700, Math.round(alpha * 0.85f));
        int goldOutline = 0xFFFFA000;

        // Cylindrical band quads
        for (int i = 0; i < segs; i++) {
            double a0 = i * Math.PI * 2.0 / segs + spin * 0.2;
            double a1 = (i + 1) * Math.PI * 2.0 / segs + spin * 0.2;

            if (projectHead(Math.cos(a0) * baseR, botY, Math.sin(a0) * baseR, cx, cy, cz, headYaw, pitch, ptScratch)
                    && projectHead(Math.cos(a1) * baseR, botY, Math.sin(a1) * baseR, cx, cy, cz, headYaw, pitch, ptScratch2)
                    && projectHead(Math.cos(a1) * baseR, topY, Math.sin(a1) * baseR, cx, cy, cz, headYaw, pitch, ptScratch3)
                    && projectHead(Math.cos(a0) * baseR, topY, Math.sin(a0) * baseR, cx, cy, cz, headYaw, pitch, ptScratch4)) {
                Render2D.drawFilledPoly4(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                        ptScratch3[0], ptScratch3[1], ptScratch4[0], ptScratch4[1], goldFill, goldOutline, 1.2f);
            }
        }

        // 5 Solid Triangular Peaks + Jewel Gems
        int peaks = 5;
        for (int i = 0; i < peaks; i++) {
            double aMid = i * Math.PI * 2.0 / peaks + spin * 0.2;
            double aLeft = aMid - 0.28;
            double aRight = aMid + 0.28;
            double peakH = topY + 0.15 * size;

            if (projectHead(Math.cos(aLeft) * baseR, topY, Math.sin(aLeft) * baseR, cx, cy, cz, headYaw, pitch, ptScratch)
                    && projectHead(Math.cos(aRight) * baseR, topY, Math.sin(aRight) * baseR, cx, cy, cz, headYaw, pitch, ptScratch2)
                    && projectHead(Math.cos(aMid) * (baseR * 1.08), peakH, Math.sin(aMid) * (baseR * 1.08), cx, cy, cz, headYaw, pitch, ptScratch3)) {
                Render2D.drawFilledPoly3(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                        ptScratch3[0], ptScratch3[1], goldFill, goldOutline, 1.4f);
                float gemX = ptScratch3[0], gemY = ptScratch3[1];
                Render2D.drawCircle(vg, gemX, gemY, 3.2f * size, col);
                Render2D.drawCircle(vg, gemX, gemY, 1.6f * size, 0xFFFFFFFF);
            }
        }
    }

    /** 3D Solid Cat Ears with depth prism and pastel inner ear */
    private void renderCatEars(long vg, double cx, double cy, double cz, float headYaw, float pitch, float size, int col, int secCol, int alpha) {
        double upY = 0.18 * size;
        int outerFill = AetherPalette.withAlpha(col, Math.round(alpha * 0.85f));
        int innerFill = AetherPalette.withAlpha(0xFFFF80AB, Math.round(alpha * 0.90f));

        for (int side = -1; side <= 1; side += 2) {
            double inX = side * 0.08 * size, inY = upY, inZ = 0.02 * size;
            double tipX = side * 0.28 * size, tipY = upY + 0.30 * size, tipZ = -0.04 * size;
            double exX = side * 0.32 * size, exY = upY, exZ = 0.02 * size;

            // Outer triangular ear face
            if (projectHead(inX, inY, inZ, cx, cy, cz, headYaw, pitch, ptScratch)
                    && projectHead(exX, exY, exZ, cx, cy, cz, headYaw, pitch, ptScratch2)
                    && projectHead(tipX, tipY, tipZ, cx, cy, cz, headYaw, pitch, ptScratch3)) {
                Render2D.drawFilledPoly3(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                        ptScratch3[0], ptScratch3[1], outerFill, col, 1.6f);

                // Inset inner ear
                double iInX = inX * 0.75 + tipX * 0.25;
                double iExX = exX * 0.75 + tipX * 0.25;
                double iTipX = tipX * 0.85 + (inX + exX) * 0.075;
                double iTipY = upY + 0.22 * size;

                if (projectHead(iInX, inY + 0.03 * size, inZ, cx, cy, cz, headYaw, pitch, ptScratch)
                        && projectHead(iExX, exY + 0.03 * size, exZ, cx, cy, cz, headYaw, pitch, ptScratch2)
                        && projectHead(iTipX, iTipY, tipZ, cx, cy, cz, headYaw, pitch, ptScratch4)) {
                    Render2D.drawFilledPoly3(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                            ptScratch4[0], ptScratch4[1], innerFill, 0xFFFF80AB, 1.0f);
                }
            }
        }
    }

    /** 3D Solid Tactical Mask */
    private void renderMask(long vg, double cx, double cy, double cz, float headYaw, float pitch, float size, int col, int alpha) {
        double mY = -0.08 * size;
        double frontZ = 0.24 * size;
        int maskFill = AetherPalette.withAlpha(col, Math.round(alpha * 0.75f));

        if (projectHead(-0.20 * size, mY, frontZ * 0.8, cx, cy, cz, headYaw, pitch, ptScratch)
                && projectHead(0.20 * size, mY, frontZ * 0.8, cx, cy, cz, headYaw, pitch, ptScratch2)
                && projectHead(0.18 * size, mY - 0.16 * size, frontZ * 0.7, cx, cy, cz, headYaw, pitch, ptScratch3)
                && projectHead(-0.18 * size, mY - 0.16 * size, frontZ * 0.7, cx, cy, cz, headYaw, pitch, ptScratch4)) {
            Render2D.drawFilledPoly4(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                    ptScratch3[0], ptScratch3[1], ptScratch4[0], ptScratch4[1], maskFill, col, 1.4f);
        }
    }

    /** 3D Articulated Solid Polygon Wings with real physics flapping wave */
    private void renderWings(long vg, double cx, double cy, double cz, float bodyYaw, float size, int col, int secCol, int alpha, float flapSpeed, boolean demon, boolean dragon, boolean fairy, boolean glow) {
        float flap = (float) Math.sin(globalTime * flapSpeed * 4.5f);
        double flapZ = flap * 0.32 * size;
        int membraneFill = AetherPalette.withAlpha(col, Math.round(alpha * 0.55f));
        int membraneMidFill = AetherPalette.withAlpha(secCol, Math.round(alpha * 0.60f));
        int boneCol = glow ? 0xFFFFFFFF : secCol;

        for (int side = -1; side <= 1; side += 2) {
            // Wing root at shoulder blades
            double rX = side * 0.12 * size, rY = 0.05 * size, rZ = -0.18 * size;
            // Elbow joint
            double eX = side * 0.60 * size, eY = (demon ? 0.48 : (dragon ? 0.52 : 0.40)) * size, eZ = -0.26 * size + flapZ * 0.7;
            // Outer wrist
            double wX = side * 1.05 * size, wY = (demon ? 0.68 : (dragon ? 0.75 : 0.55)) * size, wZ = -0.34 * size + flapZ * 1.3;
            // Wing tip
            double tX = side * 1.45 * size, tY = (demon ? 0.52 : (dragon ? 0.65 : 0.45)) * size, tZ = -0.42 * size + flapZ * 1.8;

            // Trailing feather/membrane nodes
            double f1X = side * 1.20 * size, f1Y = 0.10 * size, f1Z = -0.36 * size + flapZ * 1.4;
            double f2X = side * 0.80 * size, f2Y = -0.12 * size, f2Z = -0.28 * size + flapZ * 0.9;
            double f3X = side * 0.42 * size, f3Y = -0.22 * size, f3Z = -0.22 * size + flapZ * 0.5;

            // Panel 1 (Inner Membrane): Triangle Root -> Elbow -> F3
            if (projectBody(rX, rY, rZ, cx, cy, cz, bodyYaw, ptScratch)
                    && projectBody(eX, eY, eZ, cx, cy, cz, bodyYaw, ptScratch2)
                    && projectBody(f3X, f3Y, f3Z, cx, cy, cz, bodyYaw, ptScratch3)) {
                Render2D.drawFilledPoly3(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                        ptScratch3[0], ptScratch3[1], membraneFill, col, 1.4f);
            }

            // Panel 2 (Middle Membrane): Quad Elbow -> Wrist -> F2 -> F3
            if (projectBody(eX, eY, eZ, cx, cy, cz, bodyYaw, ptScratch)
                    && projectBody(wX, wY, wZ, cx, cy, cz, bodyYaw, ptScratch2)
                    && projectBody(f2X, f2Y, f2Z, cx, cy, cz, bodyYaw, ptScratch3)
                    && projectBody(f3X, f3Y, f3Z, cx, cy, cz, bodyYaw, ptScratch4)) {
                Render2D.drawFilledPoly4(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                        ptScratch3[0], ptScratch3[1], ptScratch4[0], ptScratch4[1], membraneMidFill, secCol, 1.4f);
            }

            // Panel 3 (Outer Membrane): Quad Wrist -> Tip -> F1 -> F2
            if (projectBody(wX, wY, wZ, cx, cy, cz, bodyYaw, ptScratch)
                    && projectBody(tX, tY, tZ, cx, cy, cz, bodyYaw, ptScratch2)
                    && projectBody(f1X, f1Y, f1Z, cx, cy, cz, bodyYaw, ptScratch3)
                    && projectBody(f2X, f2Y, f2Z, cx, cy, cz, bodyYaw, ptScratch4)) {
                Render2D.drawFilledPoly4(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                        ptScratch3[0], ptScratch3[1], ptScratch4[0], ptScratch4[1], membraneFill, col, 1.4f);
            }

            // Structural Bone Spars with Glowing Trim
            if (projectBody(rX, rY, rZ, cx, cy, cz, bodyYaw, ptScratch)
                    && projectBody(eX, eY, eZ, cx, cy, cz, bodyYaw, ptScratch2)) {
                Render2D.drawLine(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1], 3.2f, boneCol);
            }
            if (projectBody(eX, eY, eZ, cx, cy, cz, bodyYaw, ptScratch)
                    && projectBody(wX, wY, wZ, cx, cy, cz, bodyYaw, ptScratch2)) {
                Render2D.drawLine(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1], 2.8f, boneCol);
            }
            if (projectBody(wX, wY, wZ, cx, cy, cz, bodyYaw, ptScratch)
                    && projectBody(tX, tY, tZ, cx, cy, cz, bodyYaw, ptScratch2)) {
                Render2D.drawLine(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1], 2.4f, boneCol);
            }

            // Demon / Dragon Claw Talons at joints
            if (demon || dragon) {
                if (projectBody(eX, eY, eZ, cx, cy, cz, bodyYaw, ptScratch)
                        && projectBody(eX + side * 0.08 * size, eY + 0.10 * size, eZ, cx, cy, cz, bodyYaw, ptScratch2)) {
                    Render2D.drawLine(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1], 3.0f, 0xFFFFFFFF);
                }
            }
        }
    }

    /** 3D Solid Segmented Dragon Tail with dorsal spikes */
    private void renderDragonTail(long vg, double cx, double cy, double cz, float bodyYaw, float size, int col, int secCol, int alpha, boolean glow) {
        int segCount = 5;
        int tailFill = AetherPalette.withAlpha(col, Math.round(alpha * 0.70f));
        int spikeFill = AetherPalette.withAlpha(secCol, Math.round(alpha * 0.90f));

        for (int i = 0; i < segCount - 1; i++) {
            double sway0 = Math.sin(globalTime * 3.2 + i * 0.7) * (0.09 * (i + 1)) * size;
            double sway1 = Math.sin(globalTime * 3.2 + (i + 1) * 0.7) * (0.09 * (i + 2)) * size;

            double w0 = (0.14 - i * 0.02) * size;
            double w1 = (0.14 - (i + 1) * 0.02) * size;

            double y0 = -i * 0.16 * size;
            double y1 = -(i + 1) * 0.16 * size;

            double z0 = -0.18 * size - (i * 0.18 * size);
            double z1 = -0.18 * size - ((i + 1) * 0.18 * size);

            // Quad vertebrae segment
            if (projectBody(sway0 - w0, y0, z0, cx, cy, cz, bodyYaw, ptScratch)
                    && projectBody(sway0 + w0, y0, z0, cx, cy, cz, bodyYaw, ptScratch2)
                    && projectBody(sway1 + w1, y1, z1, cx, cy, cz, bodyYaw, ptScratch3)
                    && projectBody(sway1 - w1, y1, z1, cx, cy, cz, bodyYaw, ptScratch4)) {
                Render2D.drawFilledPoly4(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                        ptScratch3[0], ptScratch3[1], ptScratch4[0], ptScratch4[1], tailFill, col, 1.2f);
            }

            // Dorsal Spike triangle
            if (projectBody(sway0 - w0 * 0.5, y0, z0, cx, cy, cz, bodyYaw, ptScratch)
                    && projectBody(sway0 + w0 * 0.5, y0, z0, cx, cy, cz, bodyYaw, ptScratch2)
                    && projectBody(sway0, y0 + 0.12 * size, z0, cx, cy, cz, bodyYaw, ptScratch3)) {
                Render2D.drawFilledPoly3(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                        ptScratch3[0], ptScratch3[1], spikeFill, 0xFFFFFFFF, 1.0f);
            }
        }
    }

    /** 3D Solid Waving Cloth Cape Grid */
    private void renderCape(long vg, double cx, double cy, double cz, float bodyYaw, float size, int col, int secCol, int alpha, boolean glow) {
        int rows = 4;
        int cols = 3;
        int capeFill = AetherPalette.withAlpha(col, Math.round(alpha * 0.75f));
        int capeBorder = 0xFFFFD700; // Gold trim

        for (int r = 0; r < rows; r++) {
            double fracY0 = (double) r / rows;
            double fracY1 = (double) (r + 1) / rows;

            double y0 = 0.05 * size - fracY0 * 0.90 * size;
            double y1 = 0.05 * size - fracY1 * 0.90 * size;

            double wave0 = Math.sin(globalTime * 3.5 + fracY0 * 4.0) * 0.08 * size;
            double wave1 = Math.sin(globalTime * 3.5 + fracY1 * 4.0) * 0.08 * size;

            double z0 = -0.20 * size - fracY0 * 0.16 * size + wave0;
            double z1 = -0.20 * size - fracY1 * 0.16 * size + wave1;

            for (int c = 0; c < cols; c++) {
                double fracX0 = (double) c / cols * 2.0 - 1.0;
                double fracX1 = (double) (c + 1) / cols * 2.0 - 1.0;

                double x0_0 = fracX0 * (0.24 * size);
                double x1_0 = fracX1 * (0.24 * size);
                double x0_1 = fracX0 * (0.28 * size);
                double x1_1 = fracX1 * (0.28 * size);

                if (projectBody(x0_0, y0, z0, cx, cy, cz, bodyYaw, ptScratch)
                        && projectBody(x1_0, y0, z0, cx, cy, cz, bodyYaw, ptScratch2)
                        && projectBody(x1_1, y1, z1, cx, cy, cz, bodyYaw, ptScratch3)
                        && projectBody(x0_1, y1, z1, cx, cy, cz, bodyYaw, ptScratch4)) {
                    int border = (r == rows - 1 || c == 0 || c == cols - 1) ? capeBorder : col;
                    Render2D.drawFilledPoly4(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                            ptScratch3[0], ptScratch3[1], ptScratch4[0], ptScratch4[1], capeFill, border, 1.2f);
                }
            }
        }
    }

    private void renderArmsCosmetic(long vg, double cx, double cy, double cz, float bodyYaw, float size, int col, int secCol, int alpha, boolean glow, String mode) {
        double armY = -0.15 * size;
        int bandFill = AetherPalette.withAlpha(col, Math.round(alpha * 0.80f));

        for (int side = -1; side <= 1; side += 2) {
            double ax = side * 0.38 * size;
            double bw = 0.09 * size;

            if (projectBody(ax - bw, armY, 0, cx, cy, cz, bodyYaw, ptScratch)
                    && projectBody(ax + bw, armY, 0, cx, cy, cz, bodyYaw, ptScratch2)
                    && projectBody(ax + bw, armY - 0.10 * size, 0, cx, cy, cz, bodyYaw, ptScratch3)
                    && projectBody(ax - bw, armY - 0.10 * size, 0, cx, cy, cz, bodyYaw, ptScratch4)) {
                Render2D.drawFilledPoly4(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                        ptScratch3[0], ptScratch3[1], ptScratch4[0], ptScratch4[1], bandFill, secCol, 1.4f);
            }
        }
    }

    private void renderLegsCosmetic(long vg, double cx, double cy, double cz, float bodyYaw, float size, int col, int secCol, int alpha, boolean glow, String mode) {
        double legTopY = -0.40 * size;
        int bandFill = AetherPalette.withAlpha(secCol, Math.round(alpha * 0.80f));

        for (int side = -1; side <= 1; side += 2) {
            double lx = side * 0.18 * size;
            double bw = 0.08 * size;

            if (projectBody(lx - bw, legTopY, 0, cx, cy, cz, bodyYaw, ptScratch)
                    && projectBody(lx + bw, legTopY, 0, cx, cy, cz, bodyYaw, ptScratch2)
                    && projectBody(lx + bw, legTopY - 0.12 * size, 0, cx, cy, cz, bodyYaw, ptScratch3)
                    && projectBody(lx - bw, legTopY - 0.12 * size, 0, cx, cy, cz, bodyYaw, ptScratch4)) {
                Render2D.drawFilledPoly4(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1],
                        ptScratch3[0], ptScratch3[1], ptScratch4[0], ptScratch4[1], bandFill, col, 1.4f);
            }
        }
    }

    private void renderOrbitingSpheres(long vg, double cx, double cy, double cz, float size, double spin, int col, int secCol, int alpha, boolean glow) {
        int count = 4;
        double orbR = 0.55 * size;
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2.0 / count + spin;
            double ox = Math.cos(a) * orbR;
            double oy = Math.sin(a * 2.0 + spin) * 0.12 * size;
            double oz = Math.sin(a) * orbR;
            if (wts.project(cx + ox, cy + oy, cz + oz, ptScratch)) {
                float sx = ptScratch[0], sy = ptScratch[1];
                float r = 4.5f * size;
                if (glow) {
                    Render2D.drawCircle(vg, sx, sy, r * 2.4f, AetherPalette.withAlpha(col, Math.round(alpha * 0.30f)));
                    Render2D.drawRing(vg, sx, sy, r * 1.6f, 1.2f, AetherPalette.withAlpha(secCol, Math.round(alpha * 0.70f)));
                }
                Render2D.drawCircle(vg, sx, sy, r, i % 2 == 0 ? col : secCol);
                Render2D.drawCircle(vg, sx - r * 0.25f, sy - r * 0.25f, r * 0.4f, 0xFFFFFFFF);
            }
        }
    }

    private void renderFlameAura(long vg, double cx, double cy, double cz, float size, double spin, int col, int alpha) {
        int embers = 16;
        double auraR = 0.45 * size;
        for (int i = 0; i < embers; i++) {
            double t = (globalTime * 1.2 + (double) i / embers) % 1.0;
            double a = i * 2.4 + spin * 0.5;
            double ox = Math.cos(a) * (auraR * (0.8 + 0.3 * Math.sin(t * 6.28)));
            double oy = (-0.6 * size) + t * (1.2 * size);
            double oz = Math.sin(a) * (auraR * (0.8 + 0.3 * Math.sin(t * 6.28)));
            if (wts.project(cx + ox, cy + oy, cz + oz, ptScratch)) {
                float sx = ptScratch[0], sy = ptScratch[1];
                float flameA = (1f - (float) t) * alpha;
                float r = (3.6f - (float) t * 2.2f) * size;
                int flameCol = AetherPalette.lerpArgb(col, 0xFFFFE57F, (float) t);
                Render2D.drawCircle(vg, sx, sy, r * 1.8f, AetherPalette.withAlpha(flameCol, Math.round(flameA * 0.35f)));
                Render2D.drawCircle(vg, sx, sy, r, AetherPalette.withAlpha(flameCol, Math.round(flameA)));
            }
        }
    }

    private void renderVortexAura(long vg, double cx, double cy, double cz, float size, double spin, int col, int alpha, boolean glow) {
        int trails = 3;
        double radius = 0.50 * size;
        for (int t = 0; t < trails; t++) {
            double phase = t * (Math.PI * 2.0 / trails);
            boolean prev = false;
            float px0 = 0f, py0 = 0f;
            for (int i = 0; i <= 16; i++) {
                double frac = (double) i / 16.0;
                double a = frac * Math.PI * 3.0 + spin * 1.5 + phase;
                double r = radius * (1.0 - frac * 0.5);
                double vx = Math.cos(a) * r;
                double vy = (-0.5 + frac * 1.2) * size;
                double vz = Math.sin(a) * r;
                if (wts.project(cx + vx, cy + vy, cz + vz, ptScratch)) {
                    if (prev) {
                        Render2D.drawLine(vg, px0, py0, ptScratch[0], ptScratch[1], 2.2f, AetherPalette.withAlpha(col, Math.round(alpha * (1f - (float) frac * 0.5f))));
                    }
                    px0 = ptScratch[0]; py0 = ptScratch[1]; prev = true;
                } else prev = false;
            }
        }
    }

    /** 3D Entity Hitboxes and Eye Vector Visualization */
    private void renderHitboxes(MinecraftClient mc, ModuleManager mm, long vg, float tickDelta) {
        if (mc.world == null || mc.player == null) return;
        int lineCol = mm.hitboxes.color.getValue();
        float thick = mm.hitboxes.lineWidth.asFloat();
        int fillAlpha = mm.hitboxes.fill.isEnabled() ? Math.round(mm.hitboxes.fillAlpha.asFloat() * 255f) : 0;
        int fillCol = AetherPalette.withAlpha(lineCol, fillAlpha);
        boolean showEye = mm.hitboxes.eyeVector.isEnabled();
        int eyeCol = mm.hitboxes.eyeColor.getValue();
        boolean invisibleOk = mm.hitboxes.invisible.isEnabled();
        boolean playersOnly = mm.hitboxes.playersOnly.isEnabled();

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            if (!(e instanceof LivingEntity)) continue;
            if (playersOnly && !(e instanceof PlayerEntity)) continue;
            if (e.isInvisible() && !invisibleOk) continue;

            Vec3d lerped = e.getLerpedPos(tickDelta);
            double ox = lerped.x - e.getX();
            double oy = lerped.y - e.getY();
            double oz = lerped.z - e.getZ();
            Box bb = e.getBoundingBox().offset(ox, oy, oz);

            render3DBox(vg, bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, lineCol, thick, fillCol, false, 0f);

            if (showEye) {
                Vec3d rot = e.getRotationVector();
                double eyeX = lerped.x;
                double eyeY = e.getEyeY() + oy;
                double eyeZ = lerped.z;
                double targetX = eyeX + rot.x * 1.6;
                double targetY = eyeY + rot.y * 1.6;
                double targetZ = eyeZ + rot.z * 1.6;

                if (wts.project(eyeX, eyeY, eyeZ, ptScratch) && wts.project(targetX, targetY, targetZ, ptScratch2)) {
                    Render2D.drawLine(vg, ptScratch[0], ptScratch[1], ptScratch2[0], ptScratch2[1], 1.8f, eyeCol);
                    Render2D.drawCircle(vg, ptScratch2[0], ptScratch2[1], 2.8f, eyeCol);
                    Render2D.drawCircle(vg, ptScratch2[0], ptScratch2[1], 1.4f, 0xFFFFFFFF);
                }
            }
        }
    }

    private void spawnFloatingDamage(LivingEntity target, float amt, boolean isHeal, ModuleManager mm) {
        if (!mm.hpCounter.isEnabled()) return;
        FloatingDamage fd = floatingDamagePool.acquire();
        fd.target = target;
        fd.offsetX = (rng.nextDouble() - 0.5) * 0.35;
        fd.offsetZ = (rng.nextDouble() - 0.5) * 0.35;
        fd.baseOffsetY = 0.25 + rng.nextDouble() * 0.25;
        fd.riseY = 0.0;
        fd.lastX = target.getX() + fd.offsetX;
        fd.lastY = target.getY() + fd.baseOffsetY;
        fd.lastZ = target.getZ() + fd.offsetZ;
        fd.amount = amt;
        fd.isHeal = isHeal;
        fd.age = 0f;
        fd.maxAge = mm.hpCounter.duration.asFloat();
        fd.vy = 0.85f + rng.nextFloat() * 0.45f;
        floatingDamages.add(fd);
        while (floatingDamages.size() > 40) {
            floatingDamagePool.release(floatingDamages.remove(0));
        }
    }

    private void updateHpTracking(MinecraftClient mc, ModuleManager mm) {
        if (mc.world == null) return;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living) || !living.isAlive()) continue;
            int id = living.getId();
            float hp = living.getHealth();
            Float prev = trackedHealth.put(id, hp);
            if (prev != null && Math.abs(hp - prev) > 0.08f) {
                float diff = prev - hp;
                boolean isHeal = diff < 0f;
                spawnFloatingDamage(living, Math.abs(diff), isHeal, mm);
            }
        }
        if (trackedHealth.size() > 256) {
            trackedHealth.clear();
        }
    }

    private void renderHpCounter(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float dt) {
        if (floatingDamages.isEmpty()) return;
        float scaleMod = mm.hpCounter.scale.asFloat();
        boolean signs = mm.hpCounter.showPlusMinus.isEnabled();
        boolean throughWalls = mm.hpCounter.throughWalls.isEnabled();
        int customDmgCol = mm.hpCounter.damageColor.getValue();
        int customHealCol = mm.hpCounter.healColor.getValue();
        String palName = mm.hpCounter.palette.getValue();

        Iterator<FloatingDamage> it = floatingDamages.iterator();
        while (it.hasNext()) {
            FloatingDamage fd = it.next();
            fd.age += dt;
            if (fd.age >= fd.maxAge) {
                it.remove();
                floatingDamagePool.release(fd);
                continue;
            }
            fd.riseY += fd.vy * dt;
            fd.vy = Math.max(0.1f, fd.vy - 0.9f * dt);

            double curX, curY, curZ;
            if (fd.target != null && fd.target.isAlive() && !fd.target.isRemoved()) {
                curX = fd.target.getX() + fd.offsetX;
                curY = fd.target.getY() + fd.baseOffsetY + fd.riseY;
                curZ = fd.target.getZ() + fd.offsetZ;
                fd.lastX = curX;
                fd.lastY = curY;
                fd.lastZ = curZ;
            } else {
                fd.target = null;
                fd.lastY += fd.vy * dt;
                curX = fd.lastX;
                curY = fd.lastY;
                curZ = fd.lastZ;
            }

            if (!throughWalls && !canSeePos(mc, curX, curY, curZ)) {
                continue;
            }

            if (wts.project(curX, curY, curZ, ptScratch)) {
                float sx = ptScratch[0];
                float sy = ptScratch[1];
                float life = fd.age / fd.maxAge;
                float pop = Math.min(1f, fd.age * 8f);
                float alpha = (1f - (life * life)) * pop;
                int a255 = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
                if (a255 <= 4) continue;

                String text;
                int color;
                if (fd.isHeal) {
                    text = (signs ? "+" : "") + String.format(java.util.Locale.ROOT, "%.1f", fd.amount);
                    color = palette.resolvePalette(palName, customHealCol, globalTime, 1.0f);
                } else {
                    text = (signs ? "-" : "") + String.format(java.util.Locale.ROOT, "%.1f", fd.amount);
                    color = palette.resolvePalette(palName, customDmgCol, globalTime, 1.0f);
                }

                float fontSize = 11.0f * scaleMod * (0.85f + 0.15f * pop);
                float tw = Render2D.measureTextBold(vg, text, fontSize) + 8f;
                float th = fontSize + 4f;

                Render2D.drawShadow(vg, sx - tw * 0.5f, sy - th * 0.5f, tw, th, 4f, 6f, AetherPalette.withAlpha(0x000000, Math.round(a255 * 0.45f)));
                Render2D.drawRoundedRect(vg, sx - tw * 0.5f, sy - th * 0.5f, tw, th, 4f, AetherPalette.withAlpha(palette.surfaceContainerHigh, Math.round(a255 * 0.85f)));
                Render2D.drawRoundedOutline(vg, sx - tw * 0.5f, sy - th * 0.5f, tw, th, 4f, 1f, AetherPalette.withAlpha(color, Math.round(a255 * 0.75f)));

                Render2D.drawTextBold(vg, text, sx, sy, fontSize, AetherPalette.withAlpha(color, a255),
                        NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            }
        }
    }

    private void renderSkyTint(ModuleManager mm, long vg, AetherPalette palette, float screenW, float screenH) {
        float a = mm.timeChanger.tintOpacity.asFloat();
        int top = AetherPalette.withAlpha(mm.timeChanger.topColor.getValue(), Math.round(a * 255f));
        int bot = AetherPalette.withAlpha(mm.timeChanger.bottomColor.getValue(), Math.round(a * 255f));
        Render2D.drawRoundedGradient(vg, 0f, 0f, screenW, screenH, 0f, top, bot, true);
    }

    private void renderCustomSky(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float screenW, float screenH, float dt) {
        float opacity = mm.customSky.opacity.asFloat();

        // Aurora waves
        if (mm.customSky.auroraWave.isEnabled()) {
            int waveCol = AetherPalette.withAlpha(0xFF38BDF8, Math.round(opacity * 70f));
            float midY = screenH * 0.35f;
            float step = screenW / 24f;
            float prevX = 0f, prevY = midY;
            for (int i = 0; i <= 24; i++) {
                float wx = i * step;
                float wy = midY + (float) Math.sin(globalTime * 1.5 + i * 0.35) * (screenH * 0.08f);
                if (i > 0) {
                    Render2D.drawLine(vg, prevX, prevY, wx, wy, 4.5f, waveCol);
                    Render2D.drawLine(vg, prevX, prevY, wx, wy, 1.8f, AetherPalette.withAlpha(0xFF818CF8, Math.round(opacity * 130f)));
                }
                prevX = wx;
                prevY = wy;
            }
        }

        // Shimmering stars
        int starCount = (int) Math.round(mm.customSky.stars.getValue());
        for (int i = 0; i < starCount; i++) {
            float hashX = (float) Math.sin(i * 127.1 + 1.0) * 0.5f + 0.5f;
            float hashY = (float) Math.cos(i * 311.7 + 2.0) * 0.5f + 0.5f;
            float sx = hashX * screenW;
            float sy = hashY * (screenH * 0.65f);
            float twinkle = 0.4f + 0.6f * (float) Math.sin(globalTime * 2.2 + i * 1.7);
            int starCol = AetherPalette.withAlpha(0xFFE0E7FF, Math.round(opacity * twinkle * 200f));
            Render2D.drawCircle(vg, sx, sy, 1.3f, starCol);
        }
    }

    private void renderColorFilter(ModuleManager mm, long vg, float screenW, float screenH) {
        float a = mm.colorFilter.intensity.asFloat();
        int col = AetherPalette.withAlpha(mm.colorFilter.filterArgb(), Math.round(a * 110f));
        Render2D.drawRoundedRect(vg, 0f, 0f, screenW, screenH, 0f, col);
    }

    private void renderMotionBlur(MinecraftClient mc, ModuleManager mm, NanoVgEngine engine, float screenW, float screenH, float dt) {
        if (mc.player == null) return;
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        Vec3d cam = wts.getCamPos();
        if (Float.isNaN(prevYaw)) {
            prevYaw = yaw;
            prevPitch = pitch;
            prevCamX = cam.x;
            prevCamY = cam.y;
            prevCamZ = cam.z;
            return;
        }
        float dYaw = wrapDeg(yaw - prevYaw);
        float dPitch = pitch - prevPitch;
        double camDx = cam.x - prevCamX;
        double camDy = cam.y - prevCamY;
        double camDz = cam.z - prevCamZ;
        prevYaw = yaw;
        prevPitch = pitch;
        prevCamX = cam.x;
        prevCamY = cam.y;
        prevCamZ = cam.z;

        float sens = mm.motionBlur.sensitivity.asFloat();
        float angMag = (float) Math.hypot(dYaw, dPitch) * sens;
        float transSpeed = (float) Math.hypot(camDx, Math.hypot(camDy, camDz)) * 20f * sens;
        float strength = Math.min(0.85f, (angMag * 0.06f + transSpeed * 0.02f)) * mm.motionBlur.strength.asFloat();
        float rawOffX = -dYaw * sens * 1.5f;
        float rawOffY = -dPitch * sens * 1.5f;

        // Exponential smoothing: kills per-frame jitter, keeps cinematic feel
        if (Float.isNaN(mbSmoothOffX)) {
            mbSmoothOffX = rawOffX;
            mbSmoothOffY = rawOffY;
            mbSmoothStrength = strength;
        } else {
            float k = Math.min(1f, dt * 14f);
            mbSmoothOffX += (rawOffX - mbSmoothOffX) * k;
            mbSmoothOffY += (rawOffY - mbSmoothOffY) * k;
            mbSmoothStrength += (strength - mbSmoothStrength) * Math.min(1f, dt * 10f);
        }

        if (mbSmoothStrength <= 0.015f || engine == null || engine.blurImage() == 0) return;

        float blurAlpha = Math.min(0.72f, mbSmoothStrength * 1.25f);
        Render2D.drawMotionBlurPass(engine, screenW, screenH, mbSmoothOffX, mbSmoothOffY, blurAlpha);
    }

    private void renderCustomScoreboard(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float screenW, float screenH) {
        List<String[]> rows = mm.scoreboard.readRows(mc);
        String title = mm.scoreboard.readTitle(mc);
        if (rows.isEmpty() && (title == null || title.isEmpty())) return;

        String myName = mc.player != null ? mc.player.getName().getString() : null;
        if (mm.nameProtect.isEnabled() && mm.nameProtect.maskInScoreboard.isEnabled() && myName != null) {
            title = mm.nameProtect.filterText(title, myName);
            for (String[] r : rows) {
                if (r[0] != null) r[0] = mm.nameProtect.filterText(r[0], myName);
                if (r[1] != null) r[1] = mm.nameProtect.filterText(r[1], myName);
            }
        }

        boolean right = "Правый".equals(mm.scoreboard.position.getValue());
        int accent = palette.resolvePalette(mm.scoreboard.palette.getValue(), mm.scoreboard.color.getValue(), globalTime, mm.scoreboard.colorSpeed.asFloat());
        int surface = AetherPalette.withAlpha(palette.surface, Math.round(mm.scoreboard.opacity.asFloat() * 255f));
        float rowH = 16f;
        float titleH = (mm.scoreboard.showTitle.isEnabled() && title != null && !title.isEmpty()) ? 22f : 0f;
        float maxW = 60f;
        for (String[] r : rows) {
            maxW = Math.max(maxW, Render2D.measureTextBold(vg, r[0], 9.6f) + Render2D.measureTextBold(vg, r[1], 9.6f) + 24f);
        }
        if (titleH > 0f) maxW = Math.max(maxW, Render2D.measureTextBold(vg, title, 10.2f) + 20f);
        float w = Math.min(230f, maxW + 8f);
        float h = titleH + rows.size() * rowH + 10f;
        float x = right ? screenW - w - 8f : 8f;
        float y = 8f;
        Render2D.drawShadow(vg, x, y, w, h, 9f, 18f, palette.darkMode ? 0x55000000 : 0x2E000000);
        Render2D.drawRoundedRect(vg, x, y, w, h, 9f, surface);
        Render2D.drawRoundedOutline(vg, x, y, w, h, 9f, 1f, AetherPalette.withAlpha(palette.outline, 120));
        Render2D.drawRoundedRect(vg, x, y, w, 3f, 3f, accent);
        float cy = y + 8f;
        if (titleH > 0f) {
            Render2D.drawTextBold(vg, title, x + w * 0.5f, cy, 10.2f, accent,
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            cy += titleH;
        }
        for (String[] r : rows) {
            int valCol = mm.scoreboard.redScores.isEnabled() ? 0xFFFF5F6D : palette.onSurfaceVariant;
            Render2D.drawTextBold(vg, r[0], x + 10f, cy + rowH * 0.5f, 9.6f, palette.onSurface,
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            Render2D.drawTextBold(vg, r[1], x + w - 10f, cy + rowH * 0.5f, 9.6f, valCol,
                    NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);
            cy += rowH;
        }
    }

    private void renderCustomTabList(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float screenW, float screenH) {
        if (mc.getNetworkHandler() == null) return;
        var players = mc.getNetworkHandler().getPlayerList();
        if (players.isEmpty()) return;
        int accent = palette.resolvePalette(mm.tabList.palette.getValue(), mm.tabList.color.getValue(), globalTime, mm.tabList.colorSpeed.asFloat());
        int surface = AetherPalette.withAlpha(palette.surface, Math.round(mm.tabList.opacity.asFloat() * 255f));
        int cols = Math.max(2, (int) Math.round(mm.tabList.columns.getValue()));
        int rows = (players.size() + cols - 1) / cols;
        float cellW = 132f;
        float cellH = 22f;
        float w = cols * cellW + 14f;
        float h = rows * cellH + 22f;
        float x = (screenW - w) * 0.5f;
        float y = 18f;
        Render2D.drawShadow(vg, x, y, w, h, 10f, 20f, palette.darkMode ? 0x55000000 : 0x2E000000);
        Render2D.drawRoundedRect(vg, x, y, w, h, 10f, surface);
        Render2D.drawRoundedOutline(vg, x, y, w, h, 10f, 1f, AetherPalette.withAlpha(palette.outline, 120));
        Render2D.drawRoundedRect(vg, x, y, w, 3f, 3f, accent);

        int i = 0;
        for (PlayerListEntry e : players) {
            int col = i % cols;
            int row = i / cols;
            float cx = x + 7f + col * cellW;
            float cy = y + 16f + row * cellH;
            String name = profileName(e);
            String myName = mc.player != null ? mc.player.getName().getString() : null;
            if (mm.nameProtect.isEnabled() && mm.nameProtect.maskInTab.isEnabled() && myName != null && name.equalsIgnoreCase(myName)) {
                name = mm.nameProtect.mask(name);
            }
            Render2D.pushScissor(vg, cx + 2f, cy, cellW - 4f, cellH);
            Render2D.drawTextBold(vg, name, cx + 8f, cy + cellH * 0.5f, 9.6f, palette.onSurface,
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            Render2D.popScissor(vg);
            if (mm.tabList.showPing.isEnabled()) {
                int ping = e.getLatency();
                int pingCol = ping > 140 ? 0xFFFFB4AB : (ping > 60 ? palette.tertiary : palette.primary);
                Render2D.drawTextBold(vg, ping + " ms", cx + cellW - 8f, cy + cellH * 0.5f, 9.0f, pingCol,
                        NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);
            }
            i++;
        }
    }

    private static String profileName(PlayerListEntry entry) {
        try {
            Object profile = entry.getProfile();
            if (profile == null) return "?";
            for (java.lang.reflect.Method m : profile.getClass().getMethods()) {
                if (m.getName().equals("getName") && m.getParameterCount() == 0 && m.getReturnType() == String.class) {
                    Object name = m.invoke(profile);
                    return name instanceof String s ? s : "?";
                }
            }
        } catch (Throwable ignored) {
        }
        return "?";
    }

    private static void drawShapeOutline(long vg, String shape, float x, float y, float w, float h, float thickness, int argb) {
        switch (shape) {
            case "Углы" -> {
                float len = Math.max(4f, Math.min(12f, Math.min(w, h) * 0.28f));
                Render2D.drawCornerRect(vg, x, y, w, h, len, thickness, argb);
            }
            case "Свечение" -> Render2D.drawGlowBox(vg, x, y, w, h, 4f, thickness, argb);
            case "Скобка" -> {
                float len = Math.max(4f, Math.min(12f, Math.min(w, h) * 0.28f));
                Render2D.drawBracketRect(vg, x, y, w, h, len, thickness, argb);
            }
            default -> Render2D.drawRoundedOutline(vg, x, y, w, h, 4f, thickness, argb);
        }
    }

    private static float wrapDeg(float deg) {
        float d = deg % 360f;
        if (d > 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private static float readBreakProgress(MinecraftClient mc) {
        try {
            Object im = mc.interactionManager;
            if (im == null) return 0f;
            for (java.lang.reflect.Field f : im.getClass().getDeclaredFields()) {
                if (f.getType() != float.class) continue;
                f.setAccessible(true);
                float v = f.getFloat(im);
                if (v > 0f) return v;
            }
        } catch (Throwable ignored) {
        }
        return 0f;
    }

    private static void drawEdgeVignette(long vg, float w, float h, int edgeArgb) {
        if (((edgeArgb >>> 24) & 0xFF) == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor inner = NanoVgEngine.argbToNvg(0x00000000, NVGColor.malloc(stack));
            NVGColor outer = NanoVgEngine.argbToNvg(edgeArgb, NVGColor.malloc(stack));
            NVGPaint paint = NVGPaint.malloc(stack);
            float rad = Math.max(w, h) * 0.62f;
            NanoVG.nvgRadialGradient(vg, w * 0.5f, h * 0.5f, rad * 0.35f, rad, inner, outer, paint);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRect(vg, 0f, 0f, w, h);
            NanoVG.nvgFillPaint(vg, paint);
            NanoVG.nvgFill(vg);
        }
    }

    private static void drawFilledArrow(long vg, float sz, int argb) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor col = NanoVgEngine.argbToNvg(argb, NVGColor.malloc(stack));
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgMoveTo(vg, 0f, -sz);
            NanoVG.nvgLineTo(vg, sz * 0.72f, sz * 0.72f);
            NanoVG.nvgLineTo(vg, 0f, sz * 0.32f);
            NanoVG.nvgLineTo(vg, -sz * 0.72f, sz * 0.72f);
            NanoVG.nvgClosePath(vg);
            NanoVG.nvgFillColor(vg, col);
            NanoVG.nvgFill(vg);
        }
    }

    private void renderChinaHat(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float tickDelta) {
        if (mc.player == null) return;
        boolean isFirstPerson = mc.options.getPerspective().isFirstPerson();
        if (isFirstPerson && !mm.chinaHat.firstPerson.isEnabled()) return;

        Vec3d lerped = mc.player.getLerpedPos(tickDelta);
        double px = lerped.x;
        double py = lerped.y;
        double pz = lerped.z;
        double eyeH = mc.player.getEyeHeight(mc.player.getPose());

        float radius = mm.chinaHat.radius.asFloat();
        float height = mm.chinaHat.height.asFloat();
        float offY = mm.chinaHat.offsetY.asFloat();
        float rSpeed = mm.chinaHat.rainbowSpeed.asFloat();
        int baseColor = mm.chinaHat.rainbow.isEnabled()
                ? AetherPalette.rainbow(globalTime * rSpeed)
                : mm.chinaHat.color.getValue();

        int segments = Math.max(12, Math.min(48, (int) mm.chinaHat.segments.asFloat()));
        boolean isTwoLayers = mm.chinaHat.layers.isEnabled();
        int layersCount = isTwoLayers ? 2 : 1;

        for (int layer = 0; layer < layersCount; layer++) {
            float lRadius = layer == 1 ? radius * 0.72f : radius;
            float lHeight = layer == 1 ? height * 0.78f : height;
            double baseY = py + eyeH + 0.38 + offY + (layer == 1 ? 0.04 : 0.0);
            double tipY = baseY + lHeight;

            if (!wts.project(px, tipY, pz, ptScratch)) continue;
            float tipScrX = ptScratch[0];
            float tipScrY = ptScratch[1];

            float[] baseScrX = new float[segments + 1];
            float[] baseScrY = new float[segments + 1];
            boolean[] baseVis = new boolean[segments + 1];

            for (int i = 0; i <= segments; i++) {
                double angle = (2.0 * Math.PI * (i % segments)) / segments;
                double bx = px + Math.cos(angle) * lRadius;
                double bz = pz + Math.sin(angle) * lRadius;
                if (wts.project(bx, baseY, bz, ptScratch2)) {
                    baseScrX[i] = ptScratch2[0];
                    baseScrY[i] = ptScratch2[1];
                    baseVis[i] = true;
                } else {
                    baseVis[i] = false;
                }
            }

            int fillAlpha = (layer == 1 ? 0x77000000 : 0x50000000) | (baseColor & 0x00FFFFFF);
            for (int i = 0; i < segments; i++) {
                if (!baseVis[i] || !baseVis[i + 1]) continue;
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    NVGColor cFill = NanoVgEngine.argbToNvg(fillAlpha, NVGColor.malloc(stack));
                    NanoVG.nvgBeginPath(vg);
                    NanoVG.nvgMoveTo(vg, tipScrX, tipScrY);
                    NanoVG.nvgLineTo(vg, baseScrX[i], baseScrY[i]);
                    NanoVG.nvgLineTo(vg, baseScrX[i + 1], baseScrY[i + 1]);
                    NanoVG.nvgClosePath(vg);
                    NanoVG.nvgFillColor(vg, cFill);
                    NanoVG.nvgFill(vg);
                }
            }

            int rimColor = 0xEE000000 | (baseColor & 0x00FFFFFF);
            if (mm.chinaHat.glow.isEnabled()) {
                int glowCol = 0x55000000 | (baseColor & 0x00FFFFFF);
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    NVGColor cGlow = NanoVgEngine.argbToNvg(glowCol, NVGColor.malloc(stack));
                    NanoVG.nvgBeginPath(vg);
                    boolean started = false;
                    for (int i = 0; i <= segments; i++) {
                        if (!baseVis[i]) { started = false; continue; }
                        if (!started) { NanoVG.nvgMoveTo(vg, baseScrX[i], baseScrY[i]); started = true; }
                        else { NanoVG.nvgLineTo(vg, baseScrX[i], baseScrY[i]); }
                    }
                    NanoVG.nvgStrokeWidth(vg, 5.0f);
                    NanoVG.nvgStrokeColor(vg, cGlow);
                    NanoVG.nvgStroke(vg);
                }
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                NVGColor cRim = NanoVgEngine.argbToNvg(rimColor, NVGColor.malloc(stack));
                NanoVG.nvgBeginPath(vg);
                boolean started = false;
                for (int i = 0; i <= segments; i++) {
                    if (!baseVis[i]) { started = false; continue; }
                    if (!started) { NanoVG.nvgMoveTo(vg, baseScrX[i], baseScrY[i]); started = true; }
                    else { NanoVG.nvgLineTo(vg, baseScrX[i], baseScrY[i]); }
                }
                NanoVG.nvgStrokeWidth(vg, layer == 1 ? 1.5f : 2.2f);
                NanoVG.nvgStrokeColor(vg, cRim);
                NanoVG.nvgStroke(vg);
            }
        }
    }

    private void renderPearlPredict(MinecraftClient mc, ModuleManager mm, long vg, AetherPalette palette, float tickDelta) {
        if (mc.world == null || mc.player == null) return;

        List<Vec3d[]> pathsToDraw = new ArrayList<>();
        List<double[]> impacts = new ArrayList<>();

        if (mm.pearlPredict.otherPlayers.isEnabled()) {
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof net.minecraft.entity.projectile.thrown.EnderPearlEntity pearl) {
                    simulatePearl(mc, pearl.getLerpedPos(tickDelta), pearl.getVelocity(), pathsToDraw, impacts);
                }
            }
        }

        if (mm.pearlPredict.predictInHand.isEnabled()) {
            boolean holding = mc.player.getMainHandStack().isOf(Items.ENDER_PEARL) || mc.player.getOffHandStack().isOf(Items.ENDER_PEARL);
            if (holding) {
                Vec3d eye = mc.player.getEyePos();
                Vec3d rot = mc.player.getRotationVector();
                Vec3d vel = rot.multiply(1.5);
                simulatePearl(mc, eye, vel, pathsToDraw, impacts);
            }
        }

        if (pathsToDraw.isEmpty()) return;

        int lineColor = mm.pearlPredict.color.getValue();
        float lineW = mm.pearlPredict.lineWidth.asFloat();
        boolean hasGlow = mm.pearlPredict.glow.isEnabled();
        boolean hasGradient = mm.pearlPredict.gradient.isEnabled();

        for (int pIdx = 0; pIdx < pathsToDraw.size(); pIdx++) {
            Vec3d[] path = pathsToDraw.get(pIdx);
            if (mm.pearlPredict.trajectory.isEnabled() && path.length > 1) {
                if (hasGlow) {
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        int glowCol = AetherPalette.withAlpha(lineColor, 45);
                        NVGColor cGlow = NanoVgEngine.argbToNvg(glowCol, NVGColor.malloc(stack));
                        NanoVG.nvgBeginPath(vg);
                        boolean started = false;
                        for (Vec3d pt : path) {
                            if (wts.project(pt.x, pt.y, pt.z, ptScratch)) {
                                if (!started) { NanoVG.nvgMoveTo(vg, ptScratch[0], ptScratch[1]); started = true; }
                                else { NanoVG.nvgLineTo(vg, ptScratch[0], ptScratch[1]); }
                            } else { started = false; }
                        }
                        NanoVG.nvgStrokeWidth(vg, lineW + 4.0f);
                        NanoVG.nvgStrokeColor(vg, cGlow);
                        NanoVG.nvgStroke(vg);
                    }
                }

                if (hasGradient) {
                    for (int s = 0; s < path.length - 1; s++) {
                        float progress = (float) s / (float) (path.length - 1);
                        int segCol = AetherPalette.lerpArgb(lineColor, 0xFFFFFFFF, progress * 0.7f);
                        if (wts.project(path[s].x, path[s].y, path[s].z, ptScratch)
                                && wts.project(path[s + 1].x, path[s + 1].y, path[s + 1].z, ptScratch2)) {
                            try (MemoryStack stack = MemoryStack.stackPush()) {
                                NVGColor cSeg = NanoVgEngine.argbToNvg(segCol, NVGColor.malloc(stack));
                                NanoVG.nvgBeginPath(vg);
                                NanoVG.nvgMoveTo(vg, ptScratch[0], ptScratch[1]);
                                NanoVG.nvgLineTo(vg, ptScratch2[0], ptScratch2[1]);
                                NanoVG.nvgStrokeWidth(vg, lineW);
                                NanoVG.nvgStrokeColor(vg, cSeg);
                                NanoVG.nvgStroke(vg);
                            }
                        }
                    }
                } else {
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        NVGColor cLine = NanoVgEngine.argbToNvg(lineColor, NVGColor.malloc(stack));
                        NanoVG.nvgBeginPath(vg);
                        boolean started = false;
                        for (Vec3d pt : path) {
                            if (wts.project(pt.x, pt.y, pt.z, ptScratch)) {
                                if (!started) { NanoVG.nvgMoveTo(vg, ptScratch[0], ptScratch[1]); started = true; }
                                else { NanoVG.nvgLineTo(vg, ptScratch[0], ptScratch[1]); }
                            } else { started = false; }
                        }
                        NanoVG.nvgStrokeWidth(vg, lineW);
                        NanoVG.nvgStrokeColor(vg, cLine);
                        NanoVG.nvgStroke(vg);
                    }
                }
            }

            if (pIdx < impacts.size()) {
                double[] imp = impacts.get(pIdx);
                double ix = imp[0], iy = imp[1], iz = imp[2];
                double seconds = imp[3];

                if (mm.pearlPredict.landingRing.isEnabled()) {
                    int ringSegs = 24;
                    float ringR = mm.pearlPredict.ringSize.asFloat();
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        NVGColor cRing = NanoVgEngine.argbToNvg(lineColor, NVGColor.malloc(stack));
                        NanoVG.nvgBeginPath(vg);
                        boolean rStarted = false;
                        for (int s = 0; s <= ringSegs; s++) {
                            double a = (2.0 * Math.PI * (s % ringSegs)) / ringSegs;
                            double rx = ix + Math.cos(a) * ringR;
                            double rz = iz + Math.sin(a) * ringR;
                            if (wts.project(rx, iy + 0.05, rz, ptScratch2)) {
                                if (!rStarted) { NanoVG.nvgMoveTo(vg, ptScratch2[0], ptScratch2[1]); rStarted = true; }
                                else { NanoVG.nvgLineTo(vg, ptScratch2[0], ptScratch2[1]); }
                            } else { rStarted = false; }
                        }
                        NanoVG.nvgStrokeWidth(vg, 2.2f);
                        NanoVG.nvgStrokeColor(vg, cRing);
                        NanoVG.nvgStroke(vg);
                    }
                }

                if (mm.pearlPredict.impactBox.isEnabled()) {
                    Box impBox = new Box(ix - 0.2, iy, iz - 0.2, ix + 0.2, iy + 0.4, iz + 0.2);
                    if (wts.projectBox(impBox, boxRect, ptScratch)) {
                        Render2D.drawRoundedRect(vg, boxRect[0], boxRect[1], boxRect[2] - boxRect[0], boxRect[3] - boxRect[1], 2f, AetherPalette.withAlpha(lineColor, 40));
                        Render2D.drawRoundedOutline(vg, boxRect[0], boxRect[1], boxRect[2] - boxRect[0], boxRect[3] - boxRect[1], 2f, 1.2f, lineColor);
                    }
                }

                if (mm.pearlPredict.showTimer.isEnabled() && wts.project(ix, iy + 0.35, iz, ptScratch)) {
                    String timeText = String.format(java.util.Locale.ROOT, "%.1fs", seconds);
                    float tx = ptScratch[0];
                    float ty = ptScratch[1];
                    float badgeW = 34f;
                    float badgeH = 16f;
                    Render2D.drawRoundedRect(vg, tx - badgeW * 0.5f, ty - badgeH * 0.5f, badgeW, badgeH, 4f, 0xCC0F172A);
                    Render2D.drawRoundedOutline(vg, tx - badgeW * 0.5f, ty - badgeH * 0.5f, badgeW, badgeH, 4f, 1f, lineColor);
                    Render2D.drawTextBold(vg, timeText, tx, ty + 0.5f, 9.0f, 0xFFFFFFFF,
                            NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
                }
            }
        }
    }

    private void simulatePearl(MinecraftClient mc, Vec3d startPos, Vec3d startVel, List<Vec3d[]> pathsOut, List<double[]> impactsOut) {
        Vec3d pos = startPos;
        Vec3d vel = startVel;
        List<Vec3d> pts = new ArrayList<>();
        pts.add(pos);

        double hitX = pos.x, hitY = pos.y, hitZ = pos.z;
        int hitTick = 0;

        for (int step = 0; step < 160; step++) {
            Vec3d nextPos = pos.add(vel);
            RaycastContext ctx = new RaycastContext(
                    pos, nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            );
            BlockHitResult bhr = mc.world.raycast(ctx);
            if (bhr != null && bhr.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
                pts.add(bhr.getPos());
                hitX = bhr.getPos().x;
                hitY = bhr.getPos().y;
                hitZ = bhr.getPos().z;
                hitTick = step + 1;
                break;
            }

            pts.add(nextPos);
            pos = nextPos;
            vel = vel.multiply(0.99).subtract(0, 0.03, 0);

            if (pos.y < -64.0) {
                hitX = pos.x;
                hitY = pos.y;
                hitZ = pos.z;
                hitTick = step + 1;
                break;
            }
        }

        pathsOut.add(pts.toArray(new Vec3d[0]));
        impactsOut.add(new double[]{hitX, hitY, hitZ, hitTick / 20.0});
    }
}
