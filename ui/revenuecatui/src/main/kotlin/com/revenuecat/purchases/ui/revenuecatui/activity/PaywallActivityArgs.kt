package com.revenuecat.purchases.ui.revenuecatui.activity

import android.os.Build
import android.os.Parcelable
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallFontFamily
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType
import kotlinx.parcelize.Parcelize

internal const val DEFAULT_DISPLAY_DISMISS_BUTTON = true
internal val defaultDisplayEdgeToEdge = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

@Parcelize
internal data class PaywallActivityArgs(
    val requiredEntitlementIdentifier: String? = null,
    val offeringId: String? = null,
    val fonts: Map<TypographyType, PaywallFontFamily?>? = null,
    val shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
    val shouldDisplayEdgeToEdge: Boolean = defaultDisplayEdgeToEdge,
) : Parcelable {
    constructor(
        requiredEntitlementIdentifier: String? = null,
        offeringId: String? = null,
        fontProvider: ParcelizableFontProvider?,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        shouldDisplayEdgeToEdge: Boolean = defaultDisplayEdgeToEdge,
    ) : this(
        requiredEntitlementIdentifier,
        offeringId,
        fontProvider?.let { TypographyType.values().associateBy({ it }, { fontProvider.getFont(it) }) },
        shouldDisplayDismissButton,
        shouldDisplayEdgeToEdge,
    )
}
