package com.revenuecat.purchases

/**
 * A service that wants to observe the [Purchases] lifecycle.
 *
 * Implementations are discovered automatically through [java.util.ServiceLoader]. To register one,
 * declare the fully qualified implementation class name in a resource file named
 * `META-INF/services/com.revenuecat.purchases.PurchasesService` and provide a public no-argument
 * constructor.
 */
@InternalRevenueCatAPI
public interface PurchasesService {
    public fun initialize(purchases: Purchases)
    public fun close(purchases: Purchases)
}
