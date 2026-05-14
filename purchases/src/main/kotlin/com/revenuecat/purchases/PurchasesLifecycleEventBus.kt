package com.revenuecat.purchases

@InternalRevenueCatAPI
public interface PurchasesLifecycleListener {
    public fun onPurchasesConfigured(purchases: Purchases)
    public fun onPurchasesClosed(purchases: Purchases)
}

@InternalRevenueCatAPI
public object PurchasesLifecycleEventBus {
    private val listeners = mutableSetOf<PurchasesLifecycleListener>()
    private var configuredPurchases: Purchases? = null

    @JvmSynthetic
    public fun register(listener: PurchasesLifecycleListener) {
        val configuredSnapshot = registerAndGetConfiguredSnapshot(listener)
        configuredSnapshot?.let { configured ->
            listener.onPurchasesConfigured(configured)
        }
    }

    @JvmSynthetic
    public fun unregister(listener: PurchasesLifecycleListener) {
        unregisterSynchronized(listener)
    }

    internal fun onConfigured(purchases: Purchases) {
        val listenersToNotify = updateConfiguredAndGetListeners(purchases)
        listenersToNotify.forEach { listener ->
            listener.onPurchasesConfigured(purchases)
        }
    }

    internal fun onClosed(purchases: Purchases) {
        val listenersToNotify = clearConfiguredAndGetListeners(purchases)
        listenersToNotify.forEach { listener ->
            listener.onPurchasesClosed(purchases)
        }
    }

    @Synchronized
    private fun registerAndGetConfiguredSnapshot(listener: PurchasesLifecycleListener): Purchases? {
        listeners.add(listener)
        return configuredPurchases
    }

    @Synchronized
    private fun unregisterSynchronized(listener: PurchasesLifecycleListener) {
        listeners.remove(listener)
    }

    @Synchronized
    private fun updateConfiguredAndGetListeners(purchases: Purchases): List<PurchasesLifecycleListener> {
        configuredPurchases = purchases
        return listeners.toList()
    }

    @Synchronized
    private fun clearConfiguredAndGetListeners(purchases: Purchases): List<PurchasesLifecycleListener> {
        if (configuredPurchases === purchases) {
            configuredPurchases = null
        }
        return listeners.toList()
    }
}
