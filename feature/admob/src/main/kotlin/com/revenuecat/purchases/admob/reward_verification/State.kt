package com.revenuecat.purchases.admob.reward_verification

import java.util.WeakHashMap

internal data class State(
    val clientTransactionId: String,
)

internal class StateStore {
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
