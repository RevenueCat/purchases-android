package com.revenuecat.purchases.ui.revenuecatui.helpers

import java.util.Date
import kotlin.time.Duration

fun Date.add(duration: Duration) = Date(this.time + duration.inWholeMilliseconds)
fun Date.subtract(duration: Duration) = add(duration * -1)
