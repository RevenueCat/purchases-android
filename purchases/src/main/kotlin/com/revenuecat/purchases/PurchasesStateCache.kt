package com.revenuecat.purchases

@OptIn(InternalRevenueCatAPI::class)
internal data class PurchasesStateCache(
    @get:Synchronized
    @set:Synchronized
    override var purchasesState: PurchasesState,
) : PurchasesStateProvider
