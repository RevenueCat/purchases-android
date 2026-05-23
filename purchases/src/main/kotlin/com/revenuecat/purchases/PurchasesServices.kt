package com.revenuecat.purchases

private val defaultServiceRegistry = PurchasesServiceRegistry()

internal object PurchasesServices {
    @OptIn(InternalRevenueCatAPI::class)
    fun default(): PurchasesService = PurchasesServiceRegistryForwarder(defaultServiceRegistry)
}

@OptIn(InternalRevenueCatAPI::class)
private class PurchasesServiceRegistryForwarder(
    private val registry: PurchasesServiceRegistry,
) : PurchasesService {
    override fun initialize(purchases: Purchases) {
        registry.onConfigured(purchases)
    }

    override fun close(purchases: Purchases) {
        registry.onClosed(purchases)
    }
}
