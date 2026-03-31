package com.revenuecat.purchases

@InternalRevenueCatAPI
public fun interface DebugEventListener {
    public fun onDebugEventReceived(event: DebugEvent)
}
