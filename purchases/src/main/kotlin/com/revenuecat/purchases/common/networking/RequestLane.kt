package com.revenuecat.purchases.common.networking

/**
 * A group of endpoints that share a dispatcher, so requests on one lane don't queue behind requests on another.
 */
internal enum class RequestLane {
    DEFAULT,

    /** Remote config fetches, so `/v1/config` overlaps offerings instead of serializing behind them. */
    REMOTE_CONFIG,

    /** Diagnostics and events, which share their dispatcher with the components that batch them. */
    EVENTS,
}
