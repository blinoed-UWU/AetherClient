package dev.aetherclient.client.render;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Manages the NanoVG OpenGL 3 context, static Noto Sans fonts (Regular/Bold),
 * Aether logo image, and wrapped Kawase Blur backdrop image handle.
 */
public final class NanoVgEngine {
    public static final String FONT_REGULAR = "aether-regular";
    public static final String FONT_BOLD = "aether-bold";

    private long vg = 0L;
    private ByteBuffer fontRegularBuffer;
    private ByteBuffer fontBoldBuffer;
    private ByteBuffer logoBuffer;
    private int logoImage = 0;
    private int blurImage = 0;
    private int lastBlurTex = 0;

    private final KawaseBlur kawaseBlur = new KawaseBlur();

    public void ensureInit() {
        if (vg != 0L) return;
        vg = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS | NanoVGGL3.NVG_STENCIL_STROKES);
        if (vg == 0L) {
            throw new IllegalStateException("Could not initialize NanoVGGL3 context");
        }

        fontRegularBuffer = loadResourceBuffer("/assets/aether/fonts/noto_medium.ttf");
        if (fontRegularBuffer == null) {
            fontRegularBuffer = loadResourceBuffer("/assets/aether/fonts/inter.ttf");
        }
        int regId = -1;
        if (fontRegularBuffer != null) {
            regId = NanoVG.nvgCreateFontMem(vg, FONT_REGULAR, fontRegularBuffer, false);
        }
        if (regId < 0) {
            System.err.println("[Aether] Warning: failed to register regular font (" + regId + ")");
        }

        fontBoldBuffer = loadResourceBuffer("/assets/aether/fonts/noto_bold.ttf");
        int boldId = -1;
        if (fontBoldBuffer != null) {
            boldId = NanoVG.nvgCreateFontMem(vg, FONT_BOLD, fontBoldBuffer, false);
        }
        if (boldId < 0 && fontRegularBuffer != null) {
            NanoVG.nvgCreateFontMem(vg, FONT_BOLD, fontRegularBuffer, false);
        }

        logoBuffer = loadResourceBuffer("/assets/aether/textures/logo.png");
        if (logoBuffer != null) {
            logoImage = NanoVG.nvgCreateImageMem(vg, 0, logoBuffer);
        }
    }

    public void updateBlurTexture(int sourceFbo, int fbWidth, int fbHeight, float blurRadius) {
        if (blurRadius <= 0.1f) {
            clearBlurTexture();
            return;
        }
        boolean recreated = kawaseBlur.renderBlur(sourceFbo, fbWidth, fbHeight, blurRadius);
        int tex = kawaseBlur.getOutputTexture();
        if (tex != 0 && (recreated || blurImage == 0 || tex != lastBlurTex)) {
            if (blurImage != 0) {
                NanoVG.nvgDeleteImage(vg, blurImage);
                blurImage = 0;
            }
            blurImage = NanoVGGL3.nvglCreateImageFromHandle(
                    vg,
                    tex,
                    kawaseBlur.getOutputWidth(),
                    kawaseBlur.getOutputHeight(),
                    NanoVG.NVG_IMAGE_FLIPY | NanoVGGL3.NVG_IMAGE_NODELETE
            );
            lastBlurTex = blurImage != 0 ? tex : 0;
        }
    }

    public void clearBlurTexture() {
        if (vg != 0L && blurImage != 0) {
            NanoVG.nvgDeleteImage(vg, blurImage);
        }
        blurImage = 0;
        lastBlurTex = 0;
    }

    public long handle() {
        if (vg == 0L) {
            throw new IllegalStateException("NanoVG context not initialized - call ensureInit() first");
        }
        return vg;
    }

    public boolean isInitialized() {
        return vg != 0L;
    }

    public int logoImage() {
        return logoImage;
    }

    public int blurImage() {
        return blurImage;
    }

    public void beginFrame(float logicalWidth, float logicalHeight, float pixelRatio) {
        NanoVG.nvgBeginFrame(vg, logicalWidth, logicalHeight, pixelRatio);
    }

    public void endFrame() {
        NanoVG.nvgEndFrame(vg);
    }

    public void destroy() {
        if (vg != 0L) {
            if (blurImage != 0) {
                NanoVG.nvgDeleteImage(vg, blurImage);
                blurImage = 0;
            }
            if (logoImage != 0) {
                NanoVG.nvgDeleteImage(vg, logoImage);
                logoImage = 0;
            }
            NanoVGGL3.nvgDelete(vg);
            vg = 0L;
        }
        lastBlurTex = 0;
        kawaseBlur.destroy();
        if (fontRegularBuffer != null) {
            MemoryUtil.memFree(fontRegularBuffer);
            fontRegularBuffer = null;
        }
        if (fontBoldBuffer != null) {
            MemoryUtil.memFree(fontBoldBuffer);
            fontBoldBuffer = null;
        }
        if (logoBuffer != null) {
            MemoryUtil.memFree(logoBuffer);
            logoBuffer = null;
        }
    }

    public static NVGColor argbToNvg(int argb, NVGColor out) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        return NanoVG.nvgRGBAf(r, g, b, a, out);
    }

    public static NVGPaint createPaint() {
        return NVGPaint.create();
    }

    private static ByteBuffer loadResourceBuffer(String resourcePath) {
        try (InputStream is = NanoVgEngine.class.getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            byte[] bytes = is.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes);
            buf.flip();
            return buf;
        } catch (IOException e) {
            return null;
        }
    }
}
