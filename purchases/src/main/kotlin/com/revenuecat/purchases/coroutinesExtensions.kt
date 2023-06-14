package com.revenuecat.purchases

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Get latest available customer info.
 * Coroutine friendly version of [Purchases.getCustomerInfo].
 *
 * @warning This function is marked as [ExperimentalPreviewRevenueCatPurchasesAPI] and may change in the future.
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error retrieving the customer info.
 * @return The [CustomerInfo] or a [PurchasesException] with the [PurchasesError]
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
suspend fun Purchases.awaitCustomerInfo(): CustomerInfo {
    return suspendCoroutine { continuation ->
        getCustomerInfoWith(
            onSuccess = continuation::resume,
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}
