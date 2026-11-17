package dev.aetherclient.client.audio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.system.MemoryUtil;

import java.nio.ShortBuffer;

/**
 * Lightweight procedural UI audio engine using Minecraft's active OpenAL context
 * (pre-baked sine/water-droplet PCM buffers, zero per-frame allocations) with a safe
 * fallback to vanilla UI SoundEvents when OpenAL is unavailable.
 */
public final class SoundEngine {
    private static final int SAMPLE_RATE = 44100;
    private static final int SOURCE_POOL_SIZE = 4;
    private static volatile SoundEngine instance;

    public static SoundEngine get() {
        if (instance == null) {
            synchronized (SoundEngine.class) {
                if (instance == null) {
                    instance = new SoundEngine();
                }
            }
        }
        return instance;
    }

    public SoundEngine() {
        instance = this;
    }

    private boolean initialized = false;
    private boolean openAlAvailable = false;
    private long boundContext = 0L;

    private int bufOpen = 0;
    private int bufClose = 0;
    private int bufToggleOn = 0;
    private int bufToggleOff = 0;
    private int bufClick = 0;
    private int bufHover = 0;
    private int bufSkeet = 0;
    private int bufNeverlose = 0;
    private int bufBell = 0;
    private int bufBubble = 0;
    private int bufPop = 0;
    private int bufCod = 0;
    private int bufMetallic = 0;

    private final int[] sources = new int[SOURCE_POOL_SIZE];
    private int nextSource = 0;

    private long lastHoverPlayMs = 0L;

