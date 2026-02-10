@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases

import android.app.Activity
import android.app.Application
import android.app.backup.BackupManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Pair
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.disk.DiskCache
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.revenuecat.purchases.ads.events.AdTracker
import com.revenuecat.purchases.blockstore.BlockstoreHelper
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Config
import com.revenuecat.purchases.common.Constants
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.DefaultLocaleProvider
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.debugLogsEnabled
import com.revenuecat.purchases.common.diagnostics.DiagnosticsSynchronizer
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.events.EventsManager
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.offerings.OfferingsManager
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.deeplinks.WebPurchaseRedemptionHelper
import com.revenuecat.purchases.google.isSuccessful
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.Callback
import com.revenuecat.purchases.interfaces.GetAmazonLWAConsentStatusCallback
import com.revenuecat.purchases.interfaces.GetCustomerCenterConfigCallback
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.GetStorefrontCallback
import com.revenuecat.purchases.interfaces.GetStorefrontLocaleCallback
import com.revenuecat.purchases.interfaces.GetVirtualCurrenciesCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.PurchaseErrorCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener
import com.revenuecat.purchases.interfaces.SyncAttributesAndOfferingsCallback
import com.revenuecat.purchases.interfaces.SyncPurchasesCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.BillingFeature
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.paywalls.DownloadedFontFamily
import com.revenuecat.purchases.paywalls.FontLoader
import com.revenuecat.purchases.paywalls.PaywallPresentedCache
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.storage.DefaultFileRepository
import com.revenuecat.purchases.storage.FileRepository
import com.revenuecat.purchases.strings.AttributionStrings
import com.revenuecat.purchases.strings.BillingStrings
import com.revenuecat.purchases.strings.ConfigureStrings
import com.revenuecat.purchases.strings.CustomerInfoStrings
import com.revenuecat.purchases.strings.IdentityStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.strings.RestoreStrings
import com.revenuecat.purchases.strings.SyncAttributesAndOfferingsStrings
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.ui.revenuecatui.CustomPaywallHandlerFactory
import com.revenuecat.purchases.utils.CustomActivityLifecycleHandler
import com.revenuecat.purchases.utils.PurchaseParamsValidator
import com.revenuecat.purchases.utils.RateLimiter
import com.revenuecat.purchases.utils.Result
import com.revenuecat.purchases.utils.isAndroidNOrNewer
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencyManager
import java.net.URL
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions")
internal class PurchasesOrchestrator(
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
    private val diagnosticsSynchronizer: DiagnosticsSynchronizer?,
    private val diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val offlineEntitlementsManager: OfflineEntitlementsManager,
    private val postReceiptHelper: PostReceiptHelper,
    private val postTransactionWithProductDetailsHelper: PostTransactionWithProductDetailsHelper,
    private val postPendingTransactionsHelper: PostPendingTransactionsHelper,
    private val syncPurchasesHelper: SyncPurchasesHelper,
    private val offeringsManager: OfferingsManager,
    private val eventsManager: EventsManager,
    private val adEventsManager: EventsManager,
    private val paywallPresentedCache: PaywallPresentedCache,
    private val purchasesStateCache: PurchasesStateCache,
    // This is nullable due to: https://github.com/RevenueCat/purchases-flutter/issues/408
    private val mainHandler: Handler? = Handler(Looper.getMainLooper()),
    private val dispatcher: Dispatcher,
    private val initialConfiguration: PurchasesConfiguration,
    private val fontLoader: FontLoader,
    private val localeProvider: DefaultLocaleProvider,
    private val webPurchaseRedemptionHelper: WebPurchaseRedemptionHelper =
        WebPurchaseRedemptionHelper(
            backend,
            identityManager,
            offlineEntitlementsManager,
            customerInfoUpdateHandler,
        ),
    private val virtualCurrencyManager: VirtualCurrencyManager,
    private val purchaseParamsValidator: PurchaseParamsValidator,
    val processLifecycleOwnerProvider: () -> LifecycleOwner = { ProcessLifecycleOwner.get() },
    private val blockstoreHelper: BlockstoreHelper = BlockstoreHelper(application, identityManager),
    private val backupManager: BackupManager = BackupManager(application),
    val fileRepository: FileRepository = DefaultFileRepository(application),
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    val adTracker: AdTracker = AdTracker(adEventsManager),
) : LifecycleDelegate, CustomActivityLifecycleHandler {

    internal var state: PurchasesState
        get() = purchasesStateCache.purchasesState
        set(value) {
            purchasesStateCache.purchasesState = value
        }

    val currentConfiguration: PurchasesConfiguration
        get() = if (initialConfiguration.appUserID == null) {
            initialConfiguration
        } else {
            initialConfiguration.copy(appUserID = this.appUserID)
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

    @get:Synchronized
    @set:Synchronized
    var customerCenterListener: CustomerCenterListener? = null

    @get:Synchronized
    @set:Synchronized
    var trackedEventListener: TrackedEventListener? = null

    val isAnonymous: Boolean
        get() = identityManager.currentUserIsAnonymous()

    val store: Store
        get() = appConfig.store

    private val lifecycleHandler: AppLifecycleHandler by lazy {
        AppLifecycleHandler(this)
    }

    var allowSharingPlayStoreAccount: Boolean
        get() {
            val currentValue = synchronized(this) { state.allowSharingPlayStoreAccount }
            return currentValue ?: identityManager.currentUserIsAnonymous()
        }

        @Synchronized set(value) {
            state = state.copy(allowSharingPlayStoreAccount = value)
        }

    @SuppressWarnings("MagicNumber")
    private val lastSyncAttributesAndOfferingsRateLimiter = RateLimiter(5, 60.seconds)

    @SuppressWarnings("MagicNumber")
    private val preferredLocaleOverrideRateLimiter = RateLimiter(2, 60.seconds)

    var storefrontCountryCode: String? = null
        private set

    val storefrontLocale: Locale?
        get() = storefrontCountryCode?.let { Locale.Builder().setRegion(it).build() }

    @Volatile
    private var _preferredUILocaleOverride: String? = initialConfiguration.preferredUILocaleOverride

    val preferredUILocaleOverride: String?
        get() = _preferredUILocaleOverride

    /**
     * Factory for creating custom paywall handlers. Marked as InternalRevenueCatAPI
     * because it's only accessed by the RevenueCatUI module.
     */
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @InternalRevenueCatAPI
    val customPaywallHandlerFactory: CustomPaywallHandlerFactory?
        get() = initialConfiguration.customPaywallHandlerFactory

    init {
        // Initialize locale provider with the initial preferred locale override
        localeProvider.setPreferredLocaleOverride(_preferredUILocaleOverride)

        identityManager.configure(backingFieldAppUserID)

        billing.stateListener = object : BillingAbstract.StateListener {
            override fun onConnected() {
                postPendingTransactionsHelper.syncPendingPurchaseQueue(
                    allowSharingPlayStoreAccount,
                )
                billing.getStorefront(
                    onSuccess = { countryCode ->
                        storefrontCountryCode = countryCode
                        debugLog { BillingStrings.BILLING_COUNTRY_CODE.format(countryCode) }
                    },
                    onError = { error ->
                        errorLog(error)
                    },
                )
            }
        }
        billing.purchasesUpdatedListener = getPurchasesUpdatedListener()
        billing.startConnectionOnMainThread()

        dispatch {
            // This needs to happen after the billing client listeners have been set. This is because
            // we perform operations with the billing client in the lifecycle observer methods.
            processLifecycleOwnerProvider().lifecycle.addObserver(lifecycleHandler)
            application.registerActivityLifecycleCallbacks(this)
        }

        if (!appConfig.dangerousSettings.autoSyncPurchases) {
            log(LogIntent.WARNING) { ConfigureStrings.AUTO_SYNC_PURCHASES_DISABLED }
        }
    }

    /** @suppress */
    override fun onAppBackgrounded() {
        synchronized(this) {
            state = state.copy(appInBackground = true)
        }
        log(LogIntent.DEBUG) { ConfigureStrings.APP_BACKGROUNDED }
        appConfig.isAppBackgrounded = true
        synchronizeSubscriberAttributesIfNeeded()
        flushEvents(Delay.NONE)
    }

    /** @suppress */
    override fun onAppForegrounded() {
        val firstTimeInForeground: Boolean
        synchronized(this) {
            firstTimeInForeground = state.firstTimeInForeground
            state = state.copy(appInBackground = false, firstTimeInForeground = false)
        }
        log(LogIntent.DEBUG) { ConfigureStrings.APP_FOREGROUNDED }
        appConfig.isAppBackgrounded = false

        enqueue {
            if (shouldRefreshCustomerInfo(firstTimeInForeground)) {
                log(LogIntent.DEBUG) { CustomerInfoStrings.CUSTOMERINFO_STALE_UPDATING_FOREGROUND }
                customerInfoHelper.retrieveCustomerInfo(
                    identityManager.currentAppUserID,
                    fetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
                    appInBackground = false,
                    allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                    callback = object : ReceiveCustomerInfoCallback {
                        override fun onReceived(customerInfo: CustomerInfo) {
                            blockstoreHelper.storeUserIdIfNeeded(customerInfo)
                        }

                        override fun onError(error: PurchasesError) {
                            // no-op
                        }
                    },
                )
            }
            offeringsManager.onAppForeground(identityManager.currentAppUserID)
            postPendingTransactionsHelper.syncPendingPurchaseQueue(allowSharingPlayStoreAccount)
            synchronizeSubscriberAttributesIfNeeded()
            offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
            flushEvents(Delay.DEFAULT)
            if (firstTimeInForeground && isAndroidNOrNewer()) {
                diagnosticsSynchronizer?.syncDiagnosticsFileIfNeeded()
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (appConfig.showInAppMessagesAutomatically) {
            showInAppMessagesIfNeeded(activity, InAppMessageType.values().toList())
        }
    }

    override fun onActivityPaused(activity: Activity) {
        flushEvents(Delay.NONE)
    }

    fun redeemWebPurchase(
        webPurchaseRedemption: WebPurchaseRedemption,
        listener: RedeemWebPurchaseListener,
    ) {
        webPurchaseRedemptionHelper.handleRedeemWebPurchase(webPurchaseRedemption, listener)
    }

    // region Public Methods

    fun getStorefrontCountryCode(callback: GetStorefrontCallback) {
        storefrontCountryCode?.let {
            callback.onReceived(it)
        } ?: run {
            billing.getStorefront(
                onSuccess = { countryCode ->
                    storefrontCountryCode = countryCode
                    callback.onReceived(countryCode)
                },
                onError = { error ->
                    errorLog(error)
                    callback.onError(error)
                },
            )
        }
    }

    @ExperimentalPreviewRevenueCatPurchasesAPI
    fun getStorefrontLocale(callback: GetStorefrontLocaleCallback) {
        getStorefrontCountryCode(
            object : GetStorefrontCallback {
                override fun onReceived(storefrontCountryCode: String) {
                    callback.onReceived(
                        storefrontLocale = Locale.Builder().setRegion(storefrontCountryCode).build(),
                    )
                }

                override fun onError(error: PurchasesError) {
                    callback.onError(error)
                }
            },
        )
    }

    fun syncAttributesAndOfferingsIfNeeded(
        callback: SyncAttributesAndOfferingsCallback,
    ) {
        val receiveOfferingsCallback = object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                callback.onSuccess(offerings)
            }

            override fun onError(error: PurchasesError) {
                callback.onError(error)
            }
        }

        if (!lastSyncAttributesAndOfferingsRateLimiter.shouldProceed()) {
            log(LogIntent.WARNING) {
                SyncAttributesAndOfferingsStrings.RATE_LIMIT_REACHED.format(
                    lastSyncAttributesAndOfferingsRateLimiter.maxCallsInPeriod,
                    lastSyncAttributesAndOfferingsRateLimiter.periodSeconds.inWholeSeconds,
                )
            }

            getOfferings(receiveOfferingsCallback)
            return
        }

        subscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserID) {
            getOfferings(receiveOfferingsCallback, fetchCurrent = true)
        }
    }

    fun syncPurchases(
        listener: SyncPurchasesCallback? = null,
    ) {
        if (appConfig.apiKeyValidationResult == APIKeyValidator.ValidationResult.SIMULATED_STORE) {
            log(LogIntent.DEBUG) { RestoreStrings.SYNC_PURCHASES_SIMULATED_STORE }
            getCustomerInfo(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    listener?.onSuccess(customerInfo)
                }

                override fun onError(error: PurchasesError) {
                    listener?.onError(error)
                }
            })
            return
        }
        syncPurchasesHelper.syncPurchases(
            isRestore = this.allowSharingPlayStoreAccount,
            appInBackground = this.state.appInBackground,
            onSuccess = { listener?.onSuccess(it) },
            onError = { listener?.onError(it) },
        )
    }

    fun syncAmazonPurchase(
        productID: String,
        receiptID: String,
        amazonUserID: String,
        isoCurrencyCode: String?,
        price: Double?,
        purchaseTime: Long?,
    ) {
        log(LogIntent.DEBUG) { PurchaseStrings.SYNCING_PURCHASE_STORE_USER_ID.format(receiptID, amazonUserID) }

        deviceCache.getPreviouslySentHashedTokens().takeIf { it.contains(receiptID.sha1()) }?.apply {
            log(LogIntent.DEBUG) { PurchaseStrings.SYNCING_PURCHASE_SKIPPING.format(receiptID, amazonUserID) }
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
                    purchaseTime = purchaseTime,
                    price = price?.takeUnless { it == 0.0 },
                    currency = isoCurrencyCode?.takeUnless { it.isBlank() },
                    marketplace = null,
                    storeUserID = amazonUserID,
                )
                postReceiptHelper.postTokenWithoutConsuming(
                    receiptID,
                    receiptInfo,
                    this.allowSharingPlayStoreAccount,
                    appUserID,
                    PostReceiptInitiationSource.RESTORE,
                    {
                        log(LogIntent.PURCHASE) {
                            PurchaseStrings.PURCHASE_SYNCED_USER_ID.format(receiptID, amazonUserID)
                        }
                    },
                    { error ->
                        log(LogIntent.RC_ERROR) {
                            PurchaseStrings.SYNCING_PURCHASE_ERROR_DETAILS_USER_ID.format(
                                receiptID,
                                amazonUserID,
                                error,
                            )
                        }
                    },
                )
            },
            { error ->
                log(LogIntent.RC_ERROR) {
                    PurchaseStrings.SYNCING_PURCHASE_ERROR_DETAILS_USER_ID.format(receiptID, amazonUserID, error)
                }
            },
        )
    }

    fun getAmazonLWAConsentStatus(callback: GetAmazonLWAConsentStatusCallback) {
        billing.getAmazonLWAConsentStatus(
            onSuccess = { callback.onSuccess(it) },
            onError = { callback.onError(it) },
        )
    }

    /**
     * Override the preferred UI locale for RevenueCat UI components like Paywalls and Customer Center.
     * This allows you to display the UI in a specific language, different from the system locale.
     *
     * @param localeString The locale string in the format "language_COUNTRY" (e.g., "en_US", "es_ES", "de_DE").
     *                     Pass null to revert to using the system default locale.
     *
     * **Note:** This only affects UI components from the RevenueCatUI module and requires
     * importing RevenueCatUI in your project. The locale override will take effect the next time
     * a paywall or customer center is displayed.
     */
    fun overridePreferredUILocale(localeString: String?): Boolean {
        val previousLocale = _preferredUILocaleOverride

        if (previousLocale == localeString) {
            debugLog { "Locale unchanged, no fresh fetch needed" }
            return false
        }

        synchronized(this) {
            _preferredUILocaleOverride = localeString
            localeProvider.setPreferredLocaleOverride(localeString)
        }

        debugLog { "Locale changed, attempting to fetch fresh offerings" }
        return fetchOfferingsWithRateLimit { offerings, error ->
            if (offerings != null) {
                debugLog { "Fresh offerings fetch completed successfully" }
            } else {
                debugLog { "Fresh offerings fetch failed: ${error?.message}" }
            }
        }
    }

    fun getOfferings(
        listener: ReceiveOfferingsCallback,
        fetchCurrent: Boolean = false,
    ) {
        offeringsManager.getOfferings(
            identityManager.currentAppUserID,
            state.appInBackground,
            { listener.onError(it) },
            { listener.onReceived(it) },
            fetchCurrent,
        )
    }

    fun getProducts(
        productIds: List<String>,
        type: ProductType? = null,
        callback: GetStoreProductsCallback,
    ) {
        val types = type?.let { setOf(type) } ?: setOf(ProductType.SUBS, ProductType.INAPP)

        val productIdsQueriedWithoutBasePlan = productIds
            .filter { !it.contains(Constants.SUBS_ID_BASE_PLAN_ID_SEPARATOR) }
            .toSet()

        val requestedBasePlansByProductId = mutableMapOf<String, MutableSet<String>>()
        val normalizedProductIds = productIds.map { productId ->
            if (productId.contains(Constants.SUBS_ID_BASE_PLAN_ID_SEPARATOR)) {
                val normalizedId = productId.substringBefore(Constants.SUBS_ID_BASE_PLAN_ID_SEPARATOR)
                val basePlanId = productId.substringAfter(Constants.SUBS_ID_BASE_PLAN_ID_SEPARATOR)
                if (normalizedId !in productIdsQueriedWithoutBasePlan) {
                    requestedBasePlansByProductId.getOrPut(normalizedId) { mutableSetOf() }.add(basePlanId)
                }
                normalizedId
            } else {
                productId
            }
        }.toSet()

        getProductsOfTypes(
            normalizedProductIds,
            types,
            object : GetStoreProductsCallback {
                override fun onReceived(storeProducts: List<StoreProduct>) {
                    val filteredProducts = if (requestedBasePlansByProductId.isEmpty()) {
                        storeProducts
                    } else {
                        storeProducts.filter { storeProduct ->
                            val productId = storeProduct.purchasingData.productId
                            val requestedBasePlans = requestedBasePlansByProductId[productId]
                            if (requestedBasePlans != null) {
                                (storeProduct as? GoogleStoreProduct)?.basePlanId in requestedBasePlans
                            } else {
                                true
                            }
                        }
                    }
                    callback.onReceived(filteredProducts)
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
        val purchaseParamsValidationResult = purchaseParamsValidator.validate(purchaseParams = purchaseParams)
        if (purchaseParamsValidationResult is Result.Error) {
            dispatch { callback.onError(purchaseParamsValidationResult.value, userCancelled = false) }
            return
        }

        with(purchaseParams) {
            oldProductId?.let { productId ->
                startProductChange(
                    activity,
                    purchasingData,
                    presentedOfferingContext,
                    productId,
                    googleReplacementMode,
                    isPersonalizedPrice,
                    callback,
                )
            } ?: run {
                startPurchase(
                    activity,
                    purchasingData,
                    presentedOfferingContext,
                    isPersonalizedPrice,
                    callback,
                )
            }
        }
    }

    @Suppress("LongMethod")
    fun restorePurchases(
        callback: ReceiveCustomerInfoCallback,
    ) {
        log(LogIntent.DEBUG) { RestoreStrings.RESTORING_PURCHASE }
        if (!allowSharingPlayStoreAccount) {
            log(LogIntent.WARNING) { RestoreStrings.SHARING_ACC_RESTORE_FALSE }
        }
        if (appConfig.apiKeyValidationResult == APIKeyValidator.ValidationResult.SIMULATED_STORE) {
            log(LogIntent.DEBUG) { RestoreStrings.RESTORE_PURCHASES_SIMULATED_STORE }
            getCustomerInfo(callback)
            return
        }

        val startTime = dateProvider.now
        diagnosticsTrackerIfEnabled?.trackRestorePurchasesStarted()

        val appUserID = identityManager.currentAppUserID

        val callbackWithTracking = if (diagnosticsTrackerIfEnabled == null) {
            callback
        } else {
            object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    diagnosticsTrackerIfEnabled.trackRestorePurchasesResult(
                        null,
                        null,
                        Duration.between(startTime, dateProvider.now),
                    )
                    callback.onReceived(customerInfo)
                }

                override fun onError(error: PurchasesError) {
                    diagnosticsTrackerIfEnabled.trackRestorePurchasesResult(
                        error.code.code,
                        error.message,
                        Duration.between(startTime, dateProvider.now),
                    )
                    callback.onError(error)
                }
            }
        }

        blockstoreHelper.aliasCurrentAndStoredUserIdsIfNeeded {
            billing.queryAllPurchases(
                appUserID,
                onReceivePurchaseHistory = { allPurchases ->
                    if (allPurchases.isEmpty()) {
                        log(LogIntent.DEBUG) { RestoreStrings.RESTORE_PURCHASES_NO_PURCHASES_FOUND }
                        getCustomerInfo(callbackWithTracking)
                    } else {
                        allPurchases.sortedBy { it.purchaseTime }.let { sortedByTime ->
                            sortedByTime.forEach { purchase ->
                                postReceiptHelper.postTransactionAndConsumeIfNeeded(
                                    purchase = purchase,
                                    storeProduct = null,
                                    subscriptionOptionForProductIDs = null,
                                    isRestore = true,
                                    appUserID = appUserID,
                                    initiationSource = PostReceiptInitiationSource.RESTORE,
                                    sdkOriginated = false,
                                    onSuccess = { _, info ->
                                        log(LogIntent.DEBUG) { RestoreStrings.PURCHASE_RESTORED.format(purchase) }
                                        if (sortedByTime.last() == purchase) {
                                            dispatch { callbackWithTracking.onReceived(info) }
                                        }
                                    },
                                    onError = { _, error ->
                                        log(LogIntent.RC_ERROR) {
                                            RestoreStrings.RESTORING_PURCHASE_ERROR.format(purchase, error)
                                        }
                                        if (sortedByTime.last() == purchase) {
                                            dispatch { callbackWithTracking.onError(error) }
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
                onReceivePurchaseHistoryError = { error ->
                    dispatch { callbackWithTracking.onError(error) }
                },
            )
        }
    }

    fun logIn(
        newAppUserID: String,
        callback: LogInCallback? = null,
    ) {
        identityManager.currentAppUserID.takeUnless { it == newAppUserID }?.let {
            blockstoreHelper.clearUserIdBackupIfNeeded {
                identityManager.logIn(
                    newAppUserID,
                    onSuccess = { customerInfo, created ->
                        dispatch {
                            callback?.onReceived(customerInfo, created)
                            customerInfoUpdateHandler.notifyListeners(customerInfo)
                        }
                        offeringsManager.fetchAndCacheOfferings(newAppUserID, state.appInBackground)
                        backupManager.dataChanged()
                    },
                    onError = { error ->
                        dispatch { callback?.onError(error) }
                    },
                )
            }
        }
            ?: customerInfoHelper.retrieveCustomerInfo(
                identityManager.currentAppUserID,
                CacheFetchPolicy.default(),
                state.appInBackground,
                allowSharingPlayStoreAccount,
                callback = receiveCustomerInfoCallback(
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
                backupManager.dataChanged()
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
            processLifecycleOwnerProvider().lifecycle.removeObserver(lifecycleHandler)
        }
    }

    private fun getCustomerInfo(
        callback: ReceiveCustomerInfoCallback,
    ) {
        getCustomerInfo(CacheFetchPolicy.default(), trackDiagnostics = false, callback)
    }

    fun getCustomerInfo(
        fetchPolicy: CacheFetchPolicy,
        trackDiagnostics: Boolean,
        callback: ReceiveCustomerInfoCallback,
    ) {
        customerInfoHelper.retrieveCustomerInfo(
            identityManager.currentAppUserID,
            fetchPolicy,
            state.appInBackground,
            allowSharingPlayStoreAccount,
            trackDiagnostics,
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
        log(LogIntent.DEBUG) { CustomerInfoStrings.INVALIDATING_CUSTOMERINFO_CACHE }
        deviceCache.clearCustomerInfoCache(appUserID)
    }

    fun getProductsOfTypes(
        productIds: Set<String>,
        types: Set<ProductType>,
        callback: GetStoreProductsCallback,
    ) {
        val validTypes = types.filter { it != ProductType.UNKNOWN }.toSet()
        getProductsOfTypes(
            productIds = productIds,
            types = validTypes,
            collectedStoreProducts = emptyList(),
            callback = callback,
        )
    }

    @OptIn(InternalRevenueCatAPI::class)
    fun track(event: FeatureEvent) {
        when (event) {
            is PaywallEvent ->
                paywallPresentedCache.receiveEvent(event)
        }

        eventsManager.track(event)

        trackedEventListener?.onEventTracked(event)
    }

    @OptIn(InternalRevenueCatAPI::class)
    fun getCustomerCenterConfig(callback: GetCustomerCenterConfigCallback) {
        backend.getCustomerCenterConfig(
            identityManager.currentAppUserID,
            onSuccessHandler = { config ->
                callback.onSuccess(config)
            },
            onErrorHandler = { error ->
                callback.onError(error)
            },
        )
    }

    fun createSupportTicket(
        email: String,
        description: String,
        onSuccess: (Boolean) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        backend.postCreateSupportTicket(
            identityManager.currentAppUserID,
            email,
            description,
            onSuccessHandler = onSuccess,
            onErrorHandler = onError,
        )
    }

    // region Subscriber Attributes
    // region Special Attributes

    fun setAttributes(attributes: Map<String, String?>) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setAttributes") }
        subscriberAttributesManager.setAttributes(attributes, appUserID)
    }

    fun setEmail(email: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setEmail") }
        subscriberAttributesManager.setAttribute(SubscriberAttributeKey.Email, email, appUserID)
    }

    fun setPhoneNumber(phoneNumber: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setPhoneNumber") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.PhoneNumber,
            phoneNumber,
            appUserID,
        )
    }

    fun setDisplayName(displayName: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setDisplayName") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.DisplayName,
            displayName,
            appUserID,
        )
    }

    fun setPushToken(fcmToken: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setPushToken") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.FCMTokens,
            fcmToken,
            appUserID,
        )
    }

    // endregion
    // region Integration IDs

    fun setMixpanelDistinctID(mixpanelDistinctID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setMixpanelDistinctID") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.MixpanelDistinctId,
            mixpanelDistinctID,
            appUserID,
        )
    }

    fun setOnesignalID(onesignalID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setOnesignalID") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.OneSignal,
            onesignalID,
            appUserID,
        )
    }

    fun setOnesignalUserID(onesignalUserID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setOnesignalUserID") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.OneSignalUserId,
            onesignalUserID,
            appUserID,
        )
    }

    fun setAirshipChannelID(airshipChannelID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setAirshipChannelID") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.Airship,
            airshipChannelID,
            appUserID,
        )
    }

    fun setFirebaseAppInstanceID(firebaseAppInstanceID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setFirebaseAppInstanceID") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.FirebaseAppInstanceId,
            firebaseAppInstanceID,
            appUserID,
        )
    }

    fun setTenjinAnalyticsInstallationID(tenjinAnalyticsInstallationID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setTenjinAnalyticsInstallationID") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.TenjinAnalyticsInstallationId,
            tenjinAnalyticsInstallationID,
            appUserID,
        )
    }

    fun setPostHogUserId(postHogUserId: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setPostHogUserId") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.IntegrationIds.PostHogUserId,
            postHogUserId,
            appUserID,
        )
    }

    // endregion
    // region Attribution IDs

    fun collectDeviceIdentifiers() {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("collectDeviceIdentifiers") }
        subscriberAttributesManager.collectDeviceIdentifiers(appUserID, application)
    }

    fun setAdjustID(adjustID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setAdjustID") }
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Adjust,
            adjustID,
            appUserID,
            application,
        )
    }

    fun setAppsflyerID(appsflyerID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setAppsflyerID") }
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.AppsFlyer,
            appsflyerID,
            appUserID,
            application,
        )
    }

    fun setFBAnonymousID(fbAnonymousID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setFBAnonymousID") }
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Facebook,
            fbAnonymousID,
            appUserID,
            application,
        )
    }

    fun setMparticleID(mparticleID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setMparticleID") }
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Mparticle,
            mparticleID,
            appUserID,
            application,
        )
    }

    fun setCleverTapID(cleverTapID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setCleverTapID") }
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.CleverTap,
            cleverTapID,
            appUserID,
            application,
        )
    }

    fun setKochavaDeviceID(kochavaDeviceID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setKochavaDeviceID") }
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Kochava,
            kochavaDeviceID,
            appUserID,
            application,
        )
    }

    fun setAirbridgeDeviceID(airbridgeDeviceID: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setAirbridgeDeviceID") }
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Airbridge,
            airbridgeDeviceID,
            appUserID,
            application,
        )
    }

    fun setSolarEngineDistinctId(solarEngineDistinctId: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setSolarEngineDistinctId") }
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.SolarEngineDistinctId,
            solarEngineDistinctId,
            appUserID,
            application,
        )
    }

    fun setSolarEngineAccountId(solarEngineAccountId: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setSolarEngineAccountId") }
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.SolarEngineAccountId,
            solarEngineAccountId,
            appUserID,
            application,
        )
    }

    fun setSolarEngineVisitorId(solarEngineVisitorId: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setSolarEngineVisitorId") }
        subscriberAttributesManager.setAttributionID(
            SubscriberAttributeKey.AttributionIds.SolarEngineVisitorId,
            solarEngineVisitorId,
            appUserID,
            application,
        )
    }

    fun setAppsFlyerConversionData(data: Map<*, *>?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setAppsFlyerConversionData") }
        subscriberAttributesManager.setAppsFlyerConversionData(appUserID, data)
    }

    // endregion

    /**
     * Fetches fresh offerings with rate limiting to prevent excessive network requests.
     *
     * @param callback Callback to handle the result
     * @return true if fresh fetch was triggered, false if rate limited
     */
    private fun fetchOfferingsWithRateLimit(callback: (Offerings?, PurchasesError?) -> Unit): Boolean {
        return if (preferredLocaleOverrideRateLimiter.shouldProceed()) {
            verboseLog { "Fetching fresh offerings" }
            getOfferings(
                object : ReceiveOfferingsCallback {
                    override fun onReceived(offerings: Offerings) {
                        callback(offerings, null)
                    }

                    override fun onError(error: PurchasesError) {
                        callback(null, error)
                    }
                },
                fetchCurrent = true,
            )
            true
        } else {
            debugLog {
                "Fresh offerings fetch rate limit reached: ${preferredLocaleOverrideRateLimiter.maxCallsInPeriod} " +
                    "per ${preferredLocaleOverrideRateLimiter.periodSeconds.inWholeSeconds} seconds. " +
                    "Fetch not triggered."
            }
            false
        }
    }

    // region Campaign parameters

    fun setMediaSource(mediaSource: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setMediaSource") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.MediaSource,
            mediaSource,
            appUserID,
        )
    }

    fun setCampaign(campaign: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setCampaign") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Campaign,
            campaign,
            appUserID,
        )
    }

    fun setAdGroup(adGroup: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setAdGroup") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.AdGroup,
            adGroup,
            appUserID,
        )
    }

    fun setAd(ad: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setAd") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Ad,
            ad,
            appUserID,
        )
    }

    fun setKeyword(keyword: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("seKeyword") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Keyword,
            keyword,
            appUserID,
        )
    }

    fun setCreative(creative: String?) {
        log(LogIntent.DEBUG) { AttributionStrings.METHOD_CALLED.format("setCreative") }
        subscriberAttributesManager.setAttribute(
            SubscriberAttributeKey.CampaignParameters.Creative,
            creative,
            appUserID,
        )
    }

    //endregion

    // region Virtual Currencies
    fun getVirtualCurrencies(
        callback: GetVirtualCurrenciesCallback,
    ) {
        virtualCurrencyManager.virtualCurrencies(callback = callback)
    }

    fun invalidateVirtualCurrenciesCache() {
        virtualCurrencyManager.invalidateVirtualCurrenciesCache()
    }

    val cachedVirtualCurrencies: VirtualCurrencies?
        get() = virtualCurrencyManager.cachedVirtualCurrencies()

    //endregion

    // region Custom entitlements computation
    fun switchUser(newAppUserID: String) {
        if (identityManager.currentAppUserID == newAppUserID) {
            warnLog { IdentityStrings.SWITCHING_USER_SAME_APP_USER_ID.format(newAppUserID) }
            return
        }

        identityManager.switchUser(newAppUserID)

        offeringsManager.fetchAndCacheOfferings(newAppUserID, state.appInBackground)
    }
    //endregion

    //endregion

    // region Paywall fonts

    @InternalRevenueCatAPI
    fun getCachedFontFamilyOrStartDownload(
        fontInfo: UiConfig.AppConfig.FontsConfig.FontInfo.Name,
    ): DownloadedFontFamily? {
        return fontLoader.getCachedFontFamilyOrStartDownload(fontInfo)
    }

    // endregion Paywall fonts

    // region Private Methods
    private fun enqueue(command: () -> Unit) {
        dispatcher.enqueue({ command() }, Delay.NONE)
    }

    private fun shouldRefreshCustomerInfo(firstTimeInForeground: Boolean): Boolean {
        return !appConfig.customEntitlementComputation &&
            (firstTimeInForeground || deviceCache.isCustomerInfoCacheStale(appUserID, appInBackground = false))
    }

    private fun getProductsOfTypes(
        productIds: Set<String>,
        types: Set<ProductType>,
        collectedStoreProducts: List<StoreProduct>,
        startTime: Date? = null,
        callback: GetStoreProductsCallback,
    ) {
        val nonNullStartTime = startTime ?: run {
            trackGetProductsStarted(productIds)
            dateProvider.now
        }

        val typesRemaining = types.toMutableSet()
        val type = typesRemaining.firstOrNull()?.also { typesRemaining.remove(it) }

        type?.let {
            billing.queryProductDetailsAsync(
                productType = it,
                productIds = productIds,
                onReceive = { storeProducts ->
                    dispatch {
                        getProductsOfTypes(
                            productIds,
                            typesRemaining,
                            collectedStoreProducts + storeProducts,
                            nonNullStartTime,
                            callback,
                        )
                    }
                },
                onError = {
                    dispatch {
                        trackGetProductsResult(nonNullStartTime, productIds, productIds, it)
                        callback.onError(it)
                    }
                },
            )
        } ?: run {
            val notFoundProductIds = productIds - collectedStoreProducts.map { it.id }.toSet()
            trackGetProductsResult(nonNullStartTime, productIds, notFoundProductIds, null)
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
                callback = completion,
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
                val sdkOriginated: Boolean

                synchronized(this@PurchasesOrchestrator) {
                    sdkOriginated = purchases.all { purchase ->
                        purchase.productIds.any {
                            state.purchaseCallbacksByProductId.containsKey(it)
                        }
                    }
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
                    sdkOriginated = sdkOriginated,
                    transactionPostSuccess = callbackPair.first,
                    transactionPostError = callbackPair.second,
                )

                // Synchronize paywall events after a new purchase
                flushEvents(Delay.NONE)
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
            // This lets the backup manager know a change in data happened that would be good to backup.
            // In this case, we want to make sure that if there is a purchase, we schedule a backup.
            backupManager.dataChanged()
            blockstoreHelper.aliasCurrentAndStoredUserIdsIfNeeded {
                blockstoreHelper.storeUserIdIfNeeded(info)
                getPurchaseCallback(storeTransaction.productIds[0])?.let { purchaseCallback ->
                    dispatch {
                        purchaseCallback.onCompleted(storeTransaction, info)
                    }
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

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private fun startPurchase(
        activity: Activity,
        purchasingData: PurchasingData,
        presentedOfferingContext: PresentedOfferingContext?,
        isPersonalizedPrice: Boolean?,
        listener: PurchaseCallback,
    ) {
        log(LogIntent.PURCHASE) {
            PurchaseStrings.PURCHASE_STARTED.format(
                " $purchasingData ${
                    presentedOfferingContext?.offeringIdentifier?.let {
                        PurchaseStrings.OFFERING + "$it"
                    }
                }",
            )
        }

        if (
            purchasingData is GooglePurchasingData.Subscription &&
            (purchasingData.addOnProducts?.isNotEmpty() == true) &&
            this.store != Store.PLAY_STORE
        ) {
            val error = PurchasesError(
                code = PurchasesErrorCode.PurchaseInvalidError,
                underlyingErrorMessage = PurchaseStrings.PURCHASING_ADD_ONS_ONLY_SUPPORTED_ON_PLAY_STORE,
            ).also { errorLog(it) }
            listener.dispatch(error)
            return
        }

        trackPurchaseStarted(purchasingData.productId, purchasingData.productType)
        val startTime = dateProvider.now

        val listenerWithDiagnostics = createCallbackWithDiagnosticsIfNeeded(listener, purchasingData, startTime)

        var userPurchasing: String? = null // Avoids race condition for userid being modified before purchase is made
        synchronized(this@PurchasesOrchestrator) {
            if (!appConfig.finishTransactions) {
                log(LogIntent.WARNING) { PurchaseStrings.PURCHASE_FINISH_TRANSACTION_FALSE }
            }
            if (!state.purchaseCallbacksByProductId.containsKey(purchasingData.productId)) {
                val mapOfProductIdToListener = mapOf(purchasingData.productId to listenerWithDiagnostics)
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
                presentedOfferingContext,
                isPersonalizedPrice,
            )
        } ?: listenerWithDiagnostics.dispatch(
            PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError).also {
                errorLog(it)
            },
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun startProductChange(
        activity: Activity,
        purchasingData: PurchasingData,
        presentedOfferingContext: PresentedOfferingContext?,
        oldProductId: String,
        googleReplacementMode: GoogleReplacementMode,
        isPersonalizedPrice: Boolean?,
        purchaseCallback: PurchaseCallback,
    ) {
        trackPurchaseStarted(purchasingData.productId, purchasingData.productType)
        val startTime = dateProvider.now

        val callbackWithDiagnostics = createCallbackWithDiagnosticsIfNeeded(purchaseCallback, purchasingData, startTime)

        if (purchasingData.productType != ProductType.SUBS) {
            PurchasesError(
                PurchasesErrorCode.PurchaseNotAllowedError,
                PurchaseStrings.UPGRADING_INVALID_TYPE,
            ).also { errorLog(it) }.also { callbackWithDiagnostics.dispatch(it) }
            return
        }

        if (
            purchasingData is GooglePurchasingData.Subscription &&
            (purchasingData.addOnProducts?.isNotEmpty() == true) &&
            this.store != Store.PLAY_STORE
        ) {
            PurchasesError(
                code = PurchasesErrorCode.PurchaseInvalidError,
                underlyingErrorMessage = PurchaseStrings.PURCHASING_ADD_ONS_ONLY_SUPPORTED_ON_PLAY_STORE,
            ).also { errorLog(it) }.also { callbackWithDiagnostics.dispatch(it) }
            return
        }

        log(LogIntent.PURCHASE) {
            PurchaseStrings.PRODUCT_CHANGE_STARTED.format(
                " $purchasingData ${
                    presentedOfferingContext?.offeringIdentifier?.let {
                        PurchaseStrings.OFFERING + "$it"
                    }
                } oldProductId: $oldProductId googleReplacementMode $googleReplacementMode",
            )
        }
        var userPurchasing: String? = null // Avoids race condition for userid being modified before purchase is made
        synchronized(this@PurchasesOrchestrator) {
            if (!appConfig.finishTransactions) {
                log(LogIntent.WARNING) { PurchaseStrings.PURCHASE_FINISH_TRANSACTION_FALSE }
            }

            if (!state.purchaseCallbacksByProductId.containsKey(purchasingData.productId)) {
                // When using DEFERRED proration mode, callback needs to be associated with the *old* product we are
                // switching from, because the transaction we receive on successful purchase is for the old product.
                // We also need to normalize oldProductId by stripping any basePlanId suffix
                // (e.g., "productId:basePlanId" becomes "productId") to ensure the callback key matches the productId
                // in the transaction returned by Google Play, which only contains the product ID without the base plan.
                val productId = if (googleReplacementMode == GoogleReplacementMode.DEFERRED) {
                    if (oldProductId.contains(Constants.SUBS_ID_BASE_PLAN_ID_SEPARATOR)) {
                        warnLog {
                            PurchaseStrings.DEFERRED_PRODUCT_CHANGE_WITH_BASE_PLAN_ID.format(oldProductId)
                        }
                    }
                    oldProductId.substringBefore(Constants.SUBS_ID_BASE_PLAN_ID_SEPARATOR)
                } else {
                    purchasingData.productId
                }
                val mapOfProductIdToListener = mapOf(productId to callbackWithDiagnostics)
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
                presentedOfferingContext,
                isPersonalizedPrice,
                callbackWithDiagnostics,
            )
        } ?: run {
            val operationInProgressError = PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError).also {
                errorLog(it)
            }
            getAndClearAllPurchaseCallbacks().forEach { it.dispatch(operationInProgressError) }
        }
    }

    private fun replaceOldPurchaseWithNewProduct(
        purchasingData: PurchasingData,
        oldProductId: String,
        googleReplacementMode: GoogleReplacementMode?,
        activity: Activity,
        appUserID: String,
        presentedOfferingContext: PresentedOfferingContext?,
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
            warnLog {
                "Using incorrect oldProductId: $oldProductId. The productId should not contain the basePlanId. " +
                    "Using productId: $previousProductId."
            }
        }

        billing.findPurchaseInPurchaseHistory(
            appUserID,
            ProductType.SUBS,
            previousProductId,
            onCompletion = { purchaseRecord ->
                log(LogIntent.PURCHASE) { PurchaseStrings.FOUND_EXISTING_PURCHASE.format(previousProductId) }

                billing.makePurchaseAsync(
                    activity,
                    appUserID,
                    purchasingData,
                    ReplaceProductInfo(purchaseRecord, googleReplacementMode),
                    presentedOfferingContext,
                    isPersonalizedPrice,
                )
            },
            onError = { error ->
                log(LogIntent.GOOGLE_ERROR) { error.toString() }
                getAndClearProductChangeCallback()
                getAndClearAllPurchaseCallbacks()
                listener.dispatch(error)
            },
        )
    }

    private fun synchronizeSubscriberAttributesIfNeeded() {
        subscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserID)
    }

    private fun flushEvents(delay: Delay) {
        eventsManager.flushEvents(delay)
        adEventsManager.flushEvents(delay)
    }

    private fun createCallbackWithDiagnosticsIfNeeded(
        originalCallback: PurchaseCallback,
        purchasingData: PurchasingData,
        startTime: Date,
    ): PurchaseCallback {
        return if (diagnosticsTrackerIfEnabled == null) {
            originalCallback
        } else {
            object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                    trackPurchaseResultIfNeeded(
                        purchasingData,
                        error = null,
                        startTime,
                        customerInfo.entitlements.verification,
                    )
                    originalCallback.onCompleted(storeTransaction, customerInfo)
                }

                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    trackPurchaseResultIfNeeded(
                        purchasingData,
                        error,
                        startTime,
                        verificationResult = null,
                    )
                    originalCallback.onError(error, userCancelled)
                }
            }
        }
    }

    // region Diagnostics

    private fun trackGetProductsStarted(requestedProductIds: Set<String>) {
        diagnosticsTrackerIfEnabled?.trackGetProductsStarted(requestedProductIds)
    }

    private fun trackGetProductsResult(
        startTime: Date,
        requestedProductIds: Set<String>,
        notFoundProductIds: Set<String>,
        error: PurchasesError?,
    ) {
        if (diagnosticsTrackerIfEnabled == null) return
        val responseTime = Duration.between(startTime, dateProvider.now)
        diagnosticsTrackerIfEnabled.trackGetProductsResult(
            requestedProductIds = requestedProductIds,
            notFoundProductIds = notFoundProductIds,
            errorMessage = error?.message,
            errorCode = error?.code?.code,
            responseTime = responseTime,
        )
    }

    private fun trackPurchaseStarted(productId: String, productType: ProductType) {
        diagnosticsTrackerIfEnabled?.trackPurchaseStarted(productId, productType)
    }

    @Suppress("LongParameterList")
    private fun trackPurchaseResultIfNeeded(
        purchasingData: PurchasingData,
        error: PurchasesError?,
        startTime: Date,
        verificationResult: VerificationResult?,
    ) {
        if (diagnosticsTrackerIfEnabled == null) return
        val responseTime = Duration.between(startTime, dateProvider.now)
        diagnosticsTrackerIfEnabled.trackPurchaseResult(
            purchasingData.productId,
            purchasingData.productType,
            error?.code?.code,
            error?.message,
            responseTime,
            verificationResult,
        )
    }

    // endregion Diagnostics

    // endregion Private Methods

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

        private var cachedImageLoader: ImageLoader? = null

        const val frameworkVersion = Config.frameworkVersion

        var proxyURL: URL? = null

        @Suppress("MagicNumber")
        @Synchronized
        fun getImageLoader(context: Context): ImageLoader {
            val currentImageLoader = cachedImageLoader
            return if (currentImageLoader == null) {
                val maxCacheSizeBytes = 25 * 1024 * 1024L // 25 MB
                val cacheFolder = "revenuecatui_cache"
                val imageLoader = ImageLoader.Builder(context)
                    .diskCache {
                        DiskCache.Builder()
                            .directory(context.cacheDir.resolve(cacheFolder))
                            .maxSizeBytes(maxCacheSizeBytes)
                            .build()
                    }
                    .build()
                cachedImageLoader = imageLoader
                imageLoader
            } else {
                currentImageLoader
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
        fun canMakePayments(
            context: Context,
            features: List<BillingFeature> = listOf(),
            callback: Callback<Boolean>,
        ) {
            BillingClient.newBuilder(context)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .setListener { _, _ -> }
                .build()
                .let { billingClient ->
                    // BillingClient 4 calls the listener functions in a thread instead of in main
                    // https://github.com/RevenueCat/purchases-android/issues/348
                    val mainHandler = Handler(context.mainLooper)
                    val hasResponded = AtomicBoolean(false)
                    billingClient.startConnection(
                        object : BillingClientStateListener {
                            override fun onBillingSetupFinished(billingResult: BillingResult) {
                                mainHandler.post {
                                    if (hasResponded.getAndSet(true)) {
                                        log(LogIntent.GOOGLE_ERROR) {
                                            PurchaseStrings.EXTRA_CONNECTION_CANMAKEPAYMENTS.format(
                                                billingResult.responseCode,
                                            )
                                        }
                                        return@post
                                    }
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
                                        log(LogIntent.GOOGLE_ERROR) {
                                            PurchaseStrings.EXCEPTION_CANMAKEPAYMENTS.format(e.localizedMessage)
                                        }

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
                                        log(LogIntent.GOOGLE_ERROR) {
                                            PurchaseStrings.EXCEPTION_CANMAKEPAYMENTS.format(e.localizedMessage)
                                        }
                                    } finally {
                                        if (hasResponded.getAndSet(true)) {
                                            log(LogIntent.GOOGLE_ERROR) {
                                                PurchaseStrings.EXTRA_CALLBACK_CANMAKEPAYMENTS
                                            }
                                        } else {
                                            callback.onReceived(false)
                                        }
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
