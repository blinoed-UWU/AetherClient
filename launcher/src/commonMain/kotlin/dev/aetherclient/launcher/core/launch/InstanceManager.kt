package dev.aetherclient.launcher.core.launch

import dev.aetherclient.launcher.core.model.GameInstanceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

object InstanceManager {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class MmcPack(
        val components: List<MmcComponent> = emptyList()
    )

    @Serializable
    private data class MmcComponent(
        val uid: String = "",
        val version: String = ""
    )

    private data class LauncherSource(
        val name: String,
        val binaryNames: List<String>,
        val instanceRoots: List<Path>
    )

    private fun defaultSources(): List<LauncherSource> {
        val home = Paths.get(System.getProperty("user.home"))
        val appDataEnv = System.getenv("APPDATA")
        val appData = if (!appDataEnv.isNullOrBlank()) Paths.get(appDataEnv) else home.resolve("AppData/Roaming")

        return listOf(
            LauncherSource(
                name = "FreesmLauncher",
                binaryNames = listOf("freesmlauncher", "freesmlauncher.exe"),
                instanceRoots = listOfNotNull(
                    home.resolve(".local/share/FreesmLauncher/instances"),
                    home.resolve(".var/app/org.freesmlauncher.FreesmLauncher/data/FreesmLauncher/instances"),
                    appData.resolve("FreesmLauncher/instances")
                )
            ),
            LauncherSource(
                name = "PrismLauncher",
                binaryNames = listOf("prismlauncher", "prismlauncher.exe"),
                instanceRoots = listOfNotNull(
                    home.resolve(".local/share/PrismLauncher/instances"),
                    home.resolve(".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances"),
                    home.resolve("Library/Application Support/PrismLauncher/instances"),
                    appData.resolve("PrismLauncher/instances")
                )
            ),
            LauncherSource(
                name = "MultiMC",
                binaryNames = listOf("multimc", "multimc.exe"),
                instanceRoots = listOfNotNull(
                    home.resolve(".local/share/multimc/instances"),
                    appData.resolve("MultiMC/instances")
                )
            )
        )
    }

    suspend fun discoverInstances(): List<GameInstanceInfo> = withContext(Dispatchers.IO) {
        val found = mutableListOf<GameInstanceInfo>()
        for (source in defaultSources()) {
            val bin = resolveBinary(source.binaryNames) ?: continue
            for (root in source.instanceRoots) {
                if (!Files.isDirectory(root)) continue
                runCatching {
                    Files.list(root).use { stream ->
                        stream.filter { Files.isDirectory(it) }.forEach { dir ->
                            parseInstanceDir(dir, source.name, bin)?.let(found::add)
                        }
                    }
                }
            }
        }
        found.sortedWith(
            compareByDescending<GameInstanceInfo> { it.isCompatible }
                .thenByDescending { it.mcVersion == "1.21.11" }
                .thenBy { it.name.lowercase() }
        )
    }

    internal fun parseInstanceDir(dir: Path, launcherName: String, launcherBinary: String): GameInstanceInfo? {
        val cfgPath = dir.resolve("instance.cfg")
        if (!Files.isRegularFile(cfgPath)) return null

        val cfgText = runCatching { Files.readString(cfgPath, StandardCharsets.UTF_8) }.getOrNull() ?: return null
        val displayName = extractCfgValue(cfgText, "name")?.ifBlank { null } ?: dir.fileName.toString()

        var mcVersion = ""
        var isFabric = false
        val packPath = dir.resolve("mmc-pack.json")
        if (Files.isRegularFile(packPath)) {
            runCatching {
                val pack = json.decodeFromString<MmcPack>(Files.readString(packPath, StandardCharsets.UTF_8))
                for (comp in pack.components) {
                    if (comp.uid == "net.minecraft") {
                        mcVersion = comp.version
                    } else if (comp.uid == "net.fabricmc.fabric-loader") {
                        isFabric = true
                    }
                }
            }
        }

        val compatible = mcVersion == "1.21.11" && isFabric
        return GameInstanceInfo(
            id = dir.fileName.toString(),
            name = displayName,
            launcherName = launcherName,
            launcherBinary = launcherBinary,
            instanceDirPath = dir.toAbsolutePath().toString(),
            mcVersion = mcVersion.ifBlank { "?" },
            isFabric = isFabric,
            isCompatible = compatible
        )
    }

    suspend fun launchInstanceWithAgent(instance: GameInstanceInfo, agentJar: Path): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(instance.isCompatible) {
                    "Профиль ${instance.name} несовместим (нужен Fabric 1.21.11)"
                }
                val cfgPath = Paths.get(instance.instanceDirPath, "instance.cfg")
                require(Files.isRegularFile(cfgPath)) { "Не найден instance.cfg для ${instance.name}" }
                require(Files.isRegularFile(agentJar)) { "Не найден файл aether-agent.jar" }