    public synchronized void ensureInit() {
        long currentCtx = 0L;
        try {
            currentCtx = ALC10.alcGetCurrentContext();
        } catch (Throwable t) {
            openAlAvailable = false;
            return;
        }
        if (currentCtx == 0L) {
            if (initialized) {
                resetHandlesOnly();
            }
            openAlAvailable = false;
            return;
        }
        if (initialized && currentCtx == boundContext) {
            return;
        }
        if (initialized) {
            resetHandlesOnly();
        }
        try {
            // Drain any stale pre-existing AL error before creating our buffers/sources
            while (AL10.alGetError() != AL10.AL_NO_ERROR) {
                // intentionally drained
            }
            bufOpen = createSweepBuffer(0.070f, 170f, 260f, 0.20f, false);
            bufClose = createSweepBuffer(0.065f, 240f, 130f, 0.18f, false);
            bufToggleOn = createSweepBuffer(0.038f, 220f, 340f, 0.20f, false);
            bufToggleOff = createSweepBuffer(0.035f, 290f, 170f, 0.18f, false);
            bufClick = createSweepBuffer(0.020f, 220f, 120f, 0.16f, false);
            bufHover = createSweepBuffer(0.007f, 170f, 190f, 0.04f, false);
            bufSkeet = createSweepBuffer(0.038f, 2800f, 1600f, 0.42f, false);
            bufNeverlose = createSweepBuffer(0.045f, 2100f, 3200f, 0.40f, true);
            bufBell = createSweepBuffer(0.22f, 1150f, 1050f, 0.38f, true);
            bufBubble = createSweepBuffer(0.060f, 420f, 950f, 0.38f, false);
            bufPop = createSweepBuffer(0.035f, 750f, 240f, 0.36f, false);
            bufCod = createSweepBuffer(0.045f, 160f, 2600f, 0.42f, true);
            bufMetallic = createSweepBuffer(0.12f, 1600f, 1400f, 0.38f, true);

            for (int i = 0; i < SOURCE_POOL_SIZE; i++) {
                sources[i] = AL10.alGenSources();
                if (sources[i] != 0) {
                    AL10.alSourcei(sources[i], AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
                    AL10.alSource3f(sources[i], AL10.AL_POSITION, 0f, 0f, 0f);
                }
            }
            if (AL10.alGetError() == AL10.AL_NO_ERROR && bufOpen != 0 && sources[0] != 0) {
                initialized = true;
                boundContext = currentCtx;
                openAlAvailable = true;
            } else {
                cleanupAlHandles();
            }
        } catch (Throwable t) {
            cleanupAlHandles();
        }
    }

    public synchronized void playOpen(MinecraftClient mc, boolean enabled, float volume) {
        if (!enabled || volume <= 0.01f) return;
        ensureInit();
        playInternal(mc, bufOpen, volume * 0.85f, 1.0f, 1.15f);
    }

    public synchronized void playClose(MinecraftClient mc, boolean enabled, float volume) {
        if (!enabled || volume <= 0.01f) return;
        ensureInit();
        playInternal(mc, bufClose, volume * 0.80f, 1.0f, 0.85f);
    }

    public synchronized void playToggle(MinecraftClient mc, boolean on, boolean enabled, float volume) {
        if (!enabled || volume <= 0.01f) return;
        ensureInit();
        playInternal(mc, on ? bufToggleOn : bufToggleOff, volume * 0.78f, 1.0f, on ? 1.32f : 0.92f);
    }

    public synchronized void playClick(MinecraftClient mc, boolean enabled, float volume) {
        if (!enabled || volume <= 0.01f) return;
        ensureInit();
        playInternal(mc, bufClick, volume * 0.65f, 1.0f, 1.10f);
    }

    public synchronized void playHover(boolean enabled, float volume) {
        if (!enabled || volume <= 0.01f) return;
        long now = System.currentTimeMillis();
        if (now - lastHoverPlayMs < 48L) return;
        lastHoverPlayMs = now;
        ensureInit();
        if (openAlAvailable && bufHover != 0 && ALC10.alcGetCurrentContext() == boundContext) {
            playAl(bufHover, Math.max(0.02f, Math.min(1.0f, volume * 0.42f)), 1.0f);
        }
    }

    private void playInternal(MinecraftClient mc, int buffer, float gain, float pitch, float fallbackPitch) {
        if (openAlAvailable && buffer != 0 && ALC10.alcGetCurrentContext() == boundContext) {
            playAl(buffer, Math.max(0.02f, Math.min(1.0f, gain)), pitch);
            return;
        }
        if (mc != null && mc.getSoundManager() != null) {
            mc.execute(() -> mc.getSoundManager().play(
                    PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK.value(), fallbackPitch, Math.min(0.6f, gain))
            ));
        }
    }

    private void playAl(int buffer, float gain, float pitch) {
        int src = pickFreeSource();
        if (src == 0) return;
        AL10.alSourceStop(src);
        AL10.alSourcei(src, AL10.AL_BUFFER, buffer);
        AL10.alSourcef(src, AL10.AL_GAIN, gain);
        AL10.alSourcef(src, AL10.AL_PITCH, pitch);
        AL10.alSourcePlay(src);
    }

