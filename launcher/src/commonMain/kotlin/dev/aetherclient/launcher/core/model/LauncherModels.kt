package dev.aetherclient.launcher.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class PaletteStyleOption(val title: String) {
    TONAL_SPOT("Tonal"),
    VIBRANT("Vibrant"),
    EXPRESSIVE("Expressive"),
    NEUTRAL("Neutral")
}

@Serializable
data class LoaderSettings(
    val darkMode: Boolean = true,
    val seedColorHex: Long = 0xFF7C5CFF,
    val paletteStyle: PaletteStyleOption = PaletteStyleOption.TONAL_SPOT,
    val language: String = "RU",
    val autoInject: Boolean = false,
    val minimizeAfterInject: Boolean = false,
    val showAllJvmProcesses: Boolean = false,
    val customAgentPath: String = "",
    val selectedInstanceId: String = ""
)

data class JvmProcessInfo(
    val pid: String,
    val displayName: String,
    val title: String,
    val subtitle: String,
    val isMinecraft: Boolean,
    val hasPreAgent: Boolean = false
)

data class GameInstanceInfo(
    val id: String,
    val name: String,
    val launcherName: String,
    val launcherBinary: String,
    val instanceDirPath: String,
    val mcVersion: String,
    val isFabric: Boolean,
    val isCompatible: Boolean
)

data class BootstrapState(
    val stepTitle: String,
    val stepDetail: String,
    val progress: Float
)

sealed interface InjectState {
    data object Idle : InjectState
    data object Injecting : InjectState
    data class Success(val pid: String) : InjectState
    data class Error(val message: String) : InjectState
}

sealed interface LaunchState {
    data object Idle : LaunchState
    data object Launching : LaunchState
    data class Started(val instanceName: String) : LaunchState
    data class Error(val message: String) : LaunchState
}
