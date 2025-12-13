package com.revenuecat.purchases

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contains data about the context in which an offering was presented.
 */
@Parcelize
@Poko
@Serializable
class PresentedOfferingContext @JvmOverloads constructor(
    /**
     * The identifier of the offering used to obtain this object.
     */
    @SerialName("offering_identifier")
    val offeringIdentifier: String,
    /**
     * The identifier of the placement used to obtain this object.
     */
    @SerialName("placement_identifier")
    val placementIdentifier: String?,
    /**
     * The targeting context used to obtain this object.
     */
    @SerialName("targeting_context")
    val targetingContext: TargetingContext?,
) : Parcelable {
    constructor(offeringIdentifier: String) : this(offeringIdentifier, null, null)

    @JvmSynthetic
    internal fun copy(
        offeringIdentifier: String = this.offeringIdentifier,
        placementIdentifier: String? = this.placementIdentifier,
        targetingContext: TargetingContext? = this.targetingContext,
    ): PresentedOfferingContext = PresentedOfferingContext(
        offeringIdentifier = offeringIdentifier,
        placementIdentifier = placementIdentifier,
        targetingContext = targetingContext,
    )

    @Parcelize
    @Poko
    @Serializable
    class TargetingContext(
        /**
         * The revision of the targeting used to obtain this object.
         */
        val revision: Int,

        /**
         * The rule id from the targeting used to obtain this object.
         */
        @SerialName("rule_id")
        val ruleId: String,
    ) : Parcelable
}
