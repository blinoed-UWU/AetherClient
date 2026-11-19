package dev.aetherclient.launcher.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aetherclient.launcher.core.model.PaletteStyleOption
import dev.aetherclient.launcher.ui.components.ElasticIconButton
import dev.aetherclient.launcher.ui.components.LiquidGlassCard
import dev.aetherclient.launcher.ui.state.LauncherController
import dev.aetherclient.launcher.ui.theme.AetherSeedPresets
import java.nio.file.Files
import java.nio.file.Paths

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(controller: LauncherController) {
    var confirmReset by remember { mutableStateOf(false) }
    val settings = controller.settings
    val isEn = settings.language.equals("EN", ignoreCase = true)
    fun tr(ru: String, en: String): String = if (isEn) en else ru

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ElasticIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = tr("Назад", "Back"),
                onClick = { controller.backToHome() },
                size = 36.dp
            )
            Text(
                text = tr("Настройки", "Settings"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Material You Liquid Glass Card
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("Язык / Language", "Language / Язык"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("RU" to "Русский", "EN" to "English").forEach { (code, label) ->
                            FilterChip(
                                selected = settings.language.equals(code, ignoreCase = true),
                                onClick = {
                                    controller.updateSettings(settings.copy(language = code))
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("Тёмная тема", "Dark Mode"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = settings.darkMode,
                        onCheckedChange = {
                            controller.updateSettings(settings.copy(darkMode = it))
                        }
                    )
                }

                Text(tr("Палитра", "Palette"), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AetherSeedPresets.forEach { preset ->
                        val selected = settings.seedColorHex == preset.hex
                        val interaction = remember { MutableInteractionSource() }
                        val hovered by interaction.collectIsHoveredAsState()
                        val ring by animateDpAsState(if (selected) 2.5.dp else 0.dp)
                        val swatchScale by animateFloatAsState(
                            targetValue = if (hovered) 1.15f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .scale(swatchScale)
                                .clip(CircleShape)
                                .background(Color(preset.hex))
                                .border(ring, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable(interactionSource = interaction, indication = null) {
                                    controller.updateSettings(settings.copy(seedColorHex = preset.hex))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = preset.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaletteStyleOption.entries.forEach { style ->
                        FilterChip(
                            selected = settings.paletteStyle == style,
                            onClick = {
                                controller.updateSettings(settings.copy(paletteStyle = style))
                            },
                            label = { Text(style.title) }
                        )
                    }
                }
            }
        }

        // Behavior Card
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(tr("Лоадер", "Loader"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                CompactToggleRow(
                    title = tr("Авто-инжект", "Auto-inject"),
                    subtitle = tr("Подключаться сразу после запуска игры", "Attach automatically when the game starts"),
                    checked = settings.autoInject,
                    onCheckedChange = { controller.updateSettings(settings.copy(autoInject = it)) }
                )

                CompactToggleRow(
                    title = tr("Сворачивать после инжекта", "Minimize after inject"),
                    subtitle = tr("Скрывать окно после успешного запуска", "Hide window after successful injection"),
                    checked = settings.minimizeAfterInject,
                    onCheckedChange = { controller.updateSettings(settings.copy(minimizeAfterInject = it)) }
                )

                CompactToggleRow(
                    title = tr("Все процессы Java", "All Java processes"),
                    subtitle = tr("Показывать сторонние окна для ручного выбора", "Show other JVM windows for manual selection"),
                    checked = settings.showAllJvmProcesses,
                    onCheckedChange = { controller.updateSettings(settings.copy(showAllJvmProcesses = it)) }
                )
            }
        }

        // Custom Path Card
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(tr("Файл клиента", "Client Payload"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                var pathText by remember { mutableStateOf(settings.customAgentPath) }
                LaunchedEffect(settings.customAgentPath) {
                    if (pathText != settings.customAgentPath) {
                        pathText = settings.customAgentPath
                    }
                }
                val trimmedCustom = pathText.trim()
                val parsedCustom = if (trimmedCustom.isNotEmpty()) {
                    runCatching { Paths.get(trimmedCustom) }.getOrNull()
                } else null
                val customPathInvalid = trimmedCustom.isNotEmpty() &&
                    (parsedCustom == null || !Files.isRegularFile(parsedCustom))

                OutlinedTextField(
                    value = pathText,
                    onValueChange = {
                        pathText = it
                        controller.updateSettings(settings.copy(customAgentPath = it))
                    },
                    label = { Text(tr("Свой путь к .jar", "Custom .jar path")) },
                    isError = customPathInvalid,
                    supportingText = if (customPathInvalid) {
                        {
                            Text(
                                if (parsedCustom == null) tr("Некорректный путь к файлу", "Invalid file path")
                                else tr("Файл не найден — используется стандартный", "File not found — using default")
                            )
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val displayPayloadPath = runCatching { controller.payloadPath.toString() }
                    .getOrElse { trimmedCustom }
                Text(
                    text = displayPayloadPath,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                OutlinedButton(
                    onClick = { controller.openPayloadsFolder() },
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Открыть папку", "Open Folder"))
                }
            }
        }

        // Config Management Card
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(tr("Конфигурация клиента", "Client Configuration"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    tr(
                        "Сброс настроек всех модулей, биндов, цветов и расположения HUD к заводским значениям.",
                        "Reset all module settings, keybinds, colors, and HUD widget positions back to default values."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                controller.configResetBanner?.let { bannerText ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = bannerText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "✕",
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { controller.dismissConfigResetBanner() }
                                    .padding(4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { controller.openConfigsFolder() },
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Папка конфигов", "Configs Folder"))
                    }

                    OutlinedButton(
                        onClick = { confirmReset = true },
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Сбросить конфиг", "Reset Config"))
                    }
                }
            }
        }

        if (confirmReset) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmReset = false },
                title = { Text(tr("Сбросить конфиг?", "Reset config?")) },
                text = {
                    Text(tr(
                        "Все .json конфиги клиента будут удалены и восстановлены заводские настройки. Действие необратимо.",
                        "All client .json configs will be deleted and factory defaults restored. This cannot be undone."
                    ))
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmReset = false
                        controller.resetClientConfig()
                    }) { Text(tr("Сбросить", "Reset"), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmReset = false }) {
                        Text(tr("Отмена", "Cancel"))
                    }
                }
            )
        }

        // OTA Auto-Update Card
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = tr("Обновления клиента (OTA)", "Client Updates (OTA)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tr("Текущая версия: v1.2.0 • GitHub Releases", "Current version: v1.2.0 • GitHub Releases"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                controller.updateBanner?.let { bannerText ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = bannerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "✕",
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { controller.dismissUpdateBanner() }
                                    .padding(4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { controller.checkForUpdates(silent = false) },
                        enabled = !controller.isCheckingUpdate && !controller.isDownloadingUpdate,
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (controller.isCheckingUpdate) tr("Проверка...", "Checking...")
                            else tr("Проверить обновления", "Check Updates")
                        )
                    }

                    if (controller.updateInfo?.isAvailable == true) {
                        OutlinedButton(
                            onClick = { controller.downloadAndApplyUpdate() },
                            enabled = !controller.isDownloadingUpdate,
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (controller.isDownloadingUpdate) tr("Загрузка ${(controller.updateProgress * 100).toInt()}%", "Downloading ${(controller.updateProgress * 100).toInt()}%")
                                else tr("Обновить сейчас", "Update Now")
                            )
                        }
                    }
                }
            }
        }

        // Trace Cleaner & Self-Destruct Card
        var cleanLogs by remember { mutableStateOf(true) }
        var wipeConfigs by remember { mutableStateOf(false) }
        var selfDestruct by remember { mutableStateOf(false) }
        var confirmDialog by remember { mutableStateOf(false) }

        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tr("Очистка следов и Самоуничтожение", "Trace Cleaner & Self-Destruct"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = tr("Удаление следов инжекта, логов, кэшей и параметров для успешного прохождения проверок на читы", "Wipes injection traces, logs, caches, and settings before screenshare/staff check"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                controller.traceCleanBanner?.let { bannerText ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.80f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = bannerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "✕",
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { controller.dismissTraceCleanBanner() }
                                    .padding(4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                CompactToggleRow(
                    title = tr("Очистить логи, кэши и дампы", "Clean logs, caches, and dumps"),
                    subtitle = tr("Удаляет временные файлы Java, дампы и папки логов", "Deletes temp java files, payloads, and logs"),
                    checked = cleanLogs,
                    onCheckedChange = { cleanLogs = it }
                )

                CompactToggleRow(
                    title = tr("Удалить конфиги клиента (Anti-Check)", "Wipe client configs (Anti-Check)"),
                    subtitle = tr("Безвозвратно удаляет все .json файлы настроек на диске", "Permanently deletes all config files on disk"),
                    checked = wipeConfigs,
                    onCheckedChange = { wipeConfigs = it }
                )

                CompactToggleRow(
                    title = tr("Удалить лаунчер (Самоуничтожение)", "Delete launcher (Self-Destruct)"),
                    subtitle = tr("Бесследно удаляет исполняемый файл лаунчера и закрывает процесс", "Permanently deletes launcher executable and terminates"),
                    checked = selfDestruct,
                    onCheckedChange = { selfDestruct = it }
                )

                OutlinedButton(
                    onClick = {
                        controller.cleanTraces(cleanLogs, wipeConfigs, selfDestruct)
                    },
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selfDestruct) tr("ОЧИСТИТЬ И САМОУНИЧТОЖИТЬСЯ", "CLEAN & SELF-DESTRUCT")
                        else tr("ОЧИСТИТЬ СЛЕДЫ", "CLEAN TRACES"),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.50f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
