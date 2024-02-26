package com.revenuecat.purchases

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Contains data about the context in which an offering was presented.
 */
@Parcelize
data class PresentedOfferingContext internal constructor(
    /**
     * The identifier of the offering used to obtain this object.
     */
    val offeringIdentifier: String,
    /**
     * The identifier of the placement used to obtain this object.
     */
    internal val placementIdentifier: String? = null,
    /**
     * The targeting context used to obtain this object.
     */
    internal val targetingContext: TargetingContext? = null,
) : Parcelable {
    constructor(offeringIdentifier: String) : this(offeringIdentifier, null, null)

    @Parcelize
    internal data class TargetingContext(
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
