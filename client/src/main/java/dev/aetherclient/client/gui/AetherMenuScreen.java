package dev.aetherclient.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Transparent host screen that unlocks the mouse cursor and suppresses vanilla
 * background darkening while NanoVG renders the Liquid Glass ClickGUI.
 */
public final class AetherMenuScreen extends Screen {
    private final Runnable onRemovedCallback;

    public AetherMenuScreen(Runnable onRemovedCallback) {
        super(Text.literal("Aether"));
        this.onRemovedCallback = onRemovedCallback;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Rendered directly in NanoVG post-frame pass
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Preserve live scene for Kawase Blur
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        if (onRemovedCallback != null) {
            onRemovedCallback.run();
        }
    }
}
