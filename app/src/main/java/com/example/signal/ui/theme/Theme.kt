package com.example.signal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Dark colour scheme ─────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = Indigo400,
    onPrimary            = Color.White,
    primaryContainer     = Indigo950,
    onPrimaryContainer   = Indigo200,

    secondary            = Color(0xFF94A3B8),
    onSecondary          = Dark900,
    secondaryContainer   = Dark700,
    onSecondaryContainer = SlateMedD,

    background           = Dark900,
    onBackground         = SlateDark,

    surface              = Dark800,
    onSurface            = SlateDark,
    surfaceVariant       = Dark750,
    onSurfaceVariant     = SlateMedD,
    surfaceTint          = Indigo500,

    error                = Rose500,
    onError              = Color.White,
    errorContainer       = Color(0xFF7F1D1D),
    onErrorContainer     = Color(0xFFFECACA),

    outline              = Dark600,
    outlineVariant       = Dark700,

    inverseSurface       = SlateDark,
    inverseOnSurface     = Dark900,
    inversePrimary       = Indigo600,

    scrim                = Color(0xFF000000),
)

// ── Light colour scheme ────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = Indigo600,
    onPrimary            = Color.White,
    primaryContainer     = Indigo100,
    onPrimaryContainer   = Indigo700,

    secondary            = Color(0xFF64748B),
    onSecondary          = Color.White,
    secondaryContainer   = Light300,
    onSecondaryContainer = SlateMedL,

    background           = Light50,
    onBackground         = SlateLight,

    surface              = Light100,
    onSurface            = SlateLight,
    surfaceVariant       = Light200,
    onSurfaceVariant     = SlateSubL,
    surfaceTint          = Indigo600,

    error                = Rose600,
    onError              = Color.White,
    errorContainer       = Color(0xFFFEE2E2),
    onErrorContainer     = Rose600,

    outline              = Light400,
    outlineVariant       = Light300,

    inverseSurface       = Color(0xFF1F2937),
    inverseOnSurface     = Light50,
    inversePrimary       = Indigo400,

    scrim                = Color(0xFF000000),
)

@Composable
fun SignalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
