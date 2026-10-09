package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.PurchaseCallback

/**
 * A [PurchaseCallback] whose caller may stop waiting for the result. [PurchasesOrchestrator] treats a cancelled
 * callback as absent, so the same product can be purchased again instead of failing with
 * [PurchasesErrorCode.OperationAlreadyInProgressError].
 */
internal interface CancellablePurchaseCallback : PurchaseCallback {
    val isCancelled: Boolean
}

internal fun PurchaseCallback.asCancellable(isCancelled: () -> Boolean): CancellablePurchaseCallback =
    object : CancellablePurchaseCallback, PurchaseCallback by this@asCancellable {
        override val isCancelled: Boolean
            get() = isCancelled()
    }
