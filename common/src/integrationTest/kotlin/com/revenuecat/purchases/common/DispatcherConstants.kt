package com.revenuecat.purchases.common

import kotlin.time.Duration.Companion.milliseconds

object DispatcherConstants {
    val jitterDelay = 500L.milliseconds
    val jitterLongDelay = 1000L.milliseconds
}
