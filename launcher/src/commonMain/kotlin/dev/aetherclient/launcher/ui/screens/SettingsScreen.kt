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
                        onClick = { controller.resetClientConfig() },
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
