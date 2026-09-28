package com.revenuecat.purchases.common

import com.revenuecat.purchases.common.networking.RequestLane

internal fun testBackendLanes(
    dispatcher: Dispatcher,
    eventsDispatcher: Dispatcher,
    remoteConfigDispatcher: Dispatcher,
): BackendLanes = BackendLanes(
    defaultDispatcher = dispatcher,
    dedicatedDispatchers = mapOf(
        RequestLane.REMOTE_CONFIG to remoteConfigDispatcher,
        RequestLane.EVENTS to eventsDispatcher,
    ),
)

internal fun testBackendLanes(dispatcher: Dispatcher, eventsDispatcher: Dispatcher): BackendLanes =
    testBackendLanes(dispatcher, eventsDispatcher, remoteConfigDispatcher = dispatcher)
