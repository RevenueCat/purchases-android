package com.revenuecat.purchases

import com.revenuecat.purchases.CacheFetchPolicy.CACHED_OR_FETCHED
import com.revenuecat.purchases.data.LogInResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Get latest available customer info.
 * Coroutine friendly version of [Purchases.getCustomerInfo].
 *
 * @param fetchPolicy Specifies cache behavior for customer info retrieval (optional).
 * Defaults to [CacheFetchPolicy.default]: [CACHED_OR_FETCHED].
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error retrieving the customer info.
 * @return The [CustomerInfo] associated to the current user.
 */
@JvmSynthetic
@Throws(PurchasesException::class)
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
 * This function will change the current appUserID.
 * Typically this would be used after a log out to identify a new user without calling configure
 *
 * Coroutine friendly version of [Purchases.logIn].
 *
 * @param appUserID The new appUserID that should be linked to the currently user
 * @throws [PurchasesException] with a [PurchasesError] if there's an error login the customer info.
 * @return The [CustomerInfo] associated to the current user.
 */
@JvmSynthetic
@Throws(PurchasesTransactionException::class)
suspend fun Purchases.awaitLogIn(appUserID: String): LogInResult {
    return suspendCoroutine { continuation ->
        logInWith(
            appUserID,
            onSuccess = { customerInfo, created ->
                continuation.resume(LogInResult(customerInfo, created))
            },
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * Logs out the Purchases client clearing the save appUserID. This will generate a random user
 * id and save it in the cache.
 *
 * Coroutine friendly version of [Purchases.logOut].
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error login out the user.
 * @return The [CustomerInfo] associated to the current user.
 */
@JvmSynthetic
@Throws(PurchasesTransactionException::class)
suspend fun Purchases.awaitLogOut(): CustomerInfo {
    return suspendCoroutine { continuation ->
        logOutWith(
            onSuccess = { continuation.resume(it) },
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * This method will send all the purchases to the RevenueCat backend. Call this when using your own implementation
 * for subscriptions anytime a sync is needed, such as when migrating existing users to RevenueCat.
 *
 * Coroutine friendly version of [Purchases.syncPurchases].
 *
 * @throws [PurchasesException] with the first [PurchasesError] found while syncing the purchases.
 * @return The [CustomerInfo] associated to the user, after all purchases have been successfully synced. If there are no
 * purchases to sync, the customer info will be returned without any changes.
 */
@JvmSynthetic
@Throws(PurchasesException::class)
suspend fun Purchases.awaitSyncPurchases(): CustomerInfo {
    return suspendCoroutine { continuation ->
        syncPurchasesWith(
            onSuccess = continuation::resume,
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}
