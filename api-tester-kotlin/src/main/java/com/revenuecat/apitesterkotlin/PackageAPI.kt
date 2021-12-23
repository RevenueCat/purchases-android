package com.revenuecat.apitesterkotlin

import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType

@Suppress("unused", "UNUSED_VARIABLE")
private class PackageAPI {
    fun check(p: Package) {
        val identifier: String = p.identifier
        val packageType: PackageType = p.packageType
        val product: SkuDetails = p.product
        val offering: String = p.offering
    }

    fun check(type: PackageType) {
        when (type) {
            PackageType.UNKNOWN,
            PackageType.CUSTOM,
            PackageType.LIFETIME,
            PackageType.ANNUAL,
            PackageType.SIX_MONTH,
            PackageType.THREE_MONTH,
            PackageType.TWO_MONTH,
            PackageType.MONTHLY,
            PackageType.WEEKLY
            -> {}
        }
    }
}
