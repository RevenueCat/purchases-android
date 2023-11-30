package com.revenuecat.purchases.ui.revenuecatui.activity

import android.os.Parcelable
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallFontFamily
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType
import kotlinx.parcelize.Parcelize

internal const val DEFAULT_DISPLAY_DISMISS_BUTTON = true

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Parcelize
internal data class PaywallActivityArgs(
    val requiredEntitlementIdentifier: String? = null,
    val offeringId: String? = null,
    val fonts: Map<TypographyType, PaywallFontFamily?>? = null,
    val shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
) : Parcelable {
    constructor(
        requiredEntitlementIdentifier: String? = null,
        offeringId: String? = null,
        fontProvider: ParcelizableFontProvider?,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
    ) : this(
        requiredEntitlementIdentifier,
        offeringId,
        fontProvider?.let { TypographyType.values().associateBy({ it }, { fontProvider.getFont(it) }) },
        shouldDisplayDismissButton,
    )
}
