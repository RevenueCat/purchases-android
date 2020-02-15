package com.revenuecat.purchases.utils

import java.util.Date

internal interface DateTimeProvider {
    val now: Date
}

internal class DefaultDateTimeProvider : DateTimeProvider {
    override val now: Date
        get() = Date()
}