package dev.aetherclient.launcher.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aetherclient.launcher.core.model.InjectState
import dev.aetherclient.launcher.core.model.LaunchState
import dev.aetherclient.launcher.ui.components.AetherAvatar
import dev.aetherclient.launcher.ui.components.BannerAction
import dev.aetherclient.launcher.ui.components.ElasticActionChip
import dev.aetherclient.launcher.ui.components.ElasticIconButton
import dev.aetherclient.launcher.ui.components.InteractiveBannerCarousel
import dev.aetherclient.launcher.ui.components.LiquidGlassCard
import dev.aetherclient.launcher.ui.components.PillBadge
import dev.aetherclient.launcher.ui.components.PulsingStatusDot
import dev.aetherclient.launcher.ui.state.HomeActionMode
import dev.aetherclient.launcher.ui.state.LauncherController

@Composable
fun BootstrapScreen(controller: LauncherController) {
    val state = controller.bootstrapState
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    val infinite = rememberInfiniteTransition()
    val logoBreath by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.scale(logoBreath),
            contentAlignment = Alignment.Center
        ) {
            AetherAvatar(
                size = 96.dp,
                cornerRadius = 26.dp,
                glow = true
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Aether Client",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(6.dp))

        PillBadge(
            text = "Minecraft 1.21.11",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

        Spacer(Modifier.height(34.dp))

        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            hoverExpand = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = state.stepTitle,
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(100.dp)),
                    strokeCap = StrokeCap.Round
                )

                AnimatedContent(
                    targetState = state.stepDetail,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun InjectorHomeScreen(controller: LauncherController) {
    val selected = controller.selectedProcess
    val selectedInstance = controller.selectedInstance
    val mode = controller.actionMode
    val injectState = controller.injectState
    val launchState = controller.launchState
    val settings = controller.settings
    val isEn = settings.language.equals("EN", ignoreCase = true)
    fun tr(ru: String, en: String): String = if (isEn) en else ru
    var processMenuExpanded by remember { mutableStateOf(false) }
    var instanceMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(controller.instances.size, mode) {
        if (controller.instances.size <= 1 || mode != HomeActionMode.LAUNCH) {
            instanceMenuExpanded = false
        }
    }
    LaunchedEffect(controller.processes.size, mode) {
        if (controller.processes.size <= 1 || mode != HomeActionMode.INJECT) {
            processMenuExpanded = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Client Identity Glass Card
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                tint = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AetherAvatar(
                        size = 58.dp,
                        cornerRadius = 18.dp,
                        glow = true
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "Aether Client",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PillBadge(
                                text = "1.21.11",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            PillBadge(
                                text = if (controller.payloadExists) tr("Готов", "Ready") else tr("Нет файла клиента", "Missing payload"),
                                containerColor = if (controller.payloadExists) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                },
                                contentColor = if (controller.payloadExists) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                        }
                    }
                }
            }

            // Interactive Auto-Rotating Banners
            InteractiveBannerCarousel(
                onAction = { action ->
                    when (action) {
                        BannerAction.OPEN_SETTINGS -> controller.openSettings()
                        BannerAction.ENABLE_AUTO_INJECT -> {
                            controller.updateSettings(settings.copy(autoInject = true))
                        }
                        BannerAction.OPEN_FOLDER -> controller.openPayloadsFolder()
                    }
                }
            )

            // 2-in-1 Action Card: Launch Existing Instance OR Inject Running Game
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Segmented 2-in-1 Switcher + Refresh
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModeSegmentPill(
                            label = tr("Запуск", "Launch"),
                            icon = Icons.Rounded.PlayArrow,
                            selected = mode == HomeActionMode.LAUNCH,
                            badgeCount = if (controller.instances.isNotEmpty()) controller.instances.size else null,
                            modifier = Modifier.weight(1f),
                            onClick = { controller.setHomeActionMode(HomeActionMode.LAUNCH) }
                        )
                        ModeSegmentPill(
                            label = tr("Инжект", "Inject"),
                            icon = Icons.Rounded.Bolt,
                            selected = mode == HomeActionMode.INJECT,
                            badgeCount = if (controller.processes.isNotEmpty()) controller.processes.size else null,
                            modifier = Modifier.weight(1f),
                            onClick = { controller.setHomeActionMode(HomeActionMode.INJECT) }
                        )
                        ElasticIconButton(
                            icon = Icons.Rounded.Refresh,
                            contentDescription = tr("Обновить список", "Refresh"),
                            onClick = { controller.refreshEverythingNow() },
                            size = 32.dp
                        )
                    }

                    if (mode == HomeActionMode.LAUNCH) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PulsingStatusDot(
                                color = when {
                                    selectedInstance == null -> MaterialTheme.colorScheme.tertiary
                                    selectedInstance.isCompatible -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.error
                                },
                                active = selectedInstance?.isCompatible == true
                            )
                            Text(
                                text = when {
                                    selectedInstance == null -> tr("Профили Freesm / Prism не найдены", "Freesm / Prism profiles not found")
                                    selectedInstance.isCompatible -> tr("Профиль ${selectedInstance.launcherName}", "Profile ${selectedInstance.launcherName}")
                                    else -> tr("Нужен Fabric 1.21.11 (${selectedInstance.launcherName})", "Requires Fabric 1.21.11 (${selectedInstance.launcherName})")
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (selectedInstance != null) {
                            Box {
                                Surface(
                                    onClick = {
                                        if (controller.instances.size > 1) {
                                            instanceMenuExpanded = true
                                        }
                                    },
                                    shape = RoundedCornerShape(15.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.55f),
                                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.10f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = selectedInstance.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val loaderTag = if (selectedInstance.isFabric) "Fabric" else "Vanilla"
                                            Text(
                                                text = "${selectedInstance.launcherName} • $loaderTag ${selectedInstance.mcVersion}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (selectedInstance.isCompatible) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                        if (controller.instances.size > 1) {
                                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Выбрать сборку")
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = instanceMenuExpanded,
                                    onDismissRequest = { instanceMenuExpanded = false }
                                ) {
                                    controller.instances.forEach { inst ->
                                        val loaderTag = if (inst.isFabric) "Fabric" else "Vanilla"
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(inst.name, fontWeight = FontWeight.SemiBold)
                                                    Text(
                                                        "${inst.launcherName} • $loaderTag ${inst.mcVersion}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            },
                                            onClick = {
                                                controller.selectInstance(inst.id)
                                                instanceMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PulsingStatusDot(
                                color = if (selected != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                active = selected != null
                            )
                            Text(
                                text = if (selected != null) tr("Игра обнаружена", "Game detected") else tr("Жду запуск Minecraft 1.21.11", "Waiting for Minecraft 1.21.11"),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (selected != null) {
                            Box {
                                Surface(
                                    onClick = {
                                        if (controller.processes.size > 1) {
                                            processMenuExpanded = true
                                        }
                                    },
                                    shape = RoundedCornerShape(15.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.55f),
                                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.10f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = selected.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = selected.subtitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (controller.processes.size > 1) {
                                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = tr("Выбрать процесс", "Select process"))
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = processMenuExpanded,
                                    onDismissRequest = { processMenuExpanded = false }
                                ) {
                                    controller.processes.forEach { proc ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(proc.title, fontWeight = FontWeight.SemiBold)
                                                    Text(proc.subtitle, style = MaterialTheme.typography.bodySmall)
                                                }
                                            },
                                            onClick = {
                                                controller.selectProcess(proc.pid)
                                                processMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Status Banner for active mode
            val errorMsg = when (mode) {
                HomeActionMode.LAUNCH -> (launchState as? LaunchState.Error)?.message
                HomeActionMode.INJECT -> (injectState as? InjectState.Error)?.message
            }
            val successMsg = when (mode) {
                HomeActionMode.LAUNCH -> (launchState as? LaunchState.Started)?.let {
                    tr(
                        "Запускаем ${it.instanceName} с Aether! Меню в игре — Right Shift.",
                        "Launching ${it.instanceName} with Aether! In-game menu — Right Shift."
                    )
                }
                HomeActionMode.INJECT -> if (injectState is InjectState.Success) {
                    tr(
                        "Готово! Меню в игре открывается на Right Shift.",
                        "Ready! Press Right Shift in-game to open the menu."
                    )
                } else null
            }

            AnimatedVisibility(visible = errorMsg != null || successMsg != null) {
                if (errorMsg != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else if (successMsg != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = successMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Main Action Button (Launch with Aether OR Inject into Running Game)
        val isBusy = when (mode) {
            HomeActionMode.LAUNCH -> launchState is LaunchState.Launching
            HomeActionMode.INJECT -> injectState is InjectState.Injecting
        }
        val isDone = when (mode) {
            HomeActionMode.LAUNCH -> launchState is LaunchState.Started
            HomeActionMode.INJECT -> injectState is InjectState.Success
        }
        val canAct = when (mode) {
            HomeActionMode.LAUNCH -> selectedInstance?.isCompatible == true && !isBusy && !isDone
            HomeActionMode.INJECT -> selected != null && !isBusy && !isDone
        }

        val buttonInteraction = remember { MutableInteractionSource() }
        val buttonHovered by buttonInteraction.collectIsHoveredAsState()
        val buttonPressed by buttonInteraction.collectIsPressedAsState()

        val buttonScale by animateFloatAsState(
            targetValue = when {
                buttonPressed && canAct -> 0.985f
                buttonHovered && canAct -> 1.016f
                else -> 1f
            },
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        )
        val buttonRadius by animateDpAsState(
            targetValue = if (buttonHovered && canAct) 22.dp else 18.dp,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        )

        val hoverPrimary = lerp(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, 0.24f)
        val buttonContainerColor by animateColorAsState(
            targetValue = when {
                isDone -> MaterialTheme.colorScheme.tertiary
                isBusy -> MaterialTheme.colorScheme.secondary
                canAct -> if (buttonHovered) hoverPrimary else MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)
            },
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        )
        val buttonContentColor by animateColorAsState(
            targetValue = when {
                isDone -> MaterialTheme.colorScheme.onTertiary
                isBusy -> MaterialTheme.colorScheme.onSecondary
                canAct -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            animationSpec = tween(220)
        )

        val actionKey = Triple(mode, canAct, when (mode) {
            HomeActionMode.LAUNCH -> launchState
            HomeActionMode.INJECT -> injectState
        })

        Button(
            onClick = {
                when (mode) {
                    HomeActionMode.LAUNCH -> controller.launchSelectedInstance()
                    HomeActionMode.INJECT -> controller.injectIntoSelected()
                }
            },
            enabled = canAct,
            interactionSource = buttonInteraction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(54.dp)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                },
            shape = RoundedCornerShape(buttonRadius),
            border = BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = if (buttonHovered && canAct) 0.34f else 0.14f)
            ),
            contentPadding = PaddingValues(horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonContainerColor,
                contentColor = buttonContentColor,
                disabledContainerColor = buttonContainerColor,
                disabledContentColor = buttonContentColor
            )
        ) {
            AnimatedContent(
                targetState = actionKey,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.94f)) togetherWith
                        (fadeOut() + scaleOut(targetScale = 0.94f))
                }
            ) { (activeMode, enabledNow, activeState) ->
                val busyNow = when (activeMode) {
                    HomeActionMode.LAUNCH -> activeState is LaunchState.Launching
                    HomeActionMode.INJECT -> activeState is InjectState.Injecting
                }
                val doneNow = when (activeMode) {
                    HomeActionMode.LAUNCH -> activeState is LaunchState.Started
                    HomeActionMode.INJECT -> activeState is InjectState.Success
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        busyNow -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.4.dp,
                                color = buttonContentColor
                            )
                            Text(
                                text = if (activeMode == HomeActionMode.LAUNCH) tr("Запускаем...", "Launching...") else tr("Подключаем...", "Injecting..."),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        doneNow -> {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                            Text(
                                text = if (activeMode == HomeActionMode.LAUNCH) tr("Запущено с Aether", "Launched with Aether") else tr("Заинжекчено", "Injected"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        else -> {
                            val labelText = when (activeMode) {
                                HomeActionMode.LAUNCH -> when {
                                    enabledNow -> tr("Запустить с Aether", "Launch with Aether")
                                    selectedInstance != null && !selectedInstance.isCompatible -> tr("Нужен Fabric 1.21.11", "Requires Fabric 1.21.11")
                                    else -> tr("Профиль не найден", "Profile not found")
                                }
                                HomeActionMode.INJECT -> if (enabledNow) tr("Инжектить", "Inject") else tr("Ожидание игры...", "Waiting for game...")
                            }
                            val btnIcon = when {
                                !enabledNow -> Icons.Rounded.HourglassTop
                                activeMode == HomeActionMode.LAUNCH -> Icons.Rounded.PlayArrow
                                else -> Icons.Rounded.Bolt
                            }
                            Icon(imageVector = btnIcon, contentDescription = null)
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElasticActionChip(
                    text = tr("Настройки", "Settings"),
                    icon = Icons.Rounded.Tune,
                    onClick = { controller.openSettings() }
                )
                ElasticActionChip(
                    text = tr("Папка", "Folder"),
                    icon = Icons.Rounded.FolderOpen,
                    onClick = { controller.openPayloadsFolder() }
                )
            }

            ElasticIconButton(
                icon = if (settings.darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = tr("Сменить тему", "Toggle theme"),
                onClick = {
                    controller.updateSettings(settings.copy(darkMode = !settings.darkMode))
                },
                size = 40.dp
            )
        }
    }
}

@Composable
private fun ModeSegmentPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badgeCount: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.55f)
        },
        animationSpec = tween(180)
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(180)
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = content,
        border = BorderStroke(
            width = 0.8.dp,
            color = Color.White.copy(alpha = if (selected) 0.28f else 0.10f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(5.dp))
            Text(
                text = if (badgeCount != null) "$label ($badgeCount)" else label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
