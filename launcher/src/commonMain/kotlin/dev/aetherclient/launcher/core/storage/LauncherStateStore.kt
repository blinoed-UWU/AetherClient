package dev.aetherclient.launcher.core.storage

import dev.aetherclient.launcher.core.model.LoaderSettings
import dev.aetherclient.launcher.core.platform.LauncherPaths
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object LauncherStateStore {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): LoaderSettings {
        val file = LauncherPaths.stateFile
        if (!Files.exists(file)) {
            val initial = LoaderSettings()
            runCatching { save(initial) }
            return initial
        }
        return runCatching {
            json.decodeFromString<LoaderSettings>(Files.readString(file))
        }.getOrElse {
            LoaderSettings()
        }
    }

    fun save(settings: LoaderSettings) {
        Files.createDirectories(LauncherPaths.rootDir)
        val file = LauncherPaths.stateFile
        val tmp = file.resolveSibling("loader_settings.json.tmp")
        Files.writeString(tmp, json.encodeToString(settings))
        runCatching {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        }.getOrElse {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
