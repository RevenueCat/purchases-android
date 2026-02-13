package com.revenuecat.purchases.utils

internal interface TimestampProvider {
    val currentTimeMillis: Long
}

internal class DefaultTimestampProvider : TimestampProvider {
    override val currentTimeMillis: Long
        get() = System.currentTimeMillis()
}
