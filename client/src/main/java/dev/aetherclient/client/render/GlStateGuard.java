package dev.aetherclient.client.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Saves and restores OpenGL state around custom Kawase Blur and NanoVG rendering
 * so Minecraft 1.21.11's Blaze3D state cache (including GL33 Sampler Objects and
 * pixel unpack parameters) remains isolated and consistent.
 */
public final class GlStateGuard {
    private static final int TRACKED_UNITS = 4;

    private int drawFbo;
    private int readFbo;
    private int program;
    private int vao;
    private int arrayBuffer;
    private int elementBuffer;
    private int pixelUnpackBuffer;
    private int activeTexture;
    private int activeTexture2D;
    private final int[] unitTextures = new int[TRACKED_UNITS];
    private final int[] unitSamplers = new int[TRACKED_UNITS];
    private final int[] viewport = new int[4];

    private int unpackRowLength;
    private int unpackSkipRows;
    private int unpackSkipPixels;
    private int unpackAlignment;

    private boolean blend;
    private int blendSrcRgb;
    private int blendDstRgb;
    private int blendSrcAlpha;
    private int blendDstAlpha;

    private boolean depthTest;
    private boolean cullFace;
    private boolean scissorTest;
    private boolean stencilTest;
    private final int[] polygonMode = new int[2];
    private final boolean[] colorMask = new boolean[4];

    public void save() {
        drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        elementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        pixelUnpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);

        activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        activeTexture2D = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        for (int unit = 0; unit < TRACKED_UNITS; unit++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            unitTextures[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            unitSamplers[unit] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        }
        GL13.glActiveTexture(activeTexture);

        unpackRowLength = GL11.glGetInteger(GL12.GL_UNPACK_ROW_LENGTH);
        unpackSkipRows = GL11.glGetInteger(GL12.GL_UNPACK_SKIP_ROWS);
        unpackSkipPixels = GL11.glGetInteger(GL12.GL_UNPACK_SKIP_PIXELS);
        unpackAlignment = GL11.glGetInteger(GL12.GL_UNPACK_ALIGNMENT);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer vp = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
            viewport[0] = vp.get(0);
            viewport[1] = vp.get(1);
            viewport[2] = vp.get(2);
            viewport[3] = vp.get(3);

            IntBuffer pm = stack.mallocInt(2);
            GL11.glGetIntegerv(GL11.GL_POLYGON_MODE, pm);
            polygonMode[0] = pm.get(0);
            polygonMode[1] = pm.get(1);

            ByteBuffer cm = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, cm);
            colorMask[0] = cm.get(0) != 0;
            colorMask[1] = cm.get(1) != 0;
            colorMask[2] = cm.get(2) != 0;
            colorMask[3] = cm.get(3) != 0;
        }

        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);

        depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        scissorTest = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        stencilTest = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
    }

    /**
     * Resets Blaze3D sampler objects and pixel unpack state that break
     * NanoVG glyph atlas / image texture uploads and sampling.
     */
    public void prepareForNanoVg() {
        for (int unit = 0; unit < TRACKED_UNITS; unit++) {
            GL33.glBindSampler(unit, 0);
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        GL11.glPixelStorei(GL12.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL12.GL_UNPACK_ALIGNMENT, 4);
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        GL11.glColorMask(true, true, true, true);
    }

    public void restore() {
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, elementBuffer);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, pixelUnpackBuffer);

        GL11.glPixelStorei(GL12.GL_UNPACK_ROW_LENGTH, unpackRowLength);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_ROWS, unpackSkipRows);
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_PIXELS, unpackSkipPixels);
        GL11.glPixelStorei(GL12.GL_UNPACK_ALIGNMENT, unpackAlignment);

        for (int unit = 0; unit < TRACKED_UNITS; unit++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, unitTextures[unit]);
            GL33.glBindSampler(unit, unitSamplers[unit]);
        }

        GL13.glActiveTexture(activeTexture);
        if (activeTexture < GL13.GL_TEXTURE0 || activeTexture >= GL13.GL_TEXTURE0 + TRACKED_UNITS) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, activeTexture2D);
        }
        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
        if (polygonMode[0] != 0) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, polygonMode[0]);
        }

        if (blend) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);

        if (depthTest) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (cullFace) GL11.glEnable(GL11.GL_CULL_FACE); else GL11.glDisable(GL11.GL_CULL_FACE);
        if (scissorTest) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (stencilTest) GL11.glEnable(GL11.GL_STENCIL_TEST); else GL11.glDisable(GL11.GL_STENCIL_TEST);
    }
}
