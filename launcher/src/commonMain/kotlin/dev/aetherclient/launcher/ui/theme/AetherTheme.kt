package dev.aetherclient.launcher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import dev.aetherclient.launcher.core.model.PaletteStyleOption

data class SeedColorPreset(
    val name: String,
    val hex: Long
)

val AetherSeedPresets = listOf(
    SeedColorPreset("Aether Iris", 0xFF7C5CFF),
    SeedColorPreset("Ocean Cyan", 0xFF0096C7),
    SeedColorPreset("Emerald Mint", 0xFF10B981),
    SeedColorPreset("Crimson Pulse", 0xFFE11D48),
    SeedColorPreset("Solar Amber", 0xFFF59E0B),
    SeedColorPreset("Sakura Pink", 0xFFEC4899),
    SeedColorPreset("Arctic Slate", 0xFF475569)
)

private val AetherShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val AetherTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun AetherTheme(
    seedColorHex: Long,
    darkTheme: Boolean,
    paletteStyle: PaletteStyleOption,
    content: @Composable () -> Unit
) {
    val mappedStyle = when (paletteStyle) {
        PaletteStyleOption.TONAL_SPOT -> PaletteStyle.TonalSpot
        PaletteStyleOption.VIBRANT -> PaletteStyle.Vibrant
        PaletteStyleOption.EXPRESSIVE -> PaletteStyle.Expressive
        PaletteStyleOption.NEUTRAL -> PaletteStyle.Neutral
    }

    DynamicMaterialTheme(
        seedColor = Color(seedColorHex),
        isDark = darkTheme,
        style = mappedStyle,
        animate = true,
        shapes = AetherShapes,
        typography = AetherTypography,
        content = content
    )
}
