package com.revenuecat.purchases

import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.suspendCancellableCoroutine
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
 * @throws [PurchasesException] with a [PurchasesError] if there's an error retrieving the offerings.
 * @return The [Offerings] available to this user.
 */
@JvmSynthetic
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
 * Fetch the configured offerings for this users. Offerings allows you to configure your in-app
 * products via RevenueCat and greatly simplifies management. See
 * [the guide](https://docs.revenuecat.com/offerings) for more info.
 *
 * Offerings will be fetched and cached on instantiation so that, by the time they are needed,
 * your prices are loaded for your purchase flow. Time is money.
 *
 * Coroutine friendly version of [Purchases.getOfferings].
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error retrieving the offerings.
 * @return The [Offerings] available to this user.
 */
@JvmSynthetic
@Throws(PurchasesException::class)
suspend fun Purchases.awaitCancellableOfferings(): Offerings {
    return suspendCancellableCoroutine { continuation ->
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
 * If a [Package] or [StoreProduct] is used to build the [PurchaseParams], the [StoreProduct.defaultOption] will
 * be purchased.
 * [StoreProduct.defaultOption] is selected via the following logic:
 *   - Filters out offers with "rc-ignore-offer" tag
 *   - Uses [SubscriptionOption] with the longest free trial or cheapest first phase
 *   - Falls back to use base plan
 *
 * @params [purchaseParams] The parameters configuring the purchase. See [PurchaseParams.Builder] for options.
 * @throws [PurchasesTransactionException] with a [PurchasesTransactionException] if there's an error when purchasing
 * and a userCancelled boolean that indicates if the user cancelled the purchase flow.
 * @return The [StoreTransaction] for this purchase and the updated [CustomerInfo] for this user.
 */
@JvmSynthetic
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
 * Initiate a purchase with the given [PurchaseParams].
 * Initialized with an [Activity] either a [Package], [StoreProduct], or [SubscriptionOption].
 *
 * If a [Package] or [StoreProduct] is used to build the [PurchaseParams], the [StoreProduct.defaultOption] will
 * be purchased.
 * [StoreProduct.defaultOption] is selected via the following logic:
 *   - Filters out offers with "rc-ignore-offer" tag
 *   - Uses [SubscriptionOption] with the longest free trial or cheapest first phase
 *   - Falls back to use base plan
 *
 * @params [purchaseParams] The parameters configuring the purchase. See [PurchaseParams.Builder] for options.
 * @throws [PurchasesTransactionException] with a [PurchasesTransactionException] if there's an error when purchasing
 * and a userCancelled boolean that indicates if the user cancelled the purchase flow.
 * @return The [StoreTransaction] for this purchase and the updated [CustomerInfo] for this user.
 */
@JvmSynthetic
@Throws(PurchasesTransactionException::class)
suspend fun Purchases.awaitCancellablePurchase(purchaseParams: PurchaseParams): PurchaseResult {
    return suspendCancellableCoroutine { continuation ->
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
 * @throws [PurchasesException] with a [PurchasesError] if there's an error retrieving the products.
 * @return A list of [StoreProduct] with the products that have been able to be fetched from the store successfully.
 * Not found products will be ignored.
 */
@JvmSynthetic
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

/**
 * Gets the StoreProduct(s) for the given list of product ids of type [type], or for all types if no type is specified.
 *
 * Coroutine friendly version of [Purchases.getProducts].
 *
 * @param [productIds] List of productIds
 * @param [type] A product type to filter by
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error retrieving the products.
 * @return A list of [StoreProduct] with the products that have been able to be fetched from the store successfully.
 * Not found products will be ignored.
 */
@JvmSynthetic
@Throws(PurchasesTransactionException::class)
suspend fun Purchases.awaitCancellableGetProducts(
    productIds: List<String>,
    type: ProductType? = null,
): List<StoreProduct> {
    return suspendCancellableCoroutine { continuation ->
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

/**
 * Restores purchases made with the current Play Store account for the current user.
 * This method will post all purchases associated with the current Play Store account to
 * RevenueCat and become associated with the current `appUserID`. If the receipt token is being
 * used by an existing user, the current `appUserID` will be aliased together with the
 * `appUserID` of the existing user. Going forward, either `appUserID` will be able to reference
 * the same user.
 *
 * You shouldn't use this method if you have your own account system. In that case
 * "restoration" is provided by your app passing the same `appUserId` used to purchase originally.
 *
 * Coroutine friendly version of [Purchases.restorePurchases].
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error login out the user.
 * @return The [CustomerInfo] with the restored purchases.
 */
@JvmSynthetic
@Throws(PurchasesTransactionException::class)
suspend fun Purchases.awaitRestore(): CustomerInfo {
    return suspendCoroutine { continuation ->
        restorePurchasesWith(
            onSuccess = { continuation.resume(it) },
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * Restores purchases made with the current Play Store account for the current user.
 * This method will post all purchases associated with the current Play Store account to
 * RevenueCat and become associated with the current `appUserID`. If the receipt token is being
 * used by an existing user, the current `appUserID` will be aliased together with the
 * `appUserID` of the existing user. Going forward, either `appUserID` will be able to reference
 * the same user.
 *
 * You shouldn't use this method if you have your own account system. In that case
 * "restoration" is provided by your app passing the same `appUserId` used to purchase originally.
 *
 * Coroutine friendly version of [Purchases.restorePurchases].
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error login out the user.
 * @return The [CustomerInfo] with the restored purchases.
 */
@JvmSynthetic
@Throws(PurchasesTransactionException::class)
suspend fun Purchases.awaitCancellableRestore(): CustomerInfo {
    return suspendCancellableCoroutine { continuation ->
        restorePurchasesWith(
            onSuccess = { continuation.resume(it) },
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}
