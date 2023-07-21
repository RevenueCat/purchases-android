package com.revenuecat.purchases

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
@Throws(PurchasesException::class)
suspend fun Purchases.awaitOfferings(): Offerings {
    return suspendCoroutine { continuation ->
        getOfferingsWith(
            onSuccess = continuation::resume,
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}
