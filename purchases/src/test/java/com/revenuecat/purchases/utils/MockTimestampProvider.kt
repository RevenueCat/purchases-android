package com.revenuecat.purchases.utils

class MockTimestampProvider : TimestampProvider {
    var overridenCurrentTimeMillis: Long? = null

    override val currentTimeMillis: Long
        get() = overridenCurrentTimeMillis ?: System.currentTimeMillis()
}
