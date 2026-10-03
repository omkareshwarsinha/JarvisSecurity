package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyanPrimary,
    onPrimary = Color(0xFF041E28),
    primaryContainer = Color(0xFF004D59),
    onPrimaryContainer = Color(0xFFB5F4FF),
    secondary = JarvisBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0D3268),
    onSecondaryContainer = Color(0xFFD6E4FF),
    tertiary = JarvisTealTertiary,
    onTertiary = Color.Black,
    background = JarvisBackground,
    onBackground = JarvisTextPrimary,
    surface = JarvisSurfaceDark,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceCard,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisBorderSubtle,
    outlineVariant = JarvisBorderGlow,
    error = JarvisCriticalRed,
    onError = Color.White,
    errorContainer = JarvisCriticalRedBg,
    onErrorContainer = JarvisCriticalRed
)

private val JarvisLightColorScheme = lightColorScheme(
    primary = JarvisCyanPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = JarvisBlueSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E40AF),
    tertiary = JarvisTealTertiaryLight,
    onTertiary = Color.White,
    background = JarvisBackgroundLight,
    onBackground = JarvisTextPrimaryLight,
    surface = JarvisSurfaceLight,
    onSurface = JarvisTextPrimaryLight,
    surfaceVariant = JarvisSurfaceCardLight,
    onSurfaceVariant = JarvisTextSecondaryLight,
    outline = JarvisBorderSubtleLight,
    outlineVariant = JarvisBorderGlowLight,
    error = JarvisCriticalRed,
    onError = Color.White,
    errorContainer = JarvisCriticalRedBg,
    onErrorContainer = JarvisCriticalRed
)

@Composable
fun JarvisTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) JarvisDarkColorScheme else JarvisLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    JarvisTheme(darkTheme = darkTheme, content = content)
}
