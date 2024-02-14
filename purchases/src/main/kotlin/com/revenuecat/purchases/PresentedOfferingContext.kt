package com.revenuecat.purchases

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Contains data about the context in which an offering was presented.
 */
@Parcelize
data class PresentedOfferingContext(
    /**
     * The identifier of the offering used to obtain this object.
     *
     * Null if not using RevenueCat offerings system, if fetched directly via `Purchases.getProducts`,
     * or on restores/syncs.
     */
    val offeringIdentifier: String? = null,
) : Parcelable
