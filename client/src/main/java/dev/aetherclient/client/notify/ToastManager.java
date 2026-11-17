package dev.aetherclient.client.notify;

import dev.aetherclient.client.i18n.I18n;
import dev.aetherclient.client.module.Module;
import dev.aetherclient.client.module.impl.NotificationsModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-contained stack of transient glass toast notifications. Toasts are pushed
 * from module toggle sites and rendered inside the world overlay so they never
 * require a dedicated render pass or extra NanoVG frame.
 */
public final class ToastManager {
    private static final ToastManager INSTANCE = new ToastManager();

    public static ToastManager get() {
        return INSTANCE;
    }

    private static final class Toast {
        String title;
        String subtitle;
        int accentArgb;
        float age;
        float maxAge;
        float enterAnim;
    }

    private final List<Toast> toasts = new ArrayList<>();
    private float globalTime = 0f;

    private ToastManager() {}

    public void push(String title, String subtitle, int accentArgb) {
        push(title, subtitle, accentArgb, 3.2f);
    }

    public void push(String title, String subtitle, int accentArgb, float maxAgeSec) {
        Toast t = new Toast();
        t.title = title;
        t.subtitle = subtitle;
        t.accentArgb = accentArgb;
        t.maxAge = Math.max(1f, maxAgeSec);
        toasts.add(t);
        while (toasts.size() > 8) {
            toasts.remove(0);
        }
    }

    public void pushModuleToggle(Module module) {
        pushModuleToggle(module, null);
    }

    public void pushModuleToggle(Module module, NotificationsModule settings) {
        boolean on = module.isEnabled();
        float maxAge = settings != null ? settings.duration.asFloat() : 3.2f;
        push(
                module.getName(),
                on ? I18n.tr("Включено", "Enabled") : I18n.tr("Выключено", "Disabled"),
                on ? 0xFF5CE092 : 0xFFB8B6C8,
                maxAge
        );
    }

    public boolean hasActive() {
        return !toasts.isEmpty();
    }

    public void clear() {
        toasts.clear();
    }

    public void render(
            NanoVgEngine engine,
            AetherPalette palette,
            float screenW,
            float screenH,
            float dt,
            NotificationsModule settings
    ) {
        if (toasts.isEmpty()) {
            return;
        }
        globalTime += dt;
        long vg = engine.handle();

        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast t = toasts.get(i);
            t.age += dt;
            t.enterAnim = Math.min(1f, t.enterAnim + dt * 5.0f);
            if (t.age >= t.maxAge && t.enterAnim >= 1f) {
                toasts.remove(i);
            }
        }
        if (toasts.isEmpty()) {
            return;
        }

        int maxVisible = (int) Math.max(1, Math.min(8, settings.maxToasts.asFloat()));
        int shown = Math.min(maxVisible, toasts.size());
        float cardW = Math.min(240f, Math.max(180f, screenW * 0.24f));
        float cardH = 44f;
        float gap = 6f;
        float margin = 12f;

        boolean top = settings.position.getValue().contains("верх");
        boolean right = settings.position.getValue().contains("прав");

        float totalH = shown * (cardH + gap) - gap;
        float originX = right ? screenW - margin - cardW : margin;
        float originY = top ? margin : screenH - margin - totalH;

        int accent = palette.resolvePalette(
                settings.palette.getValue(),
                settings.accentColor.getValue(),
                globalTime,
                settings.colorSpeed.asFloat()
        );

        int idx = 0;
        for (int i = Math.max(0, toasts.size() - shown); i < toasts.size(); i++) {
            Toast t = toasts.get(i);
            float slide = (1f - t.enterAnim) * (top ? -cardH : cardH);
            float tx = originX;
            float ty = originY + idx * (cardH + gap) + slide;

            float cardAlpha = Math.max(0f, Math.min(1f, t.enterAnim));
            if (t.age > t.maxAge - 0.35f) {
                cardAlpha = Math.max(0f, (t.maxAge - t.age) / 0.35f);
            }

            NanoVG.nvgSave(vg);
            NanoVG.nvgGlobalAlpha(vg, cardAlpha);
            Render2D.drawLiquidGlassPanel(
                    engine, tx, ty, cardW, cardH, 9f, screenW, screenH, palette,
                    settings.opacity.asFloat(), -999f, -999f, 0f
            );
            Render2D.drawRoundedRect(vg, tx, ty + 5f, 3f, cardH - 10f, 1.5f, t.accentArgb == 0 ? accent : t.accentArgb);
            Render2D.drawTextBold(vg, t.title, tx + 12f, ty + 13f, 10.4f,
                    palette.onSurface, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            Render2D.drawText(vg, t.subtitle, tx + 12f, ty + 29f, 9.4f,
                    palette.onSurfaceVariant, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
            NanoVG.nvgRestore(vg);

            idx++;
        }
    }
}
