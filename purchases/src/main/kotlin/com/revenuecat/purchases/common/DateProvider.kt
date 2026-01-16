package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import java.util.Date

@InternalRevenueCatAPI
interface DateProvider {
    val now: Date
}

@InternalRevenueCatAPI
class DefaultDateProvider : DateProvider {
    override val now: Date
        get() = Date()
}
