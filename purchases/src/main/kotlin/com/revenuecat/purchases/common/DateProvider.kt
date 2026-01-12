package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatStoreAPI
import java.util.Date

@InternalRevenueCatStoreAPI
interface DateProvider {
    val now: Date
}

@InternalRevenueCatStoreAPI
class DefaultDateProvider : DateProvider {
    override val now: Date
        get() = Date()
}
