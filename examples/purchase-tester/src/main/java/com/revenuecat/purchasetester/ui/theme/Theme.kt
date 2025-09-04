package com.revenuecat.purchasetester.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val LightColorScheme = lightColorScheme(
    primary = vibrant_red_primary,
    secondary = coral_pink_secondary,
    tertiary = soft_pink_tertiary,
    background = pure_white_background,
    surface = light_gray_surface,
    onBackground = dark_text,
    onSurface = dark_text,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = dark_text,
    secondaryContainer = bright_pink_primary,
    onSecondaryContainer = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = bright_pink_primary,
    secondary = muted_pink_secondary,
    tertiary = light_pink_tertiary,
    background = dark_background,
    surface = dark_surface,
    onBackground = white_text,
    onSurface = white_text,
    onPrimary = dark_text,
    onSecondary = dark_text,
    onTertiary = dark_text,
    secondaryContainer = bright_pink_primary,
    onSecondaryContainer = Color.White,
)

@Composable
fun PurchaseTesterAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val win = (view.context as Activity).window
            win.statusBarColor = if (darkTheme)  Color.Black.toArgb() else vibrant_red_primary.toArgb()
        }
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
