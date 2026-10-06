package com.revenuecat.purchases.ui.revenuecatui.fonts

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.revenuecat.purchases.ui.revenuecatui.extensions.copyWithFontProvider

@Composable
internal fun PaywallTheme(
    fontProvider: FontProvider?,
    isDarkModeOverride: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val themedConfiguration = remember(configuration, isDarkModeOverride) {
        if (isDarkModeOverride == null) {
            configuration
        } else {
            Configuration(configuration).apply {
                val nightMode = if (isDarkModeOverride) {
                    Configuration.UI_MODE_NIGHT_YES
                } else {
                    Configuration.UI_MODE_NIGHT_NO
                }
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            }
        }
    }
    CompositionLocalProvider(LocalConfiguration provides themedConfiguration) {
        PaywallFontTheme(fontProvider, content)
    }
}

@Composable
private fun PaywallFontTheme(
    fontProvider: FontProvider?,
    content: @Composable () -> Unit,
) {
    if (fontProvider == null) {
        content()
    } else {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            typography = MaterialTheme.typography.copyWithFontProvider(fontProvider),
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}
