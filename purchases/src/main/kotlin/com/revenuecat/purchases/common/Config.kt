package com.revenuecat.purchases.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.api.BuildConfig

@InternalRevenueCatAPI
public object Config {
    public var logLevel: LogLevel = LogLevel.debugLogsEnabled(BuildConfig.DEBUG)

    public const val frameworkVersion: String = "9.23.0-SNAPSHOT"
}
