package com.winesnap.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette mirrors the web app (Tailwind red-800 / stone-*).
val Wine = Color(0xFF991B1B)
val WineDark = Color(0xFF7F1D1D)
val WineLight = Color(0xFFFEF2F2)
val Stone50 = Color(0xFFFAFAF9)
val Stone200 = Color(0xFFE7E5E4)
val Stone400 = Color(0xFFA8A29E)
val Stone500 = Color(0xFF78716C)
val Stone700 = Color(0xFF44403C)
val Stone900 = Color(0xFF1C1917)
val Emerald50 = Color(0xFFECFDF5)
val Emerald700 = Color(0xFF047857)

private val colors = lightColorScheme(
    primary = Wine,
    onPrimary = Color.White,
    secondary = Stone500,
    background = Stone50,
    onBackground = Stone900,
    surface = Color.White,
    onSurface = Stone900,
    outline = Stone200,
    error = Wine,
    errorContainer = WineLight,
)

@Composable
fun WineSnapTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
