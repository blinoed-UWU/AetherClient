package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.module.impl.TargetHudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.render.WorldToScreen;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.nanovg.NanoVG;

public final class TargetHudElement extends HudElement {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private final TargetHudModule module;
    private final HudModule hudModule;
    private WorldToScreen wts;

    private LivingEntity currentTarget;
    private Object lastWorld;
    private long lastSeenMs;
    private float smoothHp = 1f;
    private float smoothAbsorb = 0f;
    private float showAnim = 0f;
    private String cachedName = "";
    private String cachedSub = "";
    private boolean hasCached = false;

    private float smoothFollowX = Float.NaN;
    private float smoothFollowY = Float.NaN;
    private final float[] ptScratch = new float[3];

    public TargetHudElement(TargetHudModule module, HudModule hudModule) {
        super("targethud", "Карточка цели", 0.56f, 0.62f);
        setTitleEn("Target HUD");
        this.module = module;
        this.hudModule = hudModule;
        this.width = 172f;
        this.height = 50f;
    }

    public void setWorldToScreen(WorldToScreen wts) {
        this.wts = wts;
    }

    public void onAttackEntity(Entity target) {
        if (target instanceof LivingEntity living && isAllowed(living)) {
            currentTarget = living;
            lastSeenMs = System.currentTimeMillis();
        }
    }

    private boolean isAllowed(LivingEntity entity) {
        if (!entity.isAlive()) return false;
        return !module.onlyPlayers.isEnabled() || entity instanceof PlayerEntity;
    }

    @Override
    public boolean isActive() {
        return module.isEnabled();
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        String style = module.cardStyle.getValue();
        if ("Компакт".equals(style)) {
            this.width = 148f;
            this.height = 36f;
        } else if ("Киберспорт".equals(style)) {
            this.width = 188f;
            this.height = module.showArmor.isEnabled() ? 56f : 50f;
        } else if ("Целестиал".equals(style)) {
            this.width = module.showArmor.isEnabled() ? 182f : 170f;
            this.height = module.showArmor.isEnabled() ? 56f : 50f;
        } else if ("Круг".equals(style)) {
            this.width = module.showArmor.isEnabled() ? 204f : 192f;
            this.height = 54f;
        } else {
            boolean hasArmor = module.showArmor.isEnabled();
            this.width = hasArmor ? 178f : 166f;
            this.height = hasArmor ? 54f : 48f;
        }
        this.elementScale = hudModule.scaleTarget.asFloat();
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        if (mc.world != lastWorld) {
            lastWorld = mc.world;
            currentTarget = null;
            showAnim = 0f;
            hasCached = false;
            smoothFollowX = Float.NaN;
            smoothFollowY = Float.NaN;
        }

        long now = System.currentTimeMillis();
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living && isAllowed(living)) {
            currentTarget = living;
            lastSeenMs = now;
        } else if (mc.world != null) {
            // Teammate/friend shared vision: if teammate is attacking or fighting a target
            for (PlayerEntity friend : mc.world.getPlayers()) {
                if (friend == mc.player) continue;
                if (!dev.aetherclient.client.friend.FriendsManager.get().isFriend(friend.getName().getString())) continue;

                for (Entity e : mc.world.getEntities()) {
                    if (!(e instanceof LivingEntity living) || e == friend || e == mc.player) continue;
                    if (dev.aetherclient.client.friend.FriendsManager.get().isFriend(e.getName().getString())) continue;
                    if (!isAllowed(living)) continue;

                    if (living.hurtTime > 0 && friend.distanceTo(living) < 5.5f) {
                        currentTarget = living;
                        lastSeenMs = now;
                        break;
                    }
                }
            }
        }

        LivingEntity display = null;
        if (currentTarget != null && currentTarget.isAlive() && (now - lastSeenMs) <= Math.round(module.holdSeconds.asFloat() * 1000f)) {
            display = currentTarget;
        } else if (editMode && mc.player != null) {
            display = mc.player;
        }

