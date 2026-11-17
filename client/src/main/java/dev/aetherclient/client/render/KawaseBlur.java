package dev.aetherclient.client.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Dual-filter Kawase Blur implemented with OpenGL 3.3 core GLSL shaders.
 * Captures the active Minecraft framebuffer and produces a downsampled blurred texture
 * that NanoVG can sample inside rounded Liquid Glass panels.
 */
public final class KawaseBlur {
    private static final String VERT_SRC = """
            #version 330 core
            layout (location = 0) in vec2 aPos;
            layout (location = 1) in vec2 aUv;
            out vec2 vUv;
            void main() {
                vUv = aUv;
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
            """;

    private static final String DOWN_FRAG_SRC = """
            #version 330 core
            in vec2 vUv;
            out vec4 FragColor;
            uniform sampler2D uTexture;
            uniform vec2 uHalfPixel;
            uniform float uOffset;
            void main() {
                vec2 off = uHalfPixel * uOffset;
                vec4 sum = texture(uTexture, vUv) * 4.0;
                sum += texture(uTexture, vUv - off);
                sum += texture(uTexture, vUv + off);
                sum += texture(uTexture, vUv + vec2(off.x, -off.y));
                sum += texture(uTexture, vUv - vec2(off.x, -off.y));
                FragColor = vec4(sum.rgb / 8.0, 1.0);
            }
            """;

    private static final String UP_FRAG_SRC = """
            #version 330 core
            in vec2 vUv;
            out vec4 FragColor;
            uniform sampler2D uTexture;
            uniform vec2 uHalfPixel;
            uniform float uOffset;
            void main() {
                vec2 off = uHalfPixel * uOffset;
                vec4 sum = texture(uTexture, vUv + vec2(-off.x * 2.0, 0.0));
                sum += texture(uTexture, vUv + vec2(-off.x, off.y)) * 2.0;
                sum += texture(uTexture, vUv + vec2(0.0, off.y * 2.0));
                sum += texture(uTexture, vUv + vec2(off.x, off.y)) * 2.0;
                sum += texture(uTexture, vUv + vec2(off.x * 2.0, 0.0));
                sum += texture(uTexture, vUv + vec2(off.x, -off.y)) * 2.0;
                sum += texture(uTexture, vUv + vec2(0.0, -off.y * 2.0));
                sum += texture(uTexture, vUv + vec2(-off.x, -off.y)) * 2.0;
                FragColor = vec4(sum.rgb / 12.0, 1.0);
            }
            """;

    private boolean initialized = false;
    private int downProgram;
    private int upProgram;
    private int downHalfPixelLoc;
    private int downOffsetLoc;
    private int upHalfPixelLoc;
    private int upOffsetLoc;

    private int quadVao;
    private int quadVbo;

    private int width = -1;
    private int height = -1;

    private final int[] fbos = new int[4];
    private final int[] textures = new int[4];
    private final int[] widths = new int[4];
    private final int[] heights = new int[4];

    private boolean resizePending = true;

    public void init() {
        if (initialized) return;
        downProgram = compileProgram(VERT_SRC, DOWN_FRAG_SRC);
        upProgram = compileProgram(VERT_SRC, UP_FRAG_SRC);

        downHalfPixelLoc = GL20.glGetUniformLocation(downProgram, "uHalfPixel");
        downOffsetLoc = GL20.glGetUniformLocation(downProgram, "uOffset");
        upHalfPixelLoc = GL20.glGetUniformLocation(upProgram, "uHalfPixel");
        upOffsetLoc = GL20.glGetUniformLocation(upProgram, "uOffset");

        float[] quad = {
                -1f, -1f, 0f, 0f,
                 1f, -1f, 1f, 0f,
                 1f,  1f, 1f, 1f,
                -1f, -1f, 0f, 0f,
                 1f,  1f, 1f, 1f,
                -1f,  1f, 0f, 1f
        };
        quadVao = GL30.glGenVertexArrays();
        quadVbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(quadVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quad, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        GL30.glBindVertexArray(0);

        initialized = true;
    }

    public boolean renderBlur(int sourceFbo, int fbWidth, int fbHeight, float radius) {
        if (fbWidth <= 0 || fbHeight <= 0) return false;
        if (sourceFbo < 0) return false;
        init();
        resizePending = false;
        if (fbWidth != width || fbHeight != height || fbos[0] == 0) {
            allocateBuffers(fbWidth, fbHeight);
            if (fbos[0] == 0) return false; // allocation failed
            resizePending = true;
        }

        checkGLError("pre-blur");

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_BLEND);

        // Copy main framebuffer into level 0 (1/2 resolution)
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbos[0]);
        GL30.glBlitFramebuffer(
                0, 0, fbWidth, fbHeight,
                0, 0, widths[0], heights[0],
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR
        );
        checkGLError("blit");

