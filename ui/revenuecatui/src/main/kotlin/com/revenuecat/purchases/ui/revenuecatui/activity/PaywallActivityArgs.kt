package com.revenuecat.purchases.ui.revenuecatui.activity

import android.os.Parcelable
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallFontFamily
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
@Parcelize
internal data class PaywallActivityArgs(
    val offeringId: String? = null,
    val fonts: Map<TypographyType, PaywallFontFamily?>? = null,
) : Parcelable {
    constructor(offeringId: String? = null, fontProvider: ParcelizableFontProvider?) : this(
        offeringId,
        fontProvider?.let { TypographyType.values().associateBy({ it }, { fontProvider.getFont(it) }) },
    )
}
