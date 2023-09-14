package com.revenuecat.purchases.ui.revenuecatui.data.processed

import androidx.compose.ui.graphics.Color
import com.revenuecat.purchases.paywalls.PaywallData

internal object ColorsFactory {
    @Suppress("CyclomaticComplexMethod")
    fun create(
        paywallDataColors: PaywallData.Configuration.Colors,
    ): TemplateConfiguration.Colors {
        val backgroundColorInt = paywallDataColors.background.colorInt
        val text1ColorInt = paywallDataColors.text1.colorInt
        val text2ColorInt = paywallDataColors.text2?.colorInt ?: text1ColorInt
        val callToActionBackgroundColorInt = paywallDataColors.callToActionBackground.colorInt
        val callToActionForegroundColorInt = paywallDataColors.callToActionForeground.colorInt
        val callToActionSecondaryBackgroundColorInt = paywallDataColors.callToActionSecondaryBackground?.colorInt
            ?: callToActionBackgroundColorInt
        val accent1ColorInt = paywallDataColors.accent1?.colorInt ?: callToActionForegroundColorInt
        val accent2ColorInt = paywallDataColors.accent2?.colorInt ?: accent1ColorInt
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
