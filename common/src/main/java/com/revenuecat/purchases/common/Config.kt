package com.revenuecat.purchases.common

import com.revenuecat.purchases.LogLevel

object Config {
    var logLevel = LogLevel.debugLogsEnabled(BuildConfig.DEBUG)

    const val frameworkVersion = "6.5.0"
}
