package com.revenuecat.purchasetester

import com.revenuecat.purchases_sample.R

enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}

val LogLevel.colorResource: Int
    get() = when (this) {
        LogLevel.DEBUG -> R.color.white
        LogLevel.INFO -> R.color.blue
        LogLevel.WARNING -> R.color.yellow
        LogLevel.ERROR -> R.color.red
    }
