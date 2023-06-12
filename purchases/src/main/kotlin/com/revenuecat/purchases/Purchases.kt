//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Pair
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Config
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.debugLogsEnabled
import com.revenuecat.purchases.common.diagnostics.DiagnosticsSynchronizer
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.infoLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.offerings.OfferingsManager
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.google.isSuccessful
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.Callback
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.PurchaseErrorCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.SyncPurchasesCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.BillingFeature
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.strings.AttributionStrings
import com.revenuecat.purchases.strings.BillingStrings
import com.revenuecat.purchases.strings.ConfigureStrings
import com.revenuecat.purchases.strings.ConfigureStrings.AUTO_SYNC_PURCHASES_DISABLED
import com.revenuecat.purchases.strings.CustomerInfoStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import java.net.URL
import java.util.Collections.emptyMap

typealias SuccessfulPurchaseCallback = (StoreTransaction, CustomerInfo) -> Unit
typealias ErrorPurchaseCallback = (StoreTransaction, PurchasesError) -> Unit

/**
 * Entry point for Purchases. It should be instantiated as soon as your app has a unique user id
 * for your user. This can be when a user logs in if you have accounts or on launch if you can
 * generate a random user identifier.
 * Make sure you follow the [quickstart](https://docs.revenuecat.com/docs/getting-started-1)
 * guide to setup your RevenueCat account.
 * @warning Only one instance of Purchases should be instantiated at a time!
 */
