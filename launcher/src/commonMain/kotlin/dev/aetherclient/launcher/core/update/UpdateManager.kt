package dev.aetherclient.launcher.core.update

import dev.aetherclient.launcher.core.platform.LauncherPaths
import dev.aetherclient.launcher.core.platform.OsType
import dev.aetherclient.launcher.core.platform.PlatformInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.regex.Pattern

data class UpdateInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isAvailable: Boolean
)

object UpdateManager {
    const val CURRENT_VERSION = "1.2.0"
    private const val VERSION_URL = "https://raw.githubusercontent.com/blinoed-UWU/AetherClient/main/version.json"

    suspend fun checkUpdate(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URI.create(VERSION_URL).toURL().openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val json = conn.inputStream.bufferedReader().use { it.readText() }
            val verMatcher = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"").matcher(json)
            val notesMatcher = Pattern.compile("\"releaseNotes\"\\s*:\\s*\"([^\"]+)\"").matcher(json)
            val jarMatcher = Pattern.compile("\"launcherJar\"\\s*:\\s*\"([^\"]+)\"").matcher(json)

            val remoteVer = if (verMatcher.find()) verMatcher.group(1) else CURRENT_VERSION
            val notes = if (notesMatcher.find()) notesMatcher.group(1) else "Новая версия AetherClient"
            val downloadUrl = if (jarMatcher.find()) jarMatcher.group(1)
            else "https://github.com/blinoed-UWU/AetherClient/releases/latest/download/AetherLauncher.jar"

            val available = isNewer(remoteVer, CURRENT_VERSION)
            UpdateInfo(remoteVer, notes, downloadUrl, available)
        }
    }

    suspend fun downloadUpdate(downloadUrl: String, onProgress: (Float) -> Unit = {}): Result<Path> = withContext(Dispatchers.IO) {
        runCatching {
            val targetDir = LauncherPaths.localAppDataDir
            Files.createDirectories(targetDir)
            val targetJar = targetDir.resolve("AetherLauncher.jar")
            val tempJar = targetDir.resolve("AetherLauncher.jar.tmp")

            val connection = URI.create(downloadUrl).toURL().openConnection()
            connection.connectTimeout = 8000
            connection.readTimeout = 15000
            val totalBytes = connection.contentLengthLong

            var http: java.net.HttpURLConnection? = null
            try {
                http = connection as java.net.HttpURLConnection
                val code = http.responseCode
                if (code !in 200..299) {
                    error("Update download failed with HTTP $code")
                }
                val lastProgressEmit = java.util.concurrent.atomic.AtomicLong(0L)
                http.inputStream.use { input ->
                    Files.newOutputStream(tempJar).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalBytes > 0) {
                                // Throttle progress to ~20 Hz so Compose state isn't
                                // invalidated for every 8 KiB chunk.
                                val now = System.nanoTime()
                                if (now - lastProgressEmit.get() > 50_000_000L && lastProgressEmit.compareAndSet(
                                        lastProgressEmit.get(), now
                                    )
                                ) {
                                    onProgress((totalRead.toFloat() / totalBytes).coerceIn(0f, 1f))
                                }
                            }
                        }
                        onProgress(1f)
                    }
                }

                Files.move(tempJar, targetJar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                targetJar
            } catch (t: Throwable) {
                Files.deleteIfExists(tempJar)
                throw t
            } finally {
                http?.disconnect()
            }
        }
    }

    private fun isNewer(remote: String, current: String): Boolean {
        if (remote.equals(current, ignoreCase = true)) return false
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(rParts.size, cParts.size)) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
