package com.revenuecat.purchases.common

import com.revenuecat.purchases.LogLevel

object Config {

    var logLevel: LogLevel = LogLevel.debugLogsEnabled(BuildConfig.DEBUG)

    const val frameworkVersion = "5.7.0-SNAPSHOT"
}
