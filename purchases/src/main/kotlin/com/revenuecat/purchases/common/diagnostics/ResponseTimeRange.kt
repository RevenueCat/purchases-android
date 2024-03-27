package com.revenuecat.purchases.common.diagnostics

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal enum class ResponseTimeRange(val stringRepresentation: String) {
    LESS_THAN_500_MS("less_than_500_ms"),
    BETWEEN_500_AND_1000_MS("between_500_and_1000_ms"),
    BETWEEN_1000_AND_2000_MS("between_1000_and_2000_ms"),
    BETWEEN_2000_AND_5000_MS("between_2000_and_5000_ms"),
    MORE_THAN_5000_MS("more_than_5000_ms"),
}

internal fun Duration.responseTimeRange(): ResponseTimeRange {
    return when {
        this < 500.milliseconds -> ResponseTimeRange.LESS_THAN_500_MS
        this < 1000.milliseconds -> ResponseTimeRange.BETWEEN_500_AND_1000_MS
        this < 2000.milliseconds -> ResponseTimeRange.BETWEEN_1000_AND_2000_MS
        this < 5000.milliseconds -> ResponseTimeRange.BETWEEN_2000_AND_5000_MS
        else -> ResponseTimeRange.MORE_THAN_5000_MS
    }
}
