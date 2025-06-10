package com.revenuecat.purchases.ui.revenuecatui.customercenter.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F67FF),
    onPrimary = Color.White,
    surface = Color(0xFFF0F4F9),
    onSurface = Color(0xFF1F1F1F),
    background = Color.Red,
    onBackground = Color(0xFF1F1F1F),
)

@Composable
internal fun CustomerCenterPreviewTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = LightColorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content,
    )
}
