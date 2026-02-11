package com.revenuecat.purchases.common

import com.revenuecat.purchases.utils.add
import com.revenuecat.purchases.utils.subtract
import java.util.Date
import kotlin.time.Duration

public fun Duration.fromNow(): Date {
    return Date().add(this)
}

public fun Duration.ago(): Date {
    return Date().subtract(this)
}