        boolean showSample = editMode && display == null;
        float targetAlpha = (display != null || showSample) ? 1f : 0f;
        showAnim += (targetAlpha - showAnim) * Math.min(1f, dt * 14f);

        float hpRatio;
        float absorbAmount = 0f;
        ItemStack[] armorStacks = null;

        if (display != null) {
            float maxHp = Math.max(1f, display.getMaxHealth());
            hpRatio = Math.max(0f, Math.min(1f, display.getHealth() / maxHp));
            absorbAmount = display.getAbsorptionAmount();

            String name = display.getName().getString();
            if (name.length() > 15) name = name.substring(0, 14) + "...";
            float dist = mc.player != null ? mc.player.distanceTo(display) : 0f;
            cachedName = name;

            if (module.showDistance.isEnabled()) {
                cachedSub = String.format(java.util.Locale.ROOT, "%.1f HP  •  %.1f %s",
                        display.getHealth(), dist, I18n.tr("м", "m"));
            } else {
                cachedSub = String.format(java.util.Locale.ROOT, "%.1f / %.1f HP",
                        display.getHealth(), maxHp);
            }
            if (absorbAmount > 0.05f && module.showAbsorption.isEnabled()) {
                cachedSub += String.format(java.util.Locale.ROOT, " +%.1f", absorbAmount);
            }

            if (module.showArmor.isEnabled()) {
                armorStacks = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    armorStacks[i] = display.getEquippedStack(ARMOR_SLOTS[i]);
                }
            }
            hasCached = true;
        } else if (showSample) {
            hpRatio = 0.85f;
            absorbAmount = 4.0f;
            cachedName = "Aether Target";
            cachedSub = I18n.tr("17.0 HP  •  3.5 м +4.0", "17.0 HP  •  3.5 m +4.0");
            hasCached = true;
        } else {
            if (showAnim < 0.02f || !hasCached) return;
            hpRatio = smoothHp;
        }

        if (showAnim < 0.02f) return;
        smoothHp += (hpRatio - smoothHp) * Math.min(1f, dt * 12f);
        smoothAbsorb += (absorbAmount - smoothAbsorb) * Math.min(1f, dt * 10f);

        String cardStyle = module.cardStyle.getValue();
        boolean hasArmor = module.showArmor.isEnabled();
        if ("Компакт".equals(cardStyle)) {
            this.width = 148f;
            this.height = 36f;
        } else if ("Киберспорт".equals(cardStyle)) {
            this.width = 188f;
            this.height = hasArmor ? 56f : 50f;
        } else if ("Целестиал".equals(cardStyle)) {
            this.width = hasArmor ? 182f : 170f;
            this.height = hasArmor ? 56f : 50f;
        } else if ("Круг".equals(cardStyle)) {
            this.width = hasArmor ? 204f : 192f;
            this.height = 54f;
        } else {
            this.width = hasArmor ? 178f : 166f;
            this.height = hasArmor ? 54f : 48f;
        }

        float renderX = x;
        float renderY = y;
        float cardScale = 1.0f;
        String posMode = module.positionMode.getValue();

        // 3D Anchor follow mode (Under feet, above head, side)
        boolean anchoredToWorld = !"Экран (HUD)".equals(posMode) && display != null && wts != null && wts.isValid();
        if (anchoredToWorld) {
            double anchorX = display.getX();
            double anchorY;
            double anchorZ = display.getZ();

            if ("Под ногами".equals(posMode)) {
                anchorY = display.getY() - 0.05;
            } else if ("Над головой".equals(posMode)) {
                anchorY = display.getY() + display.getHeight() + 0.35;
            } else {
                anchorY = display.getY() + display.getHeight() * 0.55;
                anchorX += 0.50;
            }

            if (wts.project(anchorX, anchorY, anchorZ, ptScratch)) {
                float targetScreenX;
                float targetScreenY;

                if ("Под ногами".equals(posMode)) {
                    targetScreenX = ptScratch[0] - width * 0.5f;
                    targetScreenY = ptScratch[1] + 6f;
                } else if ("Над головой".equals(posMode)) {
                    targetScreenX = ptScratch[0] - width * 0.5f;
                    targetScreenY = ptScratch[1] - height - 8f;
                } else {
                    targetScreenX = ptScratch[0] + 12f;
                    targetScreenY = ptScratch[1] - height * 0.5f;
                }

                targetScreenX = Math.max(8f, Math.min(screenW - width - 8f, targetScreenX));
                targetScreenY = Math.max(8f, Math.min(screenH - height - 8f, targetScreenY));

                if (module.smoothFollow.isEnabled()) {
                    if (Float.isNaN(smoothFollowX)) {
                        smoothFollowX = targetScreenX;
                        smoothFollowY = targetScreenY;
                    } else {
                        float speed = module.followSpeed.asFloat();
                        smoothFollowX += (targetScreenX - smoothFollowX) * Math.min(1.0f, dt * speed);
                        smoothFollowY += (targetScreenY - smoothFollowY) * Math.min(1.0f, dt * speed);
                    }
                    renderX = smoothFollowX;
                    renderY = smoothFollowY;
                } else {
                    renderX = targetScreenX;
                    renderY = targetScreenY;
                }

                if (module.scaleByDistance.isEnabled() && mc.player != null) {
                    float dist = mc.player.distanceTo(display);
                    cardScale = Math.max(0.70f, Math.min(1.15f, 6.8f / (float) Math.sqrt(Math.max(1f, dist))));
                }
            }
        } else {
            smoothFollowX = Float.NaN;
            smoothFollowY = Float.NaN;
        }

        long vg = engine.handle();
        NanoVG.nvgSave(vg);
        NanoVG.nvgGlobalAlpha(vg, showAnim);

        if (cardScale != 1.0f) {
            float cx = renderX + width * 0.5f;
            float cy = renderY + height * 0.5f;
            NanoVG.nvgTranslate(vg, cx, cy);
            NanoVG.nvgScale(vg, cardScale, cardScale);
            NanoVG.nvgTranslate(vg, -cx, -cy);
        }

        String initial = cachedName.isEmpty() ? "?" : cachedName.substring(0, 1).toUpperCase(java.util.Locale.ROOT);

        if ("Компакт".equals(cardStyle)) {
            // Minimalist Low-Profile Pill
            Render2D.drawShadow(vg, renderX, renderY, width, height, 7f, 10f, 0x44000000);
            Render2D.drawRoundedRect(vg, renderX, renderY, width, height, 8f, AetherPalette.withAlpha(palette.surfaceContainerHigh, 235));
            Render2D.drawRoundedOutline(vg, renderX, renderY, width, height, 8f, 1.1f, AetherPalette.withAlpha(palette.primary, 150));

            // Small 22x22 Avatar
            Render2D.drawRoundedGradient(vg, renderX + 6f, renderY + 7f, 22f, 22f, 6f, palette.primary, palette.tertiary, false);
            Render2D.drawTextBold(vg, initial, renderX + 17f, renderY + 18f, 10.5f, palette.onPrimary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            // Single line info
            Render2D.drawTextBold(vg, cachedName, renderX + 34f, renderY + 14f, 10.0f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            int percent = Math.max(0, Math.min(100, Math.round(smoothHp * 100f)));
            Render2D.drawTextBold(vg, percent + "%", renderX + width - 8f, renderY + 14f, 9.5f, palette.primary, NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);

            // Compact Bar
            float barX = renderX + 34f;
            float barY = renderY + 23f;
            float barW = width - 42f;
            float barH = 3.2f;
            Render2D.drawRoundedRect(vg, barX, barY, barW, barH, 1.6f, AetherPalette.withAlpha(palette.onSurface, 36));
            if (smoothHp > 0.002f) {
                float fillW = Math.min(barW, Math.max(3.2f, barW * smoothHp));
                Render2D.drawRoundedGradient(vg, barX, barY, fillW, barH, 1.6f, palette.primary, palette.tertiary, false);
            }
        } else if ("Целестиал".equals(cardStyle)) {
            // Ethereal Midnight Celestial Aesthetic
            Render2D.drawGlowBox(vg, renderX, renderY, width, height, 11f, 5.0f, AetherPalette.withAlpha(0xFF818CF8, 70));
            Render2D.drawRoundedRect(vg, renderX, renderY, width, height, 10.5f, 0xEB0A0D16);
            Render2D.drawRoundedGradient(vg, renderX, renderY, width, 2.5f, 1f, 0xFF818CF8, 0xFF38BDF8, false);
            Render2D.drawRoundedOutline(vg, renderX, renderY, width, height, 10.5f, 1.25f, AetherPalette.withAlpha(0xFF818CF8, 180));

            // Celestial Avatar with cyan-violet glow
            float avSize = hasArmor ? 33f : 30f;
            Render2D.drawRoundedGradient(vg, renderX + 8.5f, renderY + 8.5f, avSize, avSize, 9f, 0xFF6366F1, 0xFF06B6D4, false);
            Render2D.drawTextBold(vg, initial, renderX + 8.5f + avSize * 0.5f, renderY + 8.5f + avSize * 0.5f + 0.5f, 13.5f,
                    0xFFFFFFFF, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            float textX = renderX + (hasArmor ? 48f : 45f);
            Render2D.drawTextBold(vg, "★ " + cachedName, textX, renderY + 15f, 11.2f, 0xFFE0E7FF, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            Render2D.drawText(vg, cachedSub, textX, renderY + 27f, 9.5f, 0xFFA5B4FC, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

            // Celestial Glowing Bar
            float barX = textX;
            float barY = renderY + 36f;
            float barW = width - (hasArmor ? 56f : 52f);
            float barH = 4.5f;
            Render2D.drawRoundedRect(vg, barX, barY, barW, barH, 2.2f, 0x442E3856);

            if (smoothHp > 0.002f) {
                float fillW = Math.min(barW, Math.max(4.5f, barW * smoothHp));
                Render2D.drawGlowBox(vg, barX, barY, fillW, barH, 2.2f, 3.0f, AetherPalette.withAlpha(0xFF38BDF8, 85));
                Render2D.drawRoundedGradient(vg, barX, barY, fillW, barH, 2.2f, 0xFF818CF8, 0xFF38BDF8, false);
            }
            if (smoothAbsorb > 0.1f && module.showAbsorption.isEnabled()) {
                float absRatio = Math.min(1.0f, smoothAbsorb / 20.0f);
                float absW = Math.min(barW, barW * absRatio);
                Render2D.drawRoundedRect(vg, barX, barY - 1f, absW, 2.0f, 1.0f, 0xFFFFD700);
            }
        } else if ("Киберспорт".equals(cardStyle)) {
            // High-Tech Esports HUD Panel
            Render2D.drawShadow(vg, renderX, renderY, width, height, 6f, 14f, 0x66000000);
            Render2D.drawRoundedRect(vg, renderX, renderY, width, height, 4f, 0xF20F1117);
            Render2D.drawCornerRect(vg, renderX, renderY, width, height, 11f, 1.8f, palette.primary);

            // Tech Avatar
            float avSize = hasArmor ? 33f : 30f;
            Render2D.drawRoundedRect(vg, renderX + 8.5f, renderY + 8.5f, avSize, avSize, 3f, 0xFF1E222D);
            Render2D.drawRoundedOutline(vg, renderX + 8.5f, renderY + 8.5f, avSize, avSize, 3f, 1.2f, palette.primary);
            Render2D.drawTextBold(vg, initial, renderX + 8.5f + avSize * 0.5f, renderY + 8.5f + avSize * 0.5f + 0.5f, 13.5f,
                    palette.primary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            float textX = renderX + (hasArmor ? 48f : 45f);
            Render2D.drawTextBold(vg, cachedName, textX, renderY + 15f, 11.2f, 0xFFFFFFFF, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

            // HP % badge
            int percent = Math.max(0, Math.min(100, Math.round(smoothHp * 100f)));
            int badgeCol = percent > 60 ? 0xFF22C55E : (percent > 25 ? 0xFFF59E0B : 0xFFEF4444);
            String badgeStr = percent + "%";
            float bw = Render2D.measureTextBold(vg, badgeStr, 9.0f) + 8f;
            Render2D.drawRoundedRect(vg, renderX + width - bw - 8f, renderY + 9f, bw, 13f, 2.5f, AetherPalette.withAlpha(badgeCol, 40));
            Render2D.drawRoundedOutline(vg, renderX + width - bw - 8f, renderY + 9f, bw, 13f, 2.5f, 1f, badgeCol);
            Render2D.drawTextBold(vg, badgeStr, renderX + width - bw * 0.5f - 8f, renderY + 15.5f, 9.0f, badgeCol,
                    NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            Render2D.drawText(vg, cachedSub, textX, renderY + 27f, 9.2f, 0xFF9CA3AF, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

            // Segmented Tactical Bar
            float barX = textX;
            float barY = renderY + 36f;
            float barW = width - (hasArmor ? 56f : 52f);
            float barH = 5.0f;
            Render2D.drawRoundedRect(vg, barX, barY, barW, barH, 1.5f, 0xFF1E222D);

            if (smoothHp > 0.002f) {
                float fillW = Math.min(barW, Math.max(4.0f, barW * smoothHp));
                Render2D.drawRoundedRect(vg, barX, barY, fillW, barH, 1.5f, badgeCol);
            }
        } else if ("Круг".equals(cardStyle)) {
            // Liquid Glass card with circular HP & Item consumption meter
            Render2D.drawLiquidGlassPanel(
                    engine, renderX, renderY, width, height, 11.5f, screenW, screenH, palette,
                    hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
            );

            // Monogram avatar
            float avSize = 32f;
            Render2D.drawRoundedGradient(vg, renderX + 8.5f, renderY + 11f, avSize, avSize, 9f, palette.primary, palette.tertiary, false);
            Render2D.drawTextBold(vg, initial, renderX + 8.5f + avSize * 0.5f, renderY + 11f + avSize * 0.5f + 0.5f, 13.5f,
                    palette.onPrimary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            float textX = renderX + 46f;
            Render2D.drawTextBold(vg, cachedName, textX, renderY + 18f, 11.5f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            Render2D.drawText(vg, cachedSub, textX, renderY + 34f, 9.5f, palette.onSurfaceVariant, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

            // Circular meter on the right
            float circCenterX = renderX + width - 26f;
            float circCenterY = renderY + height * 0.5f;
            float circRadius = 18f;

            // Background circle track
            Render2D.drawCircleOutline(vg, circCenterX, circCenterY, circRadius, 3.2f, AetherPalette.withAlpha(palette.onSurface, 36));

            // Outer Health Arc
            int hpCol = smoothHp > 0.55f ? 0xFF34D399 : (smoothHp > 0.25f ? 0xFFFBBF24 : 0xFFF87171);
            if (smoothHp > 0.01f) {
                float endDeg = -90f + 360f * Math.min(1.0f, smoothHp);
                Render2D.drawArcDeg(vg, circCenterX, circCenterY, circRadius, -90f, endDeg, 3.2f, hpCol);
            }

            // Consumption progress: is target eating, drinking, or using an item?
            boolean isUsing = display != null && display.isUsingItem();
            if (isUsing) {
                ItemStack activeItem = display.getActiveItem();
                int useTime = display.getItemUseTime();
                int maxUse = Math.max(1, activeItem.getMaxUseTime(display));
                float prog = Math.min(1.0f, (float) useTime / (float) maxUse);

                // Inner eating/drinking progress circle
                Render2D.drawArcDeg(vg, circCenterX, circCenterY, circRadius - 5.5f, -90f, -90f + 360f * prog, 2.5f, 0xFFFFB74D);

                int pct = Math.round(prog * 100f);
                Render2D.drawTextBold(vg, pct + "%", circCenterX, circCenterY, 8.5f, 0xFFFFE082, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            } else {
                float hpVal = display != null ? display.getHealth() : 20f;
                String hpStr = String.format(java.util.Locale.ROOT, "%.0f", hpVal);
                Render2D.drawTextBold(vg, hpStr, circCenterX, circCenterY, 10.5f, palette.onSurface, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            }
        } else {
            // Liquid Glass Panel with M3 elevation & specular shine
            Render2D.drawLiquidGlassPanel(
                    engine, renderX, renderY, width, height, 11.5f, screenW, screenH, palette,
                    hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
            );

            // Monogram avatar pill
            float avSize = hasArmor ? 33f : 30f;
            Render2D.drawRoundedGradient(vg, renderX + 8.5f, renderY + 8.5f, avSize, avSize, 9f, palette.primary, palette.tertiary, false);
            Render2D.drawTextBold(vg, initial, renderX + 8.5f + avSize * 0.5f, renderY + 8.5f + avSize * 0.5f + 0.5f, 13.5f,
                    palette.onPrimary, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);

            float textX = renderX + (hasArmor ? 48f : 45f);
            Render2D.drawTextBold(vg, cachedName, textX, renderY + 15f, 11.2f, palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            Render2D.drawText(vg, cachedSub, textX, renderY + 27f, 9.5f, palette.onSurfaceVariant, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

            // Health Bar
            float barX = textX;
            float barY = renderY + 36f;
            float barW = width - (hasArmor ? 56f : 52f);
            float barH = 4.2f;

            Render2D.drawRoundedRect(vg, barX, barY, barW, barH, 2.1f, AetherPalette.withAlpha(palette.onSurface, 36));

            if (smoothHp > 0.002f) {
                float fillW = Math.min(barW, Math.max(4.2f, barW * smoothHp));
                if ("Градиент".equals(module.barStyle.getValue())) {
                    Render2D.drawRoundedGradient(vg, barX, barY, fillW, barH, 2.1f, palette.primary, palette.tertiary, false);
                } else {
                    Render2D.drawRoundedRect(vg, barX, barY, fillW, barH, 2.1f, palette.primary);
                }
            }

            if (smoothAbsorb > 0.1f && module.showAbsorption.isEnabled()) {
                float absRatio = Math.min(1.0f, smoothAbsorb / 20.0f);
                float absW = Math.min(barW, barW * absRatio);
                Render2D.drawRoundedRect(vg, barX, barY - 1f, absW, 2.0f, 1.0f, 0xFFFFD700);
            }
        }

        // Target Armor Mini-Pills (rendered for all styles if enabled)
        if (hasArmor && armorStacks != null && !"Компакт".equals(cardStyle)) {
            float armorStartX = renderX + width - 42f;
            float armorY = renderY + 11f;
            for (int i = 0; i < 4; i++) {
                ItemStack stack = armorStacks[i];
                float ax = armorStartX + i * 8.5f;
                int dotCol = 0x55888888;
                if (stack != null && !stack.isEmpty()) {
                    if (stack.isDamageable()) {
                        float dura = 1.0f - (float) stack.getDamage() / Math.max(1, stack.getMaxDamage());
                        dotCol = dura > 0.6f ? 0xFF4ADE80 : (dura > 0.25f ? 0xFFFBBF24 : 0xFFF87171);
                    } else {
                        dotCol = palette.primary;
                    }
                }
                Render2D.drawRoundedRect(vg, ax, armorY, 6.5f, 5.0f, 2.0f, dotCol);
            }
        }

        NanoVG.nvgRestore(vg);
    }
}
