package com.revenuecat.purchases.ui.revenuecatui.activity

import android.os.Build
import android.os.Parcelable
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.OfferingSelection
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallFontFamily
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType
import kotlinx.parcelize.Parcelize

internal const val DEFAULT_DISPLAY_DISMISS_BUTTON = true
internal val defaultEdgeToEdge = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

@Parcelize
internal data class PaywallActivityArgs(
    val requiredEntitlementIdentifier: String? = null,
    val offeringIdAndPresentedOfferingContext: OfferingSelection.IdAndPresentedOfferingContext? = null,
    val fonts: Map<TypographyType, PaywallFontFamily?>? = null,
    val shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
    val edgeToEdge: Boolean = defaultEdgeToEdge,
    val wasLaunchedThroughSDK: Boolean = true,
    val customVariables: Map<String, CustomVariableValue> = emptyMap(),
) : Parcelable {
    constructor(
        requiredEntitlementIdentifier: String? = null,
        offeringIdAndPresentedOfferingContext: OfferingSelection.IdAndPresentedOfferingContext? = null,
        fontProvider: ParcelizableFontProvider?,
        shouldDisplayDismissButton: Boolean = DEFAULT_DISPLAY_DISMISS_BUTTON,
        edgeToEdge: Boolean = defaultEdgeToEdge,
        wasLaunchedThroughSDK: Boolean = true,
        customVariables: Map<String, CustomVariableValue> = emptyMap(),
    ) : this(
        requiredEntitlementIdentifier,
        offeringIdAndPresentedOfferingContext,
        fontProvider?.let { TypographyType.values().associateBy({ it }, { fontProvider.getFont(it) }) },
        shouldDisplayDismissButton,
        edgeToEdge,
        wasLaunchedThroughSDK,
        customVariables,
    )
}
