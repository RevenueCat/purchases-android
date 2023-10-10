package com.revenuecat.purchases.ui.revenuecatui.activity

import android.os.Parcelable
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PaywallActivityArgs(
    val offeringId: String? = null,
    val mapFonts: Map<TypographyType, Int?>? = null,
) : Parcelable
