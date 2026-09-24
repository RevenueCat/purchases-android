package com.revenuecat.purchases.common

import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.RequestLane
import com.revenuecat.purchases.strings.NetworkStrings

/**
 * Holds the [Dispatcher] for each [RequestLane], falling back to [defaultDispatcher] when a lane has no
 * dedicated one.
 */
internal class BackendLanes(
    private val defaultDispatcher: Dispatcher,
    private val dedicatedDispatchers: Map<RequestLane, Dispatcher>,
) {
    operator fun get(lane: RequestLane): Dispatcher {
        dedicatedDispatchers[lane]?.let { return it }

        // Single-lane setups have no dedicated dispatchers by design, so every lane intentionally shares
        // defaultDispatcher and should stay silent.
        if (lane != RequestLane.DEFAULT && dedicatedDispatchers.isNotEmpty()) {
            warnLog { NetworkStrings.MISSING_DEDICATED_LANE_DISPATCHER.format(lane.name) }
        }
        return defaultDispatcher
    }

    operator fun get(endpoint: Endpoint): Dispatcher = get(endpoint.lane)
}
