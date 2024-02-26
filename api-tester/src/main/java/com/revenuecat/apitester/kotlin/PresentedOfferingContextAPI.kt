package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.PresentedOfferingContext

@Suppress("unused", "UNUSED_VARIABLE")
private class PresentedOfferingContextAPI {
    fun check(presentedOfferingContext: PresentedOfferingContext) {
        val offeringIdentifier: String = presentedOfferingContext.offeringIdentifier
        val placementIdentifier: String? = presentedOfferingContext.placementIdentifier
        val targetingContext: PresentedOfferingContext.TargetingContext? = presentedOfferingContext.targetingContext
    }

    fun checkConstructor(
        offeringId: String,
        placementId: String?,
        targetingContext: PresentedOfferingContext.TargetingContext,
    ) {
        val poc1 = PresentedOfferingContext(offeringId)
        val poc2 = PresentedOfferingContext(offeringId, placementId)
        val poc3 = PresentedOfferingContext(offeringId, placementId, null)
        val poc4 = PresentedOfferingContext(offeringId, null, targetingContext)
        val poc5 = PresentedOfferingContext(offeringId, placementId, targetingContext)
    }
}
