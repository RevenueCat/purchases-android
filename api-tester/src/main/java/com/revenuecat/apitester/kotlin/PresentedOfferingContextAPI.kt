package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.PresentedOfferingContext

@Suppress("unused", "UNUSED_VARIABLE")
private class PresentedOfferingContextAPI {
    fun check(presentedOfferingContext: PresentedOfferingContext) {
        val offeringIdentifier: String = presentedOfferingContext.offeringIdentifier
        val placementIdentifier: String? = presentedOfferingContext.placementIdentifier
        val targetingRevision: Int? = presentedOfferingContext.targetingRevision
        val targetingRuleId: String? = presentedOfferingContext.targetingRuleId
    }

    fun checkConstructor(offeringId: String, placementId: String?, targetingRevision: Int?, targetingRuleId: String?) {
        val presentedOfferingContext = PresentedOfferingContext(
            offeringId,
            placementId,
            targetingRevision,
            targetingRuleId,
        )
    }
}
