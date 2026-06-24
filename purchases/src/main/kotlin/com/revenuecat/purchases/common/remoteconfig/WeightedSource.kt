package com.revenuecat.purchases.common.remoteconfig

internal interface WeightedSource {
    val priority: Int
    val weight: Int
}
