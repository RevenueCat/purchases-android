package com.revenuecat.purchases.ui.revenuecatui.activity

import android.os.Parcelable
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PaywallActivityArgs(
    val offeringId: String? = null,
    val fonts: Map<TypographyType, Int?>? = null,
) : Parcelable {
    constructor(offeringId: String? = null, provider: FontResourceProvider?) : this(
        offeringId,
        provider?.let { TypographyType.values().associateBy({ it }, { provider.getFontResourceId(it) }) },
    )
}
