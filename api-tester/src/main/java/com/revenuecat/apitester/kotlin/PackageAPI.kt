package com.revenuecat.apitester.kotlin

import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType

@Suppress("unused", "UNUSED_VARIABLE")
private class PackageAPI {
    fun check(p: Package) {
        with(p) {
            val identifier: String = identifier
            val packageType: PackageType = packageType
            val product: SkuDetails = product
            val offering: String = offering
        }
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
        }.exhaustive
    }
}
