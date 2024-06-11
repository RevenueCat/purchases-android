package com.revenuecat.purchases.common

import java.util.Date
import kotlin.time.Duration

internal fun Duration.Companion.between(startTime: Date, endTime: Date): Duration {
    return (endTime.time - startTime.time).milliseconds
}

internal fun min(duration1: Duration, duration2: Duration): Duration {
    return if (duration1 < duration2) duration1 else duration2
}
