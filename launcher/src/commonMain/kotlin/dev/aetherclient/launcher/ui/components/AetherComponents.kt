package dev.aetherclient.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.aetherclient.launcher.resources.Res
import dev.aetherclient.launcher.resources.aether_logo
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

fun Modifier.liquidJelly(
    maxMagneticPullPx: Float = 5.5f,
    maxDragPx: Float = 16f,
    hoverScale: Float = 1.016f,
    pressScale: Float = 0.976f,
    enabled: Boolean = true,
    onPointerUpdate: ((Offset?, Boolean) -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    var layoutCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var pressRootPos by remember { mutableStateOf(Offset.Zero) }
    var magneticTarget by remember { mutableStateOf(Offset.Zero) }
    var dragTarget by remember { mutableStateOf(Offset.Zero) }
    var currentAppliedOffset by remember { mutableStateOf(Offset.Zero) }

    val animatedOffset by animateOffsetAsState(
        targetValue = magneticTarget + dragTarget,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 360f)
    )
    SideEffect {
        currentAppliedOffset = animatedOffset
    }

    val dragStretchX = abs(animatedOffset.x) * 0.0024f - abs(animatedOffset.y) * 0.0014f
    val dragStretchY = abs(animatedOffset.y) * 0.0024f - abs(animatedOffset.x) * 0.0014f

    val baseScale by animateFloatAsState(
        targetValue = when {
            isPressed -> pressScale
            isHovered -> hoverScale
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.46f, stiffness = 420f)
    )

    this
        .onGloballyPositioned { coords ->
            layoutCoords = coords
            boxSize = coords.size
        }
        .pointerInput(maxMagneticPullPx, maxDragPx) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val coords = layoutCoords
                    val w = boxSize.width.toFloat().coerceAtLeast(1f)
                    val h = boxSize.height.toFloat().coerceAtLeast(1f)
                    val change = event.changes.firstOrNull()

                    if (change != null && coords != null && coords.isAttached) {
                        val rootPos = coords.localToRoot(change.position)
                        val restOrigin = coords.localToRoot(Offset.Zero) - currentAppliedOffset
                        val localFromRest = rootPos - restOrigin
                        val nx = ((localFromRest.x / w) - 0.5f) * 2f
                        val ny = ((localFromRest.y / h) - 0.5f) * 2f
                        val cNx = nx.coerceIn(-1f, 1f)
                        val cNy = ny.coerceIn(-1f, 1f)
                        val edgeProximity = max(abs(cNx), abs(cNy))

                        when (event.type) {
                            PointerEventType.Enter -> {
                                isHovered = true
                                val pullFactor = 0.35f + 0.65f * edgeProximity
                                magneticTarget = Offset(
                                    cNx * maxMagneticPullPx * pullFactor,
                                    cNy * maxMagneticPullPx * pullFactor
                                )
                                onPointerUpdate?.invoke(localFromRest, true)
                            }
                            PointerEventType.Move -> {
                                if (isPressed) {
                                    val raw = rootPos - pressRootPos
                                    val dx = raw.x * maxDragPx / (abs(raw.x) + maxDragPx * 1.35f)
                                    val dy = raw.y * maxDragPx / (abs(raw.y) + maxDragPx * 1.35f)
                                    dragTarget = Offset(dx, dy)
                                    magneticTarget = Offset.Zero
                                } else {
                                    isHovered = true
                                    val pullFactor = 0.35f + 0.65f * edgeProximity
                                    magneticTarget = Offset(
                                        cNx * maxMagneticPullPx * pullFactor,
                                        cNy * maxMagneticPullPx * pullFactor
                                    )
                                }
                                onPointerUpdate?.invoke(localFromRest, isHovered)
                            }
                            PointerEventType.Press -> {
                                if (change.pressed) {
                                    isPressed = true
                                    pressRootPos = rootPos
                                }
                            }
                            PointerEventType.Release -> {
                                isPressed = false
                                dragTarget = Offset.Zero
                                if (!isHovered) {
                                    magneticTarget = Offset.Zero
                                    onPointerUpdate?.invoke(null, false)
                                }
                            }
                            PointerEventType.Exit -> {
                                isPressed = false
                                dragTarget = Offset.Zero
                                isHovered = false
                                magneticTarget = Offset.Zero
                                onPointerUpdate?.invoke(null, false)
                            }
                        }
                    } else if (event.type == PointerEventType.Exit) {
                        isPressed = false
                        dragTarget = Offset.Zero
                        isHovered = false
                        magneticTarget = Offset.Zero
                        onPointerUpdate?.invoke(null, false)
                    } else if (event.type == PointerEventType.Release) {
                        isPressed = false
                        dragTarget = Offset.Zero
                    }
                }
            }
        }
        .graphicsLayer {
            translationX = animatedOffset.x
            translationY = animatedOffset.y
            scaleX = baseScale + dragStretchX
            scaleY = baseScale + dragStretchY
        }
}

