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
        targetingContext: PresentedOfferingContext.TargetingContext?,
    ) {
        val poc1 = PresentedOfferingContext(offeringId)
        val poc2 = PresentedOfferingContext(offeringId, placementId)
        val poc3 = PresentedOfferingContext(offeringId, placementId, targetingContext)
    }
}

@Suppress("unused", "UNUSED_VARIABLE")
private class TargetingContextAPI {
    fun check(targetingContext: PresentedOfferingContext.TargetingContext) {
        val revision: Int = targetingContext.revision
        val ruleID: String = targetingContext.ruleId
    }

    fun checkConstructor(
        revision: Int,
        ruleId: String,
    ) {
        val targetingContext = PresentedOfferingContext.TargetingContext(revision, ruleId)
    }
}
