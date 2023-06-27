package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.VerificationResult

@Suppress("unused", "UNUSED_VARIABLE", "DEPRECATION")
private class EntitlementInfosAPI {
    fun check(infos: EntitlementInfos) {
        val active: Map<String, EntitlementInfo> = infos.active
        val all: Map<String, EntitlementInfo> = infos.all
        val i: EntitlementInfo? = infos[""]
        val verification: VerificationResult = infos.verification
    }

    fun checkConstructor(
        all: Map<String, EntitlementInfo>,
        verificationResult: VerificationResult,
    ) {
        val entitlementInfos = EntitlementInfos(
            all = all,
            verification = verificationResult,
        )
        val entitlementInfos2 = EntitlementInfos(all = all)
    }
}
