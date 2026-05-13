package com.revenuecat.purchases

import java.util.Collections

@InternalRevenueCatAPI
public interface PurchasesLifecycleListener {
    public fun onPurchasesConfigured(purchases: Purchases)
    public fun onPurchasesClosed(purchases: Purchases)
}

@InternalRevenueCatAPI
public object PurchasesLifecycleEventBus {
    private val listeners = Collections.synchronizedSet(mutableSetOf<PurchasesLifecycleListener>())

    @JvmSynthetic
    public fun register(listener: PurchasesLifecycleListener) {
        listeners.add(listener)
        if (Purchases.isConfigured) {
            listener.onPurchasesConfigured(Purchases.sharedInstance)
        }
    }

    @JvmSynthetic
    public fun unregister(listener: PurchasesLifecycleListener) {
        listeners.remove(listener)
    }

    internal fun onConfigured(purchases: Purchases) {
        snapshotListeners().forEach { listener ->
            listener.onPurchasesConfigured(purchases)
        }
    }

    internal fun onClosed(purchases: Purchases) {
        snapshotListeners().forEach { listener ->
            listener.onPurchasesClosed(purchases)
        }
    }

    private fun snapshotListeners(): List<PurchasesLifecycleListener> {
        synchronized(listeners) {
            return listeners.toList()
        }
    }
}
