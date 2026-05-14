package com.revenuecat.purchases

import com.revenuecat.purchases.CacheFetchPolicy.CACHED_OR_FETCHED
import com.revenuecat.purchases.common.safeResume
import com.revenuecat.purchases.common.safeResumeWithException
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.data.LogInResult
import com.revenuecat.purchases.interfaces.GetCustomerCenterConfigCallback
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale

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
    return suspendCancellableCoroutine { continuation ->
        getCustomerInfoWith(
            fetchPolicy,
            onSuccess = { continuation.safeResume(it) },
            onError = { continuation.safeResumeWithException(PurchasesException(it)) },
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
    return suspendCancellableCoroutine { continuation ->
        logInWith(
            appUserID,
            onSuccess = { customerInfo, created ->
                continuation.safeResume(LogInResult(customerInfo, created))
            },
            onError = { continuation.safeResumeWithException(PurchasesException(it)) },
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
    return suspendCancellableCoroutine { continuation ->
        logOutWith(
            onSuccess = { continuation.safeResume(it) },
            onError = { continuation.safeResumeWithException(PurchasesException(it)) },
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
    return suspendCancellableCoroutine { continuation ->
        syncPurchasesWith(
            onSuccess = { continuation.safeResume(it) },
            onError = { continuation.safeResumeWithException(PurchasesException(it)) },
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
    return suspendCancellableCoroutine { continuation ->
        syncAttributesAndOfferingsIfNeededWith(
            onSuccess = { continuation.safeResume(it) },
            onError = { continuation.safeResumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * Sets attribution data from Appstack's attribution params, then syncs attributes and fetches
 * offerings so that Appstack-based targeting is applied before the coroutine returns.
 *
 * Note: Offerings retrieval is rate limited to 5 calls per minute. If the rate limit is reached,
 * cached offerings will be returned instead.
 *
 * Pass the map received from `AppstackAttributionSdk.getAttributionParams()` directly to this method.
 * The SDK will extract relevant attribution information and set the appropriate attributes.
 * Note that this method will never unset any attributes. To unset an attribute, call the individual
 * setter with a `null` value.
 *
 * Coroutine friendly version of [Purchases.setAppstackAttributionParams].
 *
 * @param data The attribution params map from `AppstackAttributionSdk.getAttributionParams()`.
 * @throws [PurchasesException] with a [PurchasesError] if there's an error syncing attributes or fetching offerings.
 * @return [Offerings] targeted with Appstack attribution data.
 */
@JvmSynthetic
@Throws(PurchasesException::class)
public suspend fun Purchases.awaitSetAppstackAttributionParams(data: Map<String, String>): Offerings {
    return suspendCancellableCoroutine { continuation ->
        setAppstackAttributionParams(
            data,
            syncAttributesAndOfferingsListener(
                onSuccess = { continuation.safeResume(it) },
                onError = { continuation.safeResumeWithException(PurchasesException(it)) },
            ),
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
    return suspendCancellableCoroutine { continuation ->
        getAmazonLWAConsentStatusWith(
            onSuccess = { continuation.safeResume(it) },
            onError = { continuation.safeResumeWithException(PurchasesException(it)) },
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
    return suspendCancellableCoroutine { continuation ->
        getCustomerCenterConfigData(object : GetCustomerCenterConfigCallback {
            override fun onSuccess(customerCenterConfig: CustomerCenterConfigData) {
                continuation.safeResume(customerCenterConfig)
            }

            override fun onError(error: PurchasesError) {
                continuation.safeResumeWithException(PurchasesException(error))
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
    return suspendCancellableCoroutine { continuation ->
        getVirtualCurrenciesWith(
            onSuccess = { continuation.safeResume(it) },
            onError = { continuation.safeResumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * This method will try to obtain the Store (Google/Amazon) locale. **Note:** this locale only has a region set.
 * If there is any error, it will return null and log said error.
 * Coroutine friendly version of [Purchases.getStorefrontLocale].
 *
 * Not supported for the Galaxy Store. Invocations for the Galaxy Store will always throw an error.
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error retrieving the country code.
 * @return The Store locale. **Note:** this locale only has a region set.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Throws(PurchasesException::class)
public suspend fun Purchases.awaitStorefrontLocale(): Locale {
    return suspendCancellableCoroutine { continuation ->
        getStorefrontLocaleWith(
            onSuccess = { continuation.safeResume(it) },
            onError = { continuation.safeResumeWithException(PurchasesException(it)) },
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
    return suspendCancellableCoroutine { continuation ->
        createSupportTicket(
            email = email,
            description = description,
            onSuccess = { wasSent ->
                continuation.safeResume(CreateSupportTicketResult(success = wasSent))
            },
            onError = { continuation.safeResumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * Fetches offerings and returns the current [Offering], or null if none is configured.
 *
 * Coroutine-friendly version of [Purchases.getCurrentOfferingWith].
 *
 * @throws [PurchasesException] with a [PurchasesError] if there's an error fetching offerings.
 * @return The current [Offering], or null if none is configured.
 */
@JvmSynthetic
@Throws(PurchasesException::class)
public suspend fun Purchases.awaitCurrentOffering(): Offering? {
    return suspendCancellableCoroutine { continuation ->
        getCurrentOfferingWith(
            onSuccess = { continuation.safeResume(it) },
            onError = { continuation.safeResumeWithException(PurchasesException(it)) },
        )
    }
}

/**
 * Fetches offerings and returns the [Offering] with the given [id], or null if not found.
 *
 * Coroutine-friendly version of [Purchases.getOfferingWith].
 *
 * @param id The identifier of the offering to fetch.
 * @throws [PurchasesException] with a [PurchasesError] if there's an error fetching offerings.
 * @return The [Offering] with the given [id], or null if not found.
 */
@JvmSynthetic
@Throws(PurchasesException::class)
public suspend fun Purchases.awaitOffering(id: String): Offering? {
    return suspendCancellableCoroutine { continuation ->
        getOfferingWith(
            id = id,
            onSuccess = { continuation.safeResume(it) },
            onError = { continuation.safeResumeWithException(PurchasesException(it)) },
        )
    }
}
