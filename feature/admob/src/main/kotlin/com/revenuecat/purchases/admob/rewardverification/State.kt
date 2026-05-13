package com.revenuecat.purchases.admob.rewardverification

import java.util.concurrent.atomic.AtomicBoolean

internal class State(
    val clientTransactionId: String,
) {
    private val completionDelivered = AtomicBoolean(false)

    fun consumeCompletionDeliveredToken(): Boolean {
        return completionDelivered.compareAndSet(false, true)
    }
}
