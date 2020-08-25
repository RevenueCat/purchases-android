package com.revenuecat.purchases.common

import java.util.Date

interface DateProvider {
    val now: Date
}

class DefaultDateProvider : DateProvider {
    override val now: Date
        get() = Date()
}
