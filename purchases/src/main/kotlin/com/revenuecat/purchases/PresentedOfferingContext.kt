package com.revenuecat.purchases

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Contains data about the context in which an offering was presented.
 */
@Parcelize
data class PresentedOfferingContext @JvmOverloads constructor(
    /**
     * The identifier of the offering used to obtain this object.
     */
    val offeringIdentifier: String,
    /**
     * The identifier of the placement used to obtain this object.
     */
    val placementIdentifier: String?,
    /**
     * The targeting context used to obtain this object.
     */
    val targetingContext: TargetingContext?,
) : Parcelable {
    constructor(offeringIdentifier: String) : this(offeringIdentifier, null, null)

    @Parcelize
    data class TargetingContext(
        /**
         * The revision of the targeting used to obtain this object.
         */
        val revision: Int,

        /**
         * The rule id from the targeting used to obtain this object.
         */
        val ruleId: String,
    ) : Parcelable
}
