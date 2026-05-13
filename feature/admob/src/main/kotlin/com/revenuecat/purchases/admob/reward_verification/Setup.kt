package com.revenuecat.purchases.admob.reward_verification

import android.util.Log
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import java.util.UUID

private const val TAG = "PurchasesAdMob"
private val stateStore = StateStore()

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun enableRewardVerificationInternal(ad: Any) {
    if (!Purchases.isConfigured) {
        Log.w(
            TAG,
            "Purchases is not configured. Call Purchases.configure() before enabling reward verification.",
        )
        return
    }

    stateStore.set(
        ad = ad,
        state = State(clientTransactionId = UUID.randomUUID().toString()),
    )
}

internal fun verificationStateForAd(ad: Any): State? {
    return stateStore.get(ad)
}

internal fun warnAndAssertIfMissingState(state: State?) {
    if (state != null) return

    Log.w(
        TAG,
        "Reward verification callback requires enableRewardVerification() before show().",
    )
    assert(state != null) {
        "Call enableRewardVerification() before using reward verification show overloads."
    }
}
