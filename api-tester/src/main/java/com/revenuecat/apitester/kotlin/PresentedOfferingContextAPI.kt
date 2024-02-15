package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.PresentedOfferingContext

@Suppress("unused", "UNUSED_VARIABLE")
private class PresentedOfferingContextAPI {
    fun check(presentedOfferingContext: PresentedOfferingContext) {
        val offeringIdentifier: String = presentedOfferingContext.offeringIdentifier
    }

    fun checkConstructor(offeringId: String) {
        val presentedOfferingContext = PresentedOfferingContext(offeringId)
    }
}
