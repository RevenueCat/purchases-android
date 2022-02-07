package com.revenuecat.purchasetester

import com.revenuecat.purchases.EntitlementInfo

fun EntitlementInfo.toBriefString(): String {
    return "$identifier -> expires $expirationDate"
}
