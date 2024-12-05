@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls

internal val ThemeImageUrls.urlsForCurrentTheme: ImageUrls
    @ReadOnlyComposable @Composable
    get() = if (isSystemInDarkTheme()) dark ?: light else light
