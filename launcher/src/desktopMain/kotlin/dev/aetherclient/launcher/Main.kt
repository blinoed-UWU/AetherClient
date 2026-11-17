package dev.aetherclient.launcher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.aetherclient.launcher.resources.Res
import dev.aetherclient.launcher.resources.aether_logo
import dev.aetherclient.launcher.ui.components.AetherAvatar
import dev.aetherclient.launcher.ui.components.ElasticIconButton
import dev.aetherclient.launcher.ui.components.LiquidGlassBackground
import dev.aetherclient.launcher.ui.screens.BootstrapScreen
import dev.aetherclient.launcher.ui.screens.InjectorHomeScreen
import dev.aetherclient.launcher.ui.screens.SettingsScreen
import dev.aetherclient.launcher.ui.state.LauncherController
import dev.aetherclient.launcher.ui.state.LoaderScreen
import dev.aetherclient.launcher.ui.theme.AetherTheme
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val controller = remember { LauncherController() }
    val windowState = rememberWindowState(
        size = DpSize(440.dp, 640.dp),
        position = WindowPosition(Alignment.Center)
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Aether",
        icon = painterResource(Res.drawable.aether_logo),
        undecorated = true,
        transparent = true,
        resizable = false
    ) {
        DisposableEffect(controller) {
            onDispose {
                controller.close()
            }
        }

        LaunchedEffect(controller.minimizeRequested) {
            if (controller.minimizeRequested) {
                windowState.isMinimized = true
                controller.minimizeRequested = false
            }
        }

        val settings = controller.settings
        var cursorPos by remember { mutableStateOf(Offset.Unspecified) }

        AetherTheme(
            seedColorHex = settings.seedColorHex,
            darkTheme = settings.darkMode,
            paletteStyle = settings.paletteStyle
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .onPointerEvent(PointerEventType.Move) { event ->
                        cursorPos = event.changes.firstOrNull()?.position ?: Offset.Unspecified
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        cursorPos = Offset.Unspecified
                    },
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.26f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    )
                )
            ) {
                LiquidGlassBackground(cursorPos = cursorPos) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        WindowDraggableArea {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .padding(start = 18.dp, end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AetherAvatar(size = 28.dp, cornerRadius = 9.dp)
                                    Text(
                                        text = "Aether",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ElasticIconButton(
                                        icon = Icons.Rounded.Remove,
                                        contentDescription = "Свернуть",
                                        onClick = { windowState.isMinimized = true },
                                        size = 30.dp
                                    )
                                    ElasticIconButton(
                                        icon = Icons.Rounded.Close,
                                        contentDescription = "Закрыть",
                                        onClick = ::exitApplication,
                                        size = 30.dp
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            AnimatedContent(
                                targetState = controller.currentScreen,
                                transitionSpec = {
                                    if (targetState == LoaderScreen.SETTINGS) {
                                        (slideInHorizontally(tween(260)) { it / 3 } + fadeIn(tween(220))) togetherWith
                                            (slideOutHorizontally(tween(260)) { -it / 4 } + fadeOut(tween(180)))
                                    } else if (initialState == LoaderScreen.SETTINGS && targetState == LoaderScreen.HOME) {
                                        (slideInHorizontally(tween(260)) { -it / 3 } + fadeIn(tween(220))) togetherWith
                                            (slideOutHorizontally(tween(260)) { it / 4 } + fadeOut(tween(180)))
                                    } else {
                                        fadeIn(tween(340)) togetherWith fadeOut(tween(240))
                                    }.using(SizeTransform(clip = false))
                                }
                            ) { screen ->
                                when (screen) {
                                    LoaderScreen.BOOTSTRAP -> BootstrapScreen(controller)
                                    LoaderScreen.HOME -> InjectorHomeScreen(controller)
                                    LoaderScreen.SETTINGS -> SettingsScreen(controller)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
