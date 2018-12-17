package com.revenuecat.purchases

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails

import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.HashSet
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import android.content.pm.PackageManager.PERMISSION_GRANTED

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
 */
class Purchases @JvmOverloads internal constructor(
    private val application: Application,
    _appUserID: String?,
    private val backend: Backend,
    private val billingWrapper: BillingWrapper,
    private val deviceCache: DeviceCache,
    var allowSharingPlayStoreAccount: Boolean = false,
    internal val postedTokens: HashSet<String> = HashSet(),
    private var cachesLastChecked: Date? = null,
    private var cachedEntitlements: Map<String, Entitlement>? = null
) : BillingWrapper.PurchasesUpdatedListener, Application.ActivityLifecycleCallbacks {

    /**
     * The passed in or generated app user ID
     */
    var appUserID: String

    private var shouldRefreshCaches = false

    /**
     * Adds a [PurchasesListener] to handle async updates from Purchases. Remember to remove the
     * listener when needed (i.e. [Activity.onDestroy] if it's an activity to avoid memory leaks)
     * @param [listener] Listener that will handle async updates from Purchases
     */
    var listener: PurchasesListener? = null
        set(value) {
            field = value
            afterSetListener(value)
        }

    init {
        this.appUserID = _appUserID?.also { identify(it) } ?:
                getAnonymousID().also { allowSharingPlayStoreAccount = true }
    }

    /**
     * Add attribution data from a supported network
     * @param [data] JSONObject containing the data to post to the attribution network
     * @param [network] [AttributionNetwork] to post the data to
     */
    fun addAttributionData(data: JSONObject, network: AttributionNetwork) {
        backend.postAttributionData(appUserID, network, data)
    }

    /**
     * Add attribution data from a supported network
     * @param [data] Map containing the data to post to the attribution network
     * @param [network] [AttributionNetwork] to post the data to
     */
    fun addAttributionData(data: Map<String, String>, network: AttributionNetwork) {
        val jsonObject = JSONObject()
        for (key in data.keys) {
            try {
                jsonObject.put(key, data[key])
            } catch (e: JSONException) {
                Log.e("Purchases", "Failed to add key $key to attribution map")
            }
        }
        backend.postAttributionData(appUserID, network, jsonObject)
    }

    /**
     * Fetch the configured entitlements for this user. Entitlements allows you to configure your
     * in-app products via RevenueCat and greatly simplifies management.
     * See [the guide](https://docs.revenuecat.com/v1.0/docs/entitlements) for more info.
     *
     * Entitlements will be fetched and cached on instantiation so that, by the time they are needed,
     * your prices are loaded for your purchase flow. Time is money.
     *
     * @param [handler] Called when entitlements are available. Called immediately if entitlements are cached.
     */
    fun getEntitlements(handler: GetEntitlementsHandler) {
        this.cachedEntitlements?.let {
            handler.onReceiveEntitlements(it)
        } ?: backend.getEntitlements(appUserID, object : Backend.EntitlementsResponseHandler() {

            override fun onReceiveEntitlements(entitlements: Map<String, Entitlement>) {
                getSkuDetails(entitlements) { detailsByID ->
                    populateSkuDetailsAndCallHandler(detailsByID, entitlements, handler)
                }
            }

            override fun onError(code: Int, message: String) {
                handler.onReceiveEntitlementsError(
                    ErrorDomains.REVENUECAT_BACKEND,
                    code,
                    "Error fetching entitlements: $message"
                )
            }
        })
    }

    /**
     * Gets the SKUDetails for the given list of subscription skus.
     * @param [skus] List of skus
     * @param [handler] Response handler
     */
    fun getSubscriptionSkus(skus: List<String>, handler: GetSkusResponseHandler) {
        getSkus(skus, BillingClient.SkuType.SUBS, handler)
    }

    /**
     * Gets the SKUDetails for the given list of non-subscription skus.
     * @param [skus] List of skus
     * @param [handler] Response handler
     */
    fun getNonSubscriptionSkus(skus: List<String>, handler: GetSkusResponseHandler) {
        getSkus(skus, BillingClient.SkuType.INAPP, handler)
    }

    /**
     * Make a purchase.
     * @param [activity] Current activity
     * @param [sku] The sku you wish to purchase
     * @param [skuType] The type of sku, INAPP or SUBS
     * @param [oldSkus] The skus you wish to upgrade from.
     */
    @JvmOverloads
    fun makePurchase(
        activity: Activity,
        sku: String,
        @BillingClient.SkuType skuType: String,
        oldSkus: ArrayList<String> = ArrayList()
    ) {
        billingWrapper.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType)
    }

    /**
     * Restores purchases made with the current Play Store account for the current user.
     * If you initialized Purchases with an `appUserID` any receipt tokens currently being used by
     * other users of your app will not be restored. If you used an anonymous id, i.e. you
     * initialized Purchases without an appUserID, any other anonymous users using the same
     * purchases will be merged.
     */
    fun restorePurchasesForPlayStoreAccount() {
        billingWrapper.queryPurchaseHistoryAsync(
            BillingClient.SkuType.SUBS,
            object : BillingWrapper.PurchaseHistoryResponseListener {
                override fun onReceivePurchaseHistory(subsPurchasesList: List<Purchase>) {
                    billingWrapper.queryPurchaseHistoryAsync(
                        BillingClient.SkuType.INAPP,
                        object : BillingWrapper.PurchaseHistoryResponseListener {
                            override fun onReceivePurchaseHistory(inAppPurchasesList: List<Purchase>) {
                                val allPurchases = ArrayList(subsPurchasesList)
                                allPurchases.addAll(inAppPurchasesList)
                                if (allPurchases.isEmpty()) {
                                    if (cachesLastChecked != null && Date().time - cachesLastChecked!!.time < 60000) {
                                        emitCachePurchaserInfo {
                                            listener?.onRestoreTransactions(it)
                                        }
                                    } else {
                                        cachesLastChecked = Date()

                                        getSubscriberInfo { listener?.onRestoreTransactions(it) }
                                    }
                                } else {
                                    postPurchases(allPurchases, true, false)
                                }
                            }

                            override fun onReceivePurchaseHistoryError(
                                responseCode: Int,
                                message: String
                            ) {
                                listener?.onRestoreTransactionsFailed(
                                    ErrorDomains.PLAY_BILLING,
                                    responseCode,
                                    message
                                )
                            }
                        })
                }

                override fun onReceivePurchaseHistoryError(responseCode: Int, message: String) {
                    listener?.onRestoreTransactionsFailed(
                        ErrorDomains.PLAY_BILLING,
                        responseCode,
                        message
                    )
                }
            })
    }

    /**
     * This function will alias two appUserIDs together.
     * @param [newAppUserID] The current user id will be aliased to the app user id passed in this parameter
     * @param [handler] An optional handler to listen for successes or errors.
     */
    fun createAlias(newAppUserID: String, handler: AliasHandler?) {
        backend.createAlias(
            appUserID,
            newAppUserID,
            {
                identify(newAppUserID)
                handler?.onSuccess()
            },
            { code, message ->
                handler?.onError(ErrorDomains.REVENUECAT_BACKEND, code, message)
            }
        )
    }

    /**
     * This function will change the current appUserID.
     * Typically this would be used after a log out to identify a new user without calling configure
     * @param appUserID The new appUserID that should be linked to the currently user
     */
    fun identify(appUserID: String) {
        clearCachedRandomId()
        this.appUserID = appUserID
        postedTokens.clear()
        makeCachesOutdatedAndNotifyIfNeeded()
    }

    /**
     * Resets the Purchases client clearing the save appUserID. This will generate a random user id and save it in the cache.
     */
    fun reset() {
        this.appUserID = createRandomIDAndCacheIt()
        allowSharingPlayStoreAccount = true
        postedTokens.clear()
        makeCachesOutdatedAndNotifyIfNeeded()
    }

    /**
     * Call close when you are done with this instance of Purchases
     */
    fun close() {
        this.backend.close()
        removeListener()
    }

    /**
     * Removes the [PurchasesListener]. You should call this to avoid memory leaks.
     * @note This method just sets [listener] to null
     */
    fun removeListener() {
        listener = null
    }

    // region Private Methods

    private fun emitCachePurchaserInfo(f: (PurchaserInfo) -> Unit) {
        deviceCache.getCachedPurchaserInfo(appUserID)?.let { f(it) }
    }

    private fun populateSkuDetailsAndCallHandler(
        details: Map<String, SkuDetails>,
        entitlements: Map<String, Entitlement>,
        handler: GetEntitlementsHandler
    ) {
        entitlements.values.flatMap { it.offerings.values }.forEach { o ->
            if (details.containsKey(o.activeProductIdentifier)) {
                o.skuDetails = details[o.activeProductIdentifier]
            } else {
                Log.e("Purchases", "Failed to find SKU for " + o.activeProductIdentifier)
            }
        }
        cachedEntitlements = entitlements
        handler.onReceiveEntitlements(entitlements)
    }

    private fun getSkus(
        skus: List<String>,
        @BillingClient.SkuType skuType: String,
        handler: GetSkusResponseHandler
    ) {
        billingWrapper.querySkuDetailsAsync(skuType, skus, object : BillingWrapper.SkuDetailsResponseListener {
            override fun onReceiveSkuDetails(skuDetails: List<SkuDetails>) {
                handler.onReceiveSkus(skuDetails)
            }
        })
    }

    private fun getCaches() {
        if (cachesLastChecked != null && Date().time - cachesLastChecked!!.time < 60000) {
            emitCachePurchaserInfo { listener?.onReceiveUpdatedPurchaserInfo(it) }
        } else {
            cachesLastChecked = Date()

            getSubscriberInfo { listener?.onReceiveUpdatedPurchaserInfo(it) }

            getEntitlements(object : GetEntitlementsHandler {
                override fun onReceiveEntitlements(entitlementMap: Map<String, Entitlement>) {}

                override fun onReceiveEntitlementsError(domain: ErrorDomains, code: Int, message: String) {}
            })
        }
    }

    private fun getSubscriberInfo(onReceived: (PurchaserInfo) -> Unit) {
        backend.getSubscriberInfo(appUserID, object : Backend.BackendResponseHandler() {
            override fun onReceivePurchaserInfo(info: PurchaserInfo) {
                deviceCache.cachePurchaserInfo(appUserID, info)
                onReceived(info)
            }

            override fun onError(code: Int, message: String?) {
                Log.e("Purchases", "Error fetching subscriber data: $message")
                cachesLastChecked = null
            }
        })
    }

    private fun postPurchases(
        purchases: List<Purchase>,
        isRestore: Boolean,
        isPurchase: Boolean
    ) {
        for (p in purchases) {
            val token = p.purchaseToken
            val sku = p.sku

            if (postedTokens.contains(token)) continue
            postedTokens.add(token)
            backend.postReceiptData(
                token,
                appUserID,
                sku,
                isRestore,
                object : Backend.BackendResponseHandler() {
                    override fun onReceivePurchaserInfo(info: PurchaserInfo) {
                        billingWrapper.consumePurchase(token)

                        deviceCache.cachePurchaserInfo(appUserID, info)
                        when {
                            isPurchase -> listener?.onCompletedPurchase(sku, info)
                            isRestore -> listener?.onRestoreTransactions(info)
                            else -> listener?.onReceiveUpdatedPurchaserInfo(info)
                        }
                    }

                    override fun onError(code: Int, message: String?) {
                        if (code < 500) {
                            billingWrapper.consumePurchase(token)
                            postedTokens.remove(token)
                        }

                        when {
                            isPurchase -> listener?.onFailedPurchase(
                                ErrorDomains.REVENUECAT_BACKEND,
                                code,
                                message
                            )
                            isRestore -> listener?.onRestoreTransactionsFailed(
                                ErrorDomains.REVENUECAT_BACKEND,
                                code,
                                message
                            )
                        }
                    }
                })
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

    private fun clearCachedRandomId() {
        deviceCache.clearCachedAppUserID()
    }

    private fun getSkuDetails(entitlements: Map<String, Entitlement>, onCompleted: (HashMap<String, SkuDetails>) -> Unit) {
        val skus =
            entitlements.values.flatMap { it.offerings.values }.map { it.activeProductIdentifier }

        billingWrapper.querySkuDetailsAsync(
            BillingClient.SkuType.SUBS,
            skus,
            object : BillingWrapper.SkuDetailsResponseListener {
                override fun onReceiveSkuDetails(subscriptionsSKUDetails: List<SkuDetails>) {
                    val detailsByID = HashMap<String, SkuDetails>()

                    val inAPPSkus = skus -
                            subscriptionsSKUDetails
                                .map { details -> details.sku to details }
                                .also { skuToDetails -> detailsByID.putAll(skuToDetails) }
                                .map { skuToDetails -> skuToDetails.first }

                    if (inAPPSkus.isNotEmpty()) {
                        billingWrapper.querySkuDetailsAsync(
                            BillingClient.SkuType.INAPP,
                            inAPPSkus,
                            object : BillingWrapper.SkuDetailsResponseListener {
                                override fun onReceiveSkuDetails(skuDetails: List<SkuDetails>) {
                                    detailsByID.putAll(skuDetails.map { it.sku to it })
                                    onCompleted(detailsByID)
                                }
                            }
                        )
                    } else {
                        onCompleted(detailsByID)
                    }
                }
            }
        )
    }

    private fun afterSetListener(value: PurchasesListener?) {
        if (value != null) {
            billingWrapper.setListener(this)
            application.registerActivityLifecycleCallbacks(this)
            getCaches()
        } else {
            billingWrapper.setListener(null)
            application.unregisterActivityLifecycleCallbacks(this)
        }
    }

    private fun makeCachesOutdatedAndNotifyIfNeeded() {
        cachesLastChecked = null
        if (listener != null) {
            getCaches()
        }
    }

    // endregion
    // region Overriden methods
    /**
     * @suppress
     */
    override fun onPurchasesUpdated(purchases: List<@JvmSuppressWildcards Purchase>) {
        postPurchases(purchases, allowSharingPlayStoreAccount, true)
    }

    /**
     * @suppress
     */
    override fun onPurchasesFailedToUpdate(responseCode: Int, message: String) {
        listener?.onFailedPurchase(ErrorDomains.PLAY_BILLING, responseCode, message)
    }

    /**
     * @suppress
     */
    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {

    }

    /**
     * @suppress
     */
    override fun onActivityStarted(activity: Activity) {

    }

    /**
     * @suppress
     */
    override fun onActivityResumed(activity: Activity) {
        if (shouldRefreshCaches) getCaches()
        shouldRefreshCaches = false
    }

    /**
     * @suppress
     */
    override fun onActivityPaused(activity: Activity) {
        shouldRefreshCaches = true
    }

    /**
     * @suppress
     */
    override fun onActivityStopped(activity: Activity) {

    }

    /**
     * @suppress
     */
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle?) {

    }

    /**
     * @suppress
     */
    override fun onActivityDestroyed(activity: Activity) {

    }
    // endregion
    // region Builder

    /**
     * Used to construct a Purchases object
     * @param context Application context that will be used to communicate with Google
     * @param apiKey RevenueCat apiKey. Make sure you follow the [quickstart](https://docs.revenuecat.com/docs/getting-started-1)
     * guide to setup your RevenueCat account.
     */
    class Builder(
        private val context: Context,
        private val apiKey: String
    ) {
        private val application: Application
            get() = context.applicationContext as Application

        private var appUserID: String? = null

        private var service: ExecutorService? = null

        init {
            if (!hasPermission(context, Manifest.permission.INTERNET))
                throw IllegalArgumentException("Purchases requires INTERNET permission.")

            if (apiKey.isBlank())
                throw IllegalArgumentException("API key must be set. Get this from the RevenueCat web app")

            if (context.applicationContext !is Application)
                throw IllegalArgumentException("Needs an application context.")
        }

        /**
         * Used to set a user identifier. Check out this [guide](https://docs.revenuecat.com/docs/user-ids)
         */
        fun appUserID(appUserID: String) = apply { this.appUserID = appUserID }

        /**
         * Used to set a network executor service for this Purchases instance
         */
        fun networkExecutorService(service: ExecutorService) = apply { this.service = service }

        /**
         * Used to build the Purchases instance
         * @return A Purchases instance. Make sure you set it as the [sharedInstance] if you
         * want to reuse it.
         */
        fun build(): Purchases {

            val service = this.service ?: createDefaultExecutor()

            val backend = Backend(
                this.apiKey,
                Dispatcher(service),
                HTTPClient(),
                PurchaserInfo.Factory,
                Entitlement.Factory
            )

            val billingWrapper = BillingWrapper(
                BillingWrapper.ClientFactory(application.applicationContext),
                Handler(application.mainLooper)
            )

            val prefs = PreferenceManager.getDefaultSharedPreferences(this.application)
            val cache = DeviceCache(prefs, apiKey)

            return Purchases(
                application,
                appUserID,
                backend,
                billingWrapper,
                cache
            )
        }

        private fun hasPermission(context: Context, permission: String): Boolean {
            return context.checkCallingOrSelfPermission(permission) == PERMISSION_GRANTED
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
    // endregion
    // region Static
    companion object {
        /**
         * Singleton instance of Purchases
         * @return A previously set singleton Purchases instance or null
         */
        @JvmStatic
        var sharedInstance: Purchases? = null
            set(value) {
                field?.close()
                field = value
            }
        /**
         * Current version of the Purchases SDK
         */
        @JvmStatic
        val frameworkVersion = "1.5.0-SNAPSHOT"
    }

    /**
     * Different error domains
     */
    enum class ErrorDomains {
        /**
        * The error is related to the RevenueCat backend
        */
        REVENUECAT_BACKEND,
        /**
         * The error is related to Play Billing
         */
        PLAY_BILLING
    }

    /**
     * Different compatible attribution networks available
     * @param serverValue Id of this attribution network in the RevenueCat server
     */
    enum class AttributionNetwork(val serverValue: Int)  {
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
        BRANCH(3)
    }
    // endregion
    // region Interfaces
    /**
     * Used to handle async updates from Purchases
     */
    interface PurchasesListener {
        /**
         * Called when purchase completes after a make purchase call or after a renewal
         * @param sku Sku of the purchased product
         * @param purchaserInfo Updated purchaser info after a successful purchase
         */
        fun onCompletedPurchase(sku: String, purchaserInfo: PurchaserInfo)

        /**
         * Called when purchase fails after trying to make a purchase
         * @param domain Can be REVENUECAT_BACKEND or PLAY_BILLING
         * @param code The error code
         * @param reason Message of the error
         */
        fun onFailedPurchase(domain: ErrorDomains, code: Int, reason: String?)

        /**
         * Called when a new purchaser info has been received
         * @param purchaserInfo Updated purchaser info after a successful purchase
         */
        fun onReceiveUpdatedPurchaserInfo(purchaserInfo: PurchaserInfo)

        /**
         * Called after successfully restoring purchases after restorePurchasesForPlayStoreAccount
         * @param purchaserInfo Updated purchaser info after a successful restore
         */
        fun onRestoreTransactions(purchaserInfo: PurchaserInfo)

        /**
         * Called when restoring transactions fails after restorePurchasesForPlayStoreAccount
         * @param domain Can be REVENUECAT_BACKEND or PLAY_BILLING
         * @param code The error code
         * @param reason Message of the error
         */
        fun onRestoreTransactionsFailed(domain: ErrorDomains, code: Int, reason: String?)
    }

    /**
     * Used when retrieving subscriptions
     */
    interface GetSkusResponseHandler {
        /**
         * Will be called after fetching subscriptions
         * @param skus List of SkuDetails
         */
        fun onReceiveSkus(skus: @JvmSuppressWildcards List<SkuDetails>)
    }

    /**
     * Used when retrieving entitlements
     */
    interface GetEntitlementsHandler {
        /**
         * Will be called after a successful fetch of entitlements
         * @param entitlementMap Map of entitlements keyed by name
         */
        fun onReceiveEntitlements(entitlementMap: Map<String, Entitlement>)

        /**
         * Will be called if there was any problem fetching entitlements
         * @param domain Can be REVENUECAT_BACKEND or PLAY_BILLING
         * @param code The error code
         * @param message Message of the error
         */
        fun onReceiveEntitlementsError(domain: ErrorDomains, code: Int, message: String)
    }

    /**
     * Used when creating an alias
     */
    interface AliasHandler {
        /**
         * Will be called after a successful create alias call
         */
        fun onSuccess()

        /**
         * Will be called if an error happened while creating the alias
         * @param domain Can be REVENUECAT_BACKEND or PLAY_BILLING
         * @param code The error code
         * @param message Message of the error
         */
        fun onError(domain: ErrorDomains, code: Int, message: String)
    }

}
