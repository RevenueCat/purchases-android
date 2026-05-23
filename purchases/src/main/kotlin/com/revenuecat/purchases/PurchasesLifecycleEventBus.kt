package com.revenuecat.purchases

@InternalRevenueCatAPI
public interface PurchasesService {
    public fun initialize(purchases: Purchases)
    public fun close(purchases: Purchases)
}

@OptIn(InternalRevenueCatAPI::class)
internal class PurchasesLifecycleEventBus {
    private val listeners = mutableSetOf<PurchasesService>()
    private var configuredPurchases: Purchases? = null

    @Synchronized
    fun register(listener: PurchasesService) {
        listeners.add(listener)
        configuredPurchases?.let { configured ->
            listener.initialize(configured)
        }
    }

    @Synchronized
    fun unregister(listener: PurchasesService) {
        listeners.remove(listener)
    }

    @Synchronized
    internal fun onConfigured(purchases: Purchases) {
        configuredPurchases = purchases
        val listenersToNotify = listeners.toList()
        // Keep callback dispatch in this critical section so register/configured/closed
        // notifications are observed in a single total order.
        listenersToNotify.forEach { listener ->
            listener.initialize(purchases)
        }
    }

    @Synchronized
    internal fun onClosed(purchases: Purchases) {
        if (configuredPurchases === purchases) {
            configuredPurchases = null
        }
        val listenersToNotify = listeners.toList()
        // See onConfigured: ordering correctness is prioritized over minimizing lock hold time.
        listenersToNotify.forEach { listener ->
            listener.close(purchases)
        }
    }
}
