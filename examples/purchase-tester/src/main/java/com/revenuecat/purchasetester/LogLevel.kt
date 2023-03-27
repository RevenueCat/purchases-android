package com.revenuecat.purchasetester

import com.revenuecat.loadShedderIntegrationTests.R
import com.revenuecat.purchases.LogLevel

val LogLevel.colorResource: Int
    get() = when (this) {
        LogLevel.VERBOSE -> R.color.white
        LogLevel.DEBUG -> R.color.white
        LogLevel.INFO -> R.color.blue
        LogLevel.WARN -> R.color.yellow
        LogLevel.ERROR -> R.color.red
    }
