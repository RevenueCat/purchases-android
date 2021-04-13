package com.revenuecat.purchasetester

import com.revenuecat.purchases.EntitlementInfo

fun EntitlementInfo.toBriefString(): String {
    return "$productIdentifier -> expires $expirationDate"
}
