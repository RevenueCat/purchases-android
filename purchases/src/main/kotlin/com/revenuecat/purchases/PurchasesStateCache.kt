package com.revenuecat.purchases

@InternalRevenueCatAPI
internal data class PurchasesStateCache(
    @get:Synchronized
    @set:Synchronized
    override var purchasesState: PurchasesState,
) : PurchasesStateProvider
