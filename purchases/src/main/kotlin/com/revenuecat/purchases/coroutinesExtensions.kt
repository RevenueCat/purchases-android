package com.revenuecat.purchases

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Get latest available customer info.
 * Coroutine friendly version of [Purchases.getCustomerInfo].
 *
 * @return The [CustomerInfo] or a [PurchasesException] with the [PurchasesError]
 */
suspend fun Purchases.awaitCustomerInfo(): CustomerInfo {
    return suspendCoroutine { continuation ->
        getCustomerInfoWith(
            onSuccess = continuation::resume,
            onError = { continuation.resumeWithException(PurchasesException(it)) }
        )
    }
}
