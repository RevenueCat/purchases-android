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
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.interfaces.Callback
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.PurchaseCompletedListener
import com.revenuecat.purchases.interfaces.ReceiveEntitlementsListener
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener
import com.revenuecat.purchases.util.AdvertisingIdClient
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val CACHE_REFRESH_PERIOD = 60000 * 5

/**
 * Entry point for Purchases. It should be instantiated as soon as your app has a unique user id
 * for your user. This can be when a user logs in if you have accounts or on launch if you can
 * generate a random user identifier.
 * Make sure you follow the [quickstart](https://docs.revenuecat.com/docs/getting-started-1)
 * guide to setup your RevenueCat account.
 * @warning Only one instance of Purchases should be instantiated at a time!
 * Set the [Purchases.sharedInstance] to let the SDK handle the singleton management for you.
 * @property [allowSharingPlayStoreAccount] If it should allow sharing Play Store accounts. False by
 * default. If true treats all purchases as restores, aliasing together appUserIDs that share a
 * Play Store account.
 * @property [finishTransactions] Default to TRUE, set this to FALSE if you are consuming
 * transactions outside of the Purchases SDK.
 */
class Purchases @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE) internal constructor(
    private val applicationContext: Context,
    backingFieldAppUserID: String?,
    private val backend: Backend,
    private val billingWrapper: BillingWrapper,
    private val deviceCache: DeviceCache,
    var allowSharingPlayStoreAccount: Boolean = false,
    private var cachesLastUpdated: Date? = null,
    var finishTransactions: Boolean = true
) {

    /**
     * The passed in or generated app user ID
     */
    lateinit var appUserID: String

    private var purchaseCallbacks: MutableMap<String, MakePurchaseListener> = mutableMapOf()
    private var lastSentPurchaserInfo: PurchaserInfo? = null

    private val receivePurchaserInfoListenerStub = object : ReceivePurchaserInfoListener {
        override fun onReceived(purchaserInfo: PurchaserInfo) {
        }

        override fun onError(error: PurchasesError) {
        }
    }
    /**
     * The listener is responsible for handling changes to purchaser information.
     * Make sure [removeUpdatedPurchaserInfoListener] is called when the listener needs to be destroyed.
     */
    var updatedPurchaserInfoListener: UpdatedPurchaserInfoListener? = null
        set(value) {
            field = value
            afterSetListener(value)
        }

    internal var cachedEntitlements: Map<String, Entitlement>? = null
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        set(value) {
            field = value
        }
        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        get() = field

    init {
        debugLog("Debug logging enabled.")
        debugLog("SDK Version - $frameworkVersion")
        debugLog("Initial App User ID - $backingFieldAppUserID")
        if (backingFieldAppUserID != null) {
            identify(backingFieldAppUserID)
        } else {
            identify(getAnonymousID().also {
                debugLog("Generated New App User ID - $it")
                allowSharingPlayStoreAccount = true
            })
        }
        billingWrapper.purchasesUpdatedListener = getPurchasesUpdatedListener()
    }

    // region Public Methods

    /**
     * This method will send all the purchases to the RevenueCat backend. Call this when using your own implementation
     * for subscriptions anytime a sync is needed, like after a successful purchase.
     *
     * @warning This function should only be called if you're not calling makePurchase.
     */
    fun syncPurchases()  {
        debugLog("Syncing purchases")
        billingWrapper.queryAllPurchases({ allPurchases ->
            if (allPurchases.isNotEmpty()) {
                postPurchasesSortedByTime(
                    allPurchases,
                    allowSharingPlayStoreAccount,
                    false,
                    { debugLog("Purchases synced") },
                    { errorLog("Error syncing purchases $it") }
                )
            }
        }, { errorLog("Error syncing purchases $it") })
    }

    /**
     * Add attribution data from a supported network
     * @param [data] JSONObject containing the data to post to the attribution network
     * @param [network] [AttributionNetwork] to post the data to
     */
    @Deprecated("use static addAttributionData", ReplaceWith("Purchases.addAttributionData(data, network)"))
    fun addAttributionData(
        data: JSONObject,
        network: AttributionNetwork
    ) {
        postAttributionData(data, network, appUserID)
    }

    /**
     * Add attribution data from a supported network
     * @param [data] Map containing the data to post to the attribution network
     * @param [network] [AttributionNetwork] to post the data to
     */
    @Deprecated("use static addAttributionData", ReplaceWith("Purchases.addAttributionData(data, network)"))
    fun addAttributionData(
        data: Map<String, String>,
        network: AttributionNetwork
    ) {
        postAttributionData(data, network, appUserID)
    }

    /**
     * Fetch the configured entitlements for this user. Entitlements allows you to configure your
     * in-app products via RevenueCat and greatly simplifies management.
     * See [the guide](https://docs.revenuecat.com/v1.0/docs/entitlements) for more info.
     *
     * Entitlements will be fetched and cached on instantiation so that, by the time they are needed,
     * your prices are loaded for your purchase flow. Time is money.
     *
     * @param [listener] Called when entitlements are available. Called immediately if entitlements are cached.
     */
    fun getEntitlements(
        listener: ReceiveEntitlementsListener
    ) {
        cachedEntitlements?.let { cachedEntitlements ->
            debugLog("Vending entitlements from cache")
            dispatch {
                listener.onReceived(cachedEntitlements)
            }
            if (isCacheStale()) {
                debugLog("Cache is stale, updating caches")
                updateCaches()
            }
        } ?: {
            debugLog("No cached entitlements, fetching")
            fetchAndCacheEntitlements(listener)
        }.invoke()
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
     * Make a purchase.
     * @param [activity] Current activity
     * @param [sku] The sku you wish to purchase
     * @param [skuType] The type of sku, INAPP or SUBS
     * @param [oldSkus] The skus you wish to upgrade from.
     * @param [listener] The listener that will be called when purchase completes.
     */
    @Deprecated("use makePurchase accepting a MakePurchaseListener instead",
        ReplaceWith("makePurchase(activity, skuDetails, oldSku, listener)", "import com.revenuecat.purchases.interfaces.MakePurchaseListener"))
    fun makePurchase(
        activity: Activity,
        sku: String,
        @BillingClient.SkuType skuType: String,
        oldSkus: ArrayList<String>,
        @Suppress("DEPRECATION") listener: PurchaseCompletedListener
    ) {
        makePurchase(activity, sku, skuType, oldSkus, object : MakePurchaseListener {
            override fun onCompleted(purchase: Purchase, purchaserInfo: PurchaserInfo) {
                listener.onCompleted(purchase.sku, purchaserInfo)
            }

            override fun onError(error: PurchasesError, userCancelled: Boolean) {
                listener.onError(error)
            }
        })
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [sku] The sku you wish to purchase
     * @param [skuType] The type of sku, INAPP or SUBS
     * @param [oldSkus] The skus you wish to upgrade from.
     * @param [listener] The listener that will be called when purchase completes.
     */
    @Deprecated("use makePurchase accepting a MakePurchaseListener instead",
        ReplaceWith("makePurchase(activity, skuDetails, oldSku, listener)", "import com.revenuecat.purchases.interfaces.MakePurchaseListener"))
    fun makePurchase(
        activity: Activity,
        sku: String,
        @BillingClient.SkuType skuType: String,
        oldSkus: ArrayList<String>,
        listener: MakePurchaseListener
    ) {
        debugLog("makePurchase - $sku")
        if (!finishTransactions) {
            debugLog("finishTransactions is set to false and makePurchase has been called. Are you sure you want to do this?")
        }
        synchronized(purchaseCallbacks) {
            if (purchaseCallbacks.containsKey(sku)) {
                dispatch {
                    listener.onError(PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError), false)
                }
            } else {
                purchaseCallbacks[sku] = listener
                billingWrapper.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType)

                billingWrapper.querySkuDetailsAsync(skuType, listOf(sku), {
                    billingWrapper.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType)
                }, {
                    listener.onError(it, false)
                })
            }
        }
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [sku] The sku you wish to purchase
     * @param [skuType] The type of sku, INAPP or SUBS
     * @param [listener] The listener that will be called when purchase completes.
     */
    @Deprecated("use makePurchase accepting a MakePurchaseListener instead",
        ReplaceWith("makePurchase(activity, sku, skuType, listener)", "import com.revenuecat.purchases.interfaces.MakePurchaseListener")
    )
    fun makePurchase(
        activity: Activity,
        sku: String,
        @BillingClient.SkuType skuType: String,
        @Suppress("DEPRECATION") listener: PurchaseCompletedListener
    ) {
        @Suppress("DEPRECATION")
        makePurchase(activity, sku, skuType, ArrayList(), listener)
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [sku] The sku you wish to purchase
     * @param [skuType] The type of sku, INAPP or SUBS
     * @param [listener] The listener that will be called when purchase completes.
     */
    @Deprecated("use makePurchase accepting SkuDetails instead",
        ReplaceWith("makePurchase(activity, skuDetails, listener)", "import com.revenuecat.purchases.interfaces.MakePurchaseListener")
    )
    fun makePurchase(
        activity: Activity,
        sku: String,
        @BillingClient.SkuType skuType: String,
        listener: MakePurchaseListener
    ) {
        makePurchase(activity, sku, skuType, ArrayList(), listener)
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [skuDetails] The skuDetails of the product you wish to purchase
     * @param [oldSku] The sku you wish to upgrade from.
     * @param [listener] The listener that will be called when purchase completes.
     */
    fun makePurchase(
        activity: Activity,
        skuDetails: SkuDetails,
        oldSku: String,
        listener: MakePurchaseListener
    ) {
        startPurchase(activity, skuDetails, oldSku, listener)
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [skuDetails] The skuDetails of the product you wish to purchase
     * @param [listener] The listener that will be called when purchase completes.
     */
    fun makePurchase(
        activity: Activity,
        skuDetails: SkuDetails,
        listener: MakePurchaseListener
    ) {
        startPurchase(activity, skuDetails, null, listener)
    }

    /**
     * Restores purchases made with the current Play Store account for the current user.
     * If you initialized Purchases with an `appUserID` any receipt tokens currently being used by
     * other users of your app will not be restored. If you used an anonymous id, i.e. you
     * initialized Purchases without an appUserID, any other anonymous users using the same
     * purchases will be merged.
     * @param [listener] The listener that will be called when purchase restore completes.
     */
    fun restorePurchases(
        listener: ReceivePurchaserInfoListener
    ) {
        debugLog("Restoring purchases")
        if (!allowSharingPlayStoreAccount) {
            debugLog("allowSharingPlayStoreAccount is set to false and restorePurchases has been called. This will 'alias' any app user id's sharing the same receipt. Are you sure you want to do this?")
        }
        billingWrapper.queryAllPurchases({ allPurchases ->
            if (allPurchases.isEmpty()) {
                getPurchaserInfo(listener)
            } else {
                postPurchasesSortedByTime(
                    allPurchases,
                    true,
                    finishTransactions,
                    { dispatch { listener.onReceived(it) } },
                    { dispatch { listener.onError(it) } }
                )
            }
        }, { dispatch { listener.onError(it) } })
    }

    /**
     * This function will alias two appUserIDs together.
     * @param [newAppUserID] The current user id will be aliased to the app user id passed in this parameter
     * @param [listener] An optional listener to listen for successes or errors.
     */
    @JvmOverloads
    fun createAlias(
        newAppUserID: String,
        listener: ReceivePurchaserInfoListener = receivePurchaserInfoListenerStub
    ) {
        if (this.appUserID != newAppUserID) {
            debugLog("Creating an alias to ${this.appUserID} from $newAppUserID")
            backend.createAlias(
                appUserID,
                newAppUserID,
                {
                    debugLog("Alias created")
                    identify(newAppUserID, listener)
                },
                { error ->
                    dispatch { listener.onError(error) }
                }
            )
        } else {
            getPurchaserInfo(listener)
        }
    }

    /**
     * This function will change the current appUserID.
     * Typically this would be used after a log out to identify a new user without calling configure
     * @param appUserID The new appUserID that should be linked to the currently user
     * @param [listener] An optional listener to listen for successes or errors.
     */
    @JvmOverloads
    fun identify(
        appUserID: String,
        listener: ReceivePurchaserInfoListener? = null
    ) {
        if (this::appUserID.isInitialized && this.appUserID == appUserID) {
            if (listener != null) {
                getPurchaserInfo(listener)
            }
        } else {
            if (this::appUserID.isInitialized) {
                debugLog("Changing App User ID: ${this.appUserID} -> $appUserID")
            } else {
                debugLog("Identifying App User ID: $appUserID")
            }
            clearCaches()
            this.appUserID = appUserID
            synchronized(purchaseCallbacks) {
                purchaseCallbacks.clear()
            }
            updateCaches(listener)
        }
    }

    /**
     * Resets the Purchases client clearing the save appUserID. This will generate a random user
     * id and save it in the cache.
     * @param [listener] An optional listener to listen for successes or errors.
     */
    @JvmOverloads
    fun reset(
        listener: ReceivePurchaserInfoListener = receivePurchaserInfoListenerStub
    ) {
        clearCaches()
        deviceCache.clearLatestAttributionData(this.appUserID)
        this.appUserID = createRandomIDAndCacheIt()
        synchronized(purchaseCallbacks) {
            purchaseCallbacks.clear()
        }
        updateCaches(listener)
    }

    /**
     * Call close when you are done with this instance of Purchases
     */
    fun close() {
        synchronized(purchaseCallbacks) {
            purchaseCallbacks.clear()
        }
        this.backend.close()
        billingWrapper.purchasesUpdatedListener = null
        updatedPurchaserInfoListener = null
    }

    /**
     * Get latest available purchaser info.
     * @param listener A listener called when purchaser info is available and not stale.
     * Called immediately if purchaser info is cached. Purchaser info can be null if an error occurred.
     */
    fun getPurchaserInfo(
        listener: ReceivePurchaserInfoListener
    ) {
        val cachedPurchaserInfo = deviceCache.getCachedPurchaserInfo(appUserID)
        if (cachedPurchaserInfo != null) {
            debugLog("Vending purchaserInfo from cache")
            dispatch { listener.onReceived(cachedPurchaserInfo) }
            if (isCacheStale()) {
                debugLog("Cache is stale, updating caches")
                updateCaches()
            }
        } else {
            debugLog("No cached purchaser info, fetching")
            updateCaches(listener)
        }
    }

    /**
     * Call this when you are finished using the [UpdatedPurchaserInfoListener]. You should call this
     * to avoid memory leaks.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun removeUpdatedPurchaserInfoListener() {
        this.updatedPurchaserInfoListener = null
    }
    // endregion
    // region Internal Methods

    private fun postAttributionData(
        data: Map<String, String>,
        network: AttributionNetwork,
        userID: String = appUserID
    ) {
        val jsonObject = JSONObject()
        for (key in data.keys) {
            try {
                jsonObject.put(key, data[key])
            } catch (e: JSONException) {
                Log.e("Purchases", "Failed to add key $key to attribution map")
            }
        }

        postAttributionData(jsonObject, network, userID)
    }

    internal fun postAttributionData(
        jsonObject: JSONObject,
        network: AttributionNetwork,
        networkUserId: String?
    ) {
        AdvertisingIdClient.getAdvertisingIdInfo(applicationContext) { adInfo ->
            val latestAttributionDataId = deviceCache.getCachedAttributionData(network, appUserID)
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

    private fun AdvertisingIdClient.AdInfo?.generateAttributionDataCacheValue(networkUserId: String?) =
        listOfNotNull(this?.takeIf { !it.isLimitAdTrackingEnabled }?.id, networkUserId).joinToString("_")

    // endregion
    // region Private Methods
    private fun fetchAndCacheEntitlements(completion: ReceiveEntitlementsListener? = null) {
        backend.getEntitlements(
            appUserID,
            { entitlements ->
                getSkuDetails(entitlements, { detailsByID ->
                    entitlements.values.flatMap { it.offerings.values }.let { offerings ->
                        val missingProducts = populateSkuDetails(offerings, detailsByID)
                        if (missingProducts.isNotEmpty()) {
                            log("Could not find SkuDetails for ${missingProducts.joinToString(", ")}")
                            log("Ensure your products are correctly configured in Play Store Developer Console")
                        }
                        if (offerings.mapNotNull { it.skuDetails }.isNotEmpty()) {
                            cachedEntitlements = entitlements
                        } else {
                            cachedEntitlements = null
                        }
                        dispatch {
                            completion?.onReceived(entitlements)
                        }
                    }
                }, {
                    dispatch {
                        completion?.onError(it)
                    }
                })
            },
            { error ->
                log("Error fetching entitlements - $error")
                dispatch {
                    completion?.onError(error)
                }
            })
    }

    private fun populateSkuDetails(
        offerings: List<Offering>,
        detailsByID: HashMap<String, SkuDetails>
    ) : List<String> {
        val missingProducts = mutableListOf<String>()
        offerings.forEach { offering ->
            if (detailsByID.containsKey(offering.activeProductIdentifier)) {
                offering.skuDetails = detailsByID[offering.activeProductIdentifier]
            } else {
                missingProducts.add(offering.activeProductIdentifier)
            }
        }
        return missingProducts
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

    private fun updateCaches(
        completion: ReceivePurchaserInfoListener? = null
    ) {
        cachesLastUpdated = Date()
        fetchAndCachePurchaserInfo(completion)
        fetchAndCacheEntitlements()
    }

    private fun fetchAndCachePurchaserInfo(completion: ReceivePurchaserInfoListener?) {
        backend.getPurchaserInfo(
            appUserID,
            { info ->
                cachePurchaserInfo(info)
                sendUpdatedPurchaserInfoToDelegateIfChanged(info)
                dispatch { completion?.onReceived(info) }
            },
            { error ->
                Log.e("Purchases", "Error fetching subscriber data: ${error.message}")
                cachesLastUpdated = null
                dispatch { completion?.onError(error) }
            })
    }

    private fun isCacheStale() =
        cachesLastUpdated == null || Date().time - cachesLastUpdated!!.time > CACHE_REFRESH_PERIOD

    private fun clearCaches() {
        if (this::appUserID.isInitialized) {
            deviceCache.clearCachedPurchaserInfo(this.appUserID)
        }
        cachesLastUpdated = null
        cachedEntitlements = null
    }

    private fun cachePurchaserInfo(info: PurchaserInfo) {
        deviceCache.cachePurchaserInfo(appUserID, info)
    }

    private fun postPurchases(
        purchases: List<Purchase>,
        allowSharingPlayStoreAccount: Boolean,
        consumeAllTransactions: Boolean,
        onSuccess: (Purchase, PurchaserInfo) -> Unit,
        onError: (Purchase, PurchasesError) -> Unit
    ) {
        purchases.forEach { purchase ->
            backend.postReceiptData(
                purchase.purchaseToken,
                appUserID,
                purchase.sku,
                allowSharingPlayStoreAccount,
                { info ->
                    if (consumeAllTransactions) {
                        billingWrapper.consumePurchase(purchase.purchaseToken)
                    }
                    cachePurchaserInfo(info)
                    sendUpdatedPurchaserInfoToDelegateIfChanged(info)
                    onSuccess(purchase, info)
                }, { error, shouldConsumePurchase ->
                    if (shouldConsumePurchase && consumeAllTransactions) {
                        billingWrapper.consumePurchase(purchase.purchaseToken)
                    }
                    onError(purchase, error)
                })
        }
    }

    private fun postPurchasesSortedByTime(
        purchases: List<Purchase>,
        allowSharingPlayStoreAccount: Boolean,
        consumeAllTransactions: Boolean,
        onSuccess: (PurchaserInfo) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        purchases.sortedBy { it.purchaseTime }.let { sortedByTime ->
            postPurchases(
                sortedByTime,
                allowSharingPlayStoreAccount,
                consumeAllTransactions,
                { purchase, info ->
                    if (sortedByTime.last() == purchase) {
                        onSuccess(info)
                    }
                },
                { purchase, error ->
                    if (sortedByTime.last() == purchase) {
                        onError(error)
                    }
                }
            )
        }
    }

    private fun getAnonymousID(): String {
        return deviceCache.getCachedAppUserID() ?: createRandomIDAndCacheIt()
    }

    private fun createRandomIDAndCacheIt(): String {
        return UUID.randomUUID().toString().also {
            deviceCache.cacheAppUserID(it)
        }
    }

    private fun getSkuDetails(
        entitlements: Map<String, Entitlement>,
        onCompleted: (HashMap<String, SkuDetails>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        val skus =
            entitlements.values.flatMap { it.offerings.values }.map { it.activeProductIdentifier }

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
            deviceCache.getCachedPurchaserInfo(appUserID)?.let {
                this.sendUpdatedPurchaserInfoToDelegateIfChanged(it)
            }
        }
    }

    private fun sendUpdatedPurchaserInfoToDelegateIfChanged(info: PurchaserInfo) {
        synchronized(this) {
            updatedPurchaserInfoListener?.let { listener ->
                if (lastSentPurchaserInfo != info) {
                    if (lastSentPurchaserInfo != null) {
                        debugLog("Purchaser info updated, sending to listener")
                    } else {
                        debugLog("Sending latest purchaser info to delegate")
                    }
                    lastSentPurchaserInfo = info
                    dispatch { listener.onReceived(info) }
                }
            }
        }
    }

    private val handler : Handler? = Handler(Looper.getMainLooper())
    private fun dispatch(action: () -> Unit) {
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            handler?.post(action) ?: Handler(Looper.getMainLooper()).post(action)
        } else {
            action()
        }
    }

    private fun getPurchasesUpdatedListener(): BillingWrapper.PurchasesUpdatedListener {
        return object : BillingWrapper.PurchasesUpdatedListener {
            override fun onPurchasesUpdated(purchases: List<@JvmSuppressWildcards Purchase>) {
                postPurchases(
                    purchases,
                    allowSharingPlayStoreAccount,
                    finishTransactions,
                    { purchase, info ->
                        synchronized(purchaseCallbacks) {
                            dispatch {
                                purchaseCallbacks.remove(purchase.sku)?.onCompleted(purchase, info)
                            }
                        }
                    },
                    { purchase, error ->
                        synchronized(purchaseCallbacks) {
                            dispatch {
                                purchaseCallbacks.remove(purchase.sku)?.onError(
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
                @BillingClient.BillingResponse responseCode: Int,
                message: String
            ) {
                synchronized(purchaseCallbacks) {
                    purchaseCallbacks.forEach { (_, callback) ->
                        val purchasesError = responseCode.billingResponseToPurchasesError(message)
                        dispatch {
                            callback.onError(purchasesError, purchasesError.code == PurchasesErrorCode.PurchaseCancelledError)
                        }
                    }
                    purchaseCallbacks.clear()
                }
            }
        }
    }

    private fun startPurchase(
        activity: Activity,
        skuDetails: SkuDetails,
        oldSku: String?,
        listener: MakePurchaseListener
    ) {
        debugLog("makePurchase - $skuDetails")
        if (!finishTransactions) {
            debugLog("finishTransactions is set to false and makePurchase has been called. Are you sure you want to do this?")
        }
        synchronized(purchaseCallbacks) {
            if (purchaseCallbacks.containsKey(skuDetails.sku)) {
                dispatch {
                    listener.onError(PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError), false)
                }
            } else {
                purchaseCallbacks[skuDetails.sku] = listener
                billingWrapper.makePurchaseAsync(activity, appUserID, skuDetails, oldSku)
            }
        }
    }

    // endregion
    // region Static
    companion object {
        internal var postponedAttributionData = mutableListOf<AttributionData>()
            @VisibleForTesting(otherwise = VisibleForTesting.NONE)
            get() = field
            @VisibleForTesting(otherwise = VisibleForTesting.NONE)
            internal set(value) {
                field = value
            }
        /**
         * Enable debug logging. Useful for debugging issues with the lovely team @RevenueCat
         */
        @JvmStatic
        var debugLogsEnabled = false

        internal var backingFieldSharedInstance: Purchases? = null
            @VisibleForTesting(otherwise = VisibleForTesting.NONE)
            get() = field
            @VisibleForTesting(otherwise = VisibleForTesting.NONE)
            internal set(value) {
                field = value
            }
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
        val frameworkVersion = "2.3.0-SNAPSHOT"

        /**
         * Configures an instance of the Purchases SDK with a specified API key. The instance will
         * be set as a singleton. You should access the singleton instance using [Purchases.sharedInstance]
         * @param apiKey The API Key generated for your app from https://app.revenuecat.com/
         * @param appUserID Optional user identifier. Use this if your app has an account system.
         * If `null` `[Purchases] will generate a unique identifier for the current device and persist
         * it the SharedPreferences. This also affects the behavior of [restorePurchases].
         * @param observerMode Optional boolean set to FALSE by default. Set to TRUE if you are using your own
         * subscription system and you want to use RevenueCat's backend only. If set to TRUE, you should be consuming
         * transactions outside of the Purchases SDK.
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
            if (!context.hasPermission(Manifest.permission.INTERNET))
                throw IllegalArgumentException("Purchases requires INTERNET permission.")

            if (apiKey.isBlank())
                throw IllegalArgumentException("API key must be set. Get this from the RevenueCat web app")

            if (context.applicationContext !is Application)
                throw IllegalArgumentException("Needs an application context.")

            val backend = Backend(
                apiKey,
                Dispatcher(service),
                HTTPClient(
                    appConfig = AppConfig(
                        context.getLocale()?.toBCP47() ?: "",
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
                    )
                )
            )

            val billingWrapper = BillingWrapper(
                BillingWrapper.ClientFactory((context.getApplication()).applicationContext),
                Handler((context.getApplication()).mainLooper)
            )

            val prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplication())
            val cache = DeviceCache(prefs, apiKey)

            return Purchases(
                context,
                appUserID,
                backend,
                billingWrapper,
                cache,
                finishTransactions = !observerMode
            ).also { sharedInstance = it }
        }

        /**
         * Check if billing is supported in the device. This method is asynchronous since it tries to connect the billing
         * client and checks for the result of the connection. If billing is supported, IN-APP purchases are supported.
         * If you want to check if SUBSCRIPTIONS or other type defined in [BillingClient.FeatureType],
         * call [isFeatureSupported].
         * @param context A context object that will be used to connect to the billing client
         * @param callback Callback that will be notified when the check is complete.
         */
        @JvmStatic
        fun isBillingSupported(context: Context, callback: Callback<Boolean>) {
            BillingClient.newBuilder(context).setListener { _, _ ->  }.build().let {
                it.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(responseCode: Int) {
                            // It also means that IN-APP items are supported for purchasing
                            try {
                                it.endConnection()
                                callback.onReceived(responseCode == BillingClient.BillingResponse.OK)
                            } catch (e: IllegalArgumentException) {
                                // Play Services not available
                                callback.onReceived(false)
                            }
                        }

                        override fun onBillingServiceDisconnected() {
                            try {
                                it.endConnection()
                            } catch (e: IllegalArgumentException) {
                            } finally {
                                callback.onReceived(false)
                            }
                        }
                    })
            }
        }

        /**
         * Use this method if you want to check if Subscriptions or other type defined in [BillingClient.FeatureType] is supported.\
         * This method is asynchronous since it requires a connected billing client.
         * @param feature A feature type to check for support. Must be one of [BillingClient.FeatureType]
         * @param context A context object that will be used to connect to the billing client
         * @param callback Callback that will be notified when the check is complete.
         */
        @JvmStatic
        fun isFeatureSupported(@BillingClient.FeatureType feature: String, context: Context, callback: Callback<Boolean>) {
            BillingClient.newBuilder(context).setListener { _, _ ->  }.build().let {
                it.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(responseCode: Int) {
                            try {
                                it.endConnection()
                                callback.onReceived(it.isFeatureSupported(feature) == BillingClient.BillingResponse.OK)
                            } catch (e: IllegalArgumentException) {
                                // Play Services not available
                                callback.onReceived(false)
                            }
                        }

                        override fun onBillingServiceDisconnected() {
                            try {
                                it.endConnection()
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
        @JvmStatic
        fun addAttributionData(
            data: JSONObject,
            network: AttributionNetwork,
            networkUserId: String? = null
        ) {
            backingFieldSharedInstance?.postAttributionData(data, network, networkUserId) ?: {
                postponedAttributionData.add(AttributionData(data, network, networkUserId))
            }.invoke()
        }

        /**
         * Add attribution data from a supported network
         * @param [data] Map containing the data to post to the attribution network
         * @param [network] [AttributionNetwork] to post the data to
         * @param [networkUserId] User Id that should be sent to the network. Default is the current App User Id
         */
        @JvmStatic
        fun addAttributionData(
            data: Map<String, String>,
            network: AttributionNetwork,
            networkUserId: String? = null
        ) {
            val jsonObject = JSONObject()
            for (key in data.keys) {
                try {
                    jsonObject.put(key, data[key])
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
        TENJIN(4)
    }

    internal data class AttributionData(
        val data: JSONObject,
        val network: AttributionNetwork,
        val networkUserId: String?
    )
    // endregion

}
