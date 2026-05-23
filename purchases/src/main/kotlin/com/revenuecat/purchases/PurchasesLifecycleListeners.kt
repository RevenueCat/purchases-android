package com.revenuecat.purchases

private val defaultLifecycleEventBus = PurchasesLifecycleEventBus()

internal object PurchasesLifecycleListeners {
    @OptIn(InternalRevenueCatAPI::class)
    fun default(): PurchasesService = PurchasesLifecycleEventBusForwarder(defaultLifecycleEventBus)
}

@OptIn(InternalRevenueCatAPI::class)
private class PurchasesLifecycleEventBusForwarder(
    private val eventBus: PurchasesLifecycleEventBus,
) : PurchasesService {
    override fun initialize(purchases: Purchases) {
        eventBus.onConfigured(purchases)
    }

    override fun close(purchases: Purchases) {
        eventBus.onClosed(purchases)
    }
}
