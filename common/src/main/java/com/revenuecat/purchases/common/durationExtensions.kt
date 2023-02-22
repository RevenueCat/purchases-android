package com.revenuecat.purchases.common

import java.util.Date
import kotlin.time.Duration

fun Duration.Companion.between(startTime: Date, endTime: Date): Duration {
    return (endTime.time - startTime.time).milliseconds
}