    private int pickFreeSource() {
        for (int s : sources) {
            if (s != 0 && AL10.alGetSourcei(s, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                return s;
            }
        }
        int s = sources[nextSource];
        nextSource = (nextSource + 1) % SOURCE_POOL_SIZE;
        return s;
    }

    private static int createSweepBuffer(float durationSec, float fStart, float fEnd, float amplitude, boolean harmonicShimmer) {
        int samples = Math.max(256, Math.round(SAMPLE_RATE * durationSec));
        ShortBuffer pcm = MemoryUtil.memAllocShort(samples);
        double phase = 0.0;
        double phase2 = 0.0;
        try {
            for (int i = 0; i < samples; i++) {
                float t = i / (float) (samples - 1);
                // Exponential pitch glide feels liquid & organic
                double freq = fStart * Math.pow(fEnd / (double) fStart, easeOutCubic(t));
                phase += (2.0 * Math.PI * freq) / SAMPLE_RATE;
                phase2 += (2.0 * Math.PI * freq * 1.98) / SAMPLE_RATE;

                // Smooth Hann attack + organic acoustic damping
                float attack = (float) Math.sin(Math.min(1.0, t / 0.12) * Math.PI * 0.5);
                float decay = (float) Math.pow(1f - t, 2.4);
                float env = attack * decay;

                double wave = Math.sin(phase) * (harmonicShimmer ? 0.80 : 0.90)
                        + Math.sin(phase2) * (harmonicShimmer ? 0.20 : 0.08);
                short val = (short) Math.max(-32767, Math.min(32767, Math.round(wave * env * amplitude * 32767.0)));
                pcm.put(i, val);
            }
            int buf = AL10.alGenBuffers();
            AL10.alBufferData(buf, AL10.AL_FORMAT_MONO16, pcm, SAMPLE_RATE);
            return buf;
        } finally {
            MemoryUtil.memFree(pcm);
        }
    }

    private static float easeOutCubic(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    public synchronized void destroy() {
        if (!initialized && !openAlAvailable) return;
        try {
            if (boundContext != 0L && ALC10.alcGetCurrentContext() == boundContext) {
                cleanupAlHandles();
            } else {
                resetHandlesOnly();
            }
        } catch (Throwable ignored) {
            resetHandlesOnly();
        }
    }

    public synchronized void playHitSound(MinecraftClient mc, String type, float volume, float userPitch) {
        if (volume <= 0.01f) return;
        ensureInit();
        int targetBuf = switch (type) {
            case "Skeet" -> bufSkeet;
            case "Neverlose" -> bufNeverlose;
            case "Bell" -> bufBell;
            case "Bubble" -> bufBubble;
            case "Pop" -> bufPop;
            case "Cod" -> bufCod;
            case "Metallic" -> bufMetallic;
            default -> 0; // Minecraft
        };
        if (targetBuf != 0 && openAlAvailable && ALC10.alcGetCurrentContext() == boundContext) {
            playAl(targetBuf, Math.max(0.05f, Math.min(1.5f, volume)), userPitch);
        } else if (mc != null && mc.getSoundManager() != null) {
            mc.execute(() -> mc.getSoundManager().play(
                    PositionedSoundInstance.ui(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, userPitch, Math.min(1.0f, volume))
            ));
        }
    }

    private void cleanupAlHandles() {
        try {
            for (int i = 0; i < sources.length; i++) {
                if (sources[i] != 0) {
                    AL10.alSourceStop(sources[i]);
                    AL10.alSourcei(sources[i], AL10.AL_BUFFER, 0);
                    AL10.alDeleteSources(sources[i]);
                }
            }
            deleteBuffer(bufOpen);
            deleteBuffer(bufClose);
            deleteBuffer(bufToggleOn);
            deleteBuffer(bufToggleOff);
            deleteBuffer(bufClick);
            deleteBuffer(bufHover);
            deleteBuffer(bufSkeet);
            deleteBuffer(bufNeverlose);
            deleteBuffer(bufBell);
            deleteBuffer(bufBubble);
            deleteBuffer(bufPop);
            deleteBuffer(bufCod);
            deleteBuffer(bufMetallic);
        } catch (Throwable ignored) {
        } finally {
            resetHandlesOnly();
        }
    }

    private void resetHandlesOnly() {
        for (int i = 0; i < sources.length; i++) {
            sources[i] = 0;
        }
        bufOpen = 0;
        bufClose = 0;
        bufToggleOn = 0;
        bufToggleOff = 0;
        bufClick = 0;
        bufHover = 0;
        bufSkeet = 0;
        bufNeverlose = 0;
        bufBell = 0;
        bufBubble = 0;
        bufPop = 0;
        bufCod = 0;
        bufMetallic = 0;
        initialized = false;
        openAlAvailable = false;
        boundContext = 0L;
    }

    private static void deleteBuffer(int buf) {
        if (buf != 0) {
            AL10.alDeleteBuffers(buf);
        }
    }
}
