package com.revenuecat.purchases.admob

import android.util.Log
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import java.util.UUID
import java.util.WeakHashMap

private const val TAG = "PurchasesAdMob"

private object RewardVerificationStateStore {
    private val clientTransactionIdByAd: MutableMap<Any, String> = WeakHashMap()

    @Synchronized
    fun setClientTransactionId(ad: Any, clientTransactionId: String) {
        clientTransactionIdByAd[ad] = clientTransactionId
    }

    @Synchronized
    fun getClientTransactionId(ad: Any): String? {
        return clientTransactionIdByAd[ad]
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun enableRewardVerificationInternal(ad: Any) {
    if (!Purchases.isConfigured) {
        Log.w(
            TAG,
            "Purchases is not configured. Call Purchases.configure() before enabling reward verification.",
        )
        return
    }

    RewardVerificationStateStore.setClientTransactionId(ad, UUID.randomUUID().toString())
}

internal fun rewardVerificationClientTransactionId(ad: Any): String? {
    return RewardVerificationStateStore.getClientTransactionId(ad)
}

internal fun warnAndAssertIfMissingVerificationState(clientTransactionId: String?) {
    if (clientTransactionId != null) return

    Log.w(
        TAG,
        "Reward verification callback requires enableRewardVerification() before show().",
    )
    assert(clientTransactionId != null) {
        "Call enableRewardVerification() before using reward verification show overloads."
    }
}
