package dev.aetherclient.launcher.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class BannerAction {
    OPEN_SETTINGS,
    ENABLE_AUTO_INJECT,
    OPEN_FOLDER
}

data class InfoBanner(
    val tag: String,
    val title: String,
    val body: String,
    val icon: ImageVector,
    val actionLabel: String? = null,
    val action: BannerAction? = null
)

private val defaultBanners = listOf(
    InfoBanner(
        tag = "Гайд",
        title = "Как заинжектить за 3 шага",
        body = "1. Запусти Minecraft 1.21.11  2. Дождись зелёной точки в радаре  3. Нажми «Инжектить».",
        icon = Icons.Rounded.Bolt
    ),
    InfoBanner(
        tag = "Управление",
        title = "Меню визуала в игре",
        body = "Нажми Right Shift после инжекта. Все модули и виджеты можно перетаскивать прямо на экране.",
        icon = Icons.Rounded.Keyboard
    ),
    InfoBanner(
        tag = "Удобство",
        title = "Автоматический инжект",
        body = "Не нажимай каждый раз — включи авто-инжект, и лоадер сам подключится к запущенной игре.",
        icon = Icons.Rounded.AutoAwesome,
        actionLabel = "Настроить",
        action = BannerAction.OPEN_SETTINGS
    ),
    InfoBanner(
        tag = "Стиль",
        title = "Твой оттенок Material You",
        body = "Выбирай цвет и тёмную или светлую тему — фон и стекло мгновенно перестроятся под него.",
        icon = Icons.Rounded.Palette,
        actionLabel = "Палитра",
        action = BannerAction.OPEN_SETTINGS
    )
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InteractiveBannerCarousel(
    onAction: (BannerAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var direction by remember { mutableIntStateOf(1) }
    var isHovered by remember { mutableStateOf(false) }
    var dragAccum by remember { mutableFloatStateOf(0f) }

    // Auto-advance banners when not hovered
    LaunchedEffect(currentIndex, isHovered) {
        if (!isHovered) {
            delay(5500)
            direction = 1
            currentIndex = (currentIndex + 1) % defaultBanners.size
        }
    }

    fun nextBanner() {
        direction = 1
        currentIndex = (currentIndex + 1) % defaultBanners.size
    }

    fun prevBanner() {
        direction = -1
        currentIndex = if (currentIndex == 0) defaultBanners.lastIndex else currentIndex - 1
    }

    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                    onDragEnd = {
                        if (dragAccum < -40f) nextBanner()
                        else if (dragAccum > 40f) prevBanner()
                        dragAccum = 0f
                    }
                )
            },
        cornerRadius = 22.dp,
        tint = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.20f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    val enter = slideInHorizontally(tween(300)) { width ->
                        if (direction >= 0) width / 3 else -width / 3
                    } + fadeIn(tween(260))
                    val exit = slideOutHorizontally(tween(260)) { width ->
                        if (direction >= 0) -width / 3 else width / 3
                    } + fadeOut(tween(200))
                    enter togetherWith exit
                }
            ) { idx ->
                val banner = defaultBanners[idx]
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = banner.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            PillBadge(
                                text = banner.tag,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (banner.actionLabel != null && banner.action != null) {
                            Surface(
                                onClick = { onAction(banner.action) },
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = banner.actionLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = banner.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = banner.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        minLines = 2,
                        maxLines = 2
                    )
                }
            }

            // Bottom indicator dots + arrow controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    defaultBanners.indices.forEach { idx ->
                        val active = idx == currentIndex
                        val pillWidth by animateDpAsState(
                            targetValue = if (active) 22.dp else 7.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            }
                        )
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(pillWidth)
                                .clip(CircleShape)
                                .background(dotColor)
                                .clickable {
                                    direction = if (idx >= currentIndex) 1 else -1
                                    currentIndex = idx
                                }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isHovered,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BannerArrowButton(
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            description = "Предыдущий баннер",
                            onClick = { prevBanner() }
                        )
                        BannerArrowButton(
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            description = "Следующий баннер",
                            onClick = { nextBanner() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerArrowButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.16f)),
        modifier = Modifier.size(26.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
