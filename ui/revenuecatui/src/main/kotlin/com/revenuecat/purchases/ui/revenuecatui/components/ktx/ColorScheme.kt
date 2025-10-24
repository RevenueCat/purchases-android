@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme

internal val ColorScheme.colorsForCurrentTheme: ColorInfo
    @JvmSynthetic @Composable
    get() = if (isSystemInDarkTheme()) dark ?: light else light
