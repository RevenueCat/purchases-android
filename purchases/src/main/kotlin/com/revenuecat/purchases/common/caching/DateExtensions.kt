package com.revenuecat.purchases.common.caching

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.ReceiptStrings
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private val CACHE_REFRESH_PERIOD_IN_FOREGROUND = 5.minutes
private val CACHE_REFRESH_PERIOD_IN_BACKGROUND = 25.hours

internal fun cacheDuration(appInBackground: Boolean): Duration {
    return when {
        appInBackground -> CACHE_REFRESH_PERIOD_IN_BACKGROUND
        else -> CACHE_REFRESH_PERIOD_IN_FOREGROUND
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal fun Date?.isCacheStale(
    appInBackground: Boolean,
    dateProvider: DateProvider = DefaultDateProvider(),
    cacheDurationProvider: (Boolean) -> Duration = ::cacheDuration,
): Boolean {
    return this?.let {
        log(LogIntent.DEBUG) { ReceiptStrings.CHECKING_IF_CACHE_STALE.format(appInBackground) }
        isCacheStale(cacheDurationProvider(appInBackground), dateProvider)
    } ?: true
}

@OptIn(InternalRevenueCatAPI::class)
internal fun Date?.isCacheStale(cacheDuration: Duration, dateProvider: DateProvider = DefaultDateProvider()): Boolean {
    return this?.let { cacheLastUpdated ->
        (dateProvider.now.time - cacheLastUpdated.time).milliseconds >= cacheDuration
    } ?: true
}
