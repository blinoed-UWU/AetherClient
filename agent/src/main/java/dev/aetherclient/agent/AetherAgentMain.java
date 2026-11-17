package dev.aetherclient.agent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Dynamic & premain Java agent entrypoint for Minecraft 1.21.11 Fabric.
 * Uses only JDK classes and separates the permanent LWJGL NanoVG native loader from
 * the reloadable Aether client classloader so unload -> re-inject cycles never violate
 * HotSpot's single-classloader native library rule.
 */
public final class AetherAgentMain {
    private static final String MC_INTERMEDIARY_CLASS = "net.minecraft.class_310";
    private static final Set<String> HOOK_CLASS_NAMES = Set.of(
            "net.minecraft.class_1041",
            "net.minecraft.class_309",
            "net.minecraft.class_312",
            "net.minecraft.class_310",
            "net.minecraft.class_329",
            "net.minecraft.class_355",
            "net.minecraft.class_916",
            "net.minecraft.class_759",
            "net.minecraft.class_9975"
    );

    private static final Object LOCK = new Object();

    private static volatile Path extractedDir;
    private static volatile boolean shutdownHookRegistered = false;
    private static volatile URLClassLoader nanovgLoader;
    private static volatile URLClassLoader activeClientLoader;
    private static volatile URLClassLoader activeAsmLoader;
    private static volatile Instrumentation savedInstrumentation;
    private static volatile ClassFileTransformer activeTransformer;

    private AetherAgentMain() {}

