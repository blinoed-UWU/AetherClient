package dev.aetherclient.client.hud.impl;

import dev.aetherclient.client.hud.HudElement;
import dev.aetherclient.client.module.impl.HudModule;
import dev.aetherclient.client.render.NanoVgEngine;
import dev.aetherclient.client.render.Render2D;
import dev.aetherclient.client.theme.AetherPalette;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayDeque;
import java.util.Deque;

public final class KeystrokesHud extends HudElement {
    private final HudModule hudModule;
    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();

    private float wAnim, aAnim, sAnim, dAnim, lmbAnim, rmbAnim, spaceAnim;

    public KeystrokesHud(HudModule hudModule) {
        super("keystrokes", "Клавиши и CPS", 0.014f, 0.34f);
        setTitleEn("Keystrokes & CPS");
        this.hudModule = hudModule;
        this.width = 85f;
        this.height = 106f;
    }

    public void recordMouseClick(int button) {
        // Prune here as well: with the widget hidden render() never runs,
        // so unpruned deques would grow for the whole session.
        long now = System.currentTimeMillis();
        if (button == 0) {
            leftClicks.addLast(now);
            pruneAndCount(leftClicks, now);
        } else if (button == 1) {
            rightClicks.addLast(now);
            pruneAndCount(rightClicks, now);
        }
    }

    private static int pruneAndCount(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() > 1000L) {
            deque.pollFirst();
        }
        return deque.size();
    }

    @Override
    public boolean isActive() {
        return hudModule.isEnabled() && hudModule.keystrokes.isEnabled();
    }

    @Override
    public void prepareSize(MinecraftClient mc, NanoVgEngine engine) {
        float keySize = 27f;
        float gap = 3.5f;
        float spaceH = 18f;
        this.width = keySize * 3f + gap * 2f;
        this.height = keySize * 3f + gap * 3f + spaceH;
        this.elementScale = hudModule.scaleKeystrokes.asFloat();
    }

    @Override
    public void render(
            MinecraftClient mc, NanoVgEngine engine, AetherPalette palette,
            float x, float y, float screenW, float screenH, boolean editMode, float dt
    ) {
        float keySize = 27f;
        float gap = 3.5f;
        float spaceH = 18f;
        this.width = keySize * 3f + gap * 2f;
        this.height = keySize * 3f + gap * 3f + spaceH;

        boolean pW = mc.options != null && mc.options.forwardKey.isPressed();
        boolean pA = mc.options != null && mc.options.leftKey.isPressed();
        boolean pS = mc.options != null && mc.options.backKey.isPressed();
        boolean pD = mc.options != null && mc.options.rightKey.isPressed();
        boolean pL = mc.options != null && mc.options.attackKey.isPressed();
        boolean pR = mc.options != null && mc.options.useKey.isPressed();
        boolean pSpace = mc.options != null && mc.options.jumpKey.isPressed();

        float speed = Math.min(1f, dt * 16f);
        wAnim += ((pW ? 1f : 0f) - wAnim) * speed;
        aAnim += ((pA ? 1f : 0f) - aAnim) * speed;
        sAnim += ((pS ? 1f : 0f) - sAnim) * speed;
        dAnim += ((pD ? 1f : 0f) - dAnim) * speed;
        lmbAnim += ((pL ? 1f : 0f) - lmbAnim) * speed;
        rmbAnim += ((pR ? 1f : 0f) - rmbAnim) * speed;
        spaceAnim += ((pSpace ? 1f : 0f) - spaceAnim) * speed;

        long now = System.currentTimeMillis();
        int lCps = pruneAndCount(leftClicks, now);
        int rCps = pruneAndCount(rightClicks, now);

        drawKeyTile(engine, palette, x + keySize + gap, y, keySize, keySize, "W", null, wAnim, screenW, screenH);
        drawKeyTile(engine, palette, x, y + keySize + gap, keySize, keySize, "A", null, aAnim, screenW, screenH);
        drawKeyTile(engine, palette, x + keySize + gap, y + keySize + gap, keySize, keySize, "S", null, sAnim, screenW, screenH);
        drawKeyTile(engine, palette, x + (keySize + gap) * 2f, y + keySize + gap, keySize, keySize, "D", null, dAnim, screenW, screenH);

        float mbY = y + (keySize + gap) * 2f;
        float mbW = (width - gap) * 0.5f;
        drawKeyTile(engine, palette, x, mbY, mbW, keySize, "LMB", lCps + " CPS", lmbAnim, screenW, screenH);
        drawKeyTile(engine, palette, x + mbW + gap, mbY, mbW, keySize, "RMB", rCps + " CPS", rmbAnim, screenW, screenH);

        float spY = mbY + keySize + gap;
        drawKeyTile(engine, palette, x, spY, width, spaceH, "SPACE", null, spaceAnim, screenW, screenH);
    }

    private void drawKeyTile(
            NanoVgEngine engine, AetherPalette palette,
            float x, float y, float w, float h,
            String title, String sub, float press,
            float screenW, float screenH
    ) {
        Render2D.drawLiquidGlassPanel(
                engine, x, y, w, h, 6.5f, screenW, screenH, palette,
                hudModule.glassOpacity.asFloat(), -999f, -999f, 0f
        );
        if (press > 0.02f) {
            int fill = AetherPalette.withAlpha(palette.primary, Math.round(press * 185f));
            Render2D.drawRoundedRect(engine.handle(), x + 0.8f, y + 0.8f, w - 1.6f, h - 1.6f, 5.8f, fill);
        }
        int textColor = AetherPalette.lerpArgb(palette.onSurface, palette.onPrimary, press);
        if (sub == null) {
            Render2D.drawTextBold(engine.handle(), title, x + w * 0.5f, y + h * 0.5f + 0.4f, h < 20f ? 9.6f : 11.4f,
                    textColor, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        } else {
            Render2D.drawTextBold(engine.handle(), title, x + w * 0.5f, y + h * 0.34f, 9.8f,
                    textColor, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
            int subColor = AetherPalette.lerpArgb(palette.onSurfaceVariant, palette.onPrimary, press * 0.85f);
            Render2D.drawText(engine.handle(), sub, x + w * 0.5f, y + h * 0.72f, 9.0f,
                    subColor, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
        }
    }
}
