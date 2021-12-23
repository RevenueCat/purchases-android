package com.revenuecat.apitesterkotlin

import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType

@Suppress("unused", "UNUSED_VARIABLE")
private class OfferingAPI {
    fun check(offering: Offering) {
        val identifier: String = offering.identifier
        val serverDescription: String = offering.serverDescription
        val availablePackages: List<Package> = offering.availablePackages
        val lifetime: Package? = offering.lifetime
        val annual: Package? = offering.annual
        val sixMonth: Package? = offering.sixMonth
        val threeMonth: Package? = offering.threeMonth
        val twoMonth: Package? = offering.twoMonth
        val monthly: Package? = offering.monthly
        val weekly: Package? = offering.weekly
        val (_: String, _: PackageType, _: SkuDetails, _: String) = offering[""]
        val (_: String, _: PackageType, _: SkuDetails, _: String) = offering.getPackage("")
    }
}
