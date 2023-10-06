package com.revenuecat.purchases.ui.revenuecatui.activity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PaywallActivityArgs(
    val offeringId: String? = null,
): Parcelable
