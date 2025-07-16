package com.revenuecat.purchases

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Contains data about the context in which an offering was presented.
 */
@Parcelize
@Poko
public class PresentedOfferingContext @JvmOverloads constructor(
    /**
     * The identifier of the offering used to obtain this object.
     */
    public val offeringIdentifier: String,
    /**
     * The identifier of the placement used to obtain this object.
     */
    public val placementIdentifier: String?,
    /**
     * The targeting context used to obtain this object.
     */
    public val targetingContext: TargetingContext?,
) : Parcelable {
    public constructor(offeringIdentifier: String) : this(offeringIdentifier, null, null)

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
    public class TargetingContext(
        /**
         * The revision of the targeting used to obtain this object.
         */
        public val revision: Int,

        /**
         * The rule id from the targeting used to obtain this object.
         */
        public val ruleId: String,
    ) : Parcelable
}
