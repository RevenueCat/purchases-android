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
     */
    val offeringIdentifier: String,

    /**
     * The identifier of the placement used to obtain this object.
     */
    val placementIdentifier: String? = null,
) : Parcelable
