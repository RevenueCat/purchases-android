package com.revenuecat.purchases

import com.revenuecat.purchases.common.errorLog
import java.util.ServiceLoader

internal object PurchasesServices {
    @OptIn(InternalRevenueCatAPI::class)
    fun default(): PurchasesService = ServiceLoaderForwarder()
}

/**
 * Forwards [Purchases] lifecycle events to every [PurchasesService] declared on the classpath.
 *
 * Implementations are discovered lazily (once) with [ServiceLoader], passing the interface's own
 * [ClassLoader] so the call stays in the shape R8 can optimize.
 */
@OptIn(InternalRevenueCatAPI::class)
private class ServiceLoaderForwarder : PurchasesService {
    // A broken provider must not crash Purchases.configure()/close(), so failures degrade to no-op.
    private val services: List<PurchasesService> by lazy {
        runCatching {
            ServiceLoader.load(
                PurchasesService::class.java,
                PurchasesService::class.java.classLoader,
            ).toList()
        }.getOrElse { error ->
            errorLog(error) { "Failed to load PurchasesService implementations." }
            emptyList()
        }
    }

    override fun initialize(purchases: Purchases) {
        services.forEach { service -> service.initialize(purchases) }
    }

    override fun close(purchases: Purchases) {
        services.forEach { service -> service.close(purchases) }
    }
}
