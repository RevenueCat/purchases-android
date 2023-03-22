package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.EntitlementInfos

@Suppress("unused", "UNUSED_VARIABLE", "DEPRECATION")
private class EntitlementInfosAPI {
    fun check(infos: EntitlementInfos) {
        val active: Map<String, EntitlementInfo> = infos.active
        val all: Map<String, EntitlementInfo> = infos.all
        val i: EntitlementInfo? = infos[""]
        // Trusted entitlements: Commented out until ready to be made public
        // val verification: VerificationResult = infos.verification
    }

    fun checkConstructor(
        all: Map<String, EntitlementInfo>,
        // Trusted entitlements: Commented out until ready to be made public
        // verificationResult: VerificationResult
    ) {
        val entitlementInfos = EntitlementInfos(
            all = all,
            // Trusted entitlements: Commented out until ready to be made public
            // verification = verificationResult
        )
        val entitlementInfos2 = EntitlementInfos(all = all)
    }
}
