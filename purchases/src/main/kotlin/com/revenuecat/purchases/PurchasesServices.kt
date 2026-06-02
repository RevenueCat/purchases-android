package com.revenuecat.purchases

import com.revenuecat.purchases.common.errorLog
import java.util.ServiceLoader

internal object PurchasesServices {
    fun default(): PurchasesServiceDispatcher = ServiceLoaderDispatcher()
}

/**
 * Drives the [Purchases] lifecycle for every [PurchasesService] on the classpath. This is the internal
 * dispatcher that [Purchases] talks to, not a [PurchasesService] itself.
 */
internal interface PurchasesServiceDispatcher {
    fun initialize(purchases: Purchases)
    fun close(purchases: Purchases)
}

@OptIn(InternalRevenueCatAPI::class)
private class ServiceLoaderDispatcher : PurchasesServiceDispatcher {
    private var services: List<PurchasesService> = emptyList()

    @Synchronized
    override fun initialize(purchases: Purchases) {
        services = loadServices()
        services.forEach { service -> service.initialize(purchases) }
    }

    @Synchronized
    override fun close(purchases: Purchases) {
        services.forEach { service -> service.close(purchases) }
        services = emptyList()
    }

    /**
     * Discovers implementations with [ServiceLoader], passing the interface's own [ClassLoader] so the
     * call stays in the shape R8 can optimize. A broken provider must not crash
     * [Purchases.configure]/[Purchases.close], so failures degrade to an empty list.
     */
    private fun loadServices(): List<PurchasesService> = runCatching {
        ServiceLoader.load(
            PurchasesService::class.java,
            PurchasesService::class.java.classLoader,
        ).toList()
    }.getOrElse { error ->
        errorLog(error) { "Failed to load PurchasesService implementations." }
        emptyList()
    }
}
