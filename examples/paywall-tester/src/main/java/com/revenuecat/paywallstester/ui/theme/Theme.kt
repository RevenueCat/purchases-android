package com.revenuecat.paywallstester.ui.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

private val LightColorScheme = lightColorScheme(
    primary = deep_purple_primary,
    secondary = muted_purple_secondary,
    tertiary = dusty_rose_tertiary,
    background = warm_beige_background,
    surface = off_white_surface,
    onBackground = charcoal_text,
    onSurface = charcoal_text,
)

private val DarkColorScheme = darkColorScheme(
    primary = light_purple_primary,
    secondary = muted_purple_dark,
    tertiary = dusty_rose_dark,
    background = rich_dark_background,
    surface = elevated_dark_surface,
    onBackground = pure_white_text,
    onSurface = pure_white_text,
)

@Composable
fun PaywallTesterAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