        GL30.glBindVertexArray(quadVao);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        float offset = Math.max(0.6f, Math.min(4.0f, radius));

        // Downsample 0 -> 1 -> 2 -> 3
        GL20.glUseProgram(downProgram);
        GL20.glUniform1f(downOffsetLoc, offset);
        for (int i = 0; i < 3; i++) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbos[i + 1]);
            GL11.glViewport(0, 0, widths[i + 1], heights[i + 1]);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[i]);
            GL20.glUniform2f(downHalfPixelLoc, 0.5f / widths[i], 0.5f / heights[i]);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            checkGLError("downsample-" + i);
        }

        // Upsample 3 -> 2 -> 1
        GL20.glUseProgram(upProgram);
        GL20.glUniform1f(upOffsetLoc, offset);
        for (int i = 3; i >= 2; i--) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbos[i - 1]);
            GL11.glViewport(0, 0, widths[i - 1], heights[i - 1]);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[i]);
            GL20.glUniform2f(upHalfPixelLoc, 0.5f / widths[i], 0.5f / heights[i]);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            checkGLError("upsample-" + i);
        }

        GL30.glBindVertexArray(0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, sourceFbo);
        checkGLError("post-blur");
        return resizePending;
    }

    private static void checkGLError(String stage) {
        int err = GL11.glGetError();
        if (err != GL11.GL_NO_ERROR) {
            String msg = "OpenGL error at " + stage + ": 0x" + Integer.toHexString(err);
            System.err.println("[KawaseBlur] " + msg);
        }
    }

    public int getOutputTexture() {
        return textures[1];
    }

    public int getOutputWidth() {
        return widths[1];
    }

    public int getOutputHeight() {
        return heights[1];
    }

    private void allocateBuffers(int fbWidth, int fbHeight) {
        deleteBuffers();
        this.width = fbWidth;
        this.height = fbHeight;
        int w = Math.max(1, fbWidth / 2);
        int h = Math.max(1, fbHeight / 2);

        for (int i = 0; i < 4; i++) {
            widths[i] = w;
            heights[i] = h;
            textures[i] = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[i]);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            fbos[i] = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbos[i]);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textures[i], 0);

            w = Math.max(1, w / 2);
            h = Math.max(1, h / 2);
        }
    }

    private void deleteBuffers() {
        for (int i = 0; i < 4; i++) {
            if (fbos[i] != 0) {
                GL30.glDeleteFramebuffers(fbos[i]);
                fbos[i] = 0;
            }
            if (textures[i] != 0) {
                GL11.glDeleteTextures(textures[i]);
                textures[i] = 0;
            }
        }
    }

    public void destroy() {
        deleteBuffers();
        if (quadVbo != 0) {
            GL15.glDeleteBuffers(quadVbo);
            quadVbo = 0;
        }
        if (quadVao != 0) {
            GL30.glDeleteVertexArrays(quadVao);
            quadVao = 0;
        }
        if (downProgram != 0) {
            GL20.glDeleteProgram(downProgram);
            downProgram = 0;
        }
        if (upProgram != 0) {
            GL20.glDeleteProgram(upProgram);
            upProgram = 0;
        }
        initialized = false;
    }

    private static int compileProgram(String vert, String frag) {
        int v = compileShader(GL20.GL_VERTEX_SHADER, vert);
        int f = compileShader(GL20.GL_FRAGMENT_SHADER, frag);
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, v);
        GL20.glAttachShader(p, f);
        GL20.glLinkProgram(p);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException("Shader link failed: " + GL20.glGetProgramInfoLog(p));
        }
        GL20.glDeleteShader(v);
        GL20.glDeleteShader(f);
        return p;
    }

    private static int compileShader(int type, String src) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException("Shader compile failed: " + GL20.glGetShaderInfoLog(s));
        }
        return s;
    }
}
