package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.api.BuildConfig

@InternalRevenueCatAPI
object Config {
    var logLevel = LogLevel.debugLogsEnabled(BuildConfig.DEBUG)

    const val frameworkVersion = "9.21.0-SNAPSHOT"
}
