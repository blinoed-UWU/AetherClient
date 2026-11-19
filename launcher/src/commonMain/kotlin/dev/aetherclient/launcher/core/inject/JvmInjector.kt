package dev.aetherclient.launcher.core.inject

import com.sun.tools.attach.VirtualMachine
import dev.aetherclient.launcher.core.model.JvmProcessInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

object JvmInjector {
    private val pidLocks = ConcurrentHashMap<String, Mutex>()
    private fun isAlive(pid: String): Boolean =
        pid.toLongOrNull()?.let { ProcessHandle.of(it).map { h -> h.isAlive }.orElse(false) } ?: false

    private fun getPidLock(pid: String): Mutex {
        val lock = pidLocks.computeIfAbsent(pid) { Mutex() }
        // Prune mutexes of dead processes so the map cannot grow unbounded
        // over a long launcher session.
        if (pidLocks.size > 64) {
            pidLocks.entries.removeIf { (p, _) -> runCatching { !isAlive(p) }.getOrDefault(true) }
        }
        return lock
    }
    private val ignoredKeywords = listOf(
        "dev.aetherclient.launcher.MainKt",
        "GradleDaemon",
        "GradleServer",
        "KotlinCompileDaemon",
        "GradleWorkerMain",
        "idea_rt.jar",
        "jdt.ls"
    )

    private val versionRegex = Regex("""(?:minecraft[-/]|instances/[^/]*?\b)(1\.\d+(?:\.\d+)?)""")
    private val instanceRegex = Regex("""[/\\]instances[/\\]([^/\\]+)[/\\]natives""")

    fun isAttachAvailable(): Boolean {
        return runCatching {
            VirtualMachine.list()
            true
        }.getOrDefault(false)
    }

    suspend fun scanProcesses(includeAllJvms: Boolean): List<JvmProcessInfo> = withContext(Dispatchers.IO) {
        val selfPid = ProcessHandle.current().pid().toString()
        val descriptors = runCatching { VirtualMachine.list() }.getOrElse { emptyList() }

        descriptors
            .asSequence()
            .filter { it.id() != selfPid }
            .mapNotNull { desc ->
                classifyProcess(
                    pid = desc.id(),
                    rawDisplay = desc.displayName().ifBlank { "JVM Process" },
                    cmdline = readCommandLine(desc.id())
                )
            }
            .filter { includeAllJvms || it.isMinecraft }
            .sortedWith(compareByDescending<JvmProcessInfo> { it.isMinecraft }.thenBy { it.pid.toIntOrNull() ?: 0 })
            .toList()
    }

    internal fun classifyProcess(pid: String, rawDisplay: String, cmdline: String): JvmProcessInfo? {
        val inspectText = "$rawDisplay $cmdline"

        if (ignoredKeywords.any { ignored -> inspectText.contains(ignored, ignoreCase = true) }) {
            return null
        }

        val isFabric = inspectText.contains("KnotClient", ignoreCase = true) ||
            inspectText.contains("fabricmc", ignoreCase = true) ||
            inspectText.contains("fabric-loader", ignoreCase = true)
        val isLauncherWrapper = inspectText.contains("org.prismlauncher.EntryPoint", ignoreCase = true) ||
            inspectText.contains("org.multimc.EntryPoint", ignoreCase = true)
        val isVanillaMc = inspectText.contains("net.minecraft.client.main.Main", ignoreCase = true) ||
            inspectText.contains("com/mojang/minecraft", ignoreCase = true) ||
            inspectText.contains("com\\mojang\\minecraft", ignoreCase = true) ||
            inspectText.contains("minecraft-", ignoreCase = true) ||
            inspectText.contains("minecraft.jar", ignoreCase = true)
        val isMc = isFabric || isVanillaMc || isLauncherWrapper

        val title = when {
            isFabric -> "Minecraft • Fabric"
            isLauncherWrapper || isVanillaMc -> "Minecraft • Java Edition"
            else -> rawDisplay.substringBefore(' ').substringAfterLast('.')
                .ifBlank { "Java Process" }
        }

        val instanceMatch = instanceRegex.find(inspectText)?.groupValues?.getOrNull(1)
        val versionMatch = versionRegex.find(inspectText)?.groupValues?.getOrNull(1)
        val detail = when {
            !instanceMatch.isNullOrBlank() -> instanceMatch
            !versionMatch.isNullOrBlank() -> versionMatch
            else -> rawDisplay.substringBefore(' ').substringAfterLast('.').ifBlank { rawDisplay.take(36) }
        }
        val subtitle = "PID $pid • $detail"
        val hasPreAgent = cmdline.contains("-javaagent:", ignoreCase = true) &&
            cmdline.contains("aether", ignoreCase = true)

        return JvmProcessInfo(
            pid = pid,
            displayName = inspectText.take(220),
            title = title,
            subtitle = subtitle,
            isMinecraft = isMc,
            hasPreAgent = hasPreAgent
        )
    }

    private fun readCommandLine(pid: String): String {
        val pidNum = pid.toLongOrNull() ?: return ""
        val fromProc = runCatching {
            val procCmd = Paths.get("/proc", pid, "cmdline")
            if (Files.isReadable(procCmd)) {
                Files.readAllBytes(procCmd)
                    .toString(StandardCharsets.UTF_8)
                    .replace('\u0000', ' ')
                    .trim()
            } else ""
        }.getOrDefault("")
        if (fromProc.isNotBlank()) return fromProc

        return runCatching {
            ProcessHandle.of(pidNum)
                .flatMap { it.info().commandLine() }
                .orElse("")
        }.getOrDefault("")
    }

    suspend fun isClientActive(pid: String): Boolean = withContext(Dispatchers.IO) {
        withTimeoutOrNull(3500L) {
            getPidLock(pid).withLock {
                runCatching {
                    val vm = VirtualMachine.attach(pid)
                    try {
                        "true".equals(vm.systemProperties.getProperty("aether.client.active"), ignoreCase = true)
                    } finally {
                        runCatching { vm.detach() }
                    }
                }.getOrDefault(false)
            }
        } ?: false
    }

    suspend fun injectAgent(
        pid: String,
        agentJar: Path,
        options: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        withTimeoutOrNull(4500L) {
            getPidLock(pid).withLock {
                runCatching {
                    if (!Files.isRegularFile(agentJar)) {
                        error("Файл агента не найден: ${agentJar.toAbsolutePath()}")
                    }
                    val vm = VirtualMachine.attach(pid)
                    try {
                        val active = "true".equals(
                            vm.systemProperties.getProperty("aether.client.active"),
                            ignoreCase = true
                        )
                        if (!active) {
                            vm.loadAgent(agentJar.toAbsolutePath().toString(), options)
                        }
                    } finally {
                        runCatching { vm.detach() }
                    }
                }
            }
        } ?: Result.failure(IllegalStateException("Превышено время ожидания подключения к JVM ($pid)"))
    }
}
