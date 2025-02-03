@file:JvmSynthetic
@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import androidx.compose.ui.graphics.Color
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.RCColor

@JvmSynthetic internal fun CustomerCenterConfigData.Appearance.getColorForTheme(
    isDark: Boolean,
    selector: (CustomerCenterConfigData.Appearance.ColorInformation) -> RCColor?,
): Color? {
    return (if (isDark) dark else light)
        ?.let(selector)
        ?.colorInt
        ?.let { Color(it) }
}
