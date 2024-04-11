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
        val text3ColorInt = paywallDataColors.text3?.colorInt ?: text2ColorInt
        val callToActionBackgroundColorInt = paywallDataColors.callToActionBackground.colorInt
        val callToActionForegroundColorInt = paywallDataColors.callToActionForeground.colorInt
        val callToActionSecondaryBackgroundColorInt = paywallDataColors.callToActionSecondaryBackground?.colorInt
        val accent1ColorInt = paywallDataColors.accent1?.colorInt ?: callToActionForegroundColorInt
        val accent2ColorInt = paywallDataColors.accent2?.colorInt ?: accent1ColorInt
        val accent3ColorInt = paywallDataColors.accent3?.colorInt ?: accent2ColorInt
        val closeButtonInt = paywallDataColors.closeButton?.colorInt
        return TemplateConfiguration.Colors(
            background = Color(backgroundColorInt),
            text1 = Color(text1ColorInt),
            text2 = Color(text2ColorInt),
            text3 = Color(text3ColorInt),
            callToActionBackground = Color(callToActionBackgroundColorInt),
            callToActionForeground = Color(callToActionForegroundColorInt),
            callToActionSecondaryBackground = callToActionSecondaryBackgroundColorInt?.let(::Color),
            accent1 = Color(accent1ColorInt),
            accent2 = Color(accent2ColorInt),
            accent3 = Color(accent3ColorInt),
            closeButton = closeButtonInt?.let { Color(it) },
        )
    }
}
