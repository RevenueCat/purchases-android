package com.revenuecat.purchases

private val defaultLifecycleEventBus = PurchasesLifecycleEventBus()

@OptIn(InternalRevenueCatAPI::class)
public object PurchasesLifecycle {
    @JvmSynthetic
    public fun register(listener: PurchasesLifecycleListener) {
        defaultLifecycleEventBus.register(listener)
    }

    @JvmSynthetic
    public fun unregister(listener: PurchasesLifecycleListener) {
        defaultLifecycleEventBus.unregister(listener)
    }
}

internal object PurchasesLifecycleListeners {
    @OptIn(InternalRevenueCatAPI::class)
    fun default(): PurchasesLifecycleListener = PurchasesLifecycleEventBusForwarder(defaultLifecycleEventBus)
}

@OptIn(InternalRevenueCatAPI::class)
private class PurchasesLifecycleEventBusForwarder(
    private val eventBus: PurchasesLifecycleEventBus,
) : PurchasesLifecycleListener {
    override fun onPurchasesConfigured(purchases: Purchases) {
        eventBus.onConfigured(purchases)
    }

    override fun onPurchasesClosed(purchases: Purchases) {
        eventBus.onClosed(purchases)
    }
}
