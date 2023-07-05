package com.revenuecat.purchases.common

import kotlin.time.Duration.Companion.milliseconds

// These values are modified in the `integrationTest` flavor to make the tests run faster.
internal object DispatcherConstants {
    val jitterDelay = 5000L.milliseconds
    val jitterLongDelay = 10000L.milliseconds
}
