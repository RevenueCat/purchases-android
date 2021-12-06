//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.util.Pair
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Config
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.ReplaceSkuInfo
import com.revenuecat.purchases.common.attribution.AttributionData
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.createOfferings
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.isSuccessful
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.networking.ETagManager
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.google.toStoreProduct
import com.revenuecat.purchases.google.toProductType
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.Callback
import com.revenuecat.purchases.interfaces.GetStoreProductCallback
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.ProductChangeListener
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.PurchaseErrorListener
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoListener
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.interfaces.toGetStoreProductCallback
import com.revenuecat.purchases.interfaces.toProductChangeCallback
import com.revenuecat.purchases.interfaces.toPurchaseCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.PaymentTransaction
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.strings.AttributionStrings
import com.revenuecat.purchases.strings.ConfigureStrings
import com.revenuecat.purchases.strings.OfferingStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.CustomerInfoStrings
import com.revenuecat.purchases.strings.RestoreStrings
import com.revenuecat.purchases.subscriberattributes.AttributionDataMigrator
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesPoster
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.subscriberattributes.getAttributeErrors
import com.revenuecat.purchases.subscriberattributes.toBackendMap
import com.revenuecat.purchases.util.AdvertisingIdClient
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import java.util.Collections.emptyMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.revenuecat.purchases.common.attribution.AttributionNetwork as CommonAttributionNetwork

typealias SuccessfulPurchaseCallback = (PaymentTransaction, CustomerInfo) -> Unit
typealias ErrorPurchaseCallback = (PaymentTransaction, PurchasesError) -> Unit

/**
 * Entry point for Purchases. It should be instantiated as soon as your app has a unique user id
 * for your user. This can be when a user logs in if you have accounts or on launch if you can
 * generate a random user identifier.
 * Make sure you follow the [quickstart](https://docs.revenuecat.com/docs/getting-started-1)
 * guide to setup your RevenueCat account.
 * @warning Only one instance of Purchases should be instantiated at a time!
 */
