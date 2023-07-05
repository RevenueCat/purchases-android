package com.revenuecat.purchases.common

import kotlin.time.Duration.Companion.milliseconds

// These values are reduced from the values used in the original code to make the tests run faster.
internal object DispatcherConstants {
    val jitterDelay = 500L.milliseconds
    val jitterLongDelay = 1000L.milliseconds
}
