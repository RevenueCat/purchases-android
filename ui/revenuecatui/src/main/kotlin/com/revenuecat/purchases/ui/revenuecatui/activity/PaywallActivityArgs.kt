package com.revenuecat.purchases.ui.revenuecatui.activity

import android.os.Build
import android.os.Parcelable
import com.revenuecat.purchases.ui.revenuecatui.OfferingPresentationInfo
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallFontFamily
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType
import kotlinx.parcelize.Parcelize

internal const val DEFAULT_DISPLAY_DISMISS_BUTTON = true
internal val defaultEdgeToEdge = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

@Parcelize
internal data class PaywallActivityArgs(
    val requiredEntitlementIdentifier: String? = null,
    val offeringInfo: OfferingPresentationInfo? = null,
    val fonts: Map<TypographyType, PaywallFontFamily?>? = null,
    val shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
    val edgeToEdge: Boolean = defaultEdgeToEdge,
) : Parcelable {
    constructor(
        requiredEntitlementIdentifier: String? = null,
        offeringInfo: OfferingPresentationInfo? = null,
        fontProvider: ParcelizableFontProvider?,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        edgeToEdge: Boolean = defaultEdgeToEdge,
    ) : this(
        requiredEntitlementIdentifier,
        offeringInfo,
        fontProvider?.let { TypographyType.values().associateBy({ it }, { fontProvider.getFont(it) }) },
        shouldDisplayDismissButton,
        edgeToEdge,
    )
}