@Composable
fun AetherAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    glow: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (hovered) 1.07f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )

    Box(
        modifier = modifier
            .size(size)
            .liquidJelly(maxMagneticPullPx = 4f, maxDragPx = 12f, hoverScale = 1.06f),
        contentAlignment = Alignment.Center
    ) {
        if (glow) {
            Box(
                modifier = Modifier
                    .size(size * 1.15f)
                    .blur(18.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.55f), tertiary.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
        }
        Image(
            painter = painterResource(Res.drawable.aether_logo),
            contentDescription = "Aether Client",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .scale(scale)
                .clip(RoundedCornerShape(cornerRadius))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            primary.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
    }
}

@Composable
fun LiquidGlassBackground(
    cursorPos: Offset,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f

    val infinite = rememberInfiniteTransition()
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2.0).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        // Blurred drifting liquid glass color orbs
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(72.dp)
        ) {
            val w = size.width
            val h = size.height

            val orb1 = Offset(
                x = w * (0.22f + 0.10f * cos(phase)),
                y = h * (0.18f + 0.08f * sin(phase))
            )
            val orb2 = Offset(
                x = w * (0.82f + 0.10f * sin(phase * 0.8f)),
                y = h * (0.48f + 0.12f * cos(phase * 0.9f))
            )
            val orb3 = Offset(
                x = w * (0.36f + 0.14f * cos(phase + 1.8f)),
                y = h * (0.86f + 0.08f * sin(phase + 1.2f))
            )

            val alphaMain = if (isDark) 0.42f else 0.30f
            val alphaSec = if (isDark) 0.34f else 0.24f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorScheme.primary.copy(alpha = alphaMain), Color.Transparent),
                    center = orb1,
                    radius = w * 0.62f
                ),
                radius = w * 0.62f,
                center = orb1
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorScheme.tertiary.copy(alpha = alphaSec), Color.Transparent),
                    center = orb2,
                    radius = w * 0.58f
                ),
                radius = w * 0.58f,
                center = orb2
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colorScheme.secondary.copy(alpha = alphaSec), Color.Transparent),
                    center = orb3,
                    radius = w * 0.56f
                ),
                radius = w * 0.56f,
                center = orb3
            )

            if (cursorPos != Offset.Unspecified) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.16f), Color.Transparent),
                        center = cursorPos,
                        radius = w * 0.42f
                    ),
                    radius = w * 0.42f,
                    center = cursorPos
                )
            }
        }

        // Subtle frosted veil
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    colorScheme.surface.copy(alpha = if (isDark) 0.38f else 0.48f)
                )
        )

        content()
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    hoverExpand: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val primary = MaterialTheme.colorScheme.primary
    var isHovered by remember { mutableStateOf(false) }
    var localPointer by remember { mutableStateOf(Offset.Unspecified) }

    val glassAlpha by animateFloatAsState(
        targetValue = if (isHovered) {
            if (isDark) 0.68f else 0.80f
        } else {
            if (isDark) 0.54f else 0.68f
        },
        animationSpec = tween(200)
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isHovered) 0.58f else 0.24f,
        animationSpec = tween(200)
    )

    val shape = RoundedCornerShape(cornerRadius)

    val borderBrush = if (isHovered && localPointer != Offset.Unspecified) {
        Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.85f else 0.95f),
                primary.copy(alpha = 0.60f),
                Color.White.copy(alpha = 0.14f)
            ),
            center = localPointer,
            radius = 220f
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) borderAlpha else borderAlpha * 1.35f),
                primary.copy(alpha = if (isHovered) 0.35f else 0.12f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    }

    val interactiveModifier = modifier
        .liquidJelly(
            maxMagneticPullPx = 3.5f,
            maxDragPx = 10f,
            hoverScale = if (hoverExpand) 1.011f else 1f,
            pressScale = 0.988f,
            onPointerUpdate = { ptr, hovered ->
                isHovered = hovered
                localPointer = ptr ?: Offset.Unspecified
            }
        )
        .clip(shape)
        .let { base ->
            if (onClick != null) base.clickable(onClick = onClick) else base
        }

    Surface(
        modifier = interactiveModifier,
        shape = shape,
        color = tint.copy(alpha = glassAlpha),
        border = BorderStroke(
            width = if (isHovered) 1.3.dp else 1.dp,
            brush = borderBrush
        )
    ) {
        Box(
            modifier = Modifier.drawBehind {
                // Top specular glass sheen
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.09f else 0.28f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.45f
                    )
                )
                // Cursor spotlight under glass
                if (isHovered && localPointer != Offset.Unspecified) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primary.copy(alpha = if (isDark) 0.20f else 0.15f),
                                Color.Transparent
                            ),
                            center = localPointer,
                            radius = size.maxDimension * 0.58f
                        ),
                        radius = size.maxDimension * 0.58f,
                        center = localPointer
                    )
                }
            }
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun ElasticActionChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var pointerHovered by remember { mutableStateOf(false) }
    var localPointer by remember { mutableStateOf(Offset.Unspecified) }
    val activeHover = hovered || pointerHovered

    val horizontalPad by animateDpAsState(
        targetValue = if (activeHover) 18.dp else 13.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )
    val bg by animateColorAsState(
        targetValue = if (activeHover) MaterialTheme.colorScheme.primaryContainer else containerColor.copy(alpha = 0.72f),
        animationSpec = tween(180)
    )
    val fg by animateColorAsState(
        targetValue = if (activeHover) MaterialTheme.colorScheme.onPrimaryContainer else contentColor,
        animationSpec = tween(180)
    )

    val borderBrush = if (activeHover && localPointer != Offset.Unspecified) {
        Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.75f), MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), Color.White.copy(alpha = 0.12f)),
            center = localPointer,
            radius = 120f
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = if (activeHover) 0.32f else 0.14f), Color.White.copy(alpha = 0.08f))
        )
    }

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.liquidJelly(
            maxMagneticPullPx = 3f,
            maxDragPx = 9f,
            hoverScale = 1.032f,
            pressScale = 0.955f,
            onPointerUpdate = { ptr, hov ->
                pointerHovered = hov
                localPointer = ptr ?: Offset.Unspecified
            }
        ),
        shape = RoundedCornerShape(100.dp),
        color = bg,
        border = BorderStroke(1.1.dp, borderBrush)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPad, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
        }
    }
}

