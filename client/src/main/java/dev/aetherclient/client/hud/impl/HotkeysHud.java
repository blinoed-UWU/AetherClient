package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.ModuleManager;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;
import java.util.List;

public final class HotkeysHud extends HudElement {
    private final ModuleManager moduleManager;
    private final HudModule hudModule;
    private final List<Module> activeKeyedModules = new ArrayList<>();

    public HotkeysHud(ModuleManager moduleManager, HudModule hudModule) {
        super("hotkeys", "Активные бинды", 0.02f, 0.45f);
        setTitleEn("Active Keybinds");
        this.moduleManager = moduleManager;
        this.hudModule = hudModule;
        this.width = 130f;
        this.height = 40f;
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.hotkeys.isEnabled();
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        this.elementScale = hudModule.scaleHotkeys.asFloat();
        activeKeyedModules.clear();
        float maxW = 120f;
        long vg = engine.handle();
        for (Module m : moduleManager.getModules()) {
            if (m.isEnabled() && m.getKeybind() != null && m.getKeybind().getValue() != null && m.getKeybind().getValue() > 0) {
                activeKeyedModules.add(m);
                String line = m.getName() + "  [" + m.getKeybind().getKeyLabel() + "]";
                float w = Render2D.measureTextBold(vg, line, 9.5f) + 24f;
                if (w > maxW) maxW = w;
            }
        }
        this.width = maxW;
        this.height = Math.max(34f, 24f + activeKeyedModules.size() * 18f);
    }

    @Override
    public void render(MinecraftClient mc, NanoVgEngine engine, AetherPalette palette, float x, float y, float screenW, float screenH, boolean editMode, float dt) {
        long vg = engine.handle();
        Render2D.drawLiquidGlassPanel(engine, x, y, width, height, 8.5f, screenW, screenH, palette, hudModule.glassOpacity.asFloat(), -999f, -999f, 0f);

        // Header
        Render2D.drawTextBold(vg, I18n.tr("Бинды", "Keybinds"), x + 10f, y + 12f, 10.0f, palette.primary,
                NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

        if (activeKeyedModules.isEmpty()) {
            if (editMode) {
                Render2D.drawText(vg, I18n.tr("Нет активных биндов", "No active keybinds"),
                        x + 10f, y + 26f, 9.0f, palette.onSurfaceVariant,
                        NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            }
            return;
        }

        float rowY = y + 25f;
        for (Module m : activeKeyedModules) {
            Render2D.drawCircle(vg, x + 12f, rowY, 2.5f, palette.primary);
            Render2D.drawText(vg, m.getName(), x + 20f, rowY, 9.2f, palette.onSurface,
                    NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);

            String keyStr = "[" + m.getKeybind().getKeyLabel() + "]";
            Render2D.drawTextBold(vg, keyStr, x + width - 10f, rowY, 8.8f, palette.primary,
                    NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE);

            rowY += 18f;
        }
    }
}
