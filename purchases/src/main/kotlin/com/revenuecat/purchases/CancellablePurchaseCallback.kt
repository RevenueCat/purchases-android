package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.PurchaseCallback

internal interface CancellablePurchaseCallback : PurchaseCallback {
    val isCancelled: Boolean
}

internal fun PurchaseCallback.asCancellable(isCancellationRequested: () -> Boolean): CancellablePurchaseCallback =
    object : CancellablePurchaseCallback, PurchaseCallback by this@asCancellable {
        override val isCancelled: Boolean
            get() = isCancellationRequested()
    }
