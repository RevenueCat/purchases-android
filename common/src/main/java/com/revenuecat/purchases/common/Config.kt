package com.revenuecat.purchases.common

import com.revenuecat.purchases.LogLevel

object Config {
    var logLevel = LogLevel.debugLogsEnabled(BuildConfig.DEBUG)

    const val frameworkVersion = "5.7.1"
}
