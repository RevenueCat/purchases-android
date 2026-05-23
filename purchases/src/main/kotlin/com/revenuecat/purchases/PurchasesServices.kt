package com.revenuecat.purchases

private val defaultServiceRegistry = PurchasesServiceRegistry()

internal object PurchasesServices {
    @OptIn(InternalRevenueCatAPI::class)
    fun default(): PurchasesService = PurchasesServiceRegistryForwarder(defaultServiceRegistry)

    @OptIn(InternalRevenueCatAPI::class)
    fun register(service: PurchasesService) {
        defaultServiceRegistry.register(service)
    }

    @OptIn(InternalRevenueCatAPI::class)
    fun unregister(service: PurchasesService) {
        defaultServiceRegistry.unregister(service)
    }
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
