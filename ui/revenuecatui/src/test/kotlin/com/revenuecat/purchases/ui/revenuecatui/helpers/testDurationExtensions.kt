package com.revenuecat.purchases.ui.revenuecatui.helpers

import java.util.Date
import kotlin.time.Duration

fun Duration.fromNow(): Date {
    return Date().add(this)
}

fun Duration.ago(): Date {
    return Date().subtract(this)
}
