package com.revenuecat.purchasetester.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ColorPrimary,
    primaryContainer = ColorPrimaryDark,
    secondary = ColorAccent,
    tertiary = Red,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = OnBackgroundLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = LightGrey,
    onSurfaceVariant = OnBackgroundLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = ColorPrimary,
    primaryContainer = ColorPrimaryDark,
    secondary = ColorAccent,
    tertiary = Red,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = OnBackgroundDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = OnBackgroundDark,
)

@Composable
fun PurchaseTesterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
