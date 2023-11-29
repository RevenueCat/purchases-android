package com.revenuecat.purchases

internal data class PurchasesStateCache(
    @get:Synchronized
    @set:Synchronized
    override var purchasesState: PurchasesState = PurchasesState(),
) : PurchasesStateProvider
