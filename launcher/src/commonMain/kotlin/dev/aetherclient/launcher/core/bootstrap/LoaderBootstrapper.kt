package dev.aetherclient.launcher.core.bootstrap

import dev.aetherclient.launcher.core.inject.JvmInjector
import dev.aetherclient.launcher.core.model.BootstrapState
import dev.aetherclient.launcher.core.platform.LauncherPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class BootstrapResult(
    val attachReady: Boolean,
    val payloadPath: Path,
    val payloadExists: Boolean
)

object LoaderBootstrapper {
    suspend fun runBootstrap(
        customAgentPath: String,
        onStep: (BootstrapState) -> Unit
    ): BootstrapResult = withContext(Dispatchers.IO) {
        onStep(BootstrapState("Инициализация", "Подготовка рабочей директории и конфигурации...", 0.22f))
        Files.createDirectories(LauncherPaths.rootDir)
        Files.createDirectories(LauncherPaths.payloadsDir)
        delay(320)

        onStep(BootstrapState("Проверка среды", "Проверка Java Attach API (jdk.attach)...", 0.54f))
        val attachReady = JvmInjector.isAttachAvailable()
        delay(300)

        onStep(BootstrapState("Подготовка визуала", "Проверка AetherClient payload-агента...", 0.82f))
        val resolvedPayload = resolvePayloadPath(customAgentPath)
        val payloadExists = Files.isRegularFile(resolvedPayload)
        delay(280)

        onStep(BootstrapState("Поиск игры", "Сканирование запущенных процессов Minecraft...", 1.0f))
        delay(240)

        BootstrapResult(
            attachReady = attachReady,
            payloadPath = resolvedPayload,
            payloadExists = payloadExists
        )
    }

    fun resolvePayloadPath(customAgentPath: String): Path {
        val custom = customAgentPath.trim()
        if (custom.isNotEmpty()) {
            val parsed = runCatching { Paths.get(custom) }.getOrNull()
            if (parsed != null && Files.isRegularFile(parsed)) {
                return parsed
            }
        }

        // 1. Check local build output first during development, or companion jar next to launcher
        val candidates = listOf(
            Paths.get("client/build/libs/aether-agent.jar"),
            Paths.get("../client/build/libs/aether-agent.jar"),
            Paths.get("aether-agent.jar"),
            Paths.get(System.getProperty("user.dir", ".")).resolve("aether-agent.jar")
        )
        for (candidate in candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().normalize()
            }
        }

        // 2. Extract embedded aether-agent.jar from launcher resources into payloadsDir
        val target = LauncherPaths.payloadsDir.resolve("aether-agent.jar")
        extractEmbeddedAgentIfNeeded(target)
        return target
    }

    fun extractEmbeddedAgentIfNeeded(target: Path): Boolean {
        return runCatching {
            val resourceStream = LoaderBootstrapper::class.java.getResourceAsStream("/payload/aether-agent.jar")
                ?: LoaderBootstrapper::class.java.classLoader.getResourceAsStream("payload/aether-agent.jar")
                ?: Thread.currentThread().contextClassLoader?.getResourceAsStream("payload/aether-agent.jar")
                ?: ClassLoader.getSystemResourceAsStream("payload/aether-agent.jar")

            if (resourceStream == null) {
                return false
            }

            resourceStream.use { input ->
                val bytes = input.readAllBytes()
                if (bytes.isEmpty()) return false

                if (Files.isRegularFile(target) && Files.size(target) == bytes.size.toLong()) {
                    return true
                }

                Files.createDirectories(target.parent)
                val temp = Files.createTempFile(target.parent, "aether-agent-", ".tmp")
                Files.write(temp, bytes)
                Files.move(
                    temp,
                    target,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE
                )
                println("[LoaderBootstrapper] Extracted embedded aether-agent.jar (${bytes.size} bytes) -> $target")
                true
            }
        }.getOrElse { err ->
            System.err.println("[LoaderBootstrapper] Failed to extract embedded agent: ${err.message}")
            false
        }
    }
}
