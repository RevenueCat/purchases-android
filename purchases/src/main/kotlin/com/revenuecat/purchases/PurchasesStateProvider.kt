package com.revenuecat.purchases

internal data class PurchasesStateProvider(
    @get:Synchronized
    @set:Synchronized
    @Volatile
    var purchasesState: PurchasesState = PurchasesState(),
)
