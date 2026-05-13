package com.revenuecat.purchases

internal object PurchasesLifecycleListeners {
    @OptIn(InternalRevenueCatAPI::class)
    fun default(): PurchasesLifecycleListener = PurchasesLifecycleEventBusForwarder
}

@OptIn(InternalRevenueCatAPI::class)
private object PurchasesLifecycleEventBusForwarder : PurchasesLifecycleListener {
    override fun onPurchasesConfigured(purchases: Purchases) {
        PurchasesLifecycleEventBus.onConfigured(purchases)
    }

    override fun onPurchasesClosed(purchases: Purchases) {
        PurchasesLifecycleEventBus.onClosed(purchases)
    }
}
