package com.revenuecat.purchases.utils

class MockTimestampUtils : TimestampUtils() {
    var mockedTimestamp: Long? = null

    override fun currentTimeMillis(): Long {
        return mockedTimestamp ?: super.currentTimeMillis()
    }
}