@Suppress("LongParameterList")
class Purchases internal constructor(
    private val application: Application,
    backingFieldAppUserID: String?,
    private val backend: Backend,
    private val billing: BillingAbstract,
    private val deviceCache: DeviceCache,
    private val dispatcher: Dispatcher,
    private val identityManager: IdentityManager,
    private val subscriberAttributesManager: SubscriberAttributesManager,
    @set:JvmSynthetic @get:JvmSynthetic internal var appConfig: AppConfig,
    private val customerInfoHelper: CustomerInfoHelper,
    diagnosticsSynchronizer: DiagnosticsSynchronizer?,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val postReceiptHelper: PostReceiptHelper,
    private val syncPurchasesHelper: SyncPurchasesHelper,
    private val offeringsManager: OfferingsManager,
    // This is nullable due to: https://github.com/RevenueCat/purchases-flutter/issues/408
    private val mainHandler: Handler? = Handler(Looper.getMainLooper()),
) : LifecycleDelegate {

    /** @suppress */
    @Suppress("RedundantGetter", "RedundantSetter")
    @Volatile
    @JvmSynthetic
    internal var state = PurchasesState()
        @JvmSynthetic @Synchronized
        get() = field

        @JvmSynthetic @Synchronized
        set(value) {
            field = value
        }

    /**
     * Default to TRUE, set this to FALSE if you are consuming and acknowledging transactions
     * outside of the Purchases SDK.
     */
    var finishTransactions: Boolean
        @Synchronized get() = appConfig.finishTransactions

        @Synchronized set(value) {
            appConfig.finishTransactions = value
        }

    /**
     * The passed in or generated app user ID
     */
    val appUserID: String
        @Synchronized get() = identityManager.currentAppUserID

    /**
     * The listener is responsible for handling changes to customer information.
     * Make sure [removeUpdatedCustomerInfoListener] is called when the listener needs to be destroyed.
     */
    var updatedCustomerInfoListener: UpdatedCustomerInfoListener?
        @Synchronized get() = customerInfoHelper.updatedCustomerInfoListener

        @Synchronized set(value) {
            customerInfoHelper.updatedCustomerInfoListener = value
        }

    /**
     * If the `appUserID` has been generated by RevenueCat
     */
    val isAnonymous: Boolean
        get() = identityManager.currentUserIsAnonymous()

    /**
     * The currently configured store
     */
    val store: Store
        get() = appConfig.store

    private val lifecycleHandler: AppLifecycleHandler by lazy {
        AppLifecycleHandler(this)
    }

    init {
        log(LogIntent.DEBUG, ConfigureStrings.DEBUG_ENABLED)
        log(LogIntent.DEBUG, ConfigureStrings.SDK_VERSION.format(frameworkVersion))
        log(LogIntent.DEBUG, ConfigureStrings.PACKAGE_NAME.format(appConfig.packageName))
        log(LogIntent.USER, ConfigureStrings.INITIAL_APP_USER_ID.format(backingFieldAppUserID))
        identityManager.configure(backingFieldAppUserID)

        dispatch {
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleHandler)
        }

        billing.stateListener = object : BillingAbstract.StateListener {
            override fun onConnected() {
                updatePendingPurchaseQueue()
            }
        }
        billing.purchasesUpdatedListener = getPurchasesUpdatedListener()
        if (!appConfig.dangerousSettings.autoSyncPurchases) {
            log(LogIntent.WARNING, AUTO_SYNC_PURCHASES_DISABLED)
        }

        diagnosticsSynchronizer?.syncDiagnosticsFileIfNeeded()
    }

    /** @suppress */
    override fun onAppBackgrounded() {
        synchronized(this) {
            state = state.copy(appInBackground = true)
        }
        log(LogIntent.DEBUG, ConfigureStrings.APP_BACKGROUNDED)
        synchronizeSubscriberAttributesIfNeeded()
    }

    /** @suppress */
    override fun onAppForegrounded() {
        val firstTimeInForeground: Boolean
        synchronized(this) {
            firstTimeInForeground = state.firstTimeInForeground
            state = state.copy(appInBackground = false, firstTimeInForeground = false)
        }
        log(LogIntent.DEBUG, ConfigureStrings.APP_FOREGROUNDED)
        if (firstTimeInForeground || deviceCache.isCustomerInfoCacheStale(appUserID, appInBackground = false)) {
            log(LogIntent.DEBUG, CustomerInfoStrings.CUSTOMERINFO_STALE_UPDATING_FOREGROUND)
            customerInfoHelper.retrieveCustomerInfo(
                identityManager.currentAppUserID,
                fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                appInBackground = false,
            )
        }
        offeringsManager.onAppForeground(identityManager.currentAppUserID)
        updatePendingPurchaseQueue()
        synchronizeSubscriberAttributesIfNeeded()
        offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
    }

    // region Public Methods

    /**
     * This method will send all the purchases to the RevenueCat backend. Call this when using your own implementation
     * for subscriptions anytime a sync is needed, such as when migrating existing users to RevenueCat. The
     * [SyncPurchasesCallback.onSuccess] callback will be called if all purchases have been synced successfully or
     * there are no purchases. Otherwise, the [SyncPurchasesCallback.onError] callback will be called with a
     * [PurchasesError] indicating the first error found.
     *
     * @param [listener] Called when all purchases have been synced with the backend, either successfully or with
     * an error. If no purchases are present, the success function will be called.
     * @warning This function should only be called if you're migrating to RevenueCat or in observer mode.
     * @warning This function could take a relatively long time to execute, depending on the amount of purchases
     * the user has. Consider that when waiting for this operation to complete.
     */
    @JvmOverloads
    fun syncPurchases(
        listener: SyncPurchasesCallback? = null,
    ) {
        syncPurchasesHelper.syncPurchases(
            isRestore = this.allowSharingPlayStoreAccount,
            appInBackground = this.state.appInBackground,
            onSuccess = { listener?.onSuccess(it) },
            onError = { listener?.onError(it) },
        )
    }

    /**
     * This method will send a purchase to the RevenueCat backend. This function should only be called if you are
     * in Amazon observer mode or performing a client side migration of your current users to RevenueCat.
     *
     * The receipt IDs are cached if successfully posted so they are not posted more than once.
     *
     * @param [productID] Product ID associated to the purchase.
     * @param [receiptID] ReceiptId that represents the Amazon purchase.
     * @param [amazonUserID] Amazon's userID. This parameter will be ignored when syncing a Google purchase.
     * @param [isoCurrencyCode] Product's currency code in ISO 4217 format.
     * @param [price] Product's price.
     */
    fun syncObserverModeAmazonPurchase(
        productID: String,
        receiptID: String,
        amazonUserID: String,
        isoCurrencyCode: String?,
        price: Double?,
    ) {
        log(LogIntent.DEBUG, PurchaseStrings.SYNCING_PURCHASE_STORE_USER_ID.format(receiptID, amazonUserID))

        deviceCache.getPreviouslySentHashedTokens().takeIf { it.contains(receiptID.sha1()) }?.apply {
            log(LogIntent.DEBUG, PurchaseStrings.SYNCING_PURCHASE_SKIPPING.format(receiptID, amazonUserID))
            return
        }

        val appUserID = identityManager.currentAppUserID
        billing.normalizePurchaseData(
            productID,
            receiptID,
            amazonUserID,
            { normalizedProductID ->

                val receiptInfo = ReceiptInfo(
                    productIDs = listOf(normalizedProductID),
                    price = price?.takeUnless { it == 0.0 },
                    currency = isoCurrencyCode?.takeUnless { it.isBlank() },
                )
                postReceiptHelper.postTokenWithoutConsuming(
                    receiptID,
                    amazonUserID,
                    receiptInfo,
                    this.allowSharingPlayStoreAccount,
                    appUserID,
                    marketplace = null,
                    {
                        val logMessage = PurchaseStrings.PURCHASE_SYNCED_USER_ID.format(receiptID, amazonUserID)
                        log(LogIntent.PURCHASE, logMessage)
                    },
                    { error ->
                        val logMessage = PurchaseStrings.SYNCING_PURCHASE_ERROR_DETAILS_USER_ID.format(
                            receiptID,
                            amazonUserID,
                            error,
                        )
                        log(LogIntent.RC_ERROR, logMessage)
                    },
                )
            },
            { error ->
                val logMessage =
                    PurchaseStrings.SYNCING_PURCHASE_ERROR_DETAILS_USER_ID.format(receiptID, amazonUserID, error)
                log(LogIntent.RC_ERROR, logMessage)
            },
        )
    }

    /**
     * Fetch the configured offerings for this users. Offerings allows you to configure your in-app
     * products vis RevenueCat and greatly simplifies management. See
     * [the guide](https://docs.revenuecat.com/offerings) for more info.
     *
     * Offerings will be fetched and cached on instantiation so that, by the time they are needed,
     * your prices are loaded for your purchase flow. Time is money.
     *
     * @param [listener] Called when offerings are available. Called immediately if offerings are cached.
     */
    fun getOfferings(
        listener: ReceiveOfferingsCallback,
    ) {
        offeringsManager.getOfferings(
            identityManager.currentAppUserID,
            state.appInBackground,
            { listener.onError(it) },
            { listener.onReceived(it) },
        )
    }

    /**
     * Gets the StoreProduct(s) for the given list of product ids for all product types.
     * @param [productIds] List of productIds
     * @param [callback] Response callback
     */
    fun getProducts(
        productIds: List<String>,
        callback: GetStoreProductsCallback,
    ) {
        getProducts(productIds, null, callback)
    }

    /**
     * Gets the StoreProduct(s) for the given list of product ids of type [type]
     * @param [productIds] List of productIds
     * @param [type] A product type to filter (no filtering applied if null)
     * @param [callback] Response callback
     */
    fun getProducts(
        productIds: List<String>,
        type: ProductType? = null,
        callback: GetStoreProductsCallback,
    ) {
        val types = type?.let { setOf(type) } ?: setOf(ProductType.SUBS, ProductType.INAPP)

        getProductsOfTypes(
            productIds.toSet(),
            types,
            object : GetStoreProductsCallback {
                override fun onReceived(storeProducts: List<StoreProduct>) {
                    callback.onReceived(storeProducts)
                }

                override fun onError(error: PurchasesError) {
                    callback.onError(error)
                }
            },
        )
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
     *   @params [purchaseParams] The parameters configuring the purchase. See [PurchaseParams.Builder] for options.
     *   @params [callback] The PurchaseCallback that will be called when purchase completes.
     */
    fun purchase(
        purchaseParams: PurchaseParams,
        callback: PurchaseCallback,
    ) {
        with(purchaseParams) {
            oldProductId?.let { productId ->
                startProductChange(
                    activity,
                    purchasingData,
                    presentedOfferingIdentifier,
                    productId,
                    googleReplacementMode,
                    isPersonalizedPrice,
                    callback,
                )
            } ?: run {
                startPurchase(
                    activity,
                    purchasingData,
                    presentedOfferingIdentifier,
                    isPersonalizedPrice,
                    callback,
                )
            }
        }
    }

    /**
     * Purchases [storeProduct].
     * If [storeProduct] represents a subscription, upgrades from the subscription specified by
     * [upgradeInfo.oldSku] and chooses [storeProduct]'s default [SubscriptionOption].
     *
     * The default [SubscriptionOption] logic:
     *   - Filters out offers with "rc-ignore-offer" tag
     *   - Uses [SubscriptionOption] WITH longest free trial or cheapest first phase
     *   - Falls back to use base plan
     *
     * If [storeProduct] represents a non-subscription, [upgradeInfo] will be ignored.
     *
     * @param [activity] Current activity
     * @param [storeProduct] The StoreProduct of the product you wish to purchase
     * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional
     * prorationMode. Amazon Appstore doesn't support changing products so upgradeInfo is ignored for Amazon purchases.
     * @param [listener] The PurchaseCallback that will be called when purchase completes.
     */
    @Deprecated(
        "Use purchase() and PurchaseParams.Builder instead",
        ReplaceWith("purchase()"),
    )
    fun purchaseProduct(
        activity: Activity,
        storeProduct: StoreProduct,
        upgradeInfo: UpgradeInfo,
        listener: ProductChangeCallback,
    ) {
        val googleReplacementMode = GoogleProrationMode.fromPlayBillingClientMode(upgradeInfo.prorationMode)
            ?.asGoogleReplacementMode

        startDeprecatedProductChange(
            activity,
            storeProduct.purchasingData,
            null,
            upgradeInfo.oldSku,
            googleReplacementMode,
            listener,
        )
    }

    /**
     * Purchases a [StoreProduct]. If purchasing a subscription, it will choose the default [SubscriptionOption].
     *
     * The default [SubscriptionOption] logic:
     *   - Filters out offers with "rc-ignore-offer" tag
     *   - Uses [SubscriptionOption] WITH longest free trial or cheapest first phase
     *   - Falls back to use base plan
     *
     * @param [activity] Current activity
     * @param [storeProduct] The StoreProduct of the product you wish to purchase
     * @param [callback] The PurchaseCallback that will be called when purchase completes.
     */
    @Deprecated(
        "Use purchase() and PurchaseParams.Builder instead",
        ReplaceWith("purchase()"),
    )
    fun purchaseProduct(
        activity: Activity,
        storeProduct: StoreProduct,
        callback: PurchaseCallback,
    ) {
        startPurchase(
            activity,
            storeProduct.purchasingData,
            null,
            null,
            callback,
        )
    }

    /**
     * Purchases a [Package].
     * If [packageToPurchase] represents a subscription, upgrades from the subscription specified by [upgradeInfo]'s
     * [oldProductId]and chooses the default [SubscriptionOption] from [packageToPurchase].
     *
     * The default [SubscriptionOption] logic:
     *   - Filters out offers with "rc-ignore-offer" tag
     *   - Uses [SubscriptionOption] WITH longest free trial or cheapest first phase
     *   - Falls back to use base plan
     *
     * If [packageToPurchase] represents a non-subscription, [upgradeInfo] will be ignored.
     *
     * @param [activity] Current activity
     * @param [packageToPurchase] The Package you wish to purchase
     * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldProductId and the optional
     * prorationMode. Amazon Appstore doesn't support changing products so upgradeInfo is ignored for Amazon purchases.
     * @param [callback] The listener that will be called when purchase completes.
     */
    @Deprecated(
        "Use purchase() and PurchaseParams.Builder instead",
        ReplaceWith("purchase()"),
    )
    fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package,
        upgradeInfo: UpgradeInfo,
        callback: ProductChangeCallback,
    ) {
        startDeprecatedProductChange(
            activity,
            packageToPurchase.product.purchasingData,
            packageToPurchase.offering,
            upgradeInfo.oldSku,
            GoogleReplacementMode.fromPlayBillingClientMode(upgradeInfo.prorationMode),
            callback,
        )
    }

    /**
     * Purchase a [Package]. If purchasing a subscription, it will choose the default [SubscriptionOption].
     *
     * The default [SubscriptionOption] logic:
     *   - Filters out offers with "rc-ignore-offer" tag
     *   - Uses [SubscriptionOption] WITH longest free trial or cheapest first phase
     *   - Falls back to use base plan
     *
     * @param [activity] Current activity
     * @param [packageToPurchase] The Package you wish to purchase
     * @param [listener] The listener that will be called when purchase completes.
     */
    @Deprecated(
        "Use purchase() and PurchaseParams.Builder instead",
        ReplaceWith("purchase()"),
    )
    fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package,
        listener: PurchaseCallback,
    ) {
        startPurchase(
            activity,
            packageToPurchase.product.purchasingData,
            packageToPurchase.offering,
            null,
            listener,
        )
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
     * @param [callback] The listener that will be called when purchase restore completes.
     */
    fun restorePurchases(
        callback: ReceiveCustomerInfoCallback,
    ) {
        log(LogIntent.DEBUG, RestoreStrings.RESTORING_PURCHASE)
        if (!allowSharingPlayStoreAccount) {
            log(LogIntent.WARNING, RestoreStrings.SHARING_ACC_RESTORE_FALSE)
        }

        val appUserID = identityManager.currentAppUserID

        billing.queryAllPurchases(
            appUserID,
            onReceivePurchaseHistory = { allPurchases ->
                if (allPurchases.isEmpty()) {
                    getCustomerInfo(callback)
                } else {
                    allPurchases.sortedBy { it.purchaseTime }.let { sortedByTime ->
                        sortedByTime.forEach { purchase ->
                            postReceiptHelper.postTransactionAndConsumeIfNeeded(
                                purchase = purchase,
                                storeProduct = null,
                                isRestore = true,
                                appUserID = appUserID,
                                onSuccess = { _, info ->
                                    log(LogIntent.DEBUG, RestoreStrings.PURCHASE_RESTORED.format(purchase))
                                    if (sortedByTime.last() == purchase) {
                                        dispatch { callback.onReceived(info) }
                                    }
                                },
                                onError = { _, error ->
                                    log(
                                        LogIntent.RC_ERROR,
                                        RestoreStrings.RESTORING_PURCHASE_ERROR
                                            .format(purchase, error),
                                    )
                                    if (sortedByTime.last() == purchase) {
                                        dispatch { callback.onError(error) }
                                    }
                                },
                            )
                        }
                    }
                }
            },
            onReceivePurchaseHistoryError = { error ->
                dispatch { callback.onError(error) }
            },
        )
    }

    /**
     * This function will change the current appUserID.
     * Typically this would be used after a log out to identify a new user without calling configure
     * @param newAppUserID The new appUserID that should be linked to the currently user
     * @param [callback] An optional listener to listen for successes or errors.
     */
    @JvmOverloads
    fun logIn(
        newAppUserID: String,
        callback: LogInCallback? = null,
    ) {
        identityManager.currentAppUserID.takeUnless { it == newAppUserID }?.let {
            identityManager.logIn(
                newAppUserID,
                onSuccess = { customerInfo, created ->
                    dispatch {
                        callback?.onReceived(customerInfo, created)
                        customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(customerInfo)
                    }
                    offeringsManager.fetchAndCacheOfferings(newAppUserID, state.appInBackground)
                },
                onError = { error ->
                    dispatch { callback?.onError(error) }
                },
            )
        }
            ?: customerInfoHelper.retrieveCustomerInfo(
                identityManager.currentAppUserID,
                CacheFetchPolicy.default(),
                state.appInBackground,
                receiveCustomerInfoCallback(
                    onSuccess = { customerInfo ->
                        dispatch { callback?.onReceived(customerInfo, false) }
                    },
                    onError = { error ->
                        dispatch { callback?.onError(error) }
                    },
                ),
            )
    }

    /**
     * Resets the Purchases client clearing the save appUserID. This will generate a random user
     * id and save it in the cache.
     * @param [callback] An optional listener to listen for successes or errors.
     */
    @JvmOverloads
    fun logOut(callback: ReceiveCustomerInfoCallback? = null) {
        identityManager.logOut { error ->
            if (error != null) {
                callback?.onError(error)
            } else {
                backend.clearCaches()
                synchronized(this@Purchases) {
                    state = state.copy(purchaseCallbacksByProductId = emptyMap())
                }
                updateAllCaches(identityManager.currentAppUserID, callback)
            }
        }
    }

    /**
     * Call close when you are done with this instance of Purchases
     */
    fun close() {
        synchronized(this@Purchases) {
            state = state.copy(purchaseCallbacksByProductId = emptyMap())
        }
        this.backend.close()

        billing.close()
        updatedCustomerInfoListener = null // Do not call on state since the setter does more stuff

        dispatch {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleHandler)
        }
    }

    /**
     * Get latest available customer info.
     * @param callback A listener called when purchaser info is available and not stale.
     * Called immediately if purchaser info is cached. Purchaser info can be null if an error occurred.
     */
    fun getCustomerInfo(
        callback: ReceiveCustomerInfoCallback,
    ) {
        getCustomerInfo(CacheFetchPolicy.default(), callback)
    }

    /**
     * Get latest available customer info.
     * @param fetchPolicy Specifies cache behavior for customer info retrieval
     * @param callback A listener called when purchaser info is available and not stale.
     * Purchaser info can be null if an error occurred.
     */
    fun getCustomerInfo(
        fetchPolicy: CacheFetchPolicy,
        callback: ReceiveCustomerInfoCallback,
    ) {
        customerInfoHelper.retrieveCustomerInfo(
            identityManager.currentAppUserID,
            fetchPolicy,
            state.appInBackground,
            callback,
        )
    }

    /**
     * Call this when you are finished using the [UpdatedCustomerInfoListener]. You should call this
     * to avoid memory leaks.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun removeUpdatedCustomerInfoListener() {
        // Don't set on state directly since setter does more things
        this.updatedCustomerInfoListener = null
    }

    /**
     * Invalidates the cache for customer information.
     *
     * Most apps will not need to use this method; invalidating the cache can leave your app in an invalid state.
     * Refer to https://rev.cat/customer-info-cache for more information on
     * using the cache properly.
     *
     * This is useful for cases where purchaser information might have been updated outside of the
     * app, like if a promotional subscription is granted through the RevenueCat dashboard.
     */
    fun invalidateCustomerInfoCache() {
        log(LogIntent.DEBUG, CustomerInfoStrings.INVALIDATING_CUSTOMERINFO_CACHE)
        deviceCache.clearCustomerInfoCache(appUserID)
    }

    // region Subscriber Attributes
    // region Special Attributes

    /**
     * Subscriber attributes are useful for storing additional, structured information on a user.
     * Since attributes are writable using a public key they should not be used for
     * managing secure or sensitive information such as subscription status, coins, etc.
     *
     * Key names starting with "$" are reserved names used by RevenueCat. For a full list of key
     * restrictions refer to our guide: https://docs.revenuecat.com/docs/subscriber-attributes
     *
     * @param attributes Map of attributes by key. Set the value as null to delete an attribute.
     */
    fun setAttributes(attributes: Map<String, String?>) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setAttributes"))
        subscriberAttributesManager.setAttributes(attributes, appUserID)
    }

    /**
     * Subscriber attribute associated with the Email address for the user
     *
     * @param email Null or empty will delete the subscriber attribute.
     */
    fun setEmail(email: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setEmail"))
        subscriberAttributesManager.setAttribute(SubscriberAttributeKey.Email, email, appUserID)
    }

    /**
     * Subscriber attribute associated with the phone number for the user
     *
     * @param phoneNumber Null or empty will delete the subscriber attribute.
     */
    fun setPhoneNumber(phoneNumber: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setPhoneNumber"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.PhoneNumber,
            phoneNumber,
            appUserID,
        )
    }

    /**
     * Subscriber attribute associated with the display name for the user
     *
     * @param displayName Null or empty will delete the subscriber attribute.
     */
    fun setDisplayName(displayName: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setDisplayName"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.DisplayName,
            displayName,
            appUserID,
        )
    }

    /**
     * Subscriber attribute associated with the push token for the user
     *
     * @param fcmToken Null or empty will delete the subscriber attribute.
     */
    fun setPushToken(fcmToken: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setPushToken"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.FCMTokens,
            fcmToken,
            appUserID,
        )
    }

    // endregion
    // region Integration IDs

    /**
     * Subscriber attribute associated with the Mixpanel Distinct ID for the user
     *
     * @param mixpanelDistinctID null or an empty string will delete the subscriber attribute.
     */
    fun setMixpanelDistinctID(mixpanelDistinctID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setMixpanelDistinctID"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.MixpanelDistinctId,
            mixpanelDistinctID,
            appUserID,
        )
    }

    /**
     * Subscriber attribute associated with the OneSignal Player Id for the user
     * Required for the RevenueCat OneSignal integration
     *
     * @param onesignalID null or an empty string will delete the subscriber attribute
     */
    fun setOnesignalID(onesignalID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setOnesignalID"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.OneSignal,
            onesignalID,
            appUserID,
        )
    }

    /**
     * Subscriber attribute associated with the Airship Channel ID
     * Required for the RevenueCat Airship integration
     *
     * @param airshipChannelID null or an empty string will delete the subscriber attribute
     */
    fun setAirshipChannelID(airshipChannelID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setAirshipChannelID"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.Airship,
            airshipChannelID,
            appUserID,
        )
    }

    /**
     * Subscriber attribute associated with the Firebase App Instance ID for the user
     * Required for the RevenueCat Firebase integration
     *
     * @param firebaseAppInstanceID null or an empty string will delete the subscriber attribute.
     */
    fun setFirebaseAppInstanceID(firebaseAppInstanceID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setFirebaseAppInstanceID"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.FirebaseAppInstanceId,
            firebaseAppInstanceID,
            appUserID,
        )
    }

    // endregion
    // region Attribution IDs

    /**
     * Automatically collect subscriber attributes associated with the device identifiers
     * $gpsAdId, $androidId, $ip
     *
     * @warning In accordance with [Google Play's data safety guidelines] (https://rev.cat/google-plays-data-safety),
     * you should not be calling this function if your app targets children.
     *
     * @warning You must declare the [AD_ID Permission](https://rev.cat/google-advertising-id) when your app targets
     * Android 13 or above. Apps that don’t declare the permission will get a string of zeros.
     */
    fun collectDeviceIdentifiers() {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("collectDeviceIdentifiers"))
        subscriberAttributesManager.collectDeviceIdentifiers(appUserID, application)
    }

    /**
     * Subscriber attribute associated with the Adjust Id for the user
     * Required for the RevenueCat Adjust integration
     *
     * @param adjustID null or an empty string will delete the subscriber attribute
     */
    fun setAdjustID(adjustID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setAdjustID"))
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Adjust,
            adjustID,
            appUserID,
            application,
        )
    }

    /**
     * Subscriber attribute associated with the AppsFlyer Id for the user
     * Required for the RevenueCat AppsFlyer integration
     *
     * @param appsflyerID null or an empty string will delete the subscriber attribute
     */
    fun setAppsflyerID(appsflyerID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setAppsflyerID"))
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.AppsFlyer,
            appsflyerID,
            appUserID,
            application,
        )
    }

    /**
     * Subscriber attribute associated with the Facebook SDK Anonymous Id for the user
     * Recommended for the RevenueCat Facebook integration
     *
     * @param fbAnonymousID null or an empty string will delete the subscriber attribute
     */
    fun setFBAnonymousID(fbAnonymousID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setFBAnonymousID"))
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Facebook,
            fbAnonymousID,
            appUserID,
            application,
        )
    }

    /**
     * Subscriber attribute associated with the mParticle Id for the user
     * Recommended for the RevenueCat mParticle integration
     *
     * @param mparticleID null or an empty string will delete the subscriber attribute
     */
    fun setMparticleID(mparticleID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setMparticleID"))
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Mparticle,
            mparticleID,
            appUserID,
            application,
        )
    }

    /**
     * Subscriber attribute associated with the CleverTap ID for the user
     * Required for the RevenueCat CleverTap integration
     *
     * @param cleverTapID null or an empty string will delete the subscriber attribute.
     */
    fun setCleverTapID(cleverTapID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setCleverTapID"))
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.CleverTap,
            cleverTapID,
            appUserID,
            application,
        )
    }

    // endregion
    // region Campaign parameters

    /**
     * Subscriber attribute associated with the install media source for the user
     *
     * @param mediaSource null or an empty string will delete the subscriber attribute.
     */
    fun setMediaSource(mediaSource: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setMediaSource"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.MediaSource,
            mediaSource,
            appUserID,
        )
    }

    /**
     * Subscriber attribute associated with the install campaign for the user
     *
     * @param campaign null or an empty string will delete the subscriber attribute.
     */
    fun setCampaign(campaign: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setCampaign"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Campaign,
            campaign,
            appUserID,
        )
    }

    /**
     * Subscriber attribute associated with the install ad group for the user
     *
     * @param adGroup null or an empty string will delete the subscriber attribute.
     */
    fun setAdGroup(adGroup: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setAdGroup"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.AdGroup,
            adGroup,
            appUserID,
        )
    }

    /**
     * Subscriber attribute associated with the install ad for the user
     *
     * @param ad null or an empty string will delete the subscriber attribute.
     */
    fun setAd(ad: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setAd"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Ad,
            ad,
            appUserID,
        )
    }

    /**
     * Subscriber attribute associated with the install keyword for the user
     *
     * @param keyword null or an empty string will delete the subscriber attribute.
     */
    fun setKeyword(keyword: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("seKeyword"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Keyword,
            keyword,
            appUserID,
        )
    }

    /**
     * Subscriber attribute associated with the install ad creative for the user
     *
     * @param creative null or an empty string will delete the subscriber attribute.
     */
    fun setCreative(creative: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setCreative"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Creative,
            creative,
            appUserID,
        )
    }

    //endregion
    //endregion
    //endregion

    // region Private Methods

    private fun getProductsOfTypes(
        productIds: Set<String>,
        types: Set<ProductType>,
        callback: GetStoreProductsCallback,
    ) {
        val validTypes = types.filter { it != ProductType.UNKNOWN }.toSet()
        getProductsOfTypes(productIds, validTypes, emptyList(), callback)
    }

    private fun getProductsOfTypes(
        productIds: Set<String>,
        types: Set<ProductType>,
        collectedStoreProducts: List<StoreProduct>,
        callback: GetStoreProductsCallback,
    ) {
        val typesRemaining = types.toMutableSet()
        val type = typesRemaining.firstOrNull()?.also { typesRemaining.remove(it) }

        type?.let {
            billing.queryProductDetailsAsync(
                it,
                productIds,
                { storeProducts ->
                    dispatch {
                        getProductsOfTypes(
                            productIds,
                            typesRemaining,
                            collectedStoreProducts + storeProducts,
                            callback,
                        )
                    }
                },
                {
                    dispatch {
                        callback.onError(it)
                    }
                },
            )
        } ?: run {
            callback.onReceived(collectedStoreProducts)
        }
    }

    private fun updateAllCaches(
        appUserID: String,
        completion: ReceiveCustomerInfoCallback? = null,
    ) {
        state.appInBackground.let { appInBackground ->
            customerInfoHelper.retrieveCustomerInfo(
                appUserID,
                CacheFetchPolicy.FETCH_CURRENT,
                appInBackground,
                completion,
            )
            offeringsManager.fetchAndCacheOfferings(appUserID, appInBackground)
        }
    }

    private fun postPurchases(
        purchases: List<StoreTransaction>,
        allowSharingPlayStoreAccount: Boolean,
        appUserID: String,
        onSuccess: (SuccessfulPurchaseCallback)? = null,
        onError: (ErrorPurchaseCallback)? = null,
    ) {
        purchases.forEach { purchase ->
            if (purchase.purchaseState != PurchaseState.PENDING) {
                billing.queryProductDetailsAsync(
                    productType = purchase.type,
                    productIds = purchase.productIds.toSet(),
                    onReceive = { storeProducts ->

                        val purchasedStoreProduct = if (purchase.type == ProductType.SUBS) {
                            storeProducts.firstOrNull { product ->
                                product.subscriptionOptions?.let { subscriptionOptions ->
                                    subscriptionOptions.any { it.id == purchase.subscriptionOptionId }
                                } ?: false
                            }
                        } else {
                            storeProducts.firstOrNull { product ->
                                product.id == purchase.productIds.firstOrNull()
                            }
                        }

                        postReceiptHelper.postTransactionAndConsumeIfNeeded(
                            purchase = purchase,
                            storeProduct = purchasedStoreProduct,
                            isRestore = allowSharingPlayStoreAccount,
                            appUserID = appUserID,
                            onSuccess = onSuccess,
                            onError = onError,
                        )
                    },
                    onError = {
                        postReceiptHelper.postTransactionAndConsumeIfNeeded(
                            purchase = purchase,
                            storeProduct = null,
                            isRestore = allowSharingPlayStoreAccount,
                            appUserID = appUserID,
                            onSuccess = onSuccess,
                            onError = onError,
                        )
                    },
                )
            } else {
                onError?.invoke(
                    purchase,
                    PurchasesError(PurchasesErrorCode.PaymentPendingError).also { errorLog(it) },
                )
            }
        }
    }

    private fun dispatch(action: () -> Unit) {
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            val handler = mainHandler ?: Handler(Looper.getMainLooper())
            handler.post(action)
        } else {
            action()
        }
    }

    private fun getPurchaseCallback(productId: String): PurchaseCallback? {
        return state.purchaseCallbacksByProductId[productId].also {
            state = state.copy(
                purchaseCallbacksByProductId = state.purchaseCallbacksByProductId.filterNot { it.key == productId },
            )
        }
    }

    private fun getAndClearProductChangeCallback(): ProductChangeCallback? {
        return state.deprecatedProductChangeCallback.also {
            state = state.copy(deprecatedProductChangeCallback = null)
        }
    }

    private fun getPurchasesUpdatedListener(): BillingAbstract.PurchasesUpdatedListener {
        return object : BillingAbstract.PurchasesUpdatedListener {
            override fun onPurchasesUpdated(purchases: List<StoreTransaction>) {
                val isDeprecatedProductChangeInProgress: Boolean
                val callbackPair: Pair<SuccessfulPurchaseCallback, ErrorPurchaseCallback>
                val deprecatedProductChangeListener: ProductChangeCallback?

                synchronized(this@Purchases) {
                    isDeprecatedProductChangeInProgress = state.deprecatedProductChangeCallback != null
                    if (isDeprecatedProductChangeInProgress) {
                        deprecatedProductChangeListener = getAndClearProductChangeCallback()
                        callbackPair = getProductChangeCompletedCallbacks(deprecatedProductChangeListener)
                    } else {
                        deprecatedProductChangeListener = null
                        callbackPair = getPurchaseCompletedCallbacks()
                    }
                }

                postPurchases(
                    purchases,
                    allowSharingPlayStoreAccount,
                    appUserID,
                    onSuccess = callbackPair.first,
                    onError = callbackPair.second,
                )
            }

            override fun onPurchasesFailedToUpdate(purchasesError: PurchasesError) {
                synchronized(this@Purchases) {
                    getAndClearProductChangeCallback()?.dispatch(purchasesError)
                        ?: getAndClearAllPurchaseCallbacks().forEach { it.dispatch(purchasesError) }
                }
            }
        }
    }

    private fun getAndClearAllPurchaseCallbacks(): List<PurchaseCallback> {
        synchronized(this@Purchases) {
            state.purchaseCallbacksByProductId.let { purchaseCallbacks ->
                state = state.copy(purchaseCallbacksByProductId = emptyMap())
                return@getAndClearAllPurchaseCallbacks purchaseCallbacks.values.toList()
            }
        }
    }

    private fun getPurchaseCompletedCallbacks(): Pair<SuccessfulPurchaseCallback, ErrorPurchaseCallback> {
        val onSuccess: SuccessfulPurchaseCallback = { storeTransaction, info ->
            getPurchaseCallback(storeTransaction.productIds[0])?.let { purchaseCallback ->
                dispatch {
                    purchaseCallback.onCompleted(storeTransaction, info)
                }
            }
        }
        val onError: ErrorPurchaseCallback = { purchase, error ->
            getPurchaseCallback(purchase.productIds[0])?.dispatch(error)
        }

        return Pair(onSuccess, onError)
    }

    private fun getProductChangeCompletedCallbacks(
        productChangeListener: ProductChangeCallback?,
    ): Pair<SuccessfulPurchaseCallback, ErrorPurchaseCallback> {
        val onSuccess: SuccessfulPurchaseCallback = { storeTransaction, info ->
            productChangeListener?.let { productChangeCallback ->
                dispatch {
                    productChangeCallback.onCompleted(storeTransaction, info)
                }
            }
        }
        val onError: ErrorPurchaseCallback = { _, error ->
            productChangeListener?.dispatch(error)
        }
        return Pair(onSuccess, onError)
    }

    private fun PurchaseErrorCallback.dispatch(error: PurchasesError) {
        dispatch {
            onError(
                error,
                error.code == PurchasesErrorCode.PurchaseCancelledError,
            )
        }
    }

    private fun startPurchase(
        activity: Activity,
        purchasingData: PurchasingData,
        presentedOfferingIdentifier: String?,
        isPersonalizedPrice: Boolean?,
        listener: PurchaseCallback,
    ) {
        log(
            LogIntent.PURCHASE,
            PurchaseStrings.PURCHASE_STARTED.format(
                " $purchasingData ${
                    presentedOfferingIdentifier?.let {
                        PurchaseStrings.OFFERING + "$presentedOfferingIdentifier"
                    }
                }",
            ),
        )
        var userPurchasing: String? = null // Avoids race condition for userid being modified before purchase is made
        synchronized(this@Purchases) {
            if (!appConfig.finishTransactions) {
                log(LogIntent.WARNING, PurchaseStrings.PURCHASE_FINISH_TRANSACTION_FALSE)
            }
            if (!state.purchaseCallbacksByProductId.containsKey(purchasingData.productId)) {
                val mapOfProductIdToListener = mapOf(purchasingData.productId to listener)
                state = state.copy(
                    purchaseCallbacksByProductId = state.purchaseCallbacksByProductId + mapOfProductIdToListener,
                )
                userPurchasing = identityManager.currentAppUserID
            }
        }

        userPurchasing?.let { appUserID ->
            billing.makePurchaseAsync(
                activity,
                appUserID,
                purchasingData,
                null,
                presentedOfferingIdentifier,
                isPersonalizedPrice,
            )
        } ?: listener.dispatch(PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError).also { errorLog(it) })
    }

    private fun startProductChange(
        activity: Activity,
        purchasingData: PurchasingData,
        offeringIdentifier: String?,
        oldProductId: String,
        googleReplacementMode: GoogleReplacementMode,
        isPersonalizedPrice: Boolean?,
        purchaseCallback: PurchaseCallback,
    ) {
        if (purchasingData.productType != ProductType.SUBS) {
            purchaseCallback.dispatch(
                PurchasesError(
                    PurchasesErrorCode.PurchaseNotAllowedError,
                    PurchaseStrings.UPGRADING_INVALID_TYPE,
                ).also { errorLog(it) },
            )
            return
        }

        log(
            LogIntent.PURCHASE,
            PurchaseStrings.PRODUCT_CHANGE_STARTED.format(
                " $purchasingData ${
                    offeringIdentifier?.let {
                        PurchaseStrings.OFFERING + "$offeringIdentifier"
                    }
                } oldProductId: $oldProductId googleReplacementMode $googleReplacementMode",

            ),
        )
        var userPurchasing: String? = null // Avoids race condition for userid being modified before purchase is made
        synchronized(this@Purchases) {
            if (!appConfig.finishTransactions) {
                log(LogIntent.WARNING, PurchaseStrings.PURCHASE_FINISH_TRANSACTION_FALSE)
            }

            if (!state.purchaseCallbacksByProductId.containsKey(purchasingData.productId)) {
                // When using DEFERRED proration mode, callback needs to be associated with the *old* product we are
                // switching from, because the transaction we receive on successful purchase is for the old product.
                //
                // TODO: This may need to change because of the new deferred behavior in BC6
                val productId =
                    if (googleReplacementMode == GoogleReplacementMode.DEFERRED) {
                        oldProductId
                    } else {
                        purchasingData.productId
                    }
                val mapOfProductIdToListener = mapOf(productId to purchaseCallback)
                state = state.copy(
                    purchaseCallbacksByProductId = state.purchaseCallbacksByProductId + mapOfProductIdToListener,
                )
                userPurchasing = identityManager.currentAppUserID
            }
        }
        userPurchasing?.let { appUserID ->
            replaceOldPurchaseWithNewProduct(
                purchasingData,
                oldProductId,
                googleReplacementMode,
                activity,
                appUserID,
                offeringIdentifier,
                isPersonalizedPrice,
                purchaseCallback,
            )
        } ?: run {
            val operationInProgressError = PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError).also {
                errorLog(it)
            }
            getAndClearAllPurchaseCallbacks().forEach { it.dispatch(operationInProgressError) }
        }
    }

    private fun startDeprecatedProductChange(
        activity: Activity,
        purchasingData: PurchasingData,
        offeringIdentifier: String?,
        oldProductId: String,
        googleReplacementMode: GoogleReplacementMode?,
        listener: ProductChangeCallback,
    ) {
        if (purchasingData.productType != ProductType.SUBS) {
            getAndClearProductChangeCallback()
            listener.dispatch(
                PurchasesError(
                    PurchasesErrorCode.PurchaseNotAllowedError,
                    PurchaseStrings.UPGRADING_INVALID_TYPE,
                ).also { errorLog(it) },
            )
            return
        }

        log(
            LogIntent.PURCHASE,
            PurchaseStrings.PRODUCT_CHANGE_STARTED.format(
                " $purchasingData ${
                    offeringIdentifier?.let {
                        PurchaseStrings.OFFERING + "$offeringIdentifier"
                    }
                } oldProductId: $oldProductId googleReplacementMode $googleReplacementMode",
            ),
        )
        var userPurchasing: String? = null // Avoids race condition for userid being modified before purchase is made
        synchronized(this@Purchases) {
            if (!appConfig.finishTransactions) {
                log(LogIntent.WARNING, PurchaseStrings.PURCHASE_FINISH_TRANSACTION_FALSE)
            }
            if (state.deprecatedProductChangeCallback == null) {
                state = state.copy(deprecatedProductChangeCallback = listener)
                userPurchasing = identityManager.currentAppUserID
            }
        }
        userPurchasing?.let { appUserID ->
            replaceOldPurchaseWithNewProduct(
                purchasingData,
                oldProductId,
                googleReplacementMode,
                activity,
                appUserID,
                offeringIdentifier,
                null,
                listener,
            )
        } ?: run {
            getAndClearProductChangeCallback()
            listener.dispatch(PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError).also { errorLog(it) })
        }
    }

    private fun replaceOldPurchaseWithNewProduct(
        purchasingData: PurchasingData,
        oldProductId: String,
        googleReplacementMode: GoogleReplacementMode?,
        activity: Activity,
        appUserID: String,
        presentedOfferingIdentifier: String?,
        isPersonalizedPrice: Boolean?,
        listener: PurchaseErrorCallback,
    ) {
        if (purchasingData.productType != ProductType.SUBS) {
            val invalidProductChangeTypeError = PurchasesError(
                PurchasesErrorCode.PurchaseNotAllowedError,
                PurchaseStrings.UPGRADING_INVALID_TYPE,
            ).also { errorLog(it) }
            getAndClearProductChangeCallback()?.dispatch(invalidProductChangeTypeError)
            getAndClearAllPurchaseCallbacks().forEach { it.dispatch(invalidProductChangeTypeError) }
            return
        }

        billing.findPurchaseInPurchaseHistory(
            appUserID,
            ProductType.SUBS,
            oldProductId,
            onCompletion = { purchaseRecord ->
                log(LogIntent.PURCHASE, PurchaseStrings.FOUND_EXISTING_PURCHASE.format(oldProductId))

                billing.makePurchaseAsync(
                    activity,
                    appUserID,
                    purchasingData,
                    ReplaceProductInfo(purchaseRecord, googleReplacementMode),
                    presentedOfferingIdentifier,
                    isPersonalizedPrice,
                )
            },
            onError = { error ->
                log(LogIntent.GOOGLE_ERROR, error.toString())
                getAndClearProductChangeCallback()
                getAndClearAllPurchaseCallbacks()
                listener.dispatch(error)
            },
        )
    }

    @JvmSynthetic
    internal fun updatePendingPurchaseQueue() {
        if (!appConfig.dangerousSettings.autoSyncPurchases) {
            log(LogIntent.DEBUG, PurchaseStrings.SKIPPING_AUTOMATIC_SYNC)
            return
        }
        if (billing.isConnected()) {
            log(LogIntent.DEBUG, PurchaseStrings.UPDATING_PENDING_PURCHASE_QUEUE)
            dispatcher.enqueue({
                appUserID.let { appUserID ->
                    billing.queryPurchases(
                        appUserID,
                        onSuccess = { purchasesByHashedToken ->
                            purchasesByHashedToken.forEach { (hash, purchase) ->
                                log(
                                    LogIntent.DEBUG,
                                    RestoreStrings.QUERYING_PURCHASE_WITH_HASH.format(purchase.type, hash),
                                )
                            }
                            deviceCache.cleanPreviouslySentTokens(purchasesByHashedToken.keys)
                            postPurchases(
                                deviceCache.getActivePurchasesNotInCache(purchasesByHashedToken),
                                allowSharingPlayStoreAccount,
                                appUserID,
                            )
                        },
                        onError = { error ->
                            log(LogIntent.GOOGLE_ERROR, error.toString())
                        },
                    )
                }
            })
        } else {
            log(LogIntent.DEBUG, PurchaseStrings.BILLING_CLIENT_NOT_CONNECTED)
        }
    }

    private fun synchronizeSubscriberAttributesIfNeeded() {
        subscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserID)
    }

    // endregion

    // region Deprecated

    /**
     * If it should allow sharing Play Store accounts. False by
     * default. If true treats all purchases as restores, aliasing together appUserIDs that share a
     * Play Store account.
     */
    @Deprecated(
        "Replaced with configuration in the RevenueCat dashboard",
        ReplaceWith("configure through the RevenueCat dashboard"),
    )
    var allowSharingPlayStoreAccount: Boolean
        @Synchronized get() =
            state.allowSharingPlayStoreAccount ?: identityManager.currentUserIsAnonymous()

        @Synchronized set(value) {
            state = state.copy(allowSharingPlayStoreAccount = value)
        }

    /**
     * Gets the StoreProduct for the given list of subscription products.
     * @param [productIds] List of productIds
     * @param [callback] Response callback
     */
    @Deprecated(
        "Replaced with getProducts() which returns both subscriptions and non-subscriptions",
        ReplaceWith("getProducts()"),
    )
    fun getSubscriptionSkus(
        productIds: List<String>,
        callback: GetStoreProductsCallback,
    ) {
        getProductsOfTypes(productIds.toSet(), setOf(ProductType.SUBS), callback)
    }

    /**
     * Gets the StoreProduct for the given list of non-subscription products.
     * @param [productIds] List of productIds
     * @param [callback] Response callback
     */
    @Deprecated(
        "Replaced with getProducts() which returns both subscriptions and non-subscriptions",
        ReplaceWith("getProducts()"),
    )
    fun getNonSubscriptionSkus(
        productIds: List<String>,
        callback: GetStoreProductsCallback,
    ) {
        getProductsOfTypes(productIds.toSet(), setOf(ProductType.INAPP), callback)
    }

    // endregion

    // region Static
    companion object {

        /**
         * DO NOT MODIFY. This is used internally by the Hybrid SDKs to indicate which platform is
         * being used
         */
        @JvmStatic
        var platformInfo: PlatformInfo = PlatformInfo(
            flavor = "native",
            version = null,
        )

        /**
         * Enable debug logging. Useful for debugging issues with the lovely team @RevenueCat
         */
        @JvmStatic
        @Deprecated(message = "Use logLevel instead")
        var debugLogsEnabled
            get() = logLevel.debugLogsEnabled
            set(value) { logLevel = LogLevel.debugLogsEnabled(value) }

        /**
         * Configure log level. Useful for debugging issues with the lovely team @RevenueCat
         * By default, LogLevel.DEBUG in debug builds, and LogLevel.INFO in release builds.
         */
        @JvmStatic
        var logLevel: LogLevel
            get() = Config.logLevel
            set(value) { Config.logLevel = value }

        /**
         * Set a custom log handler for redirecting logs to your own logging system.
         * Defaults to [android.util.Log].
         *
         * By default, this sends info, warning, and error messages.
         * If you wish to receive Debug level messages, see [debugLogsEnabled].
         */
        @JvmStatic
        var logHandler: LogHandler
            @Synchronized get() = currentLogHandler

            @Synchronized set(value) {
                currentLogHandler = value
            }

        @JvmSynthetic
        internal var backingFieldSharedInstance: Purchases? = null

        /**
         * Singleton instance of Purchases. [configure] will set this
         * @return A previously set singleton Purchases instance
         * @throws UninitializedPropertyAccessException if the shared instance has not been configured.
         */
        @JvmStatic
        var sharedInstance: Purchases
            get() =
                backingFieldSharedInstance
                    ?: throw UninitializedPropertyAccessException(ConfigureStrings.NO_SINGLETON_INSTANCE)

            @VisibleForTesting(otherwise = VisibleForTesting.NONE)
            internal set(value) {
                backingFieldSharedInstance?.close()
                backingFieldSharedInstance = value
            }

        /**
         * Current version of the Purchases SDK
         */
        @JvmStatic
        val frameworkVersion = Config.frameworkVersion

        /**
         * Set this property to your proxy URL before configuring Purchases *only*
         * if you've received a proxy key value from your RevenueCat contact.
         */
        @JvmStatic
        var proxyURL: URL? = null

        /**
         * True if [configure] has been called and [Purchases.sharedInstance] is set
         */
        @JvmStatic
        val isConfigured: Boolean
            get() = this.backingFieldSharedInstance != null

        /**
         * Configures an instance of the Purchases SDK with a specified API key. The instance will
         * be set as a singleton. You should access the singleton instance using [Purchases.sharedInstance]
         * @param configuration TODO
         * @return An instantiated `[Purchases] object that has been set as a singleton.
         */
        @JvmStatic
        fun configure(
            configuration: PurchasesConfiguration,
        ): Purchases {
            if (isConfigured) {
                infoLog(ConfigureStrings.INSTANCE_ALREADY_EXISTS)
            }
            return PurchasesFactory().createPurchases(
                configuration,
                platformInfo,
                proxyURL,
            ).also {
                @SuppressLint("RestrictedApi")
                sharedInstance = it
            }
        }

        /**
         * Note: This method only works for the Google Play Store. There is no Amazon equivalent at this time.
         * Calling from an Amazon-configured app will return true.
         *
         * Check if billing is supported for the current Play user (meaning IN-APP purchases are supported)
         * and optionally, whether all features in the list of specified feature types are supported. This method is
         * asynchronous since it requires a connected BillingClient.
         * @param context A context object that will be used to connect to the billing client
         * @param features A list of feature types to check for support. Feature types must be one of [BillingFeature]
         *                 By default, is an empty list and no specific feature support will be checked.
         * @param callback Callback that will be notified when the check is complete.
         */
        @JvmStatic
        @JvmOverloads
        fun canMakePayments(
            context: Context,
            features: List<BillingFeature> = listOf(),
            callback: Callback<Boolean>,
        ) {
            val currentStore = sharedInstance.appConfig.store
            if (currentStore != Store.PLAY_STORE) {
                log(LogIntent.RC_ERROR, BillingStrings.CANNOT_CALL_CAN_MAKE_PAYMENTS)
                callback.onReceived(true)
                return
            }

            BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener { _, _ -> }
                .build()
                .let { billingClient ->
                    // BillingClient 4 calls the listener functions in a thread instead of in main
                    // https://github.com/RevenueCat/purchases-android/issues/348
                    val mainHandler = Handler(context.mainLooper)
                    billingClient.startConnection(
                        object : BillingClientStateListener {
                            override fun onBillingSetupFinished(billingResult: BillingResult) {
                                mainHandler.post {
                                    try {
                                        if (!billingResult.isSuccessful()) {
                                            callback.onReceived(false)
                                            billingClient.endConnection()
                                            return@post
                                        }
                                        // If billing is supported, IN-APP purchases are supported.
                                        val featureSupportedResultOk = features.all {
                                            billingClient.isFeatureSupported(it.playBillingClientName).isSuccessful()
                                        }

                                        billingClient.endConnection()

                                        callback.onReceived(featureSupportedResultOk)
                                    } catch (e: IllegalArgumentException) {
                                        // Play Services not available
                                        callback.onReceived(false)
                                    }
                                }
                            }

                            override fun onBillingServiceDisconnected() {
                                mainHandler.post {
                                    try {
                                        billingClient.endConnection()
                                    } catch (e: IllegalArgumentException) {
                                    } finally {
                                        callback.onReceived(false)
                                    }
                                }
                            }
                        },
                    )
                }
        }
    }

    // endregion
}
