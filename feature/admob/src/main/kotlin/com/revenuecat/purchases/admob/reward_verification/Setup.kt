package com.revenuecat.purchases.admob.reward_verification

import android.util.Log
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import java.util.UUID
import java.util.WeakHashMap

private const val TAG = "PurchasesAdMob"

internal object Setup {

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun install(onAd: Any) {
        if (!Purchases.isConfigured) {
            Log.w(
                TAG,
                "Purchases is not configured. Call Purchases.configure() before enabling reward verification.",
            )
            return
        }

        stateStore.set(
            ad = onAd,
            state = State(clientTransactionId = UUID.randomUUID().toString()),
        )
    }

    fun verificationState(forAd: Any): State? {
        return stateStore.get(forAd)
    }

    fun warnAndAssertIfMissingState(state: State?) {
        if (state != null) return

        Log.w(
            TAG,
            "Reward verification callback requires enableRewardVerification() before show().",
        )
        assert(state != null) {
            "Call enableRewardVerification() before using reward verification show overloads."
        }
    }

    private class StateStore {
        private val stateByAd: MutableMap<Any, State> = WeakHashMap()

        @Synchronized
        fun set(ad: Any, state: State) {
            stateByAd[ad] = state
        }

        @Synchronized
        fun get(ad: Any): State? {
            return stateByAd[ad]
        }
    }

    private val stateStore = StateStore()
}
