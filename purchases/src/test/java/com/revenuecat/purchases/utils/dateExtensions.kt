package com.revenuecat.purchases.utils

import java.util.Date
import kotlin.time.Duration

public fun Date.add(duration: Duration) = Date(this.time + duration.inWholeMilliseconds)
public fun Date.subtract(duration: Duration) = add(duration * -1)
