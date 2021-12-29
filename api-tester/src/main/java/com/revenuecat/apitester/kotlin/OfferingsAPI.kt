package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings

@Suppress("unused", "UNUSED_VARIABLE")
private class OfferingsAPI {
    fun check(offerings: Offerings) {
        val current: Offering? = offerings.current
        val all: Map<String, Offering> = offerings.all
        val o1: Offering? = offerings.getOffering("")
        val o2: Offering? = offerings[""]
    }
}
