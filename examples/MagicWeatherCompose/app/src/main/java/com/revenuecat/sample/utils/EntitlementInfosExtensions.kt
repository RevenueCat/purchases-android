package com.revenuecat.sample.utils

import com.revenuecat.purchases.EntitlementInfos

fun EntitlementInfos.hasActiveEntitlements(): Boolean {
    return this.active.isNotEmpty()
}

fun EntitlementInfos.getActiveEntitlements(): Set<String> {
    return this.active.keys
}
