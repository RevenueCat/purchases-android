package com.revenuecat.purchases.common

import kotlin.time.Duration.Companion.milliseconds

object DispatcherConstants {
    val jitterDelay = 5000L.milliseconds
    val jitterLongDelay = 10000L.milliseconds
}
