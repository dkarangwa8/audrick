package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BralirwaCrimson,
    onPrimary = Color.White,
    primaryContainer = BralirwaCrimsonLight,
    onPrimaryContainer = BralirwaOnCrimsonLight,
    secondary = LagerGold,
    onSecondary = Color.White,
    secondaryContainer = LagerGoldSoft,
    onSecondaryContainer = Color(0xFF3E2600),
    tertiary = EmeraldFresh,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldSoft,
    onTertiaryContainer = Color(0xFF002110),
    background = AlabasterBg,
    onBackground = InkCharcoal,
    surface = WarmSurface,
    onSurface = InkCharcoal,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = InkMuted,
    surfaceContainerLow = WarmSurface,
    surfaceContainer = WarmSurfaceVariant,
    surfaceContainerHigh = WarmSurfaceHigh,
    outline = WarmOutline,
    error = CrimsonCritical,
    onError = Color.White,
    errorContainer = CrimsonCriticalSoft,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6B5B),
    onPrimary = Color(0xFF450A0A),
    primaryContainer = BralirwaCrimsonDark,
    onPrimaryContainer = BralirwaCrimsonLight,
    secondary = LagerGoldDark,
    onSecondary = Color(0xFF2B1A00),
    secondaryContainer = Color(0xFF4F3404),
    onSecondaryContainer = LagerGoldSoft,
    tertiary = Color(0xFF4ADE80),
    onTertiary = Color(0xFF00391B),
    tertiaryContainer = Color(0xFF0B4628),
    onTertiaryContainer = EmeraldSoft,
    background = ObsidianBg,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = IvoryMuted,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = CrimsonCriticalSoft
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BralirwaPosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
