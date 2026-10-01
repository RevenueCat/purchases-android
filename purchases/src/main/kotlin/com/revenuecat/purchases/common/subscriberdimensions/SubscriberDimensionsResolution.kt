package com.revenuecat.purchases.common.subscriberdimensions

/**
 * Outcome of reading the `subscriber_dimensions` topic from committed config. The topic is optional, so a
 * committed config that carries no usable copy is an answer ([NotConfigured]) a warm can cache and serve from
 * memory, rather than a miss that would send every read back to disk.
 */
internal sealed class SubscriberDimensionsResolution {
    data class Found(val dimensions: SubscriberDimensions) : SubscriberDimensionsResolution()

    object NotConfigured : SubscriberDimensionsResolution()
}