@Suppress("LongParameterList")
class Purchases @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE) internal constructor(
    private val application: Application,
    backingFieldAppUserID: String?,
    private val backend: Backend,
    private val billing: BillingAbstract,
    private val deviceCache: DeviceCache,
    private val dispatcher: Dispatcher,
    private val identityManager: IdentityManager,
    private val subscriberAttributesManager: SubscriberAttributesManager,
    @JvmSynthetic internal var appConfig: AppConfig
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

    /*
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
     * The listener is responsible for handling changes to purchaser information.
     * Make sure [removeUpdatedCustomerInfoListener] is called when the listener needs to be destroyed.
     */
    var updatedCustomerInfoListener: UpdatedCustomerInfoListener?
        @Synchronized get() = state.updatedCustomerInfoListener
        set(value) {
            synchronized(this@Purchases) {
                state = state.copy(updatedCustomerInfoListener = value)
            }
            afterSetListener(value)
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
            fetchAndCacheCustomerInfo(identityManager.currentAppUserID, appInBackground = false)
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
     * for subscriptions anytime a sync is needed, like after a successful purchase.
     *
     * @warning This function should only be called if you're not calling any purchase method.
     */
    fun syncPurchases() {
        log(LogIntent.DEBUG, PurchaseStrings.SYNCING_PURCHASES)

        val appUserID = identityManager.currentAppUserID

        billing.queryAllPurchases(
            appUserID,
            onReceivePurchaseHistory = { allPurchases ->
                if (allPurchases.isNotEmpty()) {
                    allPurchases.forEach { purchase ->
                        val unsyncedSubscriberAttributesByKey =
                            subscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserID)
                        val productInfo = ReceiptInfo(productIDs = purchase.skus)
                        backend.postReceiptData(
                            purchaseToken = purchase.purchaseToken,
                            appUserID = appUserID,
                            isRestore = this.allowSharingPlayStoreAccount,
                            observerMode = !this.finishTransactions,
                            subscriberAttributes = unsyncedSubscriberAttributesByKey.toBackendMap(),
                            receiptInfo = productInfo,
                            storeAppUserID = purchase.storeUserID,
                            onSuccess = { info, body ->
                                subscriberAttributesManager.markAsSynced(
                                    appUserID,
                                    unsyncedSubscriberAttributesByKey,
                                    body.getAttributeErrors()
                                )
                                deviceCache.addSuccessfullyPostedToken(purchase.purchaseToken)
                                cacheCustomerInfo(info)
                                sendUpdatedCustomerInfoToDelegateIfChanged(info)
                                log(LogIntent.PURCHASE, PurchaseStrings.PURCHASE_SYNCED.format(purchase))
                            },
                            onError = { error, errorIsFinishable, body ->
                                if (errorIsFinishable) {
                                    subscriberAttributesManager.markAsSynced(
                                        appUserID,
                                        unsyncedSubscriberAttributesByKey,
                                        body.getAttributeErrors()
                                    )
                                    deviceCache.addSuccessfullyPostedToken(purchase.purchaseToken)
                                }
                                log(LogIntent.RC_ERROR, PurchaseStrings.SYNCING_PURCHASES_ERROR_DETAILS
                                        .format(purchase, error))
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
        listener: ReceiveOfferingsListener
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
                    log(LogIntent.DEBUG,
                            if (appInBackground) OfferingStrings.OFFERINGS_STALE_UPDATING_IN_BACKGROUND
                            else OfferingStrings.OFFERINGS_STALE_UPDATING_IN_FOREGROUND)
                    fetchAndCacheOfferings(appUserID, appInBackground)
                    log(LogIntent.RC_SUCCESS, OfferingStrings.OFFERINGS_UPDATED_FROM_NETWORK)
                }
            }
        }
    }

    /**
     * Gets the SKUDetails for the given list of subscription skus.
     * @param [skus] List of skus
     * @param [listener] Response listener
     */
    fun getSubscriptionSkus(
        skus: List<String>,
        listener: GetSkusResponseListener
    ) {
        getSkus(skus.toSet(), BillingClient.SkuType.SUBS.toProductType(), listener.toGetStoreProductCallback())
    }

    /**
     * Gets the StoreProduct for the given list of subscription skus.
     * @param [skus] List of skus
     * @param [callback] Response callback
     */
    @JvmSynthetic
    internal fun getSubscriptionSkus(
        skus: List<String>,
        callback: GetStoreProductCallback
    ) {
        getSkus(skus.toSet(), ProductType.SUBS, callback)
    }

    /**
     * Gets the SKUDetails for the given list of non-subscription skus.
     * @param [skus] List of skus
     * @param [listener] Response listener
     */
    fun getNonSubscriptionSkus(
        skus: List<String>,
        listener: GetSkusResponseListener
    ) {
        getSkus(skus.toSet(), BillingClient.SkuType.INAPP.toProductType(), listener.toGetStoreProductCallback())
    }

    /**
     * Gets the SKUDetails for the given list of non-subscription skus.
     * @param [skus] List of skus
     * @param [callback] Response callback
     */
    @JvmSynthetic
    internal fun getNonSubscriptionSkus(
        skus: List<String>,
        callback: GetStoreProductCallback
    ) {
        getSkus(skus.toSet(), ProductType.INAPP, callback)
    }

    fun purchaseProduct(
        activity: Activity,
        skuDetails: SkuDetails,
        upgradeInfo: UpgradeInfo,
        listener: ProductChangeListener
    ) {
        purchaseProduct(
            activity,
            skuDetails.toStoreProduct(),
            upgradeInfo,
            listener.toProductChangeCallback()
        )
    }

    @JvmSynthetic
    internal fun purchaseProduct(
        activity: Activity,
        storeProduct: StoreProduct,
        upgradeInfo: UpgradeInfo,
        listener: ProductChangeCallback
    ) {
        startProductChange(
            activity,
            storeProduct,
            null,
            upgradeInfo,
            listener
        )
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [skuDetails] The skuDetails of the product you wish to purchase
     * @param [listener] The listener that will be called when purchase completes.
     */
    fun purchaseProduct(
        activity: Activity,
        skuDetails: SkuDetails,
        listener: MakePurchaseListener
    ) {
        purchaseProduct(
            activity,
            skuDetails.toStoreProduct(),
            listener.toPurchaseCallback()
        )
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [storeProduct] The StoreProduct of the product you wish to purchase
     * @param [callback] The PurchaseCallback that will be called when purchase completes.
     */
    @JvmSynthetic
    internal fun purchaseProduct(
        activity: Activity,
        storeProduct: StoreProduct,
        callback: PurchaseCallback
    ) {
        startPurchase(activity, storeProduct, null, callback)
    }

    /**
     * Change the product from [productChangeInfo] with the one in [packageToPurchase].
     *
     * @param [activity] Current activity
     * @param [packageToPurchase] The new package to purchase
     * @param [upgradeInfo] The oldProduct of this object will be replaced with the product in [packageToPurchase].
     * An optional [BillingFlowParams.ProrationMode] can also be specified.
     * @param [listener] The listener that will be called when the purchase of the new product completes.
     */
    fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package,
        upgradeInfo: UpgradeInfo,
        listener: ProductChangeListener
    ) {
        purchasePackage(
            activity,
            packageToPurchase,
            upgradeInfo,
            listener.toProductChangeCallback()
        )
    }

    @JvmSynthetic
    internal fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package,
        upgradeInfo: UpgradeInfo,
        callback: ProductChangeCallback
    ) {
        startProductChange(
            activity,
            packageToPurchase.product.toStoreProduct(),
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
    fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package,
        listener: MakePurchaseListener
    ) {
        purchasePackage(
            activity,
            packageToPurchase,
            listener.toPurchaseCallback()
        )
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [packageToPurchase] The Package you wish to purchase
     * @param [listener] The listener that will be called when purchase completes.
     */
    @JvmSynthetic
    internal fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package,
        listener: PurchaseCallback
    ) {
        startPurchase(
            activity,
            packageToPurchase.product.toStoreProduct(),
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
     * @param [listener] The listener that will be called when purchase restore completes.
     */
    fun restorePurchases(
        listener: ReceiveCustomerInfoListener
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
                        getCustomerInfo(listener)
                    } else {
                        allPurchases.sortedBy { it.purchaseTime }.let { sortedByTime ->
                            sortedByTime.forEach { purchase ->
                                val unsyncedSubscriberAttributesByKey =
                                    subscriberAttributesManager.getUnsyncedSubscriberAttributes(
                                        appUserID
                                    )
                                val receiptInfo = ReceiptInfo(productIDs = purchase.skus)
                                backend.postReceiptData(
                                    purchaseToken = purchase.purchaseToken,
                                    appUserID = appUserID,
                                    isRestore = true,
                                    observerMode = !finishTransactions,
                                    subscriberAttributes = unsyncedSubscriberAttributesByKey.toBackendMap(),
                                    receiptInfo = receiptInfo,
                                    storeAppUserID = purchase.storeUserID,
                                    onSuccess = { info, body ->
                                        subscriberAttributesManager.markAsSynced(
                                            appUserID,
                                            unsyncedSubscriberAttributesByKey,
                                            body.getAttributeErrors()
                                        )
                                        billing.consumeAndSave(finishTransactions, purchase)
                                        cacheCustomerInfo(info)
                                        sendUpdatedCustomerInfoToDelegateIfChanged(info)
                                        log(LogIntent.DEBUG, RestoreStrings.PURCHASE_RESTORED.format(purchase))
                                        if (sortedByTime.last() == purchase) {
                                            dispatch { listener.onReceived(info) }
                                        }
                                    },
                                    onError = { error, errorIsFinishable, body ->
                                        if (errorIsFinishable) {
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
                                            dispatch { listener.onError(error) }
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                onReceivePurchaseHistoryError = { error ->
                    dispatch { listener.onError(error) }
                }
            )
        }
    }

    /**
     * This function will alias two appUserIDs together.
     * @param [newAppUserID] The current user id will be aliased to the app user id passed in this parameter
     * @param [listener] An optional listener to listen for successes or errors.
     */
    @Deprecated(
        "Use logIn instead",
        ReplaceWith("Purchases.sharedInstance.logIn(newAppUserID, LogInCallback?)")
    )
    @JvmOverloads
    fun createAlias(
        newAppUserID: String,
        listener: ReceiveCustomerInfoListener? = null
    ) {
        identityManager.currentAppUserID.takeUnless { it == newAppUserID }?.let {
            identityManager.createAlias(
                newAppUserID,
                {
                    synchronized(this@Purchases) {
                        state = state.copy(purchaseCallbacks = emptyMap())
                    }
                    updateAllCaches(newAppUserID, listener)
                },
                { error ->
                    dispatch { listener?.onError(error) }
                }
            )
        } ?: retrieveCustomerInfo(identityManager.currentAppUserID, listener)
    }

    /**
     * This function will change the current appUserID.
     * Typically this would be used after a log out to identify a new user without calling configure
     * @param newAppUserID The new appUserID that should be linked to the currently user
     * @param [listener] An optional listener to listen for successes or errors.
     */
    @Deprecated(
        "Use logIn instead",
        ReplaceWith("Purchases.sharedInstance.logIn(newAppUserID, LogInCallback?)")
    )
    @JvmOverloads
    fun identify(
        newAppUserID: String,
        listener: ReceiveCustomerInfoListener? = null
    ) {
        identityManager.currentAppUserID.takeUnless { it == newAppUserID }?.let {
            identityManager.identify(
                newAppUserID,
                {
                    synchronized(this@Purchases) {
                        state = state.copy(purchaseCallbacks = emptyMap())
                    }
                    updateAllCaches(newAppUserID, listener)
                },
                { error ->
                    dispatch { listener?.onError(error) }
                }
            )
        } ?: retrieveCustomerInfo(identityManager.currentAppUserID, listener)
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
                        sendUpdatedCustomerInfoToDelegateIfChanged(customerInfo)
                    }
                    fetchAndCacheOfferings(newAppUserID, state.appInBackground)
                },
                onError = { error ->
                    dispatch { callback?.onError(error) }
                })
        }
            ?: retrieveCustomerInfo(identityManager.currentAppUserID, receiveCustomerInfoListener(
                onSuccess = { customerInfo ->
                    dispatch { callback?.onReceived(customerInfo, false) }
                },
                onError = { error ->
                    dispatch { callback?.onError(error) }
                }
            ))
    }

    /**
     * Resets the Purchases client clearing the save appUserID. This will generate a random user
     * id and save it in the cache.
     * @param [listener] An optional listener to listen for successes or errors.
     */
    @JvmOverloads
    fun logOut(listener: ReceiveCustomerInfoListener? = null) {
        val error: PurchasesError? = identityManager.logOut()
        if (error != null) {
            listener?.onError(error)
        } else {
            backend.clearCaches()
            synchronized(this@Purchases) {
                state = state.copy(purchaseCallbacks = emptyMap())
            }
            updateAllCaches(identityManager.currentAppUserID, listener)
        }
    }

    /**
     * Resets the Purchases client clearing the save appUserID. This will generate a random user
     * id and save it in the cache.
     * @param [listener] An optional listener to listen for successes or errors.
     */
    @Deprecated(
        "Use logOut instead",
        ReplaceWith("Purchases.sharedInstance.logOut(ReceiveCustomerInfoListener?)")
    )
    @JvmOverloads
    fun reset(
        listener: ReceiveCustomerInfoListener? = null
    ) {
        deviceCache.clearLatestAttributionData(identityManager.currentAppUserID)
        identityManager.reset()
        backend.clearCaches()
        synchronized(this@Purchases) {
            state = state.copy(purchaseCallbacks = emptyMap())
        }
        updateAllCaches(identityManager.currentAppUserID, listener)
    }

    /**
     * Call close when you are done with this instance of Purchases
     */
    fun close() {
        synchronized(this@Purchases) {
            state = state.copy(purchaseCallbacks = emptyMap())
        }
        this.backend.close()
        billing.purchasesUpdatedListener = null
        updatedCustomerInfoListener = null // Do not call on state since the setter does more stuff

        dispatch {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleHandler)
        }
    }

    /**
     * Get latest available purchaser info.
     * @param listener A listener called when purchaser info is available and not stale.
     * Called immediately if purchaser info is cached. Purchaser info can be null if an error occurred.
     */
    fun getCustomerInfo(
        listener: ReceiveCustomerInfoListener
    ) {
        retrieveCustomerInfo(identityManager.currentAppUserID, listener)
    }

    private fun retrieveCustomerInfo(
        appUserID: String,
        listener: ReceiveCustomerInfoListener? = null
    ) {
        val cachedCustomerInfo = deviceCache.getCachedCustomerInfo(appUserID)
        if (cachedCustomerInfo != null) {
            log(LogIntent.DEBUG, CustomerInfoStrings.VENDING_CACHE)
            dispatch { listener?.onReceived(cachedCustomerInfo) }
            state.appInBackground.let { appInBackground ->
                if (deviceCache.isCustomerInfoCacheStale(appUserID, appInBackground)) {
                    log(LogIntent.DEBUG,
                            if (appInBackground) CustomerInfoStrings.CUSTOMERINFO_STALE_UPDATING_BACKGROUND
                            else CustomerInfoStrings.CUSTOMERINFO_STALE_UPDATING_FOREGROUND)
                    fetchAndCacheCustomerInfo(appUserID, appInBackground)
                    log(LogIntent.RC_SUCCESS, CustomerInfoStrings.CUSTOMERINFO_UPDATED_FROM_NETWORK)
                }
            }
        } else {
            log(LogIntent.DEBUG, CustomerInfoStrings.NO_CACHED_CUSTOMERINFO)
            fetchAndCacheCustomerInfo(appUserID, state.appInBackground, listener)
            log(LogIntent.RC_SUCCESS, CustomerInfoStrings.CUSTOMERINFO_UPDATED_FROM_NETWORK)
        }
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
     * Invalidates the cache for purchaser information.
     *
     * Most apps will not need to use this method; invalidating the cache can leave your app in an invalid state.
     * Refer to https://docs.revenuecat.com/docs/purchaserinfo#section-get-user-information for more information on
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
    // region Attribution IDs

    /**
     * Automatically collect subscriber attributes associated with the device identifiers
     * $gpsAdId, $androidId, $ip
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
     * Subscriber attribute associated with the OneSignal Player Id for the user
     * Required for the RevenueCat OneSignal integration
     *
     * @param onesignalID null or an empty string will delete the subscriber attribute
     */
    fun setOnesignalID(onesignalID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setOnesignalID"))
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.OneSignal,
            onesignalID,
            appUserID,
            application
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
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Airship,
            airshipChannelID,
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

    // region Internal Methods

    @JvmSynthetic
    internal fun postAttributionData(
        jsonObject: JSONObject,
        network: CommonAttributionNetwork,
        networkUserId: String?
    ) {
        AdvertisingIdClient.getAdvertisingIdInfo(application) { adInfo ->
            identityManager.currentAppUserID.let { appUserID ->
                val latestAttributionDataId =
                    deviceCache.getCachedAttributionData(network, appUserID)
                val newCacheValue = adInfo.generateAttributionDataCacheValue(networkUserId)

                if (latestAttributionDataId != null && latestAttributionDataId == newCacheValue) {
                    log(LogIntent.DEBUG, AttributionStrings.SKIP_SAME_ATTRIBUTES)
                } else {
                    if (adInfo?.isLimitAdTrackingEnabled == false) {
                        jsonObject.put("rc_gps_adid", adInfo.id)
                    }

                    jsonObject.put("rc_attribution_network_id", networkUserId)

                    subscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                        jsonObject,
                        network,
                        appUserID
                    )
                    deviceCache.cacheAttributionData(network, appUserID, newCacheValue)
                }
            }
        }
    }
    // endregion

    // region Private Methods

    private fun AdvertisingIdClient.AdInfo?.generateAttributionDataCacheValue(networkUserId: String?) =
        listOfNotNull(
            this?.takeIf { !it.isLimitAdTrackingEnabled }?.id,
            networkUserId
        ).joinToString("_")

    private fun fetchAndCacheOfferings(
        appUserID: String,
        appInBackground: Boolean,
        completion: ReceiveOfferingsListener? = null
    ) {
        deviceCache.setOfferingsCacheTimestampToNow()
        backend.getOfferings(
            appUserID,
            appInBackground,
            { offeringsJSON ->
                try {
                    val jsonArrayOfOfferings = offeringsJSON.getJSONArray("offerings")
                    val skus = mutableSetOf<String>()
                    for (i in 0 until jsonArrayOfOfferings.length()) {
                        val jsonPackagesArray =
                            jsonArrayOfOfferings.getJSONObject(i).getJSONArray("packages")
                        for (j in 0 until jsonPackagesArray.length()) {
                            skus.add(
                                jsonPackagesArray.getJSONObject(j)
                                    .getString("platform_product_identifier")
                            )
                        }
                    }
                    getSkuDetails(skus, { detailsByID ->
                        val offerings =
                            offeringsJSON.createOfferings(detailsByID)
                        logMissingProducts(offerings, detailsByID)
                        synchronized(this@Purchases) {
                            deviceCache.cacheOfferings(offerings)
                        }
                        dispatch {
                            completion?.onReceived(offerings)
                        }
                    }, { error ->
                        handleErrorFetchingOfferings(error, completion)
                    })
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

    private fun handleErrorFetchingOfferings(
        error: PurchasesError,
        completion: ReceiveOfferingsListener?
    ) {
        log(LogIntent.GOOGLE_ERROR, OfferingStrings.FETCHING_OFFERINGS_ERROR.format(error))
        deviceCache.clearOfferingsCacheTimestamp()
        dispatch {
            completion?.onError(error)
        }
    }

    private fun logMissingProducts(
        offerings: Offerings,
        storeProductByID: HashMap<String, StoreProduct>
    ) = offerings.all.values
        .flatMap { it.availablePackages }
        .map { it.product.sku }
        .filterNot { storeProductByID.containsKey(it) }
        .takeIf { it.isNotEmpty() }
        ?.let { missingProducts ->
            log(LogIntent.GOOGLE_WARNING, OfferingStrings.CANNOT_FIND_PRODUCT_CONFIGURATION_ERROR
                    .format(missingProducts.joinToString(", ")))
        }

    private fun getSkus(
        skus: Set<String>,
        productType: ProductType,
        callback: GetStoreProductCallback
    ) {
        billing.querySkuDetailsAsync(
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
        completion: ReceiveCustomerInfoListener? = null
    ) {
        state.appInBackground.let { appInBackground ->
            fetchAndCacheCustomerInfo(appUserID, appInBackground, completion)
            fetchAndCacheOfferings(appUserID, appInBackground)
        }
    }

    private fun fetchAndCacheCustomerInfo(
        appUserID: String,
        appInBackground: Boolean,
        completion: ReceiveCustomerInfoListener? = null
    ) {
        deviceCache.setCustomerInfoCacheTimestampToNow(appUserID)
        backend.getCustomerInfo(
            appUserID,
            appInBackground,
            { info ->
                cacheCustomerInfo(info)
                sendUpdatedCustomerInfoToDelegateIfChanged(info)
                dispatch { completion?.onReceived(info) }
            },
            { error ->
                Log.e("Purchases", "Error fetching customer data: ${error.message}")
                deviceCache.clearCustomerInfoCacheTimestamp(appUserID)
                dispatch { completion?.onError(error) }
            })
    }

    @Synchronized
    private fun cacheCustomerInfo(info: CustomerInfo) {
        deviceCache.cacheCustomerInfo(identityManager.currentAppUserID, info)
    }

    private fun postPurchases(
        purchases: List<PaymentTransaction>,
        allowSharingPlayStoreAccount: Boolean,
        consumeAllTransactions: Boolean,
        appUserID: String,
        onSuccess: (SuccessfulPurchaseCallback)? = null,
        onError: (ErrorPurchaseCallback)? = null
    ) {
        purchases.forEach { purchase ->
            if (purchase.purchaseState == PurchaseState.PURCHASED) {
                billing.querySkuDetailsAsync(
                    productType = purchase.type,
                    skus = purchase.skus.toSet(),
                    onReceive = { storeProducts ->
                        postToBackend(
                            purchase = purchase,
                            storeProduct = storeProducts.takeUnless { it.isEmpty() }?.get(0),
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
                onError?.invoke(
                    purchase,
                    PurchasesError(PurchasesErrorCode.PaymentPendingError).also { errorLog(it) }
                )
            }
        }
    }

    @JvmSynthetic
    internal fun postToBackend(
        purchase: PaymentTransaction,
        storeProduct: StoreProduct?,
        allowSharingPlayStoreAccount: Boolean,
        consumeAllTransactions: Boolean,
        appUserID: String,
        onSuccess: (SuccessfulPurchaseCallback)? = null,
        onError: (ErrorPurchaseCallback)? = null
    ) {
        val unsyncedSubscriberAttributesByKey =
            subscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserID)
        val receiptInfo = ReceiptInfo(
            productIDs = purchase.skus,
            offeringIdentifier = purchase.presentedOfferingIdentifier,
            storeProduct = storeProduct
        )
        backend.postReceiptData(
            purchaseToken = purchase.purchaseToken,
            appUserID = appUserID,
            isRestore = allowSharingPlayStoreAccount,
            observerMode = !consumeAllTransactions,
            subscriberAttributes = unsyncedSubscriberAttributesByKey.toBackendMap(),
            receiptInfo = receiptInfo,
            storeAppUserID = purchase.storeUserID,
            onSuccess = { info, body ->
                subscriberAttributesManager.markAsSynced(
                    appUserID,
                    unsyncedSubscriberAttributesByKey,
                    body.getAttributeErrors()
                )
                billing.consumeAndSave(consumeAllTransactions, purchase)
                cacheCustomerInfo(info)
                sendUpdatedCustomerInfoToDelegateIfChanged(info)
                onSuccess?.let { it(purchase, info) }
            },
            onError = { error, errorIsFinishable, body ->
                if (errorIsFinishable) {
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

    private fun getSkuDetails(
        skus: Set<String>,
        onCompleted: (HashMap<String, StoreProduct>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        billing.querySkuDetailsAsync(
            ProductType.SUBS,
            skus,
            { subscriptionsSKUDetails ->
                val detailsByID = HashMap<String, StoreProduct>()
                val inAPPSkus =
                    skus - subscriptionsSKUDetails
                        .map { details -> details.sku to details }
                        .also { skuToDetails -> detailsByID.putAll(skuToDetails) }
                        .map { skuToDetails -> skuToDetails.first }

                if (inAPPSkus.isNotEmpty()) {
                    billing.querySkuDetailsAsync(
                        ProductType.INAPP,
                        inAPPSkus,
                        { skuDetails ->
                            detailsByID.putAll(skuDetails.map { it.sku to it })
                            onCompleted(detailsByID)
                        }, {
                            onError(it)
                        }
                    )
                } else {
                    onCompleted(detailsByID)
                }
            }, {
                onError(it)
            })
    }

    private fun afterSetListener(listener: UpdatedCustomerInfoListener?) {
        if (listener != null) {
            log(LogIntent.DEBUG, ConfigureStrings.LISTENER_SET)
            deviceCache.getCachedCustomerInfo(identityManager.currentAppUserID)?.let {
                this.sendUpdatedCustomerInfoToDelegateIfChanged(it)
            }
        }
    }

    private fun sendUpdatedCustomerInfoToDelegateIfChanged(info: CustomerInfo) {
        synchronized(this@Purchases) { state.updatedCustomerInfoListener to state.lastSentCustomerInfo }
            .let { (listener, lastSentCustomerInfo) ->
                if (listener != null && lastSentCustomerInfo != info) {
                    if (lastSentCustomerInfo != null) {
                        log(LogIntent.DEBUG, CustomerInfoStrings.CUSTOMERINFO_UPDATED_NOTIFYING_LISTENER)
                    } else {
                        log(LogIntent.DEBUG, CustomerInfoStrings.SENDING_LATEST_CUSTOMERINFO_TO_LISTENER)
                    }
                    synchronized(this@Purchases) {
                        state = state.copy(lastSentCustomerInfo = info)
                    }
                    dispatch { listener.onReceived(info) }
                }
            }
    }

    private val handler: Handler? = Handler(Looper.getMainLooper())
    private fun dispatch(action: () -> Unit) {
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            handler?.post(action) ?: Handler(Looper.getMainLooper()).post(action)
        } else {
            action()
        }
    }

    private fun getPurchaseCallback(sku: String): PurchaseCallback? {
        return state.purchaseCallbacks[sku].also {
            state =
                state.copy(purchaseCallbacks = state.purchaseCallbacks.filterNot { it.key == sku })
        }
    }

    private fun getAndClearProductChangeCallback(): ProductChangeCallback? {
        return state.productChangeCallback.also {
            state = state.copy(productChangeCallback = null)
        }
    }

    private fun getPurchasesUpdatedListener(): BillingAbstract.PurchasesUpdatedListener {
        return object : BillingAbstract.PurchasesUpdatedListener {
            override fun onPurchasesUpdated(purchases: List<PaymentTransaction>) {
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
                    } ?: state.purchaseCallbacks.let { purchaseCallbacks ->
                        state = state.copy(purchaseCallbacks = emptyMap())
                        purchaseCallbacks.values.forEach { it.dispatch(purchasesError) }
                    }
                }
            }
        }
    }

    private fun getPurchaseCompletedCallbacks(): Pair<SuccessfulPurchaseCallback, ErrorPurchaseCallback> {
        val onSuccess: SuccessfulPurchaseCallback = { paymentTransaction, info ->
            getPurchaseCallback(paymentTransaction.skus[0])?.let { purchaseCallback ->
                dispatch {
                    purchaseCallback.onCompleted(paymentTransaction, info)
                }
            }
        }
        val onError: ErrorPurchaseCallback = { purchase, error ->
            getPurchaseCallback(purchase.skus[0])?.dispatch(error)
        }

        return Pair(onSuccess, onError)
    }

    private fun getProductChangeCompletedCallbacks(
        productChangeListener: ProductChangeCallback?
    ): Pair<SuccessfulPurchaseCallback, ErrorPurchaseCallback> {
        val onSuccess: SuccessfulPurchaseCallback = { paymentTransaction, info ->
            productChangeListener?.let { productChangeCallback ->
                dispatch {
                    productChangeCallback.onCompleted(paymentTransaction, info)
                }
            }
        }
        val onError: ErrorPurchaseCallback = { _, error ->
            productChangeListener?.dispatch(error)
        }
        return Pair(onSuccess, onError)
    }

    private fun PurchaseErrorListener.dispatch(error: PurchasesError) {
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
        presentedOfferingIdentifier: String?,
        listener: PurchaseCallback
    ) {
        log(LogIntent.PURCHASE, PurchaseStrings.PURCHASE_STARTED.format(
                " $storeProduct ${presentedOfferingIdentifier?.let {
                    PurchaseStrings.OFFERING + "$presentedOfferingIdentifier"
                }}"
        ))
        var userPurchasing: String? = null // Avoids race condition for userid being modified before purchase is made
        synchronized(this@Purchases) {
            if (!appConfig.finishTransactions) {
                log(LogIntent.WARNING, PurchaseStrings.PURCHASE_FINISH_TRANSACTION_FALSE)
            }
            if (!state.purchaseCallbacks.containsKey(storeProduct.sku)) {
                state = state.copy(
                    purchaseCallbacks = state.purchaseCallbacks + mapOf(storeProduct.sku to listener)
                )
                userPurchasing = identityManager.currentAppUserID
            }
        }
        userPurchasing?.let { appUserID ->
            billing.makePurchaseAsync(
                activity,
                appUserID,
                storeProduct,
                null,
                presentedOfferingIdentifier
            )
        } ?: listener.dispatch(PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError).also { errorLog(it) })
    }

    private fun startProductChange(
        activity: Activity,
        storeProduct: StoreProduct,
        offeringIdentifier: String?,
        upgradeInfo: UpgradeInfo,
        listener: ProductChangeCallback
    ) {
        log(LogIntent.PURCHASE, PurchaseStrings.PRODUCT_CHANGE_STARTED.format(
                " $storeProduct ${offeringIdentifier?.let {
                    PurchaseStrings.OFFERING + "$offeringIdentifier"
                }} UpgradeInfo: $upgradeInfo"

        ))
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
        upgradeInfo: UpgradeInfo,
        activity: Activity,
        appUserID: String,
        presentedOfferingIdentifier: String?,
        listener: PurchaseErrorListener
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
        if (billing.isConnected()) {
            log(LogIntent.DEBUG, PurchaseStrings.UPDATING_PENDING_PURCHASE_QUEUE)
            dispatcher.enqueue({
                appUserID.let { appUserID ->
                    billing.queryPurchases(
                        appUserID,
                        onSuccess = { purchasesByHashedToken ->
                            purchasesByHashedToken.forEach { (hash, purchase) ->
                                log(LogIntent.DEBUG,
                                    RestoreStrings.QUERYING_PURCHASE_WITH_HASH.format(purchase.type, hash))
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

        @JvmSynthetic
        internal var postponedAttributionData = mutableListOf<AttributionData>()

        /**
         * Enable debug logging. Useful for debugging issues with the lovely team @RevenueCat
         */
        @JvmStatic
        var debugLogsEnabled
            get() = Config.debugLogsEnabled
            set(value) {
                Config.debugLogsEnabled = value
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
                val iterator = postponedAttributionData.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    value.postAttributionData(next.data, next.network, next.networkUserId)
                    iterator.remove()
                }
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
         * @param apiKey The API Key generated for your app from https://app.revenuecat.com/
         * @param appUserID Optional user identifier. Use this if your app has an account system.
         * If `null` `[Purchases] will generate a unique identifier for the current device and persist
         * it the SharedPreferences. This also affects the behavior of [restorePurchases].
         * @param observerMode Optional boolean set to FALSE by default. Set to TRUE if you are using your own
         * subscription system and you want to use RevenueCat's backend only. If set to TRUE, you should
         * be consuming and acknowledging transactions outside of the Purchases SDK.
         * @param service Optional [ExecutorService] to use for the backend calls.
         * @return An instantiated `[Purchases] object that has been set as a singleton.
         */
        @JvmOverloads
        @JvmStatic
        fun configure(
            context: Context,
            apiKey: String,
            appUserID: String? = null,
            observerMode: Boolean = false,
            service: ExecutorService = createDefaultExecutor()
        ): Purchases {
            val builtConfiguration = PurchasesConfiguration.Builder(context, apiKey)
                .appUserID(appUserID)
                .observerMode(observerMode)
                .service(service)
                .build()
            return configure(builtConfiguration)
        }

        /**
         * Configures an instance of the Purchases SDK with a specified API key. The instance will
         * be set as a singleton. You should access the singleton instance using [Purchases.sharedInstance]
         * @param configuration TODO
         * @return An instantiated `[Purchases] object that has been set as a singleton.
         */
        @JvmStatic
        @JvmSynthetic
        internal fun configure(
            configuration: PurchasesConfiguration
        ): Purchases {
            with(configuration) {
                require(context.hasPermission(Manifest.permission.INTERNET)) {
                    "Purchases requires INTERNET permission."
                }

                require(!apiKey.isBlank()) { "API key must be set. Get this from the RevenueCat web app" }

                require(context.applicationContext is Application) { "Needs an application context." }
                val application = context.getApplication()
                val appConfig = AppConfig(
                    context,
                    observerMode,
                    platformInfo,
                    proxyURL,
                    store
                )

                val prefs = PreferenceManager.getDefaultSharedPreferences(application)

                val sharedPreferencesForETags = ETagManager.initializeSharedPreferences(context)
                val eTagManager = ETagManager(sharedPreferencesForETags)

                val dispatcher = Dispatcher(service ?: createDefaultExecutor())
                val backend = Backend(
                    apiKey,
                    dispatcher,
                    HTTPClient(appConfig, eTagManager)
                )
                val subscriberAttributesPoster = SubscriberAttributesPoster(backend)

                val cache = DeviceCache(prefs, apiKey)

                val billing: BillingAbstract = BillingFactory.createBilling(store, application, backend, cache)
                val attributionFetcher = AttributionFetcherFactory.createAttributionFetcher(store, dispatcher)

                val subscriberAttributesCache = SubscriberAttributesCache(cache)
                val attributionDataMigrator = AttributionDataMigrator()
                return Purchases(
                    application,
                    appUserID,
                    backend,
                    billing,
                    cache,
                    dispatcher,
                    IdentityManager(cache, subscriberAttributesCache, backend),
                    SubscriberAttributesManager(
                        subscriberAttributesCache,
                        subscriberAttributesPoster,
                        attributionFetcher,
                        attributionDataMigrator
                    ),
                    appConfig
                ).also {
                    @SuppressLint("RestrictedApi")
                    sharedInstance = it
                }
            }
        }

        /**
         * Check if billing is supported for the current Play user (meaning IN-APP purchases are supported)
         * and optionally, whether a list of specified feature types are supported. This method is asynchronous
         * since it requires a connected BillingClient.
         * @param context A context object that will be used to connect to the billing client
         * @param feature A list of feature types to check for support. Feature types must be one of [BillingFeature]
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

                                        var featureSupportedResultOk = features.all {
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
                                val mainHandler = Handler(context.mainLooper)
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

        /**
         * Check if billing is supported in the device. This method is asynchronous since it tries
         * to connect the billing client and checks for the result of the connection.
         * If billing is supported, IN-APP purchases are supported.
         * If you want to check if SUBSCRIPTIONS or other type defined in [BillingClient.FeatureType],
         * call [isFeatureSupported].
         * @param context A context object that will be used to connect to the billing client
         * @param callback Callback that will be notified when the check is complete.
         */
        @Deprecated(
            message = "use canMakePayments instead",
            replaceWith = ReplaceWith("canMakePayments(context, features, Callback<Boolean>)")
        )
        @JvmStatic
        fun isBillingSupported(context: Context, callback: Callback<Boolean>) {
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
                                    // It also means that IN-APP items are supported for purchasing
                                    try {
                                        billingClient.endConnection()
                                        val resultIsOK = billingResult.isSuccessful()
                                        callback.onReceived(resultIsOK)
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

        /**
         * Use this method if you want to check if Subscriptions or other type defined in
         * [BillingClient.FeatureType] is supported.
         * This method is asynchronous since it requires a connected billing client.
         * @param feature A feature type to check for support. Must be one of [BillingClient.FeatureType]
         * @param context A context object that will be used to connect to the billing client
         * @param callback Callback that will be notified when the check is complete.
         */
        @Deprecated(
            message = "use canMakePayments instead",
            replaceWith = ReplaceWith("canMakePayments(context, features, Callback<Boolean>)")
        )
        @JvmStatic
        fun isFeatureSupported(
            @BillingClient.FeatureType feature: String,
            context: Context,
            callback: Callback<Boolean>
        ) {
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
                                        val featureSupportedResult = billingClient.isFeatureSupported(feature)
                                        billingClient.endConnection()
                                        val responseIsOK = featureSupportedResult.isSuccessful()
                                        callback.onReceived(responseIsOK)
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

        /**
         * Add attribution data from a supported network
         * @param [data] JSONObject containing the data to post to the attribution network
         * @param [network] [AttributionNetwork] to post the data to
         * @param [networkUserId] User Id that should be sent to the network. Default is the current App User Id
         */
        @Deprecated(
            "Use the .set<NetworkId> functions instead",
            ReplaceWith(".set<NetworkId>")
        )
        @JvmStatic
        fun addAttributionData(
            data: JSONObject,
            network: AttributionNetwork,
            networkUserId: String? = null
        ) {
            backingFieldSharedInstance?.postAttributionData(data, network.convert(), networkUserId) ?: {
                postponedAttributionData.add(
                    AttributionData(
                        data,
                        network.convert(),
                        networkUserId
                    )
                )
            }.invoke()
        }

        /**
         * Add attribution data from a supported network
         * @param [data] Map containing the data to post to the attribution network
         * @param [network] [AttributionNetwork] to post the data to
         * @param [networkUserId] User Id that should be sent to the network. Default is the current App User Id
         */
        @Deprecated(
            "Use the .set<NetworkId> functions instead",
            ReplaceWith(".set<NetworkId>")
        )
        @JvmStatic
        fun addAttributionData(
            data: Map<String, Any?>,
            network: AttributionNetwork,
            networkUserId: String? = null
        ) {
            val jsonObject = JSONObject()
            for (key in data.keys) {
                try {
                    data[key]?.let {
                        jsonObject.put(key, it)
                    } ?: jsonObject.put(key, JSONObject.NULL)
                } catch (e: JSONException) {
                    Log.e("Purchases", "Failed to add key $key to attribution map")
                }
            }
            this.addAttributionData(jsonObject, network, networkUserId)
        }

        private fun Context.getApplication() = applicationContext as Application

        private fun Context.hasPermission(permission: String): Boolean {
            return checkCallingOrSelfPermission(permission) == PERMISSION_GRANTED
        }

        private fun createDefaultExecutor(): ExecutorService {
            return Executors.newSingleThreadScheduledExecutor()
        }
    }

    /**
     * Different compatible attribution networks available
     * @param serverValue Id of this attribution network in the RevenueCat server
     */
    @Suppress("unused", "MagicNumber")
    enum class AttributionNetwork(val serverValue: Int) {
        /**
         * [https://www.adjust.com/]
         */
        ADJUST(1),

        /**
         * [https://www.appsflyer.com/]
         */
        APPSFLYER(2),

        /**
         * [http://branch.io/]
         */
        BRANCH(3),

        /**
         * [http://tenjin.io/]
         */
        TENJIN(4),

        /**
         * [https://developers.facebook.com/]
         */
        FACEBOOK(5),

        /**
         * [https://www.mparticle.com/]
         */
        MPARTICLE(6)
    }

    // endregion
}

internal fun Purchases.AttributionNetwork.convert(): CommonAttributionNetwork {
    return CommonAttributionNetwork.values()
        .first { attributionNetwork -> attributionNetwork.serverValue == this.serverValue }
}
