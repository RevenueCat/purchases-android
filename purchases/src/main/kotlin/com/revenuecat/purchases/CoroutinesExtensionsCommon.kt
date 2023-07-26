package com.revenuecat.purchases

import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
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

/**
 * Initiate a purchase with the given [PurchaseParams].
 * Initialized with an [Activity] either a [Package], [StoreProduct], or [SubscriptionOption].
 *
 * If a [Package] or [StoreProduct] is used to build the [PurchaseParams], the [defaultOption] will be purchased.
 * [defaultOption] is selected via the following logic:
 *   - Filters out offers with "rc-ignore-offer" tag
 *   - Uses [SubscriptionOption] with the longest free trial or cheapest first phase
 *   - Falls back to use base plan
 *
 * @params [purchaseParams] The parameters configuring the purchase. See [PurchaseParams.Builder] for options.
 * @throws [PurchasesTransactionException] with a [PurchasesTransactionException] if there's an error when purchasing
 * and a userCancelled boolean that indicates if the user cancelled the purchase flow.
 * @return The [StoreTransaction] for this purchase and the updated [CustomerInfo] for this user.
 *
 * @warning This function is marked as [ExperimentalPreviewRevenueCatPurchasesAPI] and may change in the future.
 * Only available in Kotlin.
 */
@JvmSynthetic
@ExperimentalPreviewRevenueCatPurchasesAPI
@Throws(PurchasesTransactionException::class)
suspend fun Purchases.awaitPurchase(purchaseParams: PurchaseParams): PurchaseResult {
    return suspendCoroutine { continuation ->
        purchase(
            purchaseParams = purchaseParams,
            callback = purchaseCompletedCallback(
                onSuccess = { storeTransaction, customerInfo ->
                    continuation.resume(PurchaseResult(storeTransaction, customerInfo))
                },
                onError = { purchasesError, userCancelled ->
                    continuation.resumeWithException(PurchasesTransactionException(purchasesError, userCancelled))
                },
            ),
        )
    }
}

/**
 * Gets the StoreProduct(s) for the given list of product ids of type [type], or for all types if no type is specified.
 *
 * Coroutine friendly version of [Purchases.getProducts].
 *
 * @param [productIds] List of productIds
 * @param [type] A product type to filter by
 *
 * @warning This function is marked as [ExperimentalPreviewRevenueCatPurchasesAPI] and may change in the future.
 * Only available in Kotlin.
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error retrieving the offerings.
 * @return The fetched list of [StoreProduct].
 */
@JvmSynthetic
@ExperimentalPreviewRevenueCatPurchasesAPI
@Throws(PurchasesTransactionException::class)
suspend fun Purchases.awaitGetProducts(
    productIds: List<String>,
    type: ProductType? = null,
): List<StoreProduct> {
    return suspendCoroutine { continuation ->
        getProductsWith(
            productIds,
            type,
            onGetStoreProducts = continuation::resume,
            onError = {
                continuation.resumeWithException(PurchasesException(it))
            },
        )
    }
}
