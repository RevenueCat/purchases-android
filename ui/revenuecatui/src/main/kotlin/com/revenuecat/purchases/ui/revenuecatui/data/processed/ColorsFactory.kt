package com.revenuecat.purchases.ui.revenuecatui.data.processed

import androidx.compose.ui.graphics.Color
import com.revenuecat.purchases.paywalls.PaywallData

internal object ColorsFactory {
    @Suppress("CyclomaticComplexMethod")
    fun create(
        colorInfo: PaywallData.Configuration.ColorInformation,
        isDarkTheme: Boolean,
    ): TemplateConfiguration.Colors {
        val light = colorInfo.light
        val dark = colorInfo.dark ?: colorInfo.light
        val backgroundColorInt = if (isDarkTheme) dark.background.colorInt else light.background.colorInt
        val text1ColorInt = if (isDarkTheme) dark.text1.colorInt else light.text1.colorInt
        val text2ColorInt =
            if (isDarkTheme) {
                dark.text2?.colorInt ?: text1ColorInt
            } else light.text2?.colorInt ?: text1ColorInt
        val callToActionBackgroundColorInt =
            if (isDarkTheme) {
                dark.callToActionBackground.colorInt
            } else light.callToActionBackground.colorInt
        val callToActionForegroundColorInt =
            if (isDarkTheme) {
                dark.callToActionForeground.colorInt
            } else light.callToActionForeground.colorInt
        val callToActionSecondaryBackgroundColorInt =
            if (isDarkTheme) {
                dark.callToActionSecondaryBackground?.colorInt ?: callToActionBackgroundColorInt
            } else light.callToActionSecondaryBackground?.colorInt ?: callToActionBackgroundColorInt
        val accent1ColorInt =
            if (isDarkTheme) {
                dark.accent1?.colorInt ?: callToActionForegroundColorInt
            } else light.accent1?.colorInt ?: callToActionForegroundColorInt
        val accent2ColorInt =
            if (isDarkTheme) {
                dark.accent2?.colorInt ?: accent1ColorInt
            } else light.accent2?.colorInt ?: accent1ColorInt
        return TemplateConfiguration.Colors(
            background = Color(backgroundColorInt),
            text1 = Color(text1ColorInt),
            text2 = Color(text2ColorInt),
            callToActionBackground = Color(callToActionBackgroundColorInt),
            callToActionForeground = Color(callToActionForegroundColorInt),
            callToActionSecondaryBackground = Color(callToActionSecondaryBackgroundColorInt),
            accent1 = Color(accent1ColorInt),
            accent2 = Color(accent2ColorInt),
        )
    }
}
