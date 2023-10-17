package com.revenuecat.purchases.ui.revenuecatui.fonts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.revenuecat.purchases.ui.revenuecatui.extensions.copyWithFontProvider

@Composable
internal fun PaywallTheme(
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
