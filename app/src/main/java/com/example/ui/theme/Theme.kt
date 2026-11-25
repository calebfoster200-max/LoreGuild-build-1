package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LoreGuildDarkColorScheme = darkColorScheme(
    primary = BurnishedGold,
    onPrimary = Color(0xFF1F1606),
    primaryContainer = DeepBronze,
    onPrimaryContainer = AmberGlow,
    secondary = ArcaneTeal,
    onSecondary = Color(0xFF07211E),
    secondaryContainer = ArcaneDeep,
    onSecondaryContainer = Color(0xFFB7F2E8),
    tertiary = CrimsonRose,
    onTertiary = Color(0xFF2B0B10),
    tertiaryContainer = CrimsonDeep,
    onTertiaryContainer = Color(0xFFFFD9DC),
    background = ObsidianNight,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = ParchmentMuted,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianElevated,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF2C3242),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF370606)
)

private val LoreGuildLightColorScheme = lightColorScheme(
    primary = BronzeInk,
    onPrimary = Color.White,
    primaryContainer = BronzeContainer,
    onPrimaryContainer = Color(0xFF2D1900),
    secondary = DeepTealInk,
    onSecondary = Color.White,
    secondaryContainer = DeepTealContainer,
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = BerryInk,
    onTertiary = Color.White,
    background = ParchmentPaper,
    onBackground = InkBlack,
    surface = ParchmentPaper,
    onSurface = InkBlack,
    surfaceVariant = ParchmentCard,
    onSurfaceVariant = InkMuted,
    surfaceContainerLow = ParchmentCard,
    surfaceContainer = ParchmentCard,
    surfaceContainerHigh = ParchmentElevated,
    outline = InkOutline,
    outlineVariant = Color(0xFFE0D7CA)
)

val LoreGuildShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LoreGuildTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LoreGuildDarkColorScheme else LoreGuildLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LoreGuildTypography,
        shapes = LoreGuildShapes,
        content = content
    )
}
