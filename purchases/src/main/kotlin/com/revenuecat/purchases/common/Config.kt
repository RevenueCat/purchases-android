package com.revenuecat.purchases.common

import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.api.BuildConfig

internal object Config {
    var logLevel = LogLevel.debugLogsEnabled(BuildConfig.DEBUG)

    const val frameworkVersion = "7.9.0-SNAPSHOT"
}
