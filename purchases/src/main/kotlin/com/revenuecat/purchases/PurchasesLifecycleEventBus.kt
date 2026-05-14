package com.revenuecat.purchases

@InternalRevenueCatAPI
public interface PurchasesLifecycleListener {
    public fun onPurchasesConfigured(purchases: Purchases)
    public fun onPurchasesClosed(purchases: Purchases)
}

@OptIn(InternalRevenueCatAPI::class)
internal class PurchasesLifecycleEventBus {
    private val listeners = mutableSetOf<PurchasesLifecycleListener>()
    private var configuredPurchases: Purchases? = null

    @Synchronized
    fun register(listener: PurchasesLifecycleListener) {
        listeners.add(listener)
        configuredPurchases?.let { configured ->
            listener.onPurchasesConfigured(configured)
        }
    }

    @Synchronized
    fun unregister(listener: PurchasesLifecycleListener) {
        listeners.remove(listener)
    }

    @Synchronized
    internal fun onConfigured(purchases: Purchases) {
        configuredPurchases = purchases
        val listenersToNotify = listeners.toList()
        listenersToNotify.forEach { listener ->
            listener.onPurchasesConfigured(purchases)
        }
    }

    @Synchronized
    internal fun onClosed(purchases: Purchases) {
        if (configuredPurchases === purchases) {
            configuredPurchases = null
        }
        val listenersToNotify = listeners.toList()
        listenersToNotify.forEach { listener ->
            listener.onPurchasesClosed(purchases)
        }
    }
}
