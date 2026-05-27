package com.revenuecat.purchases

@InternalRevenueCatAPI
public interface PurchasesService {
    public fun initialize(purchases: Purchases)
    public fun close(purchases: Purchases)
}

@OptIn(InternalRevenueCatAPI::class)
internal class PurchasesServiceRegistry {
    private val services = mutableSetOf<PurchasesService>()
    private var configuredPurchases: Purchases? = null

    @Synchronized
    fun register(service: PurchasesService) {
        val added = services.add(service)
        if (!added) return
        configuredPurchases?.let { configured ->
            service.initialize(configured)
        }
    }

    @Synchronized
    fun unregister(service: PurchasesService) {
        services.remove(service)
    }

    @Synchronized
    internal fun onConfigured(purchases: Purchases) {
        configuredPurchases = purchases
        val servicesToNotify = services.toList()
        // Keep callback dispatch in this critical section so register/configured/closed
        // notifications are observed in a single total order.
        servicesToNotify.forEach { service ->
            service.initialize(purchases)
        }
    }

    @Synchronized
    internal fun onClosed(purchases: Purchases) {
        if (configuredPurchases === purchases) {
            configuredPurchases = null
        }
        val servicesToNotify = services.toList()
        // See onConfigured: ordering correctness is prioritized over minimizing lock hold time.
        servicesToNotify.forEach { service ->
            service.close(purchases)
        }
    }
}
