package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.nanovg.NanoVG;

public final class RadarHud extends HudElement {
    private final HudModule hudModule;

    public RadarHud(HudModule hudModule) {
        super("radar", "Мини-радар", 0.985f, 0.36f);
        setTitleEn("Mini Radar");
        this.hudModule = hudModule;
        this.width = 104f;
        this.height = 104f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.radar.isEnabled();
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        this.width = 104f;
        this.height = 104f;
        this.elementScale = hudModule.scaleRadar.asFloat();
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        this.width = 104f;
        this.height = 104f;
        long vg = engine.handle();

        Render2D.drawLiquidGlassPanel(
                engine, x, y, width, height, 14f, screenW, screenH, palette,
                hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
        );

        float cx = x + width * 0.5f;
        float cy = y + height * 0.5f;
        float range = 48f;
        float half = width * 0.5f - 8f;

        Render2D.drawLine(vg, x + 8f, cy, x + width - 8f, cy, 0.8f, AetherPalette.withAlpha(palette.outline, 75));
        Render2D.drawLine(vg, cx, y + 8f, cx, y + height - 8f, 0.8f, AetherPalette.withAlpha(palette.outline, 75));
        Render2D.drawRing(vg, cx, cy, half * 0.52f, 0.7f, AetherPalette.withAlpha(palette.outline, 55));

        if (mc.player != null && mc.world != null) {
            double yawRad = Math.toRadians(mc.player.getYaw());
            double cos = Math.cos(yawRad);
            double sin = Math.sin(yawRad);

            Render2D.pushScissor(vg, x + 4f, y + 4f, width - 8f, height - 8f);
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player) continue;
                boolean isPlayer = e instanceof PlayerEntity;
                boolean isHostile = e instanceof HostileEntity;
                if (!isPlayer && !isHostile) continue;

                double dx = e.getX() - mc.player.getX();
                double dz = e.getZ() - mc.player.getZ();
                double rx = -(dx * cos + dz * sin);
                double ry = -(dx * sin - dz * cos);

                float px = cx + (float) (rx / range) * half;
                float py = cy + (float) (ry / range) * half;
                if (px < x + 6f || px > x + width - 6f || py < y + 6f || py > y + height - 6f) continue;

                int dotCol = isPlayer ? palette.primary : palette.tertiary;
                Render2D.drawCircle(vg, px, py, isPlayer ? 3.2f : 2.3f, dotCol);
            }
            Render2D.popScissor(vg);
        }

        // Center player marker
        Render2D.drawCircle(vg, cx, cy, 3.0f, palette.onSurface);
        Render2D.drawTextBold(vg, I18n.tr("РАДАР", "RADAR"), x + 9f, y + 10f, 8.2f,
                AetherPalette.withAlpha(palette.onSurfaceVariant, 180), NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
    }
}
