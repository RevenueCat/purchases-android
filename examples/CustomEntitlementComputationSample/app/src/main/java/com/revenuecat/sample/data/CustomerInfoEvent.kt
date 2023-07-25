package com.revenuecat.sample.data

import com.revenuecat.purchases.CustomerInfo
import java.util.Date
import java.util.UUID

data class CustomerInfoEvent(
    val date: Date = Date(),
    val customerInfo: CustomerInfo,
    val id: UUID = UUID.randomUUID(),
)