    public static void premain(String agentArgs, Instrumentation inst) {
        savedInstrumentation = inst;
        System.setProperty("aether.agent.loaded", "true");
        try {
            synchronized (LOCK) {
                Class<?> mcClass = findMinecraftClass(inst);
                if (mcClass != null) {
                    attachLocked(agentArgs, inst, mcClass);
                    return;
                }
                Path workDir = ensureExtractedPayloads();
                Class<?> bridgeClass = ensureBridgeAndTransformer(inst, workDir);

                Thread bootThread = new Thread(() -> {
                    long deadlineNanos = System.nanoTime() + TimeUnit.MINUTES.toNanos(3);
                    while (System.nanoTime() < deadlineNanos) {
                        try {
                            Class<?> loadedMc = findMinecraftClass(inst);
                            if (loadedMc != null) {
                                synchronized (LOCK) {
                                    bootClientLocked(agentArgs, loadedMc.getClassLoader(), workDir, bridgeClass);
                                }
                                return;
                            }
                            Thread.sleep(100L);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        } catch (Throwable t) {
                            System.err.println("[AetherAgent] Deferred premain bootstrap failed:");
                            t.printStackTrace(System.err);
                            return;
                        }
                    }
                    System.err.println("[AetherAgent] Timed out waiting for net.minecraft.class_310 during premain");
                }, "Aether-PreBoot");
                bootThread.setDaemon(true);
                bootThread.start();
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            throw new RuntimeException("AetherAgent premain setup failed: " + t.getMessage(), t);
        }
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        savedInstrumentation = inst;
        System.setProperty("aether.agent.loaded", "true");
        try {
            synchronized (LOCK) {
                Class<?> mcClass = findMinecraftClass(inst);
                if (mcClass == null) {
                    throw new IllegalStateException("Minecraft 1.21.11 class_310 is not loaded in target JVM");
                }
                attachLocked(agentArgs, inst, mcClass);
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            throw new RuntimeException("AetherAgent injection failed: " + t.getMessage(), t);
        }
    }

    private static void attachLocked(String agentArgs, Instrumentation inst, Class<?> mcClass) throws Exception {
        Class<?> existingBridge = findBridgeClass();
        if (existingBridge != null && (boolean) existingBridge.getMethod("isInitialized").invoke(null)) {
            return;
        }
        Path workDir = ensureExtractedPayloads();
        Class<?> bridgeClass = ensureBridgeAndTransformer(inst, workDir);
        bootClientLocked(agentArgs, mcClass.getClassLoader(), workDir, bridgeClass);
    }

    private static Class<?> ensureBridgeAndTransformer(Instrumentation inst, Path workDir) throws Exception {
        Class<?> bridgeClass = findBridgeClass();
        if (bridgeClass == null) {
            Path bridgeJar = workDir.resolve("aether-bridge.jar");
            inst.appendToBootstrapClassLoaderSearch(new JarFile(bridgeJar.toFile()));
            bridgeClass = Class.forName("dev.aetherclient.bridge.AetherBridge", true, null);
        }

        boolean alreadyTransformed = (boolean) bridgeClass.getMethod("areHooksTransformed").invoke(null);
        if (!alreadyTransformed) {
            installTransformer(inst, workDir);
            bridgeClass.getMethod("markHooksTransformed").invoke(null);
        }
        return bridgeClass;
    }

    private static void bootClientLocked(
            String agentArgs,
            ClassLoader knotLoader,
            Path workDir,
            Class<?> bridgeClass
    ) throws Exception {
        boolean alreadyInit = (boolean) bridgeClass.getMethod("isInitialized").invoke(null);
        if (alreadyInit) {
            return;
        }

        // Keep LWJGL NanoVG + natives in a single JVM-lifetime classloader so unload -> re-inject works cleanly
        if (nanovgLoader == null) {
            List<URL> nvgUrls = new ArrayList<>();
            nvgUrls.add(workDir.resolve("lwjgl-nanovg.jar").toUri().toURL());
            Path nativesDir = workDir.resolve("natives");
            if (Files.isDirectory(nativesDir)) {
                try (var stream = Files.list(nativesDir)) {
                    for (Path p : stream.filter(p -> p.toString().endsWith(".jar")).toList()) {
                        nvgUrls.add(p.toUri().toURL());
                    }
                }
            }
            nanovgLoader = new URLClassLoader(nvgUrls.toArray(URL[]::new), knotLoader);
        }

        URLClassLoader prevClient = activeClientLoader;
        if (prevClient != null) {
            activeClientLoader = null;
            try {
                prevClient.close();
            } catch (IOException ignored) {}
        }

        URLClassLoader clientLoader = new URLClassLoader(
                new URL[]{workDir.resolve("aether-client.jar").toUri().toURL()},
                nanovgLoader
        );
        activeClientLoader = clientLoader;
        bridgeClass.getMethod("setUnloadHandler", Runnable.class).invoke(null, (Runnable) AetherAgentMain::onClientUnloaded);
        Class<?> entry = Class.forName("dev.aetherclient.client.AetherClientMain", true, clientLoader);
        entry.getMethod("bootstrap", String.class).invoke(null, agentArgs == null ? "" : agentArgs);
    }

    private static void onClientUnloaded() {
        Thread gcThread = new Thread(() -> {
            try {
                Thread.sleep(60L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            synchronized (LOCK) {
                // 1. Remove transformer and revert transformed bytecode back to clean vanilla
                Instrumentation inst = savedInstrumentation;
                ClassFileTransformer transformer = activeTransformer;
                if (inst != null && transformer != null) {
                    try {
                        inst.removeTransformer(transformer);
                    } catch (Throwable ignored) {}
                    activeTransformer = null;
                    try {
                        List<Class<?>> toRetransform = new ArrayList<>();
                        for (Class<?> cls : inst.getAllLoadedClasses()) {
                            if (HOOK_CLASS_NAMES.contains(cls.getName()) && inst.isModifiableClass(cls)) {
                                toRetransform.add(cls);
                            }
                        }
                        if (!toRetransform.isEmpty()) {
                            inst.retransformClasses(toRetransform.toArray(Class[]::new));
                        }
                    } catch (Throwable t) {
                        System.err.println("[AetherAgent] Class retransformation on unhook failed: " + t.getMessage());
                    }
                }

                // 2. Reset bridge state
                Class<?> bridgeClass = findBridgeClass();
                if (bridgeClass != null) {
                    try {
                        bridgeClass.getMethod("markHooksTransformedReset").invoke(null);
                    } catch (Throwable ignored) {}
                }

                // 3. Close client and asm classloaders
                URLClassLoader prev = activeClientLoader;
                if (prev != null) {
                    activeClientLoader = null;
                    try {
                        prev.close();
                    } catch (IOException ignored) {}
                }

                URLClassLoader asm = activeAsmLoader;
                if (asm != null) {
                    activeAsmLoader = null;
                    try {
                        asm.close();
                    } catch (IOException ignored) {}
                }

                // 4. Wipe system properties
                try {
                    System.clearProperty("aether.agent.loaded");
                    System.clearProperty("aether.client.active");
                } catch (Throwable ignored) {}

                // 5. Clean up temporary extracted directory
                Path dir = extractedDir;
                if (dir != null) {
                    extractedDir = null;
                    deleteRecursivelyQuietly(dir);
                }
            }
            // 6. Force GC to collect all unloaded classes and memory
            System.gc();
            System.gc();
        }, "Aether-UnloadGC");
        gcThread.setDaemon(true);
        gcThread.start();
    }

    private static Class<?> findMinecraftClass(Instrumentation inst) {
        for (Class<?> cls : inst.getAllLoadedClasses()) {
            if (MC_INTERMEDIARY_CLASS.equals(cls.getName())) {
                return cls;
            }
        }
        return null;
    }

    private static Class<?> findBridgeClass() {
        try {
            return Class.forName("dev.aetherclient.bridge.AetherBridge", true, null);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Path ensureExtractedPayloads() throws IOException {
        if (extractedDir != null && Files.isDirectory(extractedDir)) {
            return extractedDir;
        }
        Path dir = Files.createTempDirectory("aether-agent-rt-");
        registerCleanupHook(dir);

        try {
            Path selfJarPath = Path.of(AetherAgentMain.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            try (JarFile jar = new JarFile(selfJarPath.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().startsWith("payload/")) continue;
                    String rel = entry.getName().substring("payload/".length());
                    Path out = dir.resolve(rel).normalize();
                    if (!out.startsWith(dir)) continue;
                    Files.createDirectories(out.getParent());
                    try (InputStream is = jar.getInputStream(entry)) {
                        Files.copy(is, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (Exception e) {
            deleteRecursivelyQuietly(dir);
            throw new IOException("Failed to extract agent payloads", e);
        }

        extractedDir = dir;
        return dir;
    }

    private static void registerCleanupHook(Path dir) {
        if (shutdownHookRegistered) return;
        shutdownHookRegistered = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            URLClassLoader prev = activeClientLoader;
            if (prev != null) {
                try { prev.close(); } catch (IOException ignored) {}
            }
            URLClassLoader nvg = nanovgLoader;
            if (nvg != null) {
                try { nvg.close(); } catch (IOException ignored) {}
            }
            deleteRecursivelyQuietly(dir);
        }, "Aether-TempCleanup"));
    }

    private static void deleteRecursivelyQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
    }

    private static void installTransformer(Instrumentation inst, Path workDir) throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(workDir.resolve("aether-transformer.jar").toUri().toURL());
        Path asmDir = workDir.resolve("asm");
        if (Files.isDirectory(asmDir)) {
            try (var stream = Files.list(asmDir)) {
                for (Path p : stream.filter(p -> p.toString().endsWith(".jar")).toList()) {
                    urls.add(p.toUri().toURL());
                }
            }
        }

        URLClassLoader asmLoader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
        activeAsmLoader = asmLoader;
        Class<?> transformerClass = Class.forName(
                "dev.aetherclient.agent.transformer.AetherClassTransformer",
                true,
                asmLoader
        );
        ClassFileTransformer transformer = (ClassFileTransformer) transformerClass.getConstructor().newInstance();
        activeTransformer = transformer;
        inst.addTransformer(transformer, true);

        List<Class<?>> toRetransform = new ArrayList<>();
        for (Class<?> cls : inst.getAllLoadedClasses()) {
            if (HOOK_CLASS_NAMES.contains(cls.getName()) && inst.isModifiableClass(cls)) {
                toRetransform.add(cls);
            }
        }
        if (!toRetransform.isEmpty()) {
            inst.retransformClasses(toRetransform.toArray(Class[]::new));
        }
    }
}