@Composable
fun ElasticIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var pointerHovered by remember { mutableStateOf(false) }
    var localPointer by remember { mutableStateOf(Offset.Unspecified) }
    val activeHover = hovered || pointerHovered

    val bg by animateColorAsState(
        targetValue = if (activeHover) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
        },
        animationSpec = tween(180)
    )

    val borderBrush = if (activeHover && localPointer != Offset.Unspecified) {
        Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.8f), MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), Color.White.copy(alpha = 0.15f)),
            center = localPointer,
            radius = 80f
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = if (activeHover) 0.34f else 0.12f), Color.White.copy(alpha = 0.08f))
        )
    }

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .size(size)
            .liquidJelly(
                maxMagneticPullPx = 3f,
                maxDragPx = 8f,
                hoverScale = 1.06f,
                pressScale = 0.92f,
                onPointerUpdate = { ptr, hov ->
                    pointerHovered = hov
                    localPointer = ptr ?: Offset.Unspecified
                }
            ),
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.1.dp, borderBrush)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (activeHover) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PillBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    icon: ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = containerColor.copy(alpha = 0.85f),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PulsingStatusDot(
    color: Color,
    active: Boolean,
    size: Dp = 9.dp
) {
    val infiniteTransition = rememberInfiniteTransition()
    val breath by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (active) 1150 else 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.size(size * 2.4f)) {
        val center = Offset(this.size.width * 0.5f, this.size.height * 0.5f)
        val coreRadius = size.toPx() * 0.5f
        val haloRadius = coreRadius * (1.2f + breath * (if (active) 0.95f else 0.55f))
        val haloAlpha = (if (active) 0.34f else 0.22f) * (1f - breath * 0.55f)

        drawCircle(
            color = color.copy(alpha = haloAlpha),
            radius = haloRadius,
            center = center
        )
        drawCircle(
            color = color,
            radius = coreRadius,
            center = center
        )
    }
}
