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
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Config
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.ReplaceSkuInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.createOfferings
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
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
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.BillingFeature
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.AttributionStrings
import com.revenuecat.purchases.strings.BillingStrings
import com.revenuecat.purchases.strings.ConfigureStrings
import com.revenuecat.purchases.strings.ConfigureStrings.AUTO_SYNC_PURCHASES_DISABLED
import com.revenuecat.purchases.strings.CustomerInfoStrings
import com.revenuecat.purchases.strings.OfferingStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.getAttributeErrors
import com.revenuecat.purchases.subscriberattributes.toBackendMap
import org.json.JSONException
import org.json.JSONObject
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
    // This is nullable due to: https://github.com/RevenueCat/purchases-flutter/issues/408
    private val mainHandler: Handler? = Handler(Looper.getMainLooper())
) : LifecycleDelegate {

    /** @suppress */
    @Suppress("RedundantGetter", "RedundantSetter")
    @Volatile
    @JvmSynthetic
    internal var state = PurchasesState()
        @JvmSynthetic @Synchronized get() = field
        @JvmSynthetic @Synchronized set(value) {
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
                appInBackground = false
            )
        }
        if (deviceCache.isOfferingsCacheStale(appInBackground = false)) {
            log(LogIntent.DEBUG, OfferingStrings.OFFERINGS_STALE_UPDATING_IN_FOREGROUND)
            fetchAndCacheOfferings(identityManager.currentAppUserID, appInBackground = false)
            log(LogIntent.RC_SUCCESS, OfferingStrings.OFFERINGS_UPDATED_FROM_NETWORK)
        }
        updatePendingPurchaseQueue()
        synchronizeSubscriberAttributesIfNeeded()
    }

    // region Public Methods

    /**
     * This method will send all the purchases to the RevenueCat backend. Call this when using your own implementation
     * for subscriptions anytime a sync is needed, like after a successful purchase, or when migrating existing
     * users to RevenueCat
     *
     * @warning This function should only be called if you're migrating to RevenueCat or in observer mode.
     */
    fun syncPurchases() {
        log(LogIntent.DEBUG, PurchaseStrings.SYNCING_PURCHASES)

        val appUserID = identityManager.currentAppUserID

        billing.queryAllPurchases(
            appUserID,
            onReceivePurchaseHistory = { allPurchases ->
                if (allPurchases.isNotEmpty()) {
                    allPurchases.forEach { purchase ->
                        val productInfo = ReceiptInfo(productIDs = purchase.productIds)
                        syncPurchaseWithBackend(
                            purchase.purchaseToken,
                            purchase.storeUserID,
                            appUserID,
                            productInfo,
                            purchase.marketplace,
                            {
                                log(LogIntent.PURCHASE, PurchaseStrings.PURCHASE_SYNCED.format(purchase))
                            },
                            { error ->
                                log(
                                    LogIntent.RC_ERROR, PurchaseStrings.SYNCING_PURCHASES_ERROR_DETAILS
                                        .format(purchase, error)
                                )
                            }
                        )
                    }
                }
            },
            onReceivePurchaseHistoryError = {
                log(LogIntent.RC_ERROR, PurchaseStrings.SYNCING_PURCHASES_ERROR.format(it))
            }
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
        price: Double?
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

                // TODO BC5 clean up amazon postreceipt

                val receiptInfo = ReceiptInfo(
                    productIDs = listOf(normalizedProductID),
                    price = price?.takeUnless { it == 0.0 },
                    currency = isoCurrencyCode?.takeUnless { it.isBlank() }
                )
                syncPurchaseWithBackend(
                    receiptID,
                    amazonUserID,
                    appUserID,
                    receiptInfo,
                    marketplace = null,
                    {
                        val logMessage = PurchaseStrings.PURCHASE_SYNCED_USER_ID.format(receiptID, amazonUserID)
                        log(LogIntent.PURCHASE, logMessage)
                    },
                    { error ->
                        val logMessage = PurchaseStrings.SYNCING_PURCHASE_ERROR_DETAILS_USER_ID.format(
                            receiptID,
                            amazonUserID,
                            error
                        )
                        log(LogIntent.RC_ERROR, logMessage)
                    }
                )
            },
            { error ->
                val logMessage =
                    PurchaseStrings.SYNCING_PURCHASE_ERROR_DETAILS_USER_ID.format(receiptID, amazonUserID, error)
                log(LogIntent.RC_ERROR, logMessage)
            }
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
        listener: ReceiveOfferingsCallback
    ) {
        val (appUserID, cachedOfferings) = synchronized(this@Purchases) {
            identityManager.currentAppUserID to deviceCache.cachedOfferings
        }
        if (cachedOfferings == null) {
            log(LogIntent.DEBUG, OfferingStrings.NO_CACHED_OFFERINGS_FETCHING_NETWORK)
            fetchAndCacheOfferings(appUserID, state.appInBackground, listener)
        } else {
            log(LogIntent.DEBUG, OfferingStrings.VENDING_OFFERINGS_CACHE)
            dispatch {
                listener.onReceived(cachedOfferings)
            }
            state.appInBackground.let { appInBackground ->
                if (deviceCache.isOfferingsCacheStale(appInBackground)) {
                    log(
                        LogIntent.DEBUG,
                        if (appInBackground) OfferingStrings.OFFERINGS_STALE_UPDATING_IN_BACKGROUND
                        else OfferingStrings.OFFERINGS_STALE_UPDATING_IN_FOREGROUND
                    )
                    fetchAndCacheOfferings(appUserID, appInBackground)
                    log(LogIntent.RC_SUCCESS, OfferingStrings.OFFERINGS_UPDATED_FROM_NETWORK)
                }
            }
        }
    }

    /**
     * Gets the StoreProduct for the given list of subscription skus.
     * @param [skus] List of skus
     * @param [callback] Response callback
     */
    // TODO deprecate, replaced with getProducts
    fun getSubscriptionSkus(
        skus: List<String>,
        callback: GetStoreProductsCallback
    ) {
        getSkus(skus.toSet(), ProductType.SUBS, callback)
    }

    /**
     * Gets the SKUDetails for the given list of non-subscription skus.
     * @param [skus] List of skus
     * @param [callback] Response callback
     */
    // TODO deprecate, replaced with getProducts
    fun getNonSubscriptionSkus(
        skus: List<String>,
        callback: GetStoreProductsCallback
    ) {
        getSkus(skus.toSet(), ProductType.INAPP, callback)
    }

    /**
     * Make a purchase upgrading from a previous sku.
     * @param [activity] Current activity
     * @param [storeProduct] The StoreProduct of the product you wish to purchase
     * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional
     * prorationMode. Amazon Appstore doesn't support changing products so upgradeInfo is ignored for Amazon purchases.
     * @param [listener] The PurchaseCallback that will be called when purchase completes.
     */
    @Deprecated(
        "Replaced with purchaseProductOption",
        ReplaceWith("purchaseProductOption(activity, storeProduct, purchaseOption, upgradeInfo, listener)")
    )
    fun purchaseProduct(
        activity: Activity,
        storeProduct: StoreProduct,
        upgradeInfo: UpgradeInfo,
        listener: ProductChangeCallback
    ) {
        val purchaseOption = storeProduct.bestPurchaseOption
        if (purchaseOption == null) {
            // TODOBC5: Improve and move error message
            errorLog("PurchaseProduct with upgrade: Product does not have any purchase option")
            return
        }
        startProductChange(
            activity,
            storeProduct,
            purchaseOption,
            null,
            upgradeInfo,
            listener
        )
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [storeProduct] The StoreProduct of the product you wish to purchase
     * @param [callback] The PurchaseCallback that will be called when purchase completes.
     */
    @Deprecated(
        "Replaced with purchaseProductOption",
        ReplaceWith("purchaseProductOption(activity, storeProduct, purchaseOption, callback)")
    )
    fun purchaseProduct(
        activity: Activity,
        storeProduct: StoreProduct,
        callback: PurchaseCallback
    ) {
        val purchaseOption = storeProduct.bestPurchaseOption
        if (purchaseOption == null) {
            // TODOBC5: Improve and move error message
            errorLog("PurchaseProduct: Product does not have any purchase option")
            return
        }
        startPurchase(activity, storeProduct, purchaseOption, null, callback)
    }

    /**
     * Purchase a [StoreProduct]'s [PurchaseOption] upgrading from a previous product.
     * @param [activity] Current activity
     * @param [storeProduct] The StoreProduct of the product you wish to purchase
     * @param [purchaseOption] Your choice of purchase options available for the StoreProduct
     * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional
     * prorationMode. Amazon Appstore doesn't support changing products so upgradeInfo is ignored for Amazon purchases.
     * @param [listener] The PurchaseCallback that will be called when purchase completes.
     */
    fun purchaseProductOption(
        activity: Activity,
        storeProduct: StoreProduct,
        purchaseOption: PurchaseOption,
        upgradeInfo: UpgradeInfo,
        listener: ProductChangeCallback
    ) {
        startProductChange(
            activity,
            storeProduct,
            purchaseOption,
            null,
            upgradeInfo,
            listener
        )
    }

    /**
     * Purchase a [StoreProduct]'s [PurchaseOption].
     * @param [activity] Current activity
     * @param [storeProduct] The StoreProduct of the product you wish to purchase
     * @param [purchaseOption] Your choice of purchase options available for the StoreProduct
     * @param [callback] The PurchaseCallback that will be called when purchase completes
     */
    fun purchaseProductOption(
        activity: Activity,
        storeProduct: StoreProduct,
        purchaseOption: PurchaseOption,
        callback: PurchaseCallback
    ) {
        startPurchase(activity, storeProduct, purchaseOption, null, callback)
    }

    /**
     * Make a purchase upgrading from a previous sku.
     * @param [activity] Current activity
     * @param [packageToPurchase] The Package you wish to purchase
     * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional
     * prorationMode. Amazon Appstore doesn't support changing products so upgradeInfo is ignored for Amazon purchases.
     * @param [callback] The listener that will be called when purchase completes.
     */
    @Deprecated(
        "Replaced with purchasePackageOption",
        ReplaceWith("purchasePackageOption(activity, packageToPurchase, purchaseOption, upgradeInfo, callback)")
    )
    fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package,
        upgradeInfo: UpgradeInfo,
        callback: ProductChangeCallback
    ) {
        val purchaseOption = packageToPurchase.product.bestPurchaseOption
        if (purchaseOption == null) {
            // TODOBC5: Improve and move error message
            errorLog("PurchasePackage with upgrade: Product does not have any purchase option")
            return
        }
        startProductChange(
            activity,
            packageToPurchase.product,
            purchaseOption,
            packageToPurchase.offering,
            upgradeInfo,
            callback
        )
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [packageToPurchase] The Package you wish to purchase
     * @param [listener] The listener that will be called when purchase completes.
     */
    @Deprecated(
        "Replaced with purchasePackageOption",
        ReplaceWith("purchasePackageOption(activity, packageToPurchase, purchaseOption, listener)")
    )
    fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package,
        listener: PurchaseCallback
    ) {
        val purchaseOption = packageToPurchase.product.bestPurchaseOption
        if (purchaseOption == null) {
            // TODOBC5: Improve and move error message
            errorLog("PurchasePackage: Product does not have any purchase option")
            return
        }
        startPurchase(
            activity,
            packageToPurchase.product,
            purchaseOption,
            packageToPurchase.offering,
            listener
        )
    }

    /**
     * Purchase a [Package]'s [PurchaseOption], switching from an old product.
     * @param [activity] Current activity
     * @param [packageToPurchase] The Package you wish to purchase
     * @param [purchaseOption] Your choice of purchase options available for the StoreProduct
     * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional
     * prorationMode. Amazon Appstore doesn't support changing products so upgradeInfo is ignored for Amazon purchases.
     * @param [callback] The listener that will be called when purchase completes.
     */
    fun purchasePackageOption(
        activity: Activity,
        packageToPurchase: Package,
        purchaseOption: PurchaseOption,
        upgradeInfo: UpgradeInfo,
        callback: ProductChangeCallback
    ) {
        startProductChange(
            activity,
            packageToPurchase.product,
            purchaseOption,
            packageToPurchase.offering,
            upgradeInfo,
            callback
        )
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [packageToPurchase] The Package you wish to purchase
     * @param [purchaseOption] Your choice of purchase options available for the StoreProduct
     * @param [listener] The listener that will be called when purchase completes.
     */
    fun purchasePackageOption(
        activity: Activity,
        packageToPurchase: Package,
        purchaseOption: PurchaseOption,
        listener: PurchaseCallback
    ) {
        startPurchase(
            activity,
            packageToPurchase.product,
            purchaseOption,
            packageToPurchase.offering,
            listener
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
        callback: ReceiveCustomerInfoCallback
    ) {
        log(LogIntent.DEBUG, RestoreStrings.RESTORING_PURCHASE)
        if (!allowSharingPlayStoreAccount) {
            log(LogIntent.WARNING, RestoreStrings.SHARING_ACC_RESTORE_FALSE)
        }

        val appUserID = identityManager.currentAppUserID

        this.finishTransactions.let { finishTransactions ->
            billing.queryAllPurchases(
                appUserID,
                onReceivePurchaseHistory = { allPurchases ->
                    if (allPurchases.isEmpty()) {
                        getCustomerInfo(callback)
                    } else {
                        allPurchases.sortedBy { it.purchaseTime }.let { sortedByTime ->
                            sortedByTime.forEach { purchase ->
                                subscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserID) { unsyncedSubscriberAttributesByKey ->
                                    val receiptInfo = ReceiptInfo(productIDs = purchase.productIds)
                                    backend.postReceiptData(
                                        purchaseToken = purchase.purchaseToken,
                                        appUserID = appUserID,
                                        isRestore = true,
                                        observerMode = !finishTransactions,
                                        subscriberAttributes = unsyncedSubscriberAttributesByKey.toBackendMap(),
                                        receiptInfo = receiptInfo,
                                        storeAppUserID = purchase.storeUserID,
                                        marketplace = purchase.marketplace,
                                        onSuccess = { info, body ->
                                            subscriberAttributesManager.markAsSynced(
                                                appUserID,
                                                unsyncedSubscriberAttributesByKey,
                                                body.getAttributeErrors()
                                            )
                                            billing.consumeAndSave(finishTransactions, purchase)
                                            customerInfoHelper.cacheCustomerInfo(info)
                                            customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(info)
                                            log(LogIntent.DEBUG, RestoreStrings.PURCHASE_RESTORED.format(purchase))
                                            if (sortedByTime.last() == purchase) {
                                                dispatch { callback.onReceived(info) }
                                            }
                                        },
                                        onError = { error, shouldConsumePurchase, body ->
                                            if (shouldConsumePurchase) {
                                                subscriberAttributesManager.markAsSynced(
                                                    appUserID,
                                                    unsyncedSubscriberAttributesByKey,
                                                    body.getAttributeErrors()
                                                )
                                                billing.consumeAndSave(finishTransactions, purchase)
                                            }
                                            log(
                                                LogIntent.RC_ERROR, RestoreStrings.RESTORING_PURCHASE_ERROR
                                                    .format(purchase, error)
                                            )
                                            if (sortedByTime.last() == purchase) {
                                                dispatch { callback.onError(error) }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                onReceivePurchaseHistoryError = { error ->
                    dispatch { callback.onError(error) }
                }
            )
        }
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
        callback: LogInCallback? = null
    ) {
        identityManager.currentAppUserID.takeUnless { it == newAppUserID }?.let {
            identityManager.logIn(newAppUserID,
                onSuccess = { customerInfo, created ->
                    dispatch {
                        callback?.onReceived(customerInfo, created)
                        customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(customerInfo)
                    }
                    fetchAndCacheOfferings(newAppUserID, state.appInBackground)
                },
                onError = { error ->
                    dispatch { callback?.onError(error) }
                })
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
                    }
                )
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
     * Get latest available purchaser info.
     * @param callback A listener called when purchaser info is available and not stale.
     * Called immediately if purchaser info is cached. Purchaser info can be null if an error occurred.
     */
    fun getCustomerInfo(
        callback: ReceiveCustomerInfoCallback
    ) {
        getCustomerInfo(CacheFetchPolicy.default(), callback)
    }

    /**
     * Get latest available purchaser info.
     * @param fetchPolicy Specifies cache behavior for customer info retrieval
     * @param callback A listener called when purchaser info is available and not stale.
     * Purchaser info can be null if an error occurred.
     */
    fun getCustomerInfo(
        fetchPolicy: CacheFetchPolicy,
        callback: ReceiveCustomerInfoCallback
    ) {
        customerInfoHelper.retrieveCustomerInfo(
            identityManager.currentAppUserID,
            fetchPolicy,
            state.appInBackground,
            callback
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
            appUserID
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
            appUserID
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
            appUserID
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
            appUserID
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
            appUserID
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
            appUserID
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
            appUserID
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
            application
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
            application
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
            application
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
            application
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
            application
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
            appUserID
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
            appUserID
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
            appUserID
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
            appUserID
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
            appUserID
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
            appUserID
        )
    }

    //endregion
    //endregion
    //endregion

    // region Private Methods

    private fun fetchAndCacheOfferings(
        appUserID: String,
        appInBackground: Boolean,
        completion: ReceiveOfferingsCallback? = null
    ) {
        deviceCache.setOfferingsCacheTimestampToNow()
        backend.getOfferings(
            appUserID,
            appInBackground,
            { offeringsJSON ->
                try {
                    val productGroupIdentifiers = extractProductGroupIdentifiers(offeringsJSON)
                    if (productGroupIdentifiers.isEmpty()) {
                        handleErrorFetchingOfferings(
                            PurchasesError(
                                PurchasesErrorCode.ConfigurationError,
                                OfferingStrings.CONFIGURATION_ERROR_NO_PRODUCTS_FOR_OFFERINGS
                            ),
                            completion
                        )
                    } else {
                        getStoreProductsById(productGroupIdentifiers, { productsById ->
                            val offerings = offeringsJSON.createOfferings(productsById)

                            logMissingProducts(offerings, productsById)

                            if (offerings.all.isEmpty()) {
                                handleErrorFetchingOfferings(
                                    PurchasesError(
                                        PurchasesErrorCode.ConfigurationError,
                                        OfferingStrings.CONFIGURATION_ERROR_PRODUCTS_NOT_FOUND
                                    ),
                                    completion
                                )
                            } else {
                                synchronized(this@Purchases) {
                                    deviceCache.cacheOfferings(offerings)
                                }
                                dispatch {
                                    completion?.onReceived(offerings)
                                }
                            }
                        }, { error ->
                            handleErrorFetchingOfferings(error, completion)
                        })
                    }
                } catch (error: JSONException) {
                    log(LogIntent.RC_ERROR, OfferingStrings.JSON_EXCEPTION_ERROR.format(error.localizedMessage))
                    handleErrorFetchingOfferings(
                        PurchasesError(
                            PurchasesErrorCode.UnexpectedBackendResponseError,
                            error.localizedMessage
                        ),
                        completion
                    )
                }
            }, { error ->
                handleErrorFetchingOfferings(error, completion)
            })
    }

    private fun extractProductGroupIdentifiers(offeringsJSON: JSONObject): Set<String> {
        val jsonOfferingsArray = offeringsJSON.getJSONArray("offerings")
        val productGroupIds = mutableSetOf<String>()
        for (i in 0 until jsonOfferingsArray.length()) {
            val jsonPackagesArray =
                jsonOfferingsArray.getJSONObject(i).getJSONArray("packages")
            for (j in 0 until jsonPackagesArray.length()) {
                jsonPackagesArray.getJSONObject(j)
                    .optString("platform_product_identifier").takeIf { it.isNotBlank() }?.let {
                        productGroupIds.add(it)
                    }
            }
        }
        return productGroupIds
    }

    private fun handleErrorFetchingOfferings(
        error: PurchasesError,
        completion: ReceiveOfferingsCallback?
    ) {
        val errorCausedByPurchases = setOf(
            PurchasesErrorCode.ConfigurationError,
            PurchasesErrorCode.UnexpectedBackendResponseError
        )
            .contains(error.code)

        log(
            if (errorCausedByPurchases) LogIntent.RC_ERROR else LogIntent.GOOGLE_ERROR,
            OfferingStrings.FETCHING_OFFERINGS_ERROR.format(error)
        )

        deviceCache.clearOfferingsCacheTimestamp()
        dispatch {
            completion?.onError(error)
        }
    }

    private fun logMissingProducts(
        offerings: Offerings,
        storeProductByID: Map<String, List<StoreProduct>>
    ) = offerings.all.values
        .flatMap { it.availablePackages }
        .map { it.product.productId }
        .filterNot { storeProductByID.containsKey(it) }
        .takeIf { it.isNotEmpty() }
        ?.let { missingProducts ->
            log(
                LogIntent.GOOGLE_WARNING, OfferingStrings.CANNOT_FIND_PRODUCT_CONFIGURATION_ERROR
                    .format(missingProducts.joinToString(", "))
            )
        }

    private fun getSkus(
        skus: Set<String>,
        productType: ProductType,
        callback: GetStoreProductsCallback
    ) {
        billing.queryProductDetailsAsync(
            productType,
            skus,
            { storeProducts ->
                dispatch {
                    callback.onReceived(storeProducts)
                }
            }, {
                dispatch {
                    callback.onError(it)
                }
            })
    }

    private fun updateAllCaches(
        appUserID: String,
        completion: ReceiveCustomerInfoCallback? = null
    ) {
        state.appInBackground.let { appInBackground ->
            customerInfoHelper.retrieveCustomerInfo(
                appUserID,
                CacheFetchPolicy.FETCH_CURRENT,
                appInBackground,
                completion
            )
            fetchAndCacheOfferings(appUserID, appInBackground)
        }
    }

    private fun postPurchases(
        purchases: List<StoreTransaction>,
        allowSharingPlayStoreAccount: Boolean,
        consumeAllTransactions: Boolean,
        appUserID: String,
        onSuccess: (SuccessfulPurchaseCallback)? = null,
        onError: (ErrorPurchaseCallback)? = null
    ) {
        purchases.forEach { purchase ->
            if (purchase.purchaseState != PurchaseState.PENDING) {
                if (purchase.purchaseOptionId != null) {
                    billing.queryProductDetailsAsync(
                        productType = purchase.type,
                        productIds = purchase.productIds.toSet(),
                        onReceive = { storeProducts ->

                            // before we defaulted to the first product, i think this was an OK assumption because
                            // we assumed only one product per Purchase (i.e. Purchase.productIds.size was 1)
                            // and before, one productId mapped perfectly to one SkuDetails
                            // TODO BC5 confirm multi line purchases
                            val purchasedStoreProduct = storeProducts.first { product ->
                                product.purchaseOptions.any { it.id == purchase.purchaseOptionId }
                            }

                            postToBackend(
                                purchase = purchase,
                                storeProduct = purchasedStoreProduct,
                                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                                consumeAllTransactions = consumeAllTransactions,
                                appUserID = appUserID,
                                onSuccess = onSuccess,
                                onError = onError
                            )
                        },
                        onError = {
                            postToBackend(
                                purchase = purchase,
                                storeProduct = null,
                                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                                consumeAllTransactions = consumeAllTransactions,
                                appUserID = appUserID,
                                onSuccess = onSuccess,
                                onError = onError
                            )
                        }
                    )
                } else {
                    postToBackend(
                        purchase = purchase,
                        storeProduct = null,
                        allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                        consumeAllTransactions = consumeAllTransactions,
                        appUserID = appUserID,
                        onSuccess = onSuccess,
                        onError = onError
                    )
                }
            } else {
                onError?.invoke(
                    purchase,
                    PurchasesError(PurchasesErrorCode.PaymentPendingError).also { errorLog(it) }
                )
            }
        }
    }

    @JvmSynthetic
    internal fun postToBackend(
        purchase: StoreTransaction,
        storeProduct: StoreProduct?,
        allowSharingPlayStoreAccount: Boolean,
        consumeAllTransactions: Boolean,
        appUserID: String,
        onSuccess: (SuccessfulPurchaseCallback)? = null,
        onError: (ErrorPurchaseCallback)? = null
    ) {
        subscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserID) { unsyncedSubscriberAttributesByKey ->
            val receiptInfo = ReceiptInfo(
                productIDs = purchase.productIds,
                offeringIdentifier = purchase.presentedOfferingIdentifier,
                storeProduct = storeProduct,
                purchaseOptionId = purchase.purchaseOptionId
            )
            backend.postReceiptData(
                purchaseToken = purchase.purchaseToken,
                appUserID = appUserID,
                isRestore = allowSharingPlayStoreAccount,
                observerMode = !consumeAllTransactions,
                subscriberAttributes = unsyncedSubscriberAttributesByKey.toBackendMap(),
                receiptInfo = receiptInfo,
                storeAppUserID = purchase.storeUserID,
                marketplace = purchase.marketplace,
                onSuccess = { info, body ->
                    subscriberAttributesManager.markAsSynced(
                        appUserID,
                        unsyncedSubscriberAttributesByKey,
                        body.getAttributeErrors()
                    )
                    billing.consumeAndSave(consumeAllTransactions, purchase)
                    customerInfoHelper.cacheCustomerInfo(info)
                    customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(info)
                    onSuccess?.let { it(purchase, info) }
                },
                onError = { error, shouldConsumePurchase, body ->
                    if (shouldConsumePurchase) {
                        subscriberAttributesManager.markAsSynced(
                            appUserID,
                            unsyncedSubscriberAttributesByKey,
                            body.getAttributeErrors()
                        )
                        billing.consumeAndSave(consumeAllTransactions, purchase)
                    }
                    onError?.let { it(purchase, error) }
                }
            )
        }
    }

    private fun getStoreProductsById(
        productIds: Set<String>,
        onCompleted: (Map<String, List<StoreProduct>>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        billing.queryProductDetailsAsync(
            ProductType.SUBS,
            productIds,
            { subscriptionProducts ->
                val productsById = subscriptionProducts.groupBy { subProduct -> subProduct.productId }.toMutableMap()
                val subscriptionIds = productsById.keys

                val inAppProductIds = productIds - subscriptionIds
                if (inAppProductIds.isNotEmpty()) {
                    billing.queryProductDetailsAsync(
                        ProductType.INAPP,
                        inAppProductIds,
                        { inAppProducts ->
                            productsById.putAll(inAppProducts.map { it.productId to listOf(it) })
                            onCompleted(productsById)
                        }, {
                            onError(it)
                        }
                    )
                } else {
                    onCompleted(productsById)
                }
            }, {
                onError(it)
            })
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
            state =
                state.copy(purchaseCallbacksByProductId = state.purchaseCallbacksByProductId.filterNot { it.key == productId })
        }
    }

    private fun getAndClearProductChangeCallback(): ProductChangeCallback? {
        return state.productChangeCallback.also {
            state = state.copy(productChangeCallback = null)
        }
    }

    private fun getPurchasesUpdatedListener(): BillingAbstract.PurchasesUpdatedListener {
        return object : BillingAbstract.PurchasesUpdatedListener {
            override fun onPurchasesUpdated(purchases: List<StoreTransaction>) {
                val productChangeInProgress: Boolean
                val callbackPair: Pair<SuccessfulPurchaseCallback, ErrorPurchaseCallback>
                val productChangeListener: ProductChangeCallback?

                synchronized(this@Purchases) {
                    productChangeInProgress = state.productChangeCallback != null
                    if (productChangeInProgress) {
                        productChangeListener = getAndClearProductChangeCallback()
                        callbackPair = getProductChangeCompletedCallbacks(productChangeListener)
                    } else {
                        productChangeListener = null
                        callbackPair = getPurchaseCompletedCallbacks()
                    }
                }

                if (productChangeInProgress && purchases.isEmpty()) {
                    // Can happen if the product change is ProrationMode.DEFERRED
                    invalidateCustomerInfoCache()
                    getCustomerInfoWith { customerInfo ->
                        productChangeListener?.let { callback ->
                            dispatch {
                                callback.onCompleted(null, customerInfo)
                            }
                        }
                    }
                    return
                }

                postPurchases(
                    purchases,
                    allowSharingPlayStoreAccount,
                    finishTransactions,
                    appUserID,
                    onSuccess = callbackPair.first,
                    onError = callbackPair.second
                )
            }

            override fun onPurchasesFailedToUpdate(purchasesError: PurchasesError) {
                synchronized(this@Purchases) {
                    state.productChangeCallback?.let { productChangeCallback ->
                        state = state.copy(productChangeCallback = null)
                        productChangeCallback.dispatch(purchasesError)
                    } ?: state.purchaseCallbacksByProductId.let { purchaseCallbacks ->
                        state = state.copy(purchaseCallbacksByProductId = emptyMap())
                        purchaseCallbacks.values.forEach { it.dispatch(purchasesError) }
                    }
                }
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
        productChangeListener: ProductChangeCallback?
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
                error.code == PurchasesErrorCode.PurchaseCancelledError
            )
        }
    }

    private fun startPurchase(
        activity: Activity,
        storeProduct: StoreProduct,
        purchaseOption: PurchaseOption,
        presentedOfferingIdentifier: String?,
        listener: PurchaseCallback
    ) {
        log(
            LogIntent.PURCHASE, PurchaseStrings.PURCHASE_STARTED.format(
                " $storeProduct ${
                    presentedOfferingIdentifier?.let {
                        PurchaseStrings.OFFERING + "$presentedOfferingIdentifier"
                    }
                }"
            )
        )
        var userPurchasing: String? = null // Avoids race condition for userid being modified before purchase is made
        synchronized(this@Purchases) {
            if (!appConfig.finishTransactions) {
                log(LogIntent.WARNING, PurchaseStrings.PURCHASE_FINISH_TRANSACTION_FALSE)
            }
            if (!state.purchaseCallbacksByProductId.containsKey(storeProduct.productId)) {
                state = state.copy(
                    purchaseCallbacksByProductId = state.purchaseCallbacksByProductId + mapOf(storeProduct.productId to listener)
                )
                userPurchasing = identityManager.currentAppUserID
            }
        }
        userPurchasing?.let { appUserID ->
            billing.makePurchaseAsync(
                activity,
                appUserID,
                storeProduct,
                purchaseOption,
                null,
                presentedOfferingIdentifier
            )
        } ?: listener.dispatch(PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError).also { errorLog(it) })
    }

    private fun startProductChange(
        activity: Activity,
        storeProduct: StoreProduct,
        purchaseOption: PurchaseOption,
        offeringIdentifier: String?,
        upgradeInfo: UpgradeInfo,
        listener: ProductChangeCallback
    ) {
        log(
            LogIntent.PURCHASE, PurchaseStrings.PRODUCT_CHANGE_STARTED.format(
                " $storeProduct ${
                    offeringIdentifier?.let {
                        PurchaseStrings.OFFERING + "$offeringIdentifier"
                    }
                } UpgradeInfo: $upgradeInfo"

            )
        )
        var userPurchasing: String? = null // Avoids race condition for userid being modified before purchase is made
        synchronized(this@Purchases) {
            if (!appConfig.finishTransactions) {
                log(LogIntent.WARNING, PurchaseStrings.PURCHASE_FINISH_TRANSACTION_FALSE)
            }
            if (state.productChangeCallback == null) {
                state = state.copy(productChangeCallback = listener)
                userPurchasing = identityManager.currentAppUserID
            }
        }
        userPurchasing?.let { appUserID ->
            replaceOldPurchaseWithNewProduct(
                storeProduct,
                purchaseOption,
                upgradeInfo,
                activity,
                appUserID,
                offeringIdentifier,
                listener
            )
        } ?: listener.dispatch(PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError).also { errorLog(it) })
    }

    private fun replaceOldPurchaseWithNewProduct(
        storeProduct: StoreProduct,
        purchaseOption: PurchaseOption,
        upgradeInfo: UpgradeInfo,
        activity: Activity,
        appUserID: String,
        presentedOfferingIdentifier: String?,
        listener: PurchaseErrorCallback
    ) {
        billing.findPurchaseInPurchaseHistory(
            appUserID,
            storeProduct.type,
            upgradeInfo.oldSku,
            onCompletion = { purchaseRecord ->
                log(LogIntent.PURCHASE, PurchaseStrings.FOUND_EXISTING_PURCHASE.format(upgradeInfo.oldSku))

                billing.makePurchaseAsync(
                    activity,
                    appUserID,
                    storeProduct,
                    purchaseOption,
                    ReplaceSkuInfo(purchaseRecord, upgradeInfo.prorationMode),
                    presentedOfferingIdentifier
                )
            },
            onError = { error ->
                log(LogIntent.GOOGLE_ERROR, error.message)
                dispatch {
                    listener.onError(error, false)
                }
            })
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
                                    RestoreStrings.QUERYING_PURCHASE_WITH_HASH.format(purchase.type, hash)
                                )
                            }
                            deviceCache.cleanPreviouslySentTokens(purchasesByHashedToken.keys)
                            postPurchases(
                                deviceCache.getActivePurchasesNotInCache(purchasesByHashedToken),
                                allowSharingPlayStoreAccount,
                                finishTransactions,
                                appUserID
                            )
                        },
                        onError = { error ->
                            log(LogIntent.GOOGLE_ERROR, error.message)
                        })
                }
            })
        } else {
            log(LogIntent.DEBUG, PurchaseStrings.BILLING_CLIENT_NOT_CONNECTED)
        }
    }

    private fun synchronizeSubscriberAttributesIfNeeded() {
        subscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserID)
    }

    private fun syncPurchaseWithBackend(
        purchaseToken: String,
        storeUserID: String?,
        appUserID: String,
        productInfo: ReceiptInfo,
        marketplace: String?,
        onSuccess: () -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        subscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserID) { unsyncedSubscriberAttributesByKey ->
            backend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserID,
                isRestore = this.allowSharingPlayStoreAccount,
                observerMode = !this.finishTransactions,
                subscriberAttributes = unsyncedSubscriberAttributesByKey.toBackendMap(),
                receiptInfo = productInfo,
                storeAppUserID = storeUserID,
                marketplace = marketplace,
                onSuccess = { info, body ->
                    subscriberAttributesManager.markAsSynced(
                        appUserID,
                        unsyncedSubscriberAttributesByKey,
                        body.getAttributeErrors()
                    )
                    deviceCache.addSuccessfullyPostedToken(purchaseToken)
                    customerInfoHelper.cacheCustomerInfo(info)
                    customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(info)
                    onSuccess()
                },
                onError = { error, shouldConsumePurchase, body ->
                    if (shouldConsumePurchase) {
                        subscriberAttributesManager.markAsSynced(
                            appUserID,
                            unsyncedSubscriberAttributesByKey,
                            body.getAttributeErrors()
                        )
                        deviceCache.addSuccessfullyPostedToken(purchaseToken)
                    }
                    onError(error)
                }
            )
        }
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
        ReplaceWith("configure through the RevenueCat dashboard")
    )
    var allowSharingPlayStoreAccount: Boolean
        @Synchronized get() =
            state.allowSharingPlayStoreAccount ?: identityManager.currentUserIsAnonymous()
        @Synchronized set(value) {
            state = state.copy(allowSharingPlayStoreAccount = value)
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
            version = null
        )

        /**
         * Enable debug logging. Useful for debugging issues with the lovely team @RevenueCat
         */
        @JvmStatic
        var debugLogsEnabled
            get() = Config.debugLogsEnabled
            set(value) {
                Config.debugLogsEnabled = value
            }

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
            configuration: PurchasesConfiguration
        ): Purchases {
            return PurchasesFactory().createPurchases(
                configuration,
                platformInfo,
                proxyURL
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
            callback: Callback<Boolean>
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
                        })
                }
        }
    }

    // endregion
}
