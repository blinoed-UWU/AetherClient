package dev.aetherclient.launcher.core.platform

import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest

enum class OsType(val mojangName: String) {
    WINDOWS("windows"),
    MACOS("osx"),
    LINUX("linux")
}

object PlatformInfo {
    val os: OsType by lazy {
        val name = System.getProperty("os.name").lowercase()
        when {
            name.contains("win") -> OsType.WINDOWS
            name.contains("mac") || name.contains("darwin") -> OsType.MACOS
            else -> OsType.LINUX
        }
    }

    fun openDirectory(path: Path) {
        Files.createDirectories(path)
        runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(path.toFile())
                return
            }
        }
        when (os) {
            OsType.LINUX -> ProcessBuilder("xdg-open", path.toAbsolutePath().toString()).start()
            OsType.MACOS -> ProcessBuilder("open", path.toAbsolutePath().toString()).start()
            OsType.WINDOWS -> ProcessBuilder("explorer", path.toAbsolutePath().toString()).start()
        }
    }
}

object LauncherPaths {
    val rootDir: Path by lazy {
        val home = System.getProperty("user.home")
        when (PlatformInfo.os) {
            OsType.WINDOWS -> {
                val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
                if (appData != null) Paths.get(appData, "AetherClient")
                else Paths.get(home, "AppData", "Roaming", "AetherClient")
            }
            OsType.MACOS -> Paths.get(home, "Library", "Application Support", "AetherClient")
            OsType.LINUX -> {
                val xdg = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
                if (xdg != null) Paths.get(xdg, "AetherClient")
                else Paths.get(home, ".local", "share", "AetherClient")
            }
        }
    }

    val stateFile: Path get() = rootDir.resolve("loader_settings.json")
    val payloadsDir: Path get() = rootDir.resolve("payloads")
    val configsDir: Path get() = rootDir.resolve("configs")
}

object HashUtils {
    fun sha1(path: Path): String = digest(path, "SHA-1")
    fun sha256(path: Path): String = digest(path, "SHA-256")

    private fun digest(path: Path, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        Files.newInputStream(path).use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                md.update(buf, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
