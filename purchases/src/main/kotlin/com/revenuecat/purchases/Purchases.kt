//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingWrapper
import com.revenuecat.purchases.common.Config
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.ProductInfo
import com.revenuecat.purchases.common.PurchaseHistoryRecordWrapper
import com.revenuecat.purchases.common.PurchaseType
import com.revenuecat.purchases.common.PurchaseWrapper
import com.revenuecat.purchases.common.ReplaceSkuInfo
import com.revenuecat.purchases.common.attribution.AttributionData
import com.revenuecat.purchases.common.billingResponseToPurchasesError
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.createOfferings
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.getBillingResponseCodeName
import com.revenuecat.purchases.common.isSuccessful
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.toHumanReadableDescription
import com.revenuecat.purchases.common.toSKUType
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.Callback
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesBackend
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.subscriberattributes.getAttributeErrors
import com.revenuecat.purchases.subscriberattributes.toBackendMap
import com.revenuecat.purchases.util.AdvertisingIdClient
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import java.util.Collections.emptyMap
import java.util.HashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import com.revenuecat.purchases.common.attribution.AttributionNetwork as CommonAttributionNetwork

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
    private val billingWrapper: BillingWrapper,
    private val deviceCache: DeviceCache,
    private val executorService: ExecutorService,
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
     * Make sure [removeUpdatedPurchaserInfoListener] is called when the listener needs to be destroyed.
     */
    var updatedPurchaserInfoListener: UpdatedPurchaserInfoListener?
        @Synchronized get() = state.updatedPurchaserInfoListener
        set(value) {
            synchronized(this@Purchases) {
                state = state.copy(updatedPurchaserInfoListener = value)
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
        debugLog("Debug logging enabled.")
        debugLog("SDK Version - $frameworkVersion")
        debugLog("Initial App User ID - $backingFieldAppUserID")
        identityManager.configure(backingFieldAppUserID)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleHandler)
        billingWrapper.stateListener = object : BillingWrapper.StateListener {
            override fun onConnected() {
                updatePendingPurchaseQueue()
            }
        }
        billingWrapper.purchasesUpdatedListener = getPurchasesUpdatedListener()
    }

    /** @suppress */
    override fun onAppBackgrounded() {
        debugLog("App backgrounded")
        synchronizeSubscriberAttributesIfNeeded()
    }

    /** @suppress */
    override fun onAppForegrounded() {
        debugLog("App foregrounded")
        if (deviceCache.isPurchaserInfoCacheStale()) {
            debugLog("PurchaserInfo cache is stale, updating caches")
            fetchAndCachePurchaserInfo(identityManager.currentAppUserID)
        }
        if (deviceCache.isOfferingsCacheStale()) {
            debugLog("Offerings cache is stale, updating caches")
            fetchAndCacheOfferings(identityManager.currentAppUserID)
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
        debugLog("Syncing purchases")
        billingWrapper.queryAllPurchases({ allPurchases ->
            if (allPurchases.isNotEmpty()) {
                identityManager.currentAppUserID.let { appUserID ->
                    allPurchases.forEach { purchase ->
                        val unsyncedSubscriberAttributesByKey =
                            subscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserID)
                        val productInfo = ProductInfo(productID = purchase.sku)
                        backend.postReceiptData(
                            purchaseToken = purchase.purchaseToken,
                            appUserID = appUserID,
                            isRestore = this.allowSharingPlayStoreAccount,
                            observerMode = !this.finishTransactions,
                            subscriberAttributes = unsyncedSubscriberAttributesByKey.toBackendMap(),
                            productInfo = productInfo,
                            onSuccess = { info, body ->
                                subscriberAttributesManager.markAsSynced(
                                    appUserID,
                                    unsyncedSubscriberAttributesByKey,
                                    body.getAttributeErrors()
                                )
                                deviceCache.addSuccessfullyPostedToken(purchase.purchaseToken)
                                cachePurchaserInfo(info)
                                sendUpdatedPurchaserInfoToDelegateIfChanged(info)
                                debugLog("Purchase $purchase synced")
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
                                errorLog("Error syncing purchase: $purchase; Error: $error")
                            }
                        )
                    }
                }
            }
        }, { errorLog("Error syncing purchases $it") })
    }

    @JvmName("-deprecated_getEntitlements")
    @Deprecated(
        message = "moved to getOfferings()",
        replaceWith = ReplaceWith(expression = "getOfferings(listener)"),
        level = DeprecationLevel.ERROR
    )
    fun getEntitlements() {
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
        synchronized(this@Purchases) {
            identityManager.currentAppUserID to deviceCache.cachedOfferings
        }.let { (appUserID, cachedOfferings) ->
            if (cachedOfferings == null) {
                debugLog("No cached offerings, fetching")
                fetchAndCacheOfferings(appUserID, listener)
            } else {
                debugLog("Vending offerings from cache")
                dispatch {
                    listener.onReceived(cachedOfferings)
                }
                if (deviceCache.isOfferingsCacheStale()) {
                    debugLog("Offerings cache is stale, updating cache")
                    fetchAndCacheOfferings(appUserID)
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
        getSkus(skus, BillingClient.SkuType.SUBS, listener)
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
        getSkus(skus, BillingClient.SkuType.INAPP, listener)
    }

    /**
     * Purchase a product.
     * @param [activity] Current activity
     * @param [skuDetails] The skuDetails of the product you wish to purchase
     * @param [upgradeInfo] The UpgradeInfo you wish to upgrade from containing the
     * oldSku and the optional prorationMode.
     * @param [listener] The listener that will be called when purchase completes.
     */
    fun purchaseProduct(
        activity: Activity,
        skuDetails: SkuDetails,
        upgradeInfo: UpgradeInfo,
        listener: MakePurchaseListener
    ) {
        startPurchase(activity, skuDetails, null, upgradeInfo, listener)
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
        startPurchase(activity, skuDetails, null, null, listener)
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [packageToPurchase] The Package you wish to purchase
     * @param [upgradeInfo] The UpgradeInfo you wish to upgrade from containing the oldSku
     * and the optional prorationMode.
     * @param [listener] The listener that will be called when purchase completes.
     */
    fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package,
        upgradeInfo: UpgradeInfo,
        listener: MakePurchaseListener
    ) {
        startPurchase(
            activity,
            packageToPurchase.product,
            packageToPurchase.offering,
            upgradeInfo,
            listener
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
        startPurchase(
            activity,
            packageToPurchase.product,
            packageToPurchase.offering,
            null,
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
        listener: ReceivePurchaserInfoListener
    ) {
        debugLog("Restoring purchases")
        if (!allowSharingPlayStoreAccount) {
            debugLog("allowSharingPlayStoreAccount is set to false and restorePurchases" +
                " has been called. This will 'alias' any app user id's sharing the same receipt. " +
                "Are you sure you want to do this?")
        }
        this.finishTransactions.let { finishTransactions ->
            billingWrapper.queryAllPurchases(
                { allPurchases ->
                    if (allPurchases.isEmpty()) {
                        getPurchaserInfo(listener)
                    } else {
                        allPurchases.sortedBy { it.purchaseTime }.let { sortedByTime ->
                            identityManager.currentAppUserID.let { appUserID ->
                                sortedByTime.forEach { purchase ->
                                    val unsyncedSubscriberAttributesByKey =
                                        subscriberAttributesManager.getUnsyncedSubscriberAttributes(
                                            appUserID
                                        )
                                    val productInfo = ProductInfo(productID = purchase.sku)
                                    backend.postReceiptData(
                                        purchaseToken = purchase.purchaseToken,
                                        appUserID = appUserID,
                                        isRestore = true,
                                        observerMode = !finishTransactions,
                                        subscriberAttributes = unsyncedSubscriberAttributesByKey.toBackendMap(),
                                        productInfo = productInfo,
                                        onSuccess = { info, body ->
                                            subscriberAttributesManager.markAsSynced(
                                                appUserID,
                                                unsyncedSubscriberAttributesByKey,
                                                body.getAttributeErrors()
                                            )
                                            consumeAndSave(finishTransactions, purchase)
                                            cachePurchaserInfo(info)
                                            sendUpdatedPurchaserInfoToDelegateIfChanged(info)
                                            debugLog("Purchase $purchase restored")
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
                                                consumeAndSave(finishTransactions, purchase)
                                            }
                                            errorLog("Error restoring purchase: $purchase; Error: $error")
                                            if (sortedByTime.last() == purchase) {
                                                dispatch { listener.onError(error) }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                { error ->
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
    @JvmOverloads
    fun createAlias(
        newAppUserID: String,
        listener: ReceivePurchaserInfoListener? = null
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
        } ?: retrievePurchaseInfo(identityManager.currentAppUserID, listener)
    }

    /**
     * This function will change the current appUserID.
     * Typically this would be used after a log out to identify a new user without calling configure
     * @param newAppUserID The new appUserID that should be linked to the currently user
     * @param [listener] An optional listener to listen for successes or errors.
     */
    @JvmOverloads
    fun identify(
        newAppUserID: String,
        listener: ReceivePurchaserInfoListener? = null
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
        } ?: retrievePurchaseInfo(identityManager.currentAppUserID, listener)
    }

    /**
     * Resets the Purchases client clearing the save appUserID. This will generate a random user
     * id and save it in the cache.
     * @param [listener] An optional listener to listen for successes or errors.
     */
    @JvmOverloads
    fun reset(
        listener: ReceivePurchaserInfoListener? = null
    ) {
        deviceCache.clearLatestAttributionData(identityManager.currentAppUserID)
        identityManager.reset()
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
        billingWrapper.purchasesUpdatedListener = null
        updatedPurchaserInfoListener = null // Do not call on state since the setter does more stuff
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleHandler)
    }

    /**
     * Get latest available purchaser info.
     * @param listener A listener called when purchaser info is available and not stale.
     * Called immediately if purchaser info is cached. Purchaser info can be null if an error occurred.
     */
    fun getPurchaserInfo(
        listener: ReceivePurchaserInfoListener
    ) {
        retrievePurchaseInfo(identityManager.currentAppUserID, listener)
    }

    private fun retrievePurchaseInfo(
        appUserID: String,
        listener: ReceivePurchaserInfoListener? = null
    ) {
        val cachedPurchaserInfo = deviceCache.getCachedPurchaserInfo(appUserID)
        if (cachedPurchaserInfo != null) {
            debugLog("Vending purchaserInfo from cache")
            dispatch { listener?.onReceived(cachedPurchaserInfo) }
            if (deviceCache.isPurchaserInfoCacheStale()) {
                debugLog("Cache is stale, updating caches")
                fetchAndCachePurchaserInfo(appUserID)
            }
        } else {
            debugLog("No cached purchaser info, fetching")
            fetchAndCachePurchaserInfo(appUserID, listener)
        }
    }

    /**
     * Call this when you are finished using the [UpdatedPurchaserInfoListener]. You should call this
     * to avoid memory leaks.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun removeUpdatedPurchaserInfoListener() {
        // Don't set on state directly since setter does more things
        this.updatedPurchaserInfoListener = null
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
    fun invalidatePurchaserInfoCache() {
        debugLog("Invalidating Purchaser info cache")
        deviceCache.clearPurchaserInfoCacheTimestamp()
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
        debugLog("setAttributes called")
        subscriberAttributesManager.setAttributes(attributes, appUserID)
    }

    /**
     * Subscriber attribute associated with the Email address for the user
     *
     * @param email Null or empty will delete the subscriber attribute.
     */
    fun setEmail(email: String?) {
        debugLog("setEmail called")
        subscriberAttributesManager.setAttribute(SubscriberAttributeKey.Email, email, appUserID)
    }

    /**
     * Subscriber attribute associated with the phone number for the user
     *
     * @param phoneNumber Null or empty will delete the subscriber attribute.
     */
    fun setPhoneNumber(phoneNumber: String?) {
        debugLog("setPhoneNumber called")
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
        debugLog("setDisplayName called")
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
        debugLog("setPushToken called")
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.FCMTokens,
            fcmToken,
            appUserID
        )
    }

    // endregion
    // region Attribution IDs

    fun collectDeviceIdentifiers() {
        executeOnBackground {
            subscriberAttributesManager.collectDeviceIdentifiers(appUserID, application)
        }
    }

    fun setAdjustID(adjustID: String?) {
        debugLog("setAdjustID called")
        executeOnBackground {
            subscriberAttributesManager.setAttributionID(
                SubscriberAttributeKey.AttributionIds.Adjust,
                adjustID,
                appUserID,
                application
            )
        }
    }

    fun setAppsflyerID(appsflyerID: String?) {
        debugLog("setAppsflyerId called")
        executeOnBackground {
            subscriberAttributesManager.setAttributionID(
                SubscriberAttributeKey.AttributionIds.AppsFlyer,
                appsflyerID,
                appUserID,
                application
            )
        }
    }

    fun setFBAnonymousID(fbAnonymousID: String?) {
        debugLog("setFBAnonymousID called")
        executeOnBackground {
            subscriberAttributesManager.setAttributionID(
                SubscriberAttributeKey.AttributionIds.Facebook,
                fbAnonymousID,
                appUserID,
                application
            )
        }
    }

    fun setMparticleID(mparticleID: String?) {
        debugLog("setMparticleID called")
        executeOnBackground {
            subscriberAttributesManager.setAttributionID(
                SubscriberAttributeKey.AttributionIds.Mparticle,
                mparticleID,
                appUserID,
                application
            )
        }
    }

    // endregion
    // region Campaign parameters

    fun setMediaSource(mediaSource: String?) {
        debugLog("setMediaSource called")
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.MediaSource,
            mediaSource,
            appUserID
        )
    }

    fun setCampaign(campaign: String?) {
        debugLog("setCampaign called")
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Campaign,
            campaign,
            appUserID
        )
    }

    fun setAdGroup(adGroup: String?) {
        debugLog("setAdGroup called")
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.AdGroup,
            adGroup,
            appUserID
        )
    }

    fun setAd(ad: String?) {
        debugLog("setAd called")
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Ad,
            ad,
            appUserID
        )
    }

    fun setKeyword(keyword: String?) {
        debugLog("setKeyword called")
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Keyword,
            keyword,
            appUserID
        )
    }

    fun setCreative(creative: String?) {
        debugLog("setCreative called")
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Creative,
            creative,
            appUserID
        )
    }

    //endregion
    //endregion

    @JvmName("-deprecated_makePurchase")
    @Deprecated(
        message = "moved to purchaseProduct()",
        replaceWith = ReplaceWith(expression = "purchaseProduct(activity, skuDetails, upgradeInfo, listener)"),
        level = DeprecationLevel.ERROR
    )
    fun makePurchase(
        activity: Activity,
        skuDetails: SkuDetails,
        oldSku: String,
        listener: MakePurchaseListener
    ) {
        purchaseProduct(activity, skuDetails, UpgradeInfo(oldSku), listener)
    }

    @JvmName("-deprecated_makePurchase")
    @Deprecated(
        message = "moved to purchaseProduct()",
        replaceWith = ReplaceWith(expression = "purchaseProduct(activity, skuDetails, listener)"),
        level = DeprecationLevel.ERROR
    )
    fun makePurchase(
        activity: Activity,
        skuDetails: SkuDetails,
        listener: MakePurchaseListener
    ) {
        purchaseProduct(activity, skuDetails, listener)
    }

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
                    debugLog("Attribution data is the same as latest. Skipping.")
                } else {
                    if (adInfo?.isLimitAdTrackingEnabled == false) {
                        jsonObject.put("rc_gps_adid", adInfo.id)
                    }

                    jsonObject.put("rc_attribution_network_id", networkUserId)

                    backend.postAttributionData(appUserID, network, jsonObject) {
                        deviceCache.cacheAttributionData(network, appUserID, newCacheValue)
                    }
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
        completion: ReceiveOfferingsListener? = null
    ) {
        deviceCache.setOfferingsCacheTimestampToNow()
        backend.getOfferings(
            appUserID,
            { offeringsJSON ->
                try {
                    val jsonArrayOfOfferings = offeringsJSON.getJSONArray("offerings")
                    val skus = mutableListOf<String>()
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
                    errorLog("JSONException when building Offerings object. Message: ${ error.localizedMessage }")
                    handleErrorFetchingOfferings(
                        PurchasesError(
                            PurchasesErrorCode.UnexpectedBackendResponseError,
                            error.localizedMessage
                        ).also { errorLog(it) },
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
        log("Error fetching offerings - $error")
        deviceCache.clearOfferingsCacheTimestamp()
        dispatch {
            completion?.onError(error)
        }
    }

    private fun logMissingProducts(
        offerings: Offerings,
        detailsByID: HashMap<String, SkuDetails>
    ) = offerings.all.values
        .flatMap { it.availablePackages }
        .map { it.product.sku }
        .filterNot { detailsByID.containsKey(it) }
        .takeIf { it.isNotEmpty() }
        ?.let { missingProducts ->
            log("Could not find SkuDetails for ${missingProducts.joinToString(", ")}")
            log("Ensure your products are correctly configured in Play Store Developer Console")
        }

    private fun getSkus(
        skus: List<String>,
        @BillingClient.SkuType skuType: String,
        completion: GetSkusResponseListener
    ) {
        billingWrapper.querySkuDetailsAsync(
            skuType,
            skus,
            { skuDetails ->
                dispatch {
                    completion.onReceived(skuDetails)
                }
            }, {
                dispatch {
                    completion.onError(it)
                }
            })
    }

    private fun updateAllCaches(
        appUserID: String,
        completion: ReceivePurchaserInfoListener? = null
    ) {
        fetchAndCachePurchaserInfo(appUserID, completion)
        fetchAndCacheOfferings(appUserID)
    }

    private fun fetchAndCachePurchaserInfo(
        appUserID: String,
        completion: ReceivePurchaserInfoListener? = null
    ) {
        deviceCache.setPurchaserInfoCacheTimestampToNow()
        backend.getPurchaserInfo(
            appUserID,
            { info ->
                cachePurchaserInfo(info)
                sendUpdatedPurchaserInfoToDelegateIfChanged(info)
                dispatch { completion?.onReceived(info) }
            },
            { error ->
                Log.e("Purchases", "Error fetching subscriber data: ${error.message}")
                deviceCache.clearPurchaserInfoCacheTimestamp()
                dispatch { completion?.onError(error) }
            })
    }

    @Synchronized
    private fun cachePurchaserInfo(info: PurchaserInfo) {
        deviceCache.cachePurchaserInfo(identityManager.currentAppUserID, info)
    }

    private fun postPurchases(
        purchases: List<PurchaseWrapper>,
        allowSharingPlayStoreAccount: Boolean,
        consumeAllTransactions: Boolean,
        appUserID: String,
        onSuccess: ((PurchaseWrapper, PurchaserInfo) -> Unit)? = null,
        onError: ((PurchaseWrapper, PurchasesError) -> Unit)? = null
    ) {
        purchases.forEach { purchase ->
            if (purchase.containedPurchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                billingWrapper.querySkuDetailsAsync(
                    itemType = purchase.type.toSKUType() ?: BillingClient.SkuType.INAPP,
                    skuList = listOf(purchase.sku),
                    onReceiveSkuDetails = { skuDetailsList ->
                        postToBackend(
                            purchase = purchase,
                            skuDetails = skuDetailsList.firstOrNull { it.sku == purchase.sku },
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
                            skuDetails = null,
                            allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                            consumeAllTransactions = consumeAllTransactions,
                            appUserID = appUserID,
                            onSuccess = onSuccess,
                            onError = onError
                        )
                    }
                )
            } else {
                onError?.let { onError ->
                    onError(
                        purchase,
                        PurchasesError(PurchasesErrorCode.PaymentPendingError).also { errorLog(it) }
                    )
                }
            }
        }
    }

    @JvmSynthetic
    internal fun postToBackend(
        purchase: PurchaseWrapper,
        skuDetails: SkuDetails?,
        allowSharingPlayStoreAccount: Boolean,
        consumeAllTransactions: Boolean,
        appUserID: String,
        onSuccess: ((PurchaseWrapper, PurchaserInfo) -> Unit)? = null,
        onError: ((PurchaseWrapper, PurchasesError) -> Unit)? = null
    ) {
        val unsyncedSubscriberAttributesByKey =
            subscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserID)
        val productInfo = ProductInfo(
            productID = purchase.sku,
            offeringIdentifier = purchase.presentedOfferingIdentifier,
            skuDetails = skuDetails
        )
        backend.postReceiptData(
            purchaseToken = purchase.purchaseToken,
            appUserID = appUserID,
            isRestore = allowSharingPlayStoreAccount,
            observerMode = !consumeAllTransactions,
            subscriberAttributes = unsyncedSubscriberAttributesByKey.toBackendMap(),
            productInfo = productInfo,
            onSuccess = { info, body ->
                subscriberAttributesManager.markAsSynced(
                    appUserID,
                    unsyncedSubscriberAttributesByKey,
                    body.getAttributeErrors()
                )
                consumeAndSave(consumeAllTransactions, purchase)
                cachePurchaserInfo(info)
                sendUpdatedPurchaserInfoToDelegateIfChanged(info)
                onSuccess?.let { it(purchase, info) }
            },
            onError = { error, errorIsFinishable, body ->
                if (errorIsFinishable) {
                    subscriberAttributesManager.markAsSynced(
                        appUserID,
                        unsyncedSubscriberAttributesByKey,
                        body.getAttributeErrors()
                    )
                    consumeAndSave(consumeAllTransactions, purchase)
                }
                onError?.let { it(purchase, error) }
            }
        )
    }

    private fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: PurchaseWrapper
    ) {
        if (purchase.type == PurchaseType.UNKNOWN) {
            // Would only get here if the purchase was trigger from outside of the app and there was
            // an issue getting the purchase type
            return
        }
        if (purchase.containedPurchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            // PENDING purchases should not be acknowledged or consumed
            return
        }
        if (shouldTryToConsume && purchase.isConsumable) {
            billingWrapper.consumePurchase(purchase.purchaseToken) { billingResult, purchaseToken ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    deviceCache.addSuccessfullyPostedToken(purchaseToken)
                } else {
                    debugLog("Error consuming purchase. Will retry next queryPurchases. " +
                        "${billingResult.toHumanReadableDescription()}")
                }
            }
        } else if (shouldTryToConsume && !purchase.containedPurchase.isAcknowledged) {
            billingWrapper.acknowledge(purchase.purchaseToken) { billingResult, purchaseToken ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    deviceCache.addSuccessfullyPostedToken(purchaseToken)
                } else {
                    debugLog("Error acknowledging purchase. Will retry next queryPurchases. " +
                        "${billingResult.toHumanReadableDescription()}")
                }
            }
        } else {
            deviceCache.addSuccessfullyPostedToken(purchase.purchaseToken)
        }
    }

    private fun consumeAndSave(
        shouldTryToConsume: Boolean,
        purchase: PurchaseHistoryRecordWrapper
    ) {
        if (purchase.type == PurchaseType.UNKNOWN) {
            // Would only get here if the purchase was trigger from outside of the app and there was
            // an issue getting the purchase type
            return
        }
        if (shouldTryToConsume && purchase.isConsumable) {
            billingWrapper.consumePurchase(purchase.purchaseToken) { billingResult, purchaseToken ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    deviceCache.addSuccessfullyPostedToken(purchaseToken)
                } else {
                    debugLog("Error consuming purchase. Will retry next queryPurchases. " +
                        "${billingResult.toHumanReadableDescription()}")
                }
            }
        } else if (shouldTryToConsume) {
            billingWrapper.acknowledge(purchase.purchaseToken) { billingResult, purchaseToken ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    deviceCache.addSuccessfullyPostedToken(purchaseToken)
                } else {
                    debugLog("Error acknowledging purchase. Will retry next queryPurchases. " +
                        "${billingResult.toHumanReadableDescription()}")
                }
            }
        } else {
            deviceCache.addSuccessfullyPostedToken(purchase.purchaseToken)
        }
    }

    private fun getSkuDetails(
        skus: List<String>,
        onCompleted: (HashMap<String, SkuDetails>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        billingWrapper.querySkuDetailsAsync(
            BillingClient.SkuType.SUBS,
            skus,
            { subscriptionsSKUDetails ->
                val detailsByID = HashMap<String, SkuDetails>()
                val inAPPSkus =
                    skus - subscriptionsSKUDetails
                        .map { details -> details.sku to details }
                        .also { skuToDetails -> detailsByID.putAll(skuToDetails) }
                        .map { skuToDetails -> skuToDetails.first }

                if (inAPPSkus.isNotEmpty()) {
                    billingWrapper.querySkuDetailsAsync(
                        BillingClient.SkuType.INAPP,
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

    private fun afterSetListener(listener: UpdatedPurchaserInfoListener?) {
        if (listener != null) {
            debugLog("Listener set")
            deviceCache.getCachedPurchaserInfo(identityManager.currentAppUserID)?.let {
                this.sendUpdatedPurchaserInfoToDelegateIfChanged(it)
            }
        }
    }

    private fun sendUpdatedPurchaserInfoToDelegateIfChanged(info: PurchaserInfo) {
        synchronized(this@Purchases) { state.updatedPurchaserInfoListener to state.lastSentPurchaserInfo }
            .let { (listener, lastSentPurchaserInfo) ->
                if (listener != null && lastSentPurchaserInfo != info) {
                    if (lastSentPurchaserInfo != null) {
                        debugLog("Purchaser info updated, sending to listener")
                    } else {
                        debugLog("Sending latest purchaser info to listener")
                    }
                    synchronized(this@Purchases) {
                        state = state.copy(lastSentPurchaserInfo = info)
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

    private fun executeOnBackground(command: () -> Unit) {
        synchronized(executorService) {
            if (!executorService.isShutdown) {
                executorService.execute(command)
            }
        }
    }

    @Synchronized
    private fun getPurchaseCallback(sku: String): MakePurchaseListener? {
        return state.purchaseCallbacks[sku].also {
            state =
                state.copy(purchaseCallbacks = state.purchaseCallbacks.filterNot { it.key == sku })
        }
    }

    private fun getPurchasesUpdatedListener(): BillingWrapper.PurchasesUpdatedListener {
        return object : BillingWrapper.PurchasesUpdatedListener {
            override fun onPurchasesUpdated(purchases: List<@JvmSuppressWildcards PurchaseWrapper>) {
                postPurchases(
                    purchases,
                    allowSharingPlayStoreAccount,
                    finishTransactions,
                    appUserID,
                    { purchaseWrapper, info ->
                        getPurchaseCallback(purchaseWrapper.sku)?.let { callback ->
                            dispatch {
                                callback.onCompleted(purchaseWrapper.containedPurchase, info)
                            }
                        }
                    },
                    { purchase, error ->
                        getPurchaseCallback(purchase.sku)?.let { callback ->
                            dispatch {
                                callback.onError(
                                    error,
                                    error.code == PurchasesErrorCode.PurchaseCancelledError
                                )
                            }
                        }
                    }
                )
            }

            override fun onPurchasesFailedToUpdate(
                purchases: List<Purchase>?,
                @BillingClient.BillingResponseCode responseCode: Int,
                message: String
            ) {
                synchronized(this@Purchases) {
                    state.purchaseCallbacks.also {
                        state = state.copy(purchaseCallbacks = emptyMap())
                    }
                }.let { purchaseCallbacks ->
                    purchaseCallbacks.forEach { (_, callback) ->
                        val purchasesError = responseCode.billingResponseToPurchasesError(message)
                            .also { errorLog(it) }
                        dispatch {
                            callback.onError(
                                purchasesError,
                                purchasesError.code == PurchasesErrorCode.PurchaseCancelledError
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startPurchase(
        activity: Activity,
        product: SkuDetails,
        presentedOfferingIdentifier: String?,
        upgradeInfo: UpgradeInfo?,
        listener: MakePurchaseListener
    ) {
        debugLog("purchase started - product:" +
            " $product ${presentedOfferingIdentifier?.let {
                " - offering: $presentedOfferingIdentifier"
            }}"
        )
        var userPurchasing: String? = null // Avoids race condition for userid being modified before purchase is made
        synchronized(this@Purchases) {
            if (!appConfig.finishTransactions) {
                debugLog("finishTransactions is set to false and a purchase has been started. " +
                    "Are you sure you want to do this?")
            }
            if (!state.purchaseCallbacks.containsKey(product.sku)) {
                state = state.copy(
                    purchaseCallbacks = state.purchaseCallbacks + mapOf(product.sku to listener)
                )
                userPurchasing = identityManager.currentAppUserID
            }
        }
        userPurchasing?.let { appUserID ->
            if (upgradeInfo != null) {
                replaceOldPurchaseWithNewProduct(
                    product,
                    upgradeInfo,
                    activity,
                    appUserID,
                    presentedOfferingIdentifier,
                    listener
                )
            } else {
                billingWrapper.makePurchaseAsync(
                    activity,
                    appUserID,
                    product,
                    upgradeInfo,
                    presentedOfferingIdentifier
                )
            }
        } ?: dispatch {
            listener.onError(
                PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError).also { errorLog(it) },
                false
            )
        }
    }

    private fun replaceOldPurchaseWithNewProduct(
        product: SkuDetails,
        upgradeInfo: UpgradeInfo,
        activity: Activity,
        appUserID: String,
        presentedOfferingIdentifier: String?,
        listener: MakePurchaseListener
    ) {
        billingWrapper.findPurchaseInPurchaseHistory(product.type, upgradeInfo.oldSku) { result, purchaseRecord ->
            if (result.isSuccessful()) {
                if (purchaseRecord != null) {
                    debugLog("Found existing purchase for sku: ${upgradeInfo.oldSku}")
                    billingWrapper.makePurchaseAsync(
                        activity,
                        appUserID,
                        product,
                        ReplaceSkuInfo(purchaseRecord, upgradeInfo.prorationMode),
                        presentedOfferingIdentifier
                    )
                } else {
                    debugLog("Couldn't find existing purchase for sku: ${upgradeInfo.oldSku}")
                    dispatch {
                        listener.onError(
                            PurchasesError(PurchasesErrorCode.PurchaseInvalidError).also { errorLog(it) },
                            false
                        )
                    }
                }
            } else {
                val message = "There was an error trying to upgrade. " +
                    "BillingResponseCode: ${result.responseCode.getBillingResponseCodeName()}"
                debugLog(message)
                dispatch {
                    listener.onError(
                        result.responseCode.billingResponseToPurchasesError(message).also { errorLog(it) },
                        false
                    )
                }
            }
        }
    }

    @JvmSynthetic
    internal fun updatePendingPurchaseQueue() {
        if (billingWrapper.isConnected()) {
            debugLog("[QueryPurchases] Updating pending purchase queue")
            synchronized(executorService) {
                if (!executorService.isShutdown) {
                    executorService.execute {
                        val queryActiveSubscriptionsResult =
                            billingWrapper.queryPurchases(BillingClient.SkuType.SUBS)
                        val queryUnconsumedInAppsRequest =
                            billingWrapper.queryPurchases(BillingClient.SkuType.INAPP)
                        if (queryActiveSubscriptionsResult?.isSuccessful() == true &&
                            queryUnconsumedInAppsRequest?.isSuccessful() == true
                        ) {
                            deviceCache.cleanPreviouslySentTokens(
                                queryActiveSubscriptionsResult.purchasesByHashedToken.keys,
                                queryUnconsumedInAppsRequest.purchasesByHashedToken.keys
                            )
                            postPurchases(
                                deviceCache.getActivePurchasesNotInCache(
                                    queryActiveSubscriptionsResult.purchasesByHashedToken,
                                    queryUnconsumedInAppsRequest.purchasesByHashedToken
                                ),
                                allowSharingPlayStoreAccount,
                                finishTransactions,
                                appUserID
                            )
                        }
                    }
                }
            }
        } else {
            debugLog("[QueryPurchases] Skipping updating pending purchase queue " +
                "since BillingClient is not connected yet")
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
        var debugLogsEnabled = Config.debugLogsEnabled

        @JvmSynthetic
        internal var backingFieldSharedInstance: Purchases? = null

        /**
         * Singleton instance of Purchases. [configure] will set this
         * @return A previously set singleton Purchases instance or null
         */
        @JvmStatic
        var sharedInstance: Purchases
            get() =
                backingFieldSharedInstance
                    ?: throw UninitializedPropertyAccessException(
                        "There is no singleton instance. " +
                            "Make sure you configure Purchases before trying to get the default instance."
                    )
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
            require(context.hasPermission(Manifest.permission.INTERNET)) { "Purchases requires INTERNET permission." }

            require(!apiKey.isBlank()) { "API key must be set. Get this from the RevenueCat web app" }

            require(context.applicationContext is Application) { "Needs an application context." }
            val application = context.getApplication()
            val appConfig = AppConfig(
                context,
                observerMode,
                platformInfo,
                proxyURL
            )

            val backend = Backend(
                apiKey,
                Dispatcher(service),
                HTTPClient(appConfig)
            )
            val subscriberAttributesBackend = SubscriberAttributesBackend(backend)

            val billingWrapper = BillingWrapper(
                BillingWrapper.ClientFactory(application),
                Handler(application.mainLooper)
            )

            val prefs = PreferenceManager.getDefaultSharedPreferences(application)
            val cache = DeviceCache(prefs, apiKey)
            val subscriberAttributesCache = SubscriberAttributesCache(cache)

            return Purchases(
                application,
                appUserID,
                backend,
                billingWrapper,
                cache,
                service,
                IdentityManager(cache, subscriberAttributesCache, backend),
                SubscriberAttributesManager(subscriberAttributesCache, subscriberAttributesBackend),
                appConfig
            ).also { sharedInstance = it }
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
        @JvmStatic
        fun isBillingSupported(context: Context, callback: Callback<Boolean>) {
            BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener { _, _ -> }
                .build()
                .let { billingClient ->
                    billingClient.startConnection(
                        object : BillingClientStateListener {
                            override fun onBillingSetupFinished(billingResult: BillingResult) {
                                // It also means that IN-APP items are supported for purchasing
                                try {
                                    billingClient.endConnection()
                                    val resultIsOK =
                                        billingResult.responseCode == BillingClient.BillingResponseCode.OK
                                    callback.onReceived(resultIsOK)
                                } catch (e: IllegalArgumentException) {
                                    // Play Services not available
                                    callback.onReceived(false)
                                }
                            }

                            override fun onBillingServiceDisconnected() {
                                try {
                                    billingClient.endConnection()
                                } catch (e: IllegalArgumentException) {
                                } finally {
                                    callback.onReceived(false)
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
                    billingClient.startConnection(
                        object : BillingClientStateListener {
                            override fun onBillingSetupFinished(billingResult: BillingResult) {
                                try {
                                    val featureSupportedResult =
                                        billingClient.isFeatureSupported(feature)
                                    billingClient.endConnection()
                                    val responseIsOK =
                                        featureSupportedResult.responseCode == BillingClient.BillingResponseCode.OK
                                    callback.onReceived(responseIsOK)
                                } catch (e: IllegalArgumentException) {
                                    // Play Services not available
                                    callback.onReceived(false)
                                }
                            }

                            override fun onBillingServiceDisconnected() {
                                try {
                                    billingClient.endConnection()
                                } catch (e: IllegalArgumentException) {
                                } finally {
                                    callback.onReceived(false)
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
            return ThreadPoolExecutor(
                1,
                2,
                0,
                TimeUnit.MILLISECONDS,
                LinkedBlockingQueue()
            )
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
