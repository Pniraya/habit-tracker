package com.habittracker.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// V1 is light-only by product decision: no dark scheme and no theme switch entry.
private val LightColors = lightColorScheme(
    primary = MossGreen,
    onPrimary = CardSurface,
    secondary = Apricot,
    tertiary = MistBlue,
    background = WarmWhite,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceTint,
    onSurfaceVariant = TextSecondary,
    outline = BorderSoft,
)

@Composable
fun HabitTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = HabitTypography,
        content = content,
    )
}
