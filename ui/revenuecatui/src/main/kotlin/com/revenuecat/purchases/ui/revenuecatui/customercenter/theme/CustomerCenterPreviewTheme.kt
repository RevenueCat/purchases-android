package com.revenuecat.purchases.ui.revenuecatui.customercenter.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@SuppressWarnings("MagicNumber")
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F67FF),
    onPrimary = Color.White,
    surface = Color(0xFFF0F4F9),
    onSurface = Color(0xFF1F1F1F),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1F1F1F),
)

@SuppressWarnings("MagicNumber")
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color.Black,
    surface = Color(0xFF2A2A2A),
    onSurface = Color(0xFFE0E0E0),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
)

@Composable
internal fun CustomerCenterPreviewTheme(
    content: @Composable () -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
