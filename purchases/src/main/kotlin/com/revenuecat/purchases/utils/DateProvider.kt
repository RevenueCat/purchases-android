package com.revenuecat.purchases.utils

import java.util.Date

internal interface DateProvider {
    val now: Date
}

internal class DefaultDateProvider : DateProvider {
    override val now: Date
        get() = Date()
}