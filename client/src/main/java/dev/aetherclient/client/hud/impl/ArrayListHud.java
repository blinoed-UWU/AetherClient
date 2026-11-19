package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.ModuleManager;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ArrayListHud extends HudElement {
    private final ModuleManager moduleManager;
    private final HudModule hudModule;

    // Per-frame scratch state for ArrayListHud: allocated once, reused every frame.
    private final java.util.List<Module> scratchItems = new ArrayList<>();
    private final java.util.List<Module> sortedScratch = new ArrayList<>();
    private float[] scratchWidths = new float[16];

    public ArrayListHud(ModuleManager moduleManager, HudModule hudModule) {
        super("arraylist", "Список модулей", 0.985f, 0.02f);
        setTitleEn("Module List");
        this.moduleManager = moduleManager;
        this.hudModule = hudModule;
        this.width = 140f;
        this.height = 80f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.arrayList.isEnabled();
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        long vg = engine.handle();
        float maxW = 0f;
        float totalH = 0f;
        float rowH = 19f;
        float rowGap = 2.5f;
        int count = 0;
        for (Module m : moduleManager.getModules()) {
            if (m == moduleManager.clickGui) continue;
            if (m.isEnabled() || m.toggleAnim > 0.02f) {
                maxW = Math.max(maxW, measureModuleWidth(vg, m));
                float anim = m.toggleAnim > 0.02f ? m.toggleAnim : (m.isEnabled() ? 1f : 0f);
                totalH += (rowH + rowGap) * anim;
                count++;
            }
        }
        if (count == 0) {
            this.width = 114f;
            this.height = 24f;
        } else {
            this.width = Math.max(84f, maxW);
            this.height = Math.max(20f, totalH - rowGap);
        }
        this.elementScale = hudModule.scaleArrayList.asFloat();
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        long vg = engine.handle();
        // Reused scratch list: no per-frame ArrayList allocation.
        List<Module> items = this.scratchItems;
        items.clear();
        for (Module m : moduleManager.getModules()) {
            if (m == moduleManager.clickGui) continue;
            float target = m.isEnabled() ? 1f : 0f;
            m.toggleAnim += (target - m.toggleAnim) * Math.min(1f, dt * 14f);
            if (m.toggleAnim > 0.02f || (editMode && items.size() < 2)) {
                items.add(m);
            }
        }

        if (items.isEmpty()) {
            this.width = 114f;
            this.height = 24f;
            return;
        }

        // Measure once per module into a parallel array and sort indices,
        // instead of O(n log n) NanoVG text measures per frame via a comparator.
        int n = items.size();
        if (scratchWidths.length < n) scratchWidths = new float[n * 2];
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
            scratchWidths[i] = measureModuleWidth(vg, items.get(i));
        }
        java.util.Arrays.sort(order, (a, b) -> Float.compare(scratchWidths[b], scratchWidths[a]));
        List<Module> sorted = this.sortedScratch;
        sorted.clear();
        for (int i = 0; i < n; i++) sorted.add(items.get(order[i]));
        items.clear();
        items.addAll(sorted);

        float maxW = 0f;
        float totalH = 0f;
        float rowH = 19f;
        float rowGap = 2.5f;
        for (int i = 0; i < n; i++) {
            maxW = Math.max(maxW, scratchWidths[i]);
            float anim = editMode ? Math.max(0.75f, items.get(i).toggleAnim) : items.get(i).toggleAnim;
            totalH += (rowH + rowGap) * anim;
        }
        this.width = Math.max(84f, maxW);
        this.height = Math.max(20f, totalH - rowGap);

        boolean alignRight = getNormX() >= 0.5f;
        float curY = y;

        for (Module m : items) {
            float anim = editMode ? Math.max(0.75f, m.toggleAnim) : m.toggleAnim;
            float w = measureModuleWidth(vg, m);
            float rowX = alignRight ? (x + width - w) : x;
            float slideOffset = alignRight ? (1f - anim) * 15f : -(1f - anim) * 15f;

            NanoVG.nvgSave(vg);
            NanoVG.nvgGlobalAlpha(vg, Math.max(0f, Math.min(1f, anim)));
            Render2D.drawLiquidGlassPanel(
                    engine, rowX + slideOffset, curY, w, rowH, 6.5f, screenW, screenH, palette,
                    hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
            );

            float accentX = alignRight ? (rowX + slideOffset + w - 2.5f) : (rowX + slideOffset);
            Render2D.drawRoundedRect(vg, accentX, curY + 3.5f, 2.5f, rowH - 7f, 1.2f, palette.primary);

            String suffix = m.getDisplaySuffix();
            float textX = rowX + slideOffset + (alignRight ? 7.5f : 9f);
            Render2D.drawTextBold(vg, m.getName(), textX, curY + rowH * 0.5f + 0.5f, 10.6f,
                    palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            if (!suffix.isEmpty()) {
                float nameW = Render2D.measureTextBold(vg, m.getName(), 10.6f);
                Render2D.drawText(vg, suffix, textX + nameW + 5f, curY + rowH * 0.5f + 0.5f, 9.8f,
                        palette.onSurfaceVariant, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            }
            NanoVG.nvgRestore(vg);

            curY += (rowH + rowGap) * anim;
        }
    }

    private float measureModuleWidth(long vg, Module m) {
        String suffix = m.getDisplaySuffix();
        float w = Render2D.measureTextBold(vg, m.getName(), 10.6f) + 18f;
        if (!suffix.isEmpty()) {
            w += 5f + Render2D.measureText(vg, suffix, 9.8f);
        }
        return w;
    }
}
