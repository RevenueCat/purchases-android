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

    // The Purchases the current services were initialized with, so they can be closed with that same
    // instance (honoring PurchasesService.close) even on a reconfigure that passes a different one.
    private var configuredPurchases: Purchases? = null

    @Synchronized
    override fun initialize(purchases: Purchases) {
        // Tear down a previous configuration's services before replacing them, so reconfiguring without
        // an explicit close() doesn't leave the old services holding resources.
        closeServices()
        services = loadServices()
        configuredPurchases = purchases
        services.forEach { service -> service.initialize(purchases) }
    }

    @Synchronized
    override fun close(purchases: Purchases) {
        closeServices()
    }

    private fun closeServices() {
        val purchases = configuredPurchases ?: return
        services.forEach { service -> service.close(purchases) }
        services = emptyList()
        configuredPurchases = null
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
