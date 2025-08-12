package com.revenuecat.purchasetester.utils

import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchasetester.R

val LogLevel.colorResource: Int
    get() = when (this) {
        LogLevel.VERBOSE -> R.color.white
        LogLevel.DEBUG -> R.color.white
        LogLevel.INFO -> R.color.blue
        LogLevel.WARN -> R.color.yellow
        LogLevel.ERROR -> R.color.red
    }
