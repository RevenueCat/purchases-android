package com.revenuecat.sample

import com.revenuecat.purchases.EntitlementInfo

fun EntitlementInfo.toBriefString(): String {
    return "$productIdentifier -> expires $expirationDate"
}
