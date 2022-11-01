package com.revenuecat.purchases.utils

interface TimestampProvider {
    val currentTimeMillis: Long
}

class DefaultTimestampProvider : TimestampProvider {
    override val currentTimeMillis: Long
        get() = System.currentTimeMillis()
}
