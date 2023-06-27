package com.revenuecat.purchases

import com.revenuecat.purchases.CacheFetchPolicy.CACHED_OR_FETCHED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Get latest available customer info.
 * Coroutine friendly version of [Purchases.getCustomerInfo].
 *
 * @warning This function is marked as [ExperimentalPreviewRevenueCatPurchasesAPI] and may change in the future.
 * Only available in Kotlin.
 *
 * @param fetchPolicy Specifies cache behavior for customer info retrieval (optional).
 * Defaults to [CacheFetchPolicy.default]: [CACHED_OR_FETCHED].
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error retrieving the customer info.
 * @return The [CustomerInfo] associated to the current user.
 */
@JvmSynthetic
@ExperimentalPreviewRevenueCatPurchasesAPI
suspend fun Purchases.awaitCustomerInfo(
    fetchPolicy: CacheFetchPolicy = CacheFetchPolicy.default(),
): CustomerInfo {
    return suspendCoroutine { continuation ->
        getCustomerInfoWith(
            fetchPolicy,
            onSuccess = continuation::resume,
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * Fetch the configured offerings for this users. Offerings allows you to configure your in-app
 * products via RevenueCat and greatly simplifies management. See
 * [the guide](https://docs.revenuecat.com/offerings) for more info.
 *
 * Offerings will be fetched and cached on instantiation so that, by the time they are needed,
 * your prices are loaded for your purchase flow. Time is money.
 *
 * Coroutine friendly version of [Purchases.getOfferings].
 *
 * @warning This function is marked as [ExperimentalPreviewRevenueCatPurchasesAPI] and may change in the future.
 * Only available in Kotlin.
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error retrieving the offerings.
 * @return The [Offerings] available to this user.
 */
@JvmSynthetic
@ExperimentalPreviewRevenueCatPurchasesAPI
suspend fun Purchases.awaitOfferings(): Offerings {
    return suspendCoroutine { continuation ->
        getOfferingsWith(
            onSuccess = continuation::resume,
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}
