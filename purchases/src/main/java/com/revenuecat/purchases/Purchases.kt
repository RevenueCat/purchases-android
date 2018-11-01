package com.revenuecat.purchases

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.annotation.IntDef
import android.util.Log

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails

import org.json.JSONException
import org.json.JSONObject

import java.lang.annotation.Retention
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
import java.lang.annotation.RetentionPolicy.SOURCE

class Purchases internal constructor(
    private val application: Application,
    appUserID: String?, private val listener: PurchasesListener,
    private val backend: Backend,
    billingWrapperFactory: BillingWrapper.Factory,
    private val deviceCache: DeviceCache
) : BillingWrapper.PurchasesUpdatedListener, Application.ActivityLifecycleCallbacks {

    /**
     * returns the passed in or generated app user ID
     * @return appUserID
     */
    val appUserID: String
    private var usingAnonymousID: Boolean? = false
    private val billingWrapper: BillingWrapper

    private val postedTokens = HashSet<String>()

    private var cachesLastChecked: Date? = null
    private var cachedEntitlements: Map<String, Entitlement>? = null

    @IntDef(ErrorDomains.REVENUECAT_BACKEND.toLong(), ErrorDomains.PLAY_BILLING.toLong())
    @Retention(SOURCE)
    annotation class ErrorDomains {
        companion object {
            val REVENUECAT_BACKEND = 0
            val PLAY_BILLING = 1
        }
    }

    @IntDef(
        AttributionNetwork.ADJUST.toLong(),
        AttributionNetwork.APPSFLYER.toLong(),
        AttributionNetwork.BRANCH.toLong()
    )
    @Retention(SOURCE)
    annotation class AttributionNetwork {
        companion object {
            val ADJUST = 1
            val APPSFLYER = 2
            val BRANCH = 3
        }
    }

    /**
     * Used to handle async updates from Purchases
     */
    interface PurchasesListener {
        fun onCompletedPurchase(sku: String, purchaserInfo: PurchaserInfo)
        fun onFailedPurchase(@ErrorDomains domain: Int, code: Int, reason: String)

        fun onReceiveUpdatedPurchaserInfo(purchaserInfo: PurchaserInfo)

        fun onRestoreTransactions(purchaserInfo: PurchaserInfo)
        fun onRestoreTransactionsFailed(@ErrorDomains domain: Int, code: Int, reason: String)
    }

    interface GetSkusResponseHandler {
        fun onReceiveSkus(skus: List<SkuDetails>)
    }

    interface GetEntitlementsHandler {
        fun onReceiveEntitlements(entitlementMap: Map<String, Entitlement>)
        fun onReceiveEntitlementsError(@ErrorDomains domain: Int, code: Int, message: String)
    }

    init {
        var appUserID = appUserID

        if (appUserID == null) {
            usingAnonymousID = true

            appUserID = deviceCache.getCachedAppUserID()

            if (appUserID == null) {
                appUserID = UUID.randomUUID().toString()
                deviceCache.cacheAppUserID(appUserID)
            }
        }
        this.appUserID = appUserID
        this.billingWrapper = billingWrapperFactory.buildWrapper(this)
        this.application.registerActivityLifecycleCallbacks(this)

        emitCachedAsUpdatedPurchaserInfo()
        getCaches()
        restorePurchasesForPlayStoreAccount()
    }

    /**
     * If true, treats all purchases as restores, aliasing together appUserIDs that share a Play acccount.
     * @param isUsingAnonymousID
     */

    fun setIsUsingAnonymousID(isUsingAnonymousID: Boolean) {
        this.usingAnonymousID = isUsingAnonymousID
    }

    /**
     * Add attribution data from a supported network
     */
    fun addAttributionData(data: JSONObject, @AttributionNetwork network: Int) {
        backend.postAttributionData(appUserID, network, data)
    }

    fun addAttributionData(data: Map<String, String>, @AttributionNetwork network: Int) {
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

    private fun emitCachedAsUpdatedPurchaserInfo() {
        val info = deviceCache.getCachedPurchaserInfo(appUserID)
        if (info != null) {
            listener.onReceiveUpdatedPurchaserInfo(info)
        }
    }

    private fun emitCachedAsRestoredTransactionsPurchaserInfo() {
        val info = deviceCache.getCachedPurchaserInfo(appUserID)
        if (info != null) {
            listener.onRestoreTransactions(info)
        }
    }

    /**
     * Fetch the configured entitlements for this user. Entitlements allows you to configure your in-app products via RevenueCat
     * and greatly simplifies management. See the guide (https://docs.revenuecat.com/v1.0/docs/entitlements) for more info.
     *
     * Entitlements will be fetched and cached on instantiation so that, by the time they are needed, your prices are
     * loaded for your purchase flow. Time is money.
     *
     * @param handler Called when entitlements are available. Called immediately if entitlements are cached.
     */
    fun getEntitlements(handler: GetEntitlementsHandler) {
        if (this.cachedEntitlements != null) {
            handler.onReceiveEntitlements(this.cachedEntitlements)
        } else {
            backend.getEntitlements(appUserID, object : Backend.EntitlementsResponseHandler() {
                override fun onReceiveEntitlements(entitlements: Map<String, Entitlement>) {
                    val skus = ArrayList<String>()
                    val detailsByID = HashMap<String, SkuDetails>()
                    for ((offerings) in entitlements.values) {
                        for ((activeProductIdentifier) in offerings.values) {
                            skus.add(activeProductIdentifier)
                        }
                    }

                    billingWrapper.querySkuDetailsAsync(
                        BillingClient.SkuType.SUBS,
                        skus
                    ) { skuDetails ->
                        val skusCopy = ArrayList(skus)
                        for (d in skuDetails) {
                            skusCopy.remove(d.sku)
                            detailsByID[d.sku] = d
                        }
                        if (skusCopy.size > 0) {
                            billingWrapper.querySkuDetailsAsync(
                                BillingClient.SkuType.INAPP,
                                skusCopy
                            ) { skuDetails ->
                                for (d in skuDetails) {
                                    detailsByID[d.sku] = d
                                }
                                populateSkuDetailsAndCallHandler(detailsByID, entitlements, handler)
                            }
                        } else {
                            populateSkuDetailsAndCallHandler(detailsByID, entitlements, handler)
                        }
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
    }

    private fun populateSkuDetailsAndCallHandler(
        details: Map<String, SkuDetails>,
        entitlements: Map<String, Entitlement>,
        handler: GetEntitlementsHandler
    ) {
        for (e in entitlements.values) {
            for (o in e.offerings.values) {
                if (details.containsKey(o.activeProductIdentifier)) {
                    o.skuDetails = details[o.activeProductIdentifier]
                } else {
                    Log.e("Purchases", "Failed to find SKU for " + o.activeProductIdentifier)
                }
            }
        }
        cachedEntitlements = entitlements
        handler.onReceiveEntitlements(entitlements)
    }


    /**
     * Gets the SKUDetails for the given list of subscription skus.
     * @param skus List of skus
     * @param handler Response handler
     */
    fun getSubscriptionSkus(skus: List<String>, handler: GetSkusResponseHandler) {
        getSkus(skus, BillingClient.SkuType.SUBS, handler)
    }

    /**
     * Gets the SKUDetails for the given list of non-subscription skus.
     * @param skus
     * @param handler
     */
    fun getNonSubscriptionSkus(skus: List<String>, handler: GetSkusResponseHandler) {
        getSkus(skus, BillingClient.SkuType.INAPP, handler)
    }

    private fun getSkus(
        skus: List<String>, @BillingClient.SkuType skuType: String,
        handler: GetSkusResponseHandler
    ) {
        billingWrapper.querySkuDetailsAsync(skuType, skus) { skuDetails ->
            handler.onReceiveSkus(
                skuDetails
            )
        }
    }

    /**
     * Make a purchase passing in the skus you wish to upgrade from.
     * @param activity Current activity
     * @param sku The sku you wish to purchase
     * @param skuType The type of sku, INAPP or SUBS
     * @param oldSkus List of old skus to upgrade from
     */
    @JvmOverloads
    fun makePurchase(
        activity: Activity, sku: String,
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
                                        emitCachedAsRestoredTransactionsPurchaserInfo()
                                    } else {
                                        cachesLastChecked = Date()

                                        // TODO: change this with a Lambda when we migrate this class to Kotlin
                                        getSubscriberInfoAndPostToRestoreTransactionListener()
                                    }
                                } else {
                                    postPurchases(allPurchases, true, false)
                                }
                            }

                            override fun onReceivePurchaseHistoryError(
                                responseCode: Int,
                                message: String
                            ) {
                                listener.onRestoreTransactionsFailed(
                                    ErrorDomains.PLAY_BILLING,
                                    responseCode,
                                    message
                                )
                            }
                        })
                }

                override fun onReceivePurchaseHistoryError(responseCode: Int, message: String) {
                    listener.onRestoreTransactionsFailed(
                        ErrorDomains.PLAY_BILLING,
                        responseCode,
                        message
                    )
                }
            })
    }

    private fun getSubscriberInfoAndPostToRestoreTransactionListener() {
        backend.getSubscriberInfo(appUserID, object : Backend.BackendResponseHandler() {
            override fun onReceivePurchaserInfo(info: PurchaserInfo) {
                deviceCache.cachePurchaserInfo(appUserID, info)
                listener.onRestoreTransactions(info)
            }

            override fun onError(code: Int, message: String) {
                Log.e("Purchases", "Error fetching subscriber data: $message")
                cachesLastChecked = null
            }
        })
    }

    /**
     * Call close when you are done with this instance of Purchases
     */
    fun close() {
        this.billingWrapper.close()
        this.backend.close()
        this.application.unregisterActivityLifecycleCallbacks(this)
    }

    /// Private Methods
    private fun getCaches() {
        if (cachesLastChecked != null && Date().time - cachesLastChecked!!.time < 60000) {
            emitCachedAsUpdatedPurchaserInfo()
        } else {
            cachesLastChecked = Date()

            // TODO: change this with a Lambda when we migrate this class to Kotlin
            getSubscriberInfoAndPostUpdatedPurchaserInfo()

            getEntitlements(object : GetEntitlementsHandler {
                override fun onReceiveEntitlements(entitlementMap: Map<String, Entitlement>) {}

                override fun onReceiveEntitlementsError(domain: Int, code: Int, message: String) {}
            })
        }
    }

    private fun getSubscriberInfoAndPostUpdatedPurchaserInfo() {
        backend.getSubscriberInfo(appUserID, object : Backend.BackendResponseHandler() {
            override fun onReceivePurchaserInfo(info: PurchaserInfo) {
                deviceCache.cachePurchaserInfo(appUserID, info)
                listener.onReceiveUpdatedPurchaserInfo(info)
            }

            override fun onError(code: Int, message: String) {
                Log.e("Purchases", "Error fetching subscriber data: $message")
                cachesLastChecked = null
            }
        })
    }

    private fun postPurchases(
        purchases: List<Purchase>,
        isRestore: Boolean?,
        isPurchase: Boolean?
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
                        if (isPurchase!!) {
                            listener.onCompletedPurchase(sku, info)
                        } else if (isRestore!!) {
                            listener.onRestoreTransactions(info)
                        } else {
                            listener.onReceiveUpdatedPurchaserInfo(info)
                        }
                    }

                    override fun onError(code: Int, message: String) {
                        if (code < 500) {
                            billingWrapper.consumePurchase(token)
                            postedTokens.remove(token)
                        }

                        if (isPurchase!!) {
                            listener.onFailedPurchase(
                                ErrorDomains.REVENUECAT_BACKEND,
                                code,
                                message
                            )
                        } else if (isRestore!!) {
                            listener.onRestoreTransactionsFailed(
                                ErrorDomains.REVENUECAT_BACKEND,
                                code,
                                message
                            )
                        }
                    }
                })
        }
    }

    override fun onPurchasesUpdated(purchases: List<Purchase>) {
        postPurchases(purchases, usingAnonymousID, true)
    }

    override fun onPurchasesFailedToUpdate(responseCode: Int, message: String) {
        listener.onFailedPurchase(ErrorDomains.PLAY_BILLING, responseCode, message)
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        getCaches()
    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }

    /**
     * Used to construct a Purchases object
     */
    class Builder(
        context: Context?,
        private val apiKey: String?,
        private val listener: PurchasesListener?
    ) {
        private val application: Application
        private var appUserID: String? = null
        private var service: ExecutorService? = null

        private fun hasPermission(context: Context, permission: String): Boolean {
            return context.checkCallingOrSelfPermission(permission) == PERMISSION_GRANTED
        }

        init {
            if (context == null) {
                throw IllegalArgumentException("Context must be set.")
            }

            if (!hasPermission(context, Manifest.permission.INTERNET)) {
                throw IllegalArgumentException("Purchases requires INTERNET permission.")
            }

            if (apiKey == null || apiKey.length == 0) {
                throw IllegalArgumentException("API key must be set. Get this from the RevenueCat web app")
            }

            val application = context.applicationContext as Application
                ?: throw IllegalArgumentException("Needs an application context.")

            if (listener == null) {
                throw IllegalArgumentException("Purchases listener must be set")
            }
            this.application = application
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

        fun build(): Purchases {

            var service = this.service
            if (service == null) {
                service = createDefaultExecutor()
            }

            val backend = Backend(
                this.apiKey, Dispatcher(service), HTTPClient(),
                PurchaserInfo.Factory, Entitlement.Factory
            )

            val billingWrapperFactory = BillingWrapper.Factory(
                BillingWrapper.ClientFactory(application.applicationContext),
                Handler(application.mainLooper)
            )

            val prefs = PreferenceManager.getDefaultSharedPreferences(this.application)
            val cache = DeviceCache(prefs, apiKey)

            return Purchases(
                this.application,
                this.appUserID,
                this.listener,
                backend,
                billingWrapperFactory,
                cache
            )
        }

        fun appUserID(appUserID: String): Builder {
            this.appUserID = appUserID
            return this
        }

        fun networkExecutorService(service: ExecutorService): Builder {
            this.service = service
            return this
        }
    }

    companion object {

        val frameworkVersion: String
            get() = "1.4.0-SNAPSHOT"
    }
}
/**
 * Make a purchase.
 * @param activity Current activity
 * @param sku The sku you wish to purchase
 * @param skuType The type of sku, INAPP or SUBS
 */
