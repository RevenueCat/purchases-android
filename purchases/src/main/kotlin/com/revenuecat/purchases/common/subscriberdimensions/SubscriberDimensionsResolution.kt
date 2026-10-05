package com.revenuecat.purchases.common.subscriberdimensions

/**
 * Outcome of reading the optional `subscriber_dimensions` topic from committed config:
 *
 * - [Found]: the topic's `default` item was read.
 * - [NotConfigured]: a committed config omits the topic. This is an answer a warm can cache and serve from
 *   memory, rather than a miss that would send every read back to disk.
 * - [Unavailable]: the SDK could not produce dimensions from the configuration it has: nothing is committed
 *   (a failed or not-yet-run sync), the topic has no `default` item or one that does not parse, or the read was
 *   superseded by config commits too often to trust.
 */
internal sealed class SubscriberDimensionsResolution {
    data class Found(val dimensions: SubscriberDimensions) : SubscriberDimensionsResolution()

    object NotConfigured : SubscriberDimensionsResolution()

    object Unavailable : SubscriberDimensionsResolution()
}
