package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VioletDarkColorScheme = darkColorScheme(
    primary = NeonOrchid,
    onPrimary = Color.White,
    primaryContainer = DeepIris,
    onPrimaryContainer = LavenderGlow,
    secondary = ElectricAmethyst,
    onSecondary = VoidObsidian,
    secondaryContainer = ElevatedPlum,
    onSecondaryContainer = LavenderGlow,
    tertiary = CyanDownload,
    onTertiary = VoidObsidian,
    background = VoidObsidian,
    onBackground = TextPrimary,
    surface = MidnightViolet,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceViolet,
    onSurfaceVariant = TextSecondary,
    outline = BorderViolet,
    outlineVariant = Color(0xFF2E1E57),
    error = CrimsonAlert,
    onError = VoidObsidian,
)

private val VioletShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun VioletTunnelTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VioletDarkColorScheme,
        typography = VioletTypography,
        shapes = VioletShapes,
        content = content
    )
}
