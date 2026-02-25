package com.revenuecat.purchases

import com.revenuecat.purchases.CacheFetchPolicy.CACHED_OR_FETCHED
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.data.LogInResult
import com.revenuecat.purchases.interfaces.GetCustomerCenterConfigCallback
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import java.util.Locale
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
public suspend fun Purchases.awaitCustomerInfo(
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
public suspend fun Purchases.awaitLogIn(appUserID: String): LogInResult {
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
public suspend fun Purchases.awaitLogOut(): CustomerInfo {
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
public suspend fun Purchases.awaitSyncPurchases(): CustomerInfo {
    return suspendCoroutine { continuation ->
        syncPurchasesWith(
            onSuccess = continuation::resume,
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * Syncs subscriber attributes and then fetches the configured offerings for this user. This method is intended to
 * be called when using Targeting Rules with Custom Attributes. Any subscriber attributes should be set before
 * calling this method to ensure the returned offerings are applied with the latest subscriber attributes.
 *
 * This method is rate limited to 5 calls per minute. It will log a warning and return offerings cache when reached.
 *
 * Refer to [the guide](https://www.revenuecat.com/docs/tools/targeting) for more targeting information
 * For more offerings information, see [Purchases.getOfferings]
 *
 * Coroutine friendly version of [Purchases.syncAttributesAndOfferingsIfNeeded].
 *
 * @throws [PurchasesException] with the first [PurchasesError] if there's an error syncing attributes
 * or fetching offerings.
 * @returns The [Offerings] fetched after syncing attributes.
 */
@JvmSynthetic
@Throws(PurchasesException::class)
public suspend fun Purchases.awaitSyncAttributesAndOfferingsIfNeeded(): Offerings {
    return suspendCoroutine { continuation ->
        syncAttributesAndOfferingsIfNeededWith(
            onSuccess = continuation::resume,
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * Note: This method only works for the Amazon Appstore. There is no Google equivalent at this time.
 * Calling from a Google-configured app will always return AmazonLWAConsentStatus.UNAVAILABLE.
 *
 * Get the Login with Amazon consent status for the current user. Used to implement one-click
 * account creation using Quick Subscribe.
 *
 * For more information, check the documentation:
 * https://developer.amazon.com/docs/in-app-purchasing/iap-quicksubscribe.html
 *
 * Coroutine friendly version of [Purchases.getAmazonLWAConsentStatus].
 *
 * @throws [PurchasesException] with the first [PurchasesError] if there's an error getting the consent status
 * @returns The AmazonLWAConsentStatus for the current user.
 */
@JvmSynthetic
@Throws(PurchasesException::class)
public suspend fun Purchases.getAmazonLWAConsentStatus(): AmazonLWAConsentStatus {
    return suspendCoroutine { continuation ->
        getAmazonLWAConsentStatusWith(
            onSuccess = continuation::resume,
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * Gets the current user's [CustomerCenterConfigData]. Used by RevenueCatUI to present the customer center.
 *
 * @throws [PurchasesException] with the first [PurchasesError] if there's an error getting the customer center
 * config data.
 * @returns The [CustomerCenterConfigData] for the current user.
 */
@JvmSynthetic
@Throws(PurchasesException::class)
@InternalRevenueCatAPI
public suspend fun Purchases.awaitCustomerCenterConfigData(): CustomerCenterConfigData {
    return suspendCoroutine { continuation ->
        getCustomerCenterConfigData(object : GetCustomerCenterConfigCallback {
            override fun onSuccess(customerCenterConfig: CustomerCenterConfigData) {
                continuation.resume(customerCenterConfig)
            }

            override fun onError(error: PurchasesError) {
                continuation.resumeWithException(PurchasesException(error))
            }
        })
    }
}

/**
 * Fetches the virtual currencies for the current subscriber.
 *
 * Coroutine friendly version of [Purchases.getVirtualCurrencies].
 *
 * @throws [PurchasesException] with a [PurchasesError] if an error occurred while fetching
 * the virtual currencies.
 *
 * @return The [VirtualCurrencies] with the subscriber's virtual currencies.
 */
@JvmSynthetic
@Throws(PurchasesException::class)
public suspend fun Purchases.awaitGetVirtualCurrencies(): VirtualCurrencies {
    return suspendCoroutine { continuation ->
        getVirtualCurrenciesWith(
            onSuccess = { continuation.resume(it) },
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * This method will try to obtain the Store (Google/Amazon) locale. **Note:** this locale only has a region set.
 * If there is any error, it will return null and log said error.
 * Coroutine friendly version of [Purchases.getStorefrontLocale].
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error retrieving the country code.
 * @return The Store locale. **Note:** this locale only has a region set.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Throws(PurchasesException::class)
public suspend fun Purchases.awaitStorefrontLocale(): Locale {
    return suspendCoroutine { continuation ->
        getStorefrontLocaleWith(
            onSuccess = continuation::resume,
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * Result of creating a support ticket.
 *
 * @property success Boolean indicating whether the ticket was successfully sent.
 */
@InternalRevenueCatAPI
public data class CreateSupportTicketResult(
    public val success: Boolean,
)

/**
 * Creates a support ticket for the current user.
 * Coroutine friendly version of [Purchases.createSupportTicket].
 *
 * @param email The user's email address for the support ticket.
 * @param description The description of the support request.
 * @return [CreateSupportTicketResult] indicating whether the ticket was successfully sent.
 * @throws [PurchasesException] with a [PurchasesError] if there's an error creating the support ticket.
 */
@JvmSynthetic
@Throws(PurchasesException::class)
@InternalRevenueCatAPI
public suspend fun Purchases.awaitCreateSupportTicket(email: String, description: String): CreateSupportTicketResult {
    return suspendCoroutine { continuation ->
        createSupportTicket(
            email = email,
            description = description,
            onSuccess = { wasSent -> continuation.resume(CreateSupportTicketResult(success = wasSent)) },
            onError = { continuation.resumeWithException(PurchasesException(it)) },
        )
    }
}