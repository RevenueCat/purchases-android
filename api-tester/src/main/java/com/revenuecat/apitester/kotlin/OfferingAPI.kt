package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package

@Suppress("unused", "UNUSED_VARIABLE")
private class OfferingAPI {
    fun check(offering: Offering) {
        with(offering) {
            val identifier: String = identifier
            val serverDescription: String = serverDescription
            val availablePackages: List<Package> = availablePackages
            val lifetime: Package? = lifetime
            val annual: Package? = annual
            val sixMonth: Package? = sixMonth
            val threeMonth: Package? = threeMonth
            val twoMonth: Package? = twoMonth
            val monthly: Package? = monthly
            val weekly: Package? = weekly
            val p1: Package = offering[""]
            val p2: Package = getPackage("")
            val metadata: Map<String, Any> = metadata
            val metadataString: String = getMetadataString("key", "default")
        }
    }
}
