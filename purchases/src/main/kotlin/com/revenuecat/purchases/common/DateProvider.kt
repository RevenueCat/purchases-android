package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import java.util.Date

@InternalRevenueCatAPI
public interface DateProvider {
    public val now: Date
}

@InternalRevenueCatAPI
public class DefaultDateProvider : DateProvider {
    override val now: Date
        get() = Date()
}
