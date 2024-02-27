package com.revenuecat.purchases.utils

internal class RateLimiter(val maxCalls: Int, val periodSeconds: Int) {
    companion object {
        const val MILLISECONDS_PER_SECOND = 1000
    }

    private val maxCallInclusive: Int
        get() = maxCalls + 1

    private val callTimestamps: LongArray = LongArray(maxCallInclusive)
    private var index = 0

    @Synchronized
    fun shouldProceed(): Boolean {
        val now = System.currentTimeMillis()
        val oldestCallIndex = (index + 1) % maxCallInclusive
        val oldestCall = callTimestamps[oldestCallIndex]

        // Check if the oldest call is outside the rate limiting period
        if (oldestCall == 0L || (now - oldestCall) > (periodSeconds * MILLISECONDS_PER_SECOND)) {
            callTimestamps[index] = now
            index = oldestCallIndex
            return true
        }
        return false
    }
}
