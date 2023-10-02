package com.revenuecat.purchases

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
import com.revenuecat.purchases.common.Constants
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.debugLogsEnabled
import com.revenuecat.purchases.common.diagnostics.DiagnosticsSynchronizer
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.offerings.OfferingsManager
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.common.warnLog
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
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.AttributionStrings
import com.revenuecat.purchases.strings.ConfigureStrings
import com.revenuecat.purchases.strings.CustomerInfoStrings
import com.revenuecat.purchases.strings.IdentityStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.utils.CustomActivityLifecycleHandler
import com.revenuecat.purchases.utils.isAndroidNOrNewer
import java.net.URL
import java.util.Collections

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions")
internal class PurchasesOrchestrator constructor(
    private val application: Application,
    backingFieldAppUserID: String?,
    private val backend: Backend,
    private val billing: BillingAbstract,
    private val deviceCache: DeviceCache,
    private val identityManager: IdentityManager,
    private val subscriberAttributesManager: SubscriberAttributesManager,
    var appConfig: AppConfig,
    private val customerInfoHelper: CustomerInfoHelper,
    private val customerInfoUpdateHandler: CustomerInfoUpdateHandler,
    diagnosticsSynchronizer: DiagnosticsSynchronizer?,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val postReceiptHelper: PostReceiptHelper,
    private val postTransactionWithProductDetailsHelper: PostTransactionWithProductDetailsHelper,
    private val postPendingTransactionsHelper: PostPendingTransactionsHelper,
    private val syncPurchasesHelper: SyncPurchasesHelper,
    private val offeringsManager: OfferingsManager,
    // This is nullable due to: https://github.com/RevenueCat/purchases-flutter/issues/408
    private val mainHandler: Handler? = Handler(Looper.getMainLooper()),
) : LifecycleDelegate, CustomActivityLifecycleHandler {

    /** @suppress */
    @Suppress("RedundantGetter", "RedundantSetter")
    @Volatile
    internal var state = PurchasesState()
        @Synchronized
        get() = field

        @Synchronized
        set(value) {
            field = value
        }

    var finishTransactions: Boolean
        @Synchronized get() = appConfig.finishTransactions

        @Synchronized set(value) {
            appConfig.finishTransactions = value
        }

    val appUserID: String
        @Synchronized get() = identityManager.currentAppUserID

    var updatedCustomerInfoListener: UpdatedCustomerInfoListener?
        @Synchronized get() = customerInfoUpdateHandler.updatedCustomerInfoListener

        @Synchronized set(value) {
            customerInfoUpdateHandler.updatedCustomerInfoListener = value
        }

    val isAnonymous: Boolean
        get() = identityManager.currentUserIsAnonymous()

    val store: Store
        get() = appConfig.store

    private val lifecycleHandler: AppLifecycleHandler by lazy {
        AppLifecycleHandler(this)
    }

    var allowSharingPlayStoreAccount: Boolean
        @Synchronized get() =
            state.allowSharingPlayStoreAccount ?: identityManager.currentUserIsAnonymous()

        @Synchronized set(value) {
            state = state.copy(allowSharingPlayStoreAccount = value)
        }

    init {
        identityManager.configure(backingFieldAppUserID)

        billing.stateListener = object : BillingAbstract.StateListener {
            override fun onConnected() {
                postPendingTransactionsHelper.syncPendingPurchaseQueue(allowSharingPlayStoreAccount)
            }
        }
        billing.purchasesUpdatedListener = getPurchasesUpdatedListener()

        dispatch {
            // This needs to happen after the billing client listeners have been set. This is because
            // we perform operations with the billing client in the lifecycle observer methods.
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleHandler)
            application.registerActivityLifecycleCallbacks(this)
        }

        if (!appConfig.dangerousSettings.autoSyncPurchases) {
            log(LogIntent.WARNING, ConfigureStrings.AUTO_SYNC_PURCHASES_DISABLED)
        }

        if (isAndroidNOrNewer()) {
            diagnosticsSynchronizer?.clearDiagnosticsFileIfTooBig()
            diagnosticsSynchronizer?.syncDiagnosticsFileIfNeeded()
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
        if (shouldRefreshCustomerInfo(firstTimeInForeground)) {
            log(LogIntent.DEBUG, CustomerInfoStrings.CUSTOMERINFO_STALE_UPDATING_FOREGROUND)
            customerInfoHelper.retrieveCustomerInfo(
                identityManager.currentAppUserID,
                fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                appInBackground = false,
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
            )
        }
        offeringsManager.onAppForeground(identityManager.currentAppUserID)
        postPendingTransactionsHelper.syncPendingPurchaseQueue(allowSharingPlayStoreAccount)
        synchronizeSubscriberAttributesIfNeeded()
        offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
    }

    override fun onActivityStarted(activity: Activity) {
        if (appConfig.showInAppMessagesAutomatically) {
            showInAppMessagesIfNeeded(activity, InAppMessageType.values().toList())
        }
    }

    // region Public Methods

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
                    PostReceiptInitiationSource.RESTORE,
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
                                initiationSource = PostReceiptInitiationSource.RESTORE,
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
                        customerInfoUpdateHandler.notifyListeners(customerInfo)
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
                allowSharingPlayStoreAccount,
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

    fun logOut(callback: ReceiveCustomerInfoCallback? = null) {
        identityManager.logOut { error ->
            if (error != null) {
                callback?.onError(error)
            } else {
                synchronized(this@PurchasesOrchestrator) {
                    state = state.copy(purchaseCallbacksByProductId = Collections.emptyMap())
                }
                updateAllCaches(identityManager.currentAppUserID, callback)
            }
        }
    }

    fun close() {
        synchronized(this@PurchasesOrchestrator) {
            state = state.copy(purchaseCallbacksByProductId = Collections.emptyMap())
        }
        this.backend.close()

        billing.close()
        updatedCustomerInfoListener = null // Do not call on state since the setter does more stuff

        dispatch {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleHandler)
        }
    }

    fun getCustomerInfo(
        callback: ReceiveCustomerInfoCallback,
    ) {
        getCustomerInfo(CacheFetchPolicy.default(), callback)
    }

    fun getCustomerInfo(
        fetchPolicy: CacheFetchPolicy,
        callback: ReceiveCustomerInfoCallback,
    ) {
        customerInfoHelper.retrieveCustomerInfo(
            identityManager.currentAppUserID,
            fetchPolicy,
            state.appInBackground,
            allowSharingPlayStoreAccount,
            callback,
        )
    }

    fun removeUpdatedCustomerInfoListener() {
        // Don't set on state directly since setter does more things
        this.updatedCustomerInfoListener = null
    }

    fun showInAppMessagesIfNeeded(activity: Activity, inAppMessageTypes: List<InAppMessageType>) {
        billing.showInAppMessagesIfNeeded(activity, inAppMessageTypes) {
            syncPurchases()
        }
    }

    fun invalidateCustomerInfoCache() {
        log(LogIntent.DEBUG, CustomerInfoStrings.INVALIDATING_CUSTOMERINFO_CACHE)
        deviceCache.clearCustomerInfoCache(appUserID)
    }

    fun getProductsOfTypes(
        productIds: Set<String>,
        types: Set<ProductType>,
        callback: GetStoreProductsCallback,
    ) {
        val validTypes = types.filter { it != ProductType.UNKNOWN }.toSet()
        getProductsOfTypes(productIds, validTypes, emptyList(), callback)
    }

    // region Subscriber Attributes
    // region Special Attributes

    fun setAttributes(attributes: Map<String, String?>) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setAttributes"))
        subscriberAttributesManager.setAttributes(attributes, appUserID)
    }

    fun setEmail(email: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setEmail"))
        subscriberAttributesManager.setAttribute(SubscriberAttributeKey.Email, email, appUserID)
    }

    fun setPhoneNumber(phoneNumber: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setPhoneNumber"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.PhoneNumber,
            phoneNumber,
            appUserID,
        )
    }

    fun setDisplayName(displayName: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setDisplayName"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.DisplayName,
            displayName,
            appUserID,
        )
    }

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

    fun setMixpanelDistinctID(mixpanelDistinctID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setMixpanelDistinctID"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.MixpanelDistinctId,
            mixpanelDistinctID,
            appUserID,
        )
    }

    fun setOnesignalID(onesignalID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setOnesignalID"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.OneSignal,
            onesignalID,
            appUserID,
        )
    }

    fun setAirshipChannelID(airshipChannelID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setAirshipChannelID"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.Airship,
            airshipChannelID,
            appUserID,
        )
    }

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

    fun collectDeviceIdentifiers() {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("collectDeviceIdentifiers"))
        subscriberAttributesManager.collectDeviceIdentifiers(appUserID, application)
    }

    fun setAdjustID(adjustID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setAdjustID"))
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Adjust,
            adjustID,
            appUserID,
            application,
        )
    }

    fun setAppsflyerID(appsflyerID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setAppsflyerID"))
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.AppsFlyer,
            appsflyerID,
            appUserID,
            application,
        )
    }

    fun setFBAnonymousID(fbAnonymousID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setFBAnonymousID"))
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Facebook,
            fbAnonymousID,
            appUserID,
            application,
        )
    }

    fun setMparticleID(mparticleID: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setMparticleID"))
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Mparticle,
            mparticleID,
            appUserID,
            application,
        )
    }

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

    fun setMediaSource(mediaSource: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setMediaSource"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.MediaSource,
            mediaSource,
            appUserID,
        )
    }

    fun setCampaign(campaign: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setCampaign"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Campaign,
            campaign,
            appUserID,
        )
    }

    fun setAdGroup(adGroup: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setAdGroup"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.AdGroup,
            adGroup,
            appUserID,
        )
    }

    fun setAd(ad: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("setAd"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Ad,
            ad,
            appUserID,
        )
    }

    fun setKeyword(keyword: String?) {
        log(LogIntent.DEBUG, AttributionStrings.METHOD_CALLED.format("seKeyword"))
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Keyword,
            keyword,
            appUserID,
        )
    }

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

    // region Custom entitlements computation
    fun switchUser(newAppUserID: String) {
        if (identityManager.currentAppUserID == newAppUserID) {
            warnLog(IdentityStrings.SWITCHING_USER_SAME_APP_USER_ID.format(newAppUserID))
            return
        }

        identityManager.switchUser(newAppUserID)

        offeringsManager.fetchAndCacheOfferings(newAppUserID, state.appInBackground)
    }
    //endregion

    //endregion

    // region Private Methods

    private fun shouldRefreshCustomerInfo(firstTimeInForeground: Boolean): Boolean {
        return !appConfig.customEntitlementComputation &&
            (firstTimeInForeground || deviceCache.isCustomerInfoCacheStale(appUserID, appInBackground = false))
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
                allowSharingPlayStoreAccount,
                completion,
            )
            offeringsManager.fetchAndCacheOfferings(appUserID, appInBackground)
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

                synchronized(this@PurchasesOrchestrator) {
                    isDeprecatedProductChangeInProgress = state.deprecatedProductChangeCallback != null
                    if (isDeprecatedProductChangeInProgress) {
                        deprecatedProductChangeListener = getAndClearProductChangeCallback()
                        callbackPair = getProductChangeCompletedCallbacks(deprecatedProductChangeListener)
                    } else {
                        deprecatedProductChangeListener = null
                        callbackPair = getPurchaseCompletedCallbacks()
                    }
                }

                postTransactionWithProductDetailsHelper.postTransactions(
                    purchases,
                    allowSharingPlayStoreAccount,
                    appUserID,
                    PostReceiptInitiationSource.PURCHASE,
                    transactionPostSuccess = callbackPair.first,
                    transactionPostError = callbackPair.second,
                )
            }

            override fun onPurchasesFailedToUpdate(purchasesError: PurchasesError) {
                synchronized(this@PurchasesOrchestrator) {
                    getAndClearProductChangeCallback()?.dispatch(purchasesError)
                        ?: getAndClearAllPurchaseCallbacks().forEach { it.dispatch(purchasesError) }
                }
            }
        }
    }

    private fun getAndClearAllPurchaseCallbacks(): List<PurchaseCallback> {
        synchronized(this@PurchasesOrchestrator) {
            state.purchaseCallbacksByProductId.let { purchaseCallbacks ->
                state = state.copy(purchaseCallbacksByProductId = Collections.emptyMap())
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

    fun startPurchase(
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
        synchronized(this@PurchasesOrchestrator) {
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

    fun startProductChange(
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
        synchronized(this@PurchasesOrchestrator) {
            if (!appConfig.finishTransactions) {
                log(LogIntent.WARNING, PurchaseStrings.PURCHASE_FINISH_TRANSACTION_FALSE)
            }

            if (!state.purchaseCallbacksByProductId.containsKey(purchasingData.productId)) {
                val productId = purchasingData.productId
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

    fun startDeprecatedProductChange(
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
        synchronized(this@PurchasesOrchestrator) {
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

        var previousProductId = oldProductId

        if (oldProductId.contains(Constants.SUBS_ID_BASE_PLAN_ID_SEPARATOR)) {
            previousProductId = oldProductId.substringBefore(Constants.SUBS_ID_BASE_PLAN_ID_SEPARATOR)
            warnLog(
                "Using incorrect oldProductId: $oldProductId. The productId should not contain the basePlanId. " +
                    "Using productId: $previousProductId.",
            )
        }

        billing.findPurchaseInPurchaseHistory(
            appUserID,
            ProductType.SUBS,
            previousProductId,
            onCompletion = { purchaseRecord ->
                log(LogIntent.PURCHASE, PurchaseStrings.FOUND_EXISTING_PURCHASE.format(previousProductId))

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

    private fun synchronizeSubscriberAttributesIfNeeded() {
        subscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserID)
    }

    // endregion

    // region Static

    internal companion object {

        var platformInfo: PlatformInfo = PlatformInfo(
            flavor = "native",
            version = null,
        )

        var debugLogsEnabled
            get() = logLevel.debugLogsEnabled
            set(value) {
                logLevel = LogLevel.debugLogsEnabled(value)
            }

        var logLevel: LogLevel
            get() = Config.logLevel
            set(value) {
                Config.logLevel = value
            }

        var logHandler: LogHandler
            @Synchronized get() = currentLogHandler

            @Synchronized set(value) {
                currentLogHandler = value
            }

        const val frameworkVersion = Config.frameworkVersion

        var proxyURL: URL? = null

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
        fun canMakePayments(
            context: Context,
            features: List<BillingFeature> = listOf(),
            callback: Callback<Boolean>,
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
