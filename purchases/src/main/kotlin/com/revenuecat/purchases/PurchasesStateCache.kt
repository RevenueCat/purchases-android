package com.revenuecat.purchases

@InternalRevenueCatStoreAPI
internal data class PurchasesStateCache(
    @get:Synchronized
    @set:Synchronized
    override var purchasesState: PurchasesState,
) : PurchasesStateProvider
