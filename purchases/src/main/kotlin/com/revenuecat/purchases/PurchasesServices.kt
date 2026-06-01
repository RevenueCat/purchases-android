package com.revenuecat.purchases

import java.util.ServiceLoader

internal object PurchasesServices {
    @OptIn(InternalRevenueCatAPI::class)
    fun default(): PurchasesService = ServiceLoaderForwarder()
}

/**
 * Forwards [Purchases] lifecycle events to every [PurchasesService] declared on the classpath.
 *
 * Implementations are discovered with [ServiceLoader]. The lookup is performed lazily and cached, so
 * the classpath is scanned at most once. Passing the interface's own [ClassLoader] keeps the call in
 * the shape R8 can optimize into direct instantiation of the known providers.
 */
@OptIn(InternalRevenueCatAPI::class)
private class ServiceLoaderForwarder : PurchasesService {
    private val services: List<PurchasesService> by lazy {
        ServiceLoader.load(
            PurchasesService::class.java,
            PurchasesService::class.java.classLoader,
        ).toList()
    }

    override fun initialize(purchases: Purchases) {
        services.forEach { service -> service.initialize(purchases) }
    }

    override fun close(purchases: Purchases) {
        services.forEach { service -> service.close(purchases) }
    }
}
