package com.revenuecat.purchases.interfaces

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.checkpoints.CheckpointResult

@InternalRevenueCatAPI
public interface CheckpointCallback {

    public fun onResult(result: CheckpointResult)

    public fun onError(error: PurchasesError)
}
