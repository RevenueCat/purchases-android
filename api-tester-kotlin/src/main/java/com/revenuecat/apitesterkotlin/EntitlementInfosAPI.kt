package com.revenuecat.apitesterkotlin

import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.EntitlementInfos

@Suppress("unused")
private class EntitlementInfosAPI {
    fun check(infos: EntitlementInfos) {
        val active: Map<String, EntitlementInfo> = infos.active
        val all: Map<String, EntitlementInfo> = infos.all
        val i: EntitlementInfo? = infos[""]
    }
}