                val original = Files.readString(cfgPath, StandardCharsets.UTF_8)
                val patched = patchInstanceCfgForAgent(original, agentJar)
                val tmpCfg = cfgPath.resolveSibling("instance.cfg.tmp")
                Files.writeString(tmpCfg, patched, StandardCharsets.UTF_8)
                runCatching {
                    Files.move(tmpCfg, cfgPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                }.getOrElse {
                    Files.move(tmpCfg, cfgPath, StandardCopyOption.REPLACE_EXISTING)
                }

                val devNull = File("/dev/null")
                val inputRedirect = if (devNull.exists()) {
                    ProcessBuilder.Redirect.from(devNull)
                } else {
                    ProcessBuilder.Redirect.PIPE
                }
                val proc = ProcessBuilder(instance.launcherBinary, "--launch", instance.id)
                    .redirectInput(inputRedirect)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                if (!devNull.exists()) {
                    runCatching { proc.outputStream.close() }
                }
                if (proc.waitFor(350L, TimeUnit.MILLISECONDS) && proc.exitValue() != 0) {
                    error("Лаунчер завершился с кодом ${proc.exitValue()}")
                }
                Unit
            }
        }

    internal fun patchInstanceCfgForAgent(cfgText: String, agentJar: Path): String {
        val absAgent = agentJar.toAbsolutePath().normalize().toString().replace('\\', '/')
        val agentFlag = if (absAgent.contains(' ')) "\"-javaagent:$absAgent\"" else "-javaagent:$absAgent"

        val normalized = cfgText.replace("\r\n", "\n")
        val lines = normalized.split('\n').toMutableList()

        val generalIdx = lines.indexOfFirst { it.trim() == "[General]" }
        if (generalIdx < 0) {
            return buildString {
                append("[General]\n")
                append("OverrideJavaArgs=true\n")
                append("JvmArgs=").append(agentFlag).append('\n')
                if (normalized.isNotBlank()) {
                    append(normalized)
                    if (!normalized.endsWith('\n')) append('\n')
                }
            }
        }

        var nextSectionIdx = lines.size
        for (i in (generalIdx + 1) until lines.size) {
            if (lines[i].trim().startsWith('[')) {
                nextSectionIdx = i
                break
            }
        }

        var existingJvmArgs = ""
        var overrideLineIdx = -1
        var legacyOverrideIdx = -1
        var jvmArgsLineIdx = -1
        for (i in (generalIdx + 1) until nextSectionIdx) {
            val trimmed = lines[i].trim()
            if (trimmed.startsWith("OverrideJavaArgs=")) {
                overrideLineIdx = i
            } else if (trimmed.startsWith("OverrideJvmArgs=")) {
                legacyOverrideIdx = i
            } else if (trimmed.startsWith("JvmArgs=")) {
                jvmArgsLineIdx = i
                existingJvmArgs = unescapeQSettings(trimmed.substringAfter("JvmArgs=").trim())
                if (existingJvmArgs.length >= 2 && existingJvmArgs.startsWith('"') && existingJvmArgs.endsWith('"')) {
                    val inner = existingJvmArgs.substring(1, existingJvmArgs.length - 1)
                    if (!inner.contains('"')) {
                        existingJvmArgs = inner.trim()
                    }
                }
            }
        }

        val cleanedExisting = stripExistingAetherAgent(existingJvmArgs)
        val combinedArgs = if (cleanedExisting.isBlank()) agentFlag else "$agentFlag $cleanedExisting"

        if (overrideLineIdx >= 0) {
            lines[overrideLineIdx] = "OverrideJavaArgs=true"
        } else if (legacyOverrideIdx >= 0) {
            lines[legacyOverrideIdx] = "OverrideJavaArgs=true"
        } else {
            lines.add(generalIdx + 1, "OverrideJavaArgs=true")
            if (jvmArgsLineIdx >= 0) jvmArgsLineIdx++
            nextSectionIdx++
        }

        if (jvmArgsLineIdx >= 0) {
            lines[jvmArgsLineIdx] = "JvmArgs=${escapeQSettings(combinedArgs)}"
        } else {
            // Insert at the end of [General] — right before the next section
            // header — so JvmArgs can never land inside a following section.
            val insertIdx = nextSectionIdx.coerceAtLeast(generalIdx + 1)
            lines.add(insertIdx, "JvmArgs=${escapeQSettings(combinedArgs)}")
        }

        val joined = lines.joinToString("\n")
        return if (joined.endsWith("\n")) joined else "$joined\n"
    }

    /**
     * instance.cfg is Qt QSettings INI: values escape backslashes and quotes.
     * Round-tripping raw values corrupts Windows paths and quoted JVM args.
     */
    internal fun escapeQSettings(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    internal fun unescapeQSettings(value: String): String =
        value.replace("\\\"", "\"").replace("\\\\", "\\")

    internal fun stripExistingAetherAgent(args: String): String {
        if (args.isBlank()) return ""
        val withoutFullyQuoted = args.replace(
            Regex(""""-javaagent:[^"]*aether[^"]*\.jar"""", RegexOption.IGNORE_CASE),
            ""
        )
        val withoutPathQuoted = withoutFullyQuoted.replace(
            Regex("""-javaagent:(?:"[^"]*aether[^"]*\.jar"|[^\s"]*aether[^\s"]*\.jar)""", RegexOption.IGNORE_CASE),
            ""
        )
        return withoutPathQuoted.replace(Regex("""\s+"""), " ").trim()
    }

    private fun extractCfgValue(cfgText: String, key: String): String? {
        val prefix = "$key="
        for (line in cfgText.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length).trim()
            }
        }
        return null
    }

    private fun resolveBinary(names: List<String>): String? {
        val pathEnv = System.getenv("PATH").orEmpty()
        val pathDirs = pathEnv.split(File.pathSeparatorChar).filter { it.isNotBlank() }.map { Paths.get(it) }
        val fallbackDirs = listOf(Paths.get("/usr/bin"), Paths.get("/usr/local/bin"))
        for (name in names) {
            for (dir in (pathDirs + fallbackDirs)) {
                val candidate = dir.resolve(name)
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                    return candidate.toAbsolutePath().toString()
                }
            }
        }
        return null
    }
}
