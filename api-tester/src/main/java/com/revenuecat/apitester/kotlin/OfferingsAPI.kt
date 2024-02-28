package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings

@Suppress("unused", "UNUSED_VARIABLE")
private class OfferingsAPI {
    fun check(offerings: Offerings) {
        with(offerings) {
            val current: Offering? = current
            val all: Map<String, Offering> = all
            val o1: Offering? = getOffering("")
            val o2: Offering? = this[""]
            val o3: Offering? = getCurrentOfferingForPlacement("")
        }
    }
}
