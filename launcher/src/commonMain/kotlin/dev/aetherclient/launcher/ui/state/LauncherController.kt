package dev.aetherclient.launcher.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.aetherclient.launcher.core.bootstrap.LoaderBootstrapper
import dev.aetherclient.launcher.core.inject.JvmInjector
import dev.aetherclient.launcher.core.launch.InstanceManager
import dev.aetherclient.launcher.core.model.BootstrapState
import dev.aetherclient.launcher.core.model.GameInstanceInfo
import dev.aetherclient.launcher.core.model.InjectState
import dev.aetherclient.launcher.core.model.JvmProcessInfo
import dev.aetherclient.launcher.core.model.LaunchState
import dev.aetherclient.launcher.core.model.LoaderSettings
import dev.aetherclient.launcher.core.platform.LauncherPaths
import dev.aetherclient.launcher.core.platform.PlatformInfo
import dev.aetherclient.launcher.core.storage.LauncherStateStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path

enum class LoaderScreen {
    BOOTSTRAP,
    HOME,
    SETTINGS
}

enum class HomeActionMode {
    LAUNCH,
    INJECT
}

class LauncherController {
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + exceptionHandler)
    private var refreshJob: Job? = null
    private var radarJob: Job? = null
    private var lastInjectAttemptMs = 0L
    private var lastInjectSuccessMs = 0L
    private var autoInjectedPids = mutableSetOf<String>()
    private val preAgentSyncPids = mutableSetOf<String>()
    private var userPickedMode = false

    var settings by mutableStateOf(LauncherStateStore.load())
        private set

    var currentScreen by mutableStateOf(LoaderScreen.BOOTSTRAP)
        private set

    var bootstrapState by mutableStateOf(BootstrapState("Запуск", "Подготовка AetherLoader...", 0.05f))
        private set

    var attachReady by mutableStateOf(false)
        private set

    var payloadPath by mutableStateOf<Path>(LauncherPaths.payloadsDir.resolve("aether-agent.jar"))
        private set

    var payloadExists by mutableStateOf(false)
        private set

    var processes by mutableStateOf<List<JvmProcessInfo>>(emptyList())
        private set

    var selectedPid by mutableStateOf<String?>(null)
        private set

    var instances by mutableStateOf<List<GameInstanceInfo>>(emptyList())
        private set

    var actionMode by mutableStateOf(HomeActionMode.INJECT)
        private set

    var injectState by mutableStateOf<InjectState>(InjectState.Idle)
        private set

    var launchState by mutableStateOf<LaunchState>(LaunchState.Idle)
        private set

    var minimizeRequested by mutableStateOf(false)

    val selectedProcess: JvmProcessInfo?
        get() = processes.firstOrNull { it.pid == selectedPid } ?: processes.firstOrNull()

    val selectedInstance: GameInstanceInfo?
        get() = instances.firstOrNull { it.id == settings.selectedInstanceId }
            ?: instances.firstOrNull { it.isCompatible }
            ?: instances.firstOrNull()

    init {
        startBootstrap()
    }

    fun startBootstrap() {
        currentScreen = LoaderScreen.BOOTSTRAP
        scope.launch {
            runCatching {
                val res = LoaderBootstrapper.runBootstrap(settings.customAgentPath) { step ->
                    bootstrapState = step
                }
                attachReady = res.attachReady
                payloadPath = res.payloadPath
                payloadExists = res.payloadExists
                instances = InstanceManager.discoverInstances()
                refreshProcessesNow()
            }.onFailure { err ->
                bootstrapState = BootstrapState(
                    "Ошибка",
                    err.message ?: "Не удалось завершить подготовку",
                    1f
                )
            }
            currentScreen = LoaderScreen.HOME
            startProcessRadar()
        }
    }

    private fun startProcessRadar() {
        radarJob?.cancel()
        radarJob = scope.launch {
            while (isActive) {
                delay(1800)
                if (currentScreen == LoaderScreen.HOME) {
                    refreshProcessesNow()
                }
            }
        }
    }

    fun refreshProcessesNow() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            runCatching {
                payloadPath = LoaderBootstrapper.resolvePayloadPath(settings.customAgentPath)
                payloadExists = Files.isRegularFile(payloadPath)
                val found = JvmInjector.scanProcesses(settings.showAllJvmProcesses)
                processes = found
                val livePids = found.mapTo(mutableSetOf()) { it.pid }
                autoInjectedPids.retainAll(livePids)
                preAgentSyncPids.retainAll(livePids)

                if (instances.isEmpty()) {
                    instances = InstanceManager.discoverInstances()
                }

                if (selectedPid == null || selectedPid !in livePids) {
                    selectedPid = found.firstOrNull()?.pid
                }

                if (!userPickedMode) {
                    actionMode = if (found.isNotEmpty() || instances.isEmpty()) {
                        HomeActionMode.INJECT
                    } else {
                        HomeActionMode.LAUNCH
                    }
                }

                val target = selectedProcess
                val currentInject = injectState
                if (currentInject is InjectState.Success && currentInject.pid !in livePids) {
                    injectState = InjectState.Idle
                } else if (
                    currentInject is InjectState.Success &&
                    currentInject.pid == target?.pid &&
                    System.currentTimeMillis() - lastInjectSuccessMs >= 2500L
                ) {
                    if (!JvmInjector.isClientActive(currentInject.pid)) {
                        injectState = InjectState.Idle
                    }
                } else if (
                    target != null &&
                    target.hasPreAgent &&
                    target.pid !in preAgentSyncPids &&
                    injectState == InjectState.Idle
                ) {
                    autoInjectedPids.add(target.pid)
                    if (JvmInjector.isClientActive(target.pid)) {
                        preAgentSyncPids.add(target.pid)
                        lastInjectSuccessMs = System.currentTimeMillis()
                        injectState = InjectState.Success(target.pid)
                    }
                }

                // Optional auto-inject when Minecraft appears without -javaagent
                if (
                    settings.autoInject &&
                    target != null &&
                    target.isMinecraft &&
                    !target.hasPreAgent &&
                    payloadExists &&
                    injectState == InjectState.Idle &&
                    target.pid !in autoInjectedPids
                ) {
                    autoInjectedPids.add(target.pid)
                    injectIntoSelected()
                }
            }
        }
    }

    fun setHomeActionMode(mode: HomeActionMode) {
        userPickedMode = true
        actionMode = mode
        if (mode == HomeActionMode.LAUNCH && launchState is LaunchState.Error) {
            launchState = LaunchState.Idle
        }
        if (mode == HomeActionMode.INJECT && injectState is InjectState.Error) {
            injectState = InjectState.Idle
        }
    }

    fun selectInstance(instanceId: String) {
        updateSettings(settings.copy(selectedInstanceId = instanceId))
        if (launchState is LaunchState.Error || launchState is LaunchState.Started) {
            launchState = LaunchState.Idle
        }
    }

    fun refreshEverythingNow() {
        scope.launch {
            runCatching {
                instances = InstanceManager.discoverInstances()
            }
            refreshProcessesNow()
        }
    }

    fun selectProcess(pid: String) {
        selectedPid = pid
        val current = injectState
        if (current is InjectState.Error || (current is InjectState.Success && current.pid != pid)) {
            injectState = InjectState.Idle
        }
    }

    fun launchSelectedInstance() {
        val target = selectedInstance ?: run {
            launchState = LaunchState.Error("Не найдено сборок FreesmLauncher или PrismLauncher.")
            return
        }
        if (!target.isCompatible) {
            launchState = LaunchState.Error("Профиль ${target.name} несовместим (нужен Fabric 1.21.11).")
            return
        }
        if (launchState is LaunchState.Launching || (launchState as? LaunchState.Started)?.instanceName == target.name) {
            return
        }

        payloadPath = LoaderBootstrapper.resolvePayloadPath(settings.customAgentPath)
        payloadExists = Files.isRegularFile(payloadPath)

        if (!payloadExists) {
            launchState = LaunchState.Error("Файл визуала aether-agent.jar ещё не собран (папка payloads пуста).")
            return
        }

        launchState = LaunchState.Launching

        scope.launch {
            val result = InstanceManager.launchInstanceWithAgent(target, payloadPath)
            result.fold(
                onSuccess = {
                    launchState = LaunchState.Started(target.name)
                    if (settings.minimizeAfterInject) {
                        delay(900)
                        minimizeRequested = true
                    }
                },
                onFailure = { err ->
                    launchState = LaunchState.Error(err.message ?: "Не удалось запустить сборку с Aether.")
                }
            )
        }
    }

    fun injectIntoSelected() {
        val target = selectedProcess ?: run {
            injectState = InjectState.Error("Запущенный процесс Minecraft 1.21.11 не найден.")
            return
        }
        if (injectState is InjectState.Injecting || (injectState as? InjectState.Success)?.pid == target.pid) {
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastInjectAttemptMs < 800L) {
            return
        }
        lastInjectAttemptMs = now

        payloadPath = LoaderBootstrapper.resolvePayloadPath(settings.customAgentPath)
        payloadExists = Files.isRegularFile(payloadPath)

        if (!payloadExists) {
            injectState = InjectState.Error("Файл визуала aether-agent.jar ещё не собран (папка payloads пуста).")
            return
        }

        injectState = InjectState.Injecting

        scope.launch {
            val result = JvmInjector.injectAgent(
                pid = target.pid,
                agentJar = payloadPath,
                options = "mc=1.21.11;loader=fabric"
            )
            result.fold(
                onSuccess = {
                    lastInjectSuccessMs = System.currentTimeMillis()
                    autoInjectedPids.add(target.pid)
                    injectState = InjectState.Success(target.pid)
                    if (settings.minimizeAfterInject) {
                        delay(700)
                        minimizeRequested = true
                    }
                },
                onFailure = { err ->
                    injectState = InjectState.Error(err.message ?: "Ошибка подключения к JVM (Attach API).")
                }
            )
        }
    }

    fun openSettings() {
        currentScreen = LoaderScreen.SETTINGS
    }

    fun backToHome() {
        currentScreen = LoaderScreen.HOME
        refreshProcessesNow()
    }

    fun updateSettings(newSettings: LoaderSettings) {
        settings = newSettings
        runCatching { LauncherStateStore.save(newSettings) }
        payloadPath = LoaderBootstrapper.resolvePayloadPath(newSettings.customAgentPath)
        payloadExists = Files.isRegularFile(payloadPath)
    }

    fun openPayloadsFolder() {
        PlatformInfo.openDirectory(LauncherPaths.payloadsDir)
    }

    var configResetBanner by mutableStateOf<String?>(null)
        private set

    fun resetClientConfig() {
        runCatching {
            val configsDir = LauncherPaths.configsDir
            if (Files.exists(configsDir)) {
                Files.list(configsDir).use { stream ->
                    stream.forEach { file ->
                        if (Files.isRegularFile(file) && file.toString().endsWith(".json")) {
                            runCatching { Files.deleteIfExists(file) }
                        }
                    }
                }
            }
            configResetBanner = if (settings.language.equals("EN", ignoreCase = true)) {
                "Client config has been reset to defaults!"
            } else {
                "Конфиг клиента успешно сброшен к заводским настройкам!"
            }
        }.onFailure { err ->
            configResetBanner = "Ошибка сброса: ${err.message}"
        }
    }

    fun dismissConfigResetBanner() {
        configResetBanner = null
    }

    fun openConfigsFolder() {
        PlatformInfo.openDirectory(LauncherPaths.configsDir)
    }

    fun close() {
        scope.cancel()
    }
}
