package com.revenuecat.purchases.ui.revenuecatui

import android.os.Parcelable
import com.revenuecat.purchases.PresentedOfferingContext
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class OfferingPresentationInfo(
    val offeringId: String,
    // Keeping nullable for backwards compatibility.
    val presentedOfferingContext: PresentedOfferingContext? = null,
) : Parcelable
