package com.revenuecat.purchases.utils

public class MockTimestampProvider : TimestampProvider {
    public var overridenCurrentTimeMillis: Long? = null

    override val currentTimeMillis: Long
        get() = overridenCurrentTimeMillis ?: System.currentTimeMillis()
}
