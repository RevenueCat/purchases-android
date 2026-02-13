package com.revenuecat.purchases.utils

import kotlin.time.Duration

internal class RateLimiter(val maxCallsInPeriod: Int, val periodSeconds: Duration) {
    private val maxCallInclusive = maxCallsInPeriod + 1
    private val callTimestamps: LongArray = LongArray(maxCallInclusive)
    private var index = 0

    @Synchronized
    fun shouldProceed(): Boolean {
        val now = System.currentTimeMillis()
        val oldestCallIndex = (index + 1) % maxCallInclusive
        val oldestCall = callTimestamps[oldestCallIndex]

        // Check if the oldest call is outside the rate limiting period
        if (oldestCall == 0L || (now - oldestCall) > periodSeconds.inWholeMilliseconds) {
            callTimestamps[index] = now
            index = oldestCallIndex
            return true
        }
        return false
    }
}
