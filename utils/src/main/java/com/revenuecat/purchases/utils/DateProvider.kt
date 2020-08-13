package com.revenuecat.purchases.utils

import java.util.Date

interface DateProvider {
    val now: Date
}

class DefaultDateProvider : DateProvider {
    override val now: Date
        get() = Date()
}
