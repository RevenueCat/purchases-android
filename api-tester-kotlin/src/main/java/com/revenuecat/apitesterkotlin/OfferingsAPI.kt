package com.revenuecat.apitesterkotlin

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings

@Suppress("unused")
private class OfferingsAPI {
    fun check() {
        val offerings: Offerings? = null

        val current: Offering? = offerings!!.current
        val all: Map<String, Offering> = offerings.all
        val o1 = offerings.getOffering("")
        val o2: Offering? = offerings[""]
    }
}
