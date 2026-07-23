package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.strings.OfflineEntitlementsStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@OptIn(InternalRevenueCatAPI::class)
@Suppress("LongParameterList")
internal class OfflineEntitlementsManager(
    private val backend: Backend,
    private val offlineCustomerInfoCalculator: OfflineCustomerInfoCalculator,
    private val deviceCache: DeviceCache,
    private val appConfig: AppConfig,
    private val diagnosticsTracker: DiagnosticsTracker?,
    private val productEntitlementMappingTopicProvider: EntitlementMappingTopicProvider? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    // We cache the offline customer info in memory, so it's not persisted.
    val offlineCustomerInfo: CustomerInfo?
        get() = _offlineCustomerInfo

    @get:Synchronized @set:Synchronized
    private var _offlineCustomerInfo: CustomerInfo? = null

    private val offlineCustomerInfoCallbackCache = mutableMapOf<String, List<OfflineCustomerInfoCallback>>()

    private val productEntitlementMappingUpdateLock = Any()
    private var productEntitlementMappingUpdateJob: Job? = null
    private val productEntitlementMappingUpdateCompletions = mutableListOf<(PurchasesError?) -> Unit>()

    @Synchronized
    fun resetOfflineCustomerInfoCache() {
        if (_offlineCustomerInfo != null) {
            debugLog { OfflineEntitlementsStrings.RESETTING_OFFLINE_CUSTOMER_INFO_CACHE }
            _offlineCustomerInfo = null
        }
    }

    fun shouldCalculateOfflineCustomerInfoInGetCustomerInfoRequest(
        isServerError: Boolean,
        appUserId: String,
    ) = isServerError &&
        isOfflineEntitlementsEnabled() &&
        deviceCache.getCachedCustomerInfo(appUserId) == null

    fun shouldCalculateOfflineCustomerInfoInPostReceipt(
        isServerError: Boolean,
    ) = isServerError && isOfflineEntitlementsEnabled()

    fun calculateAndCacheOfflineCustomerInfo(
        appUserId: String,
        onSuccess: (CustomerInfo) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        if (!appConfig.enableOfflineEntitlements) {
            onError(
                PurchasesError(
                    PurchasesErrorCode.UnsupportedError,
                    OfflineEntitlementsStrings.OFFLINE_ENTITLEMENTS_NOT_ENABLED,
                ),
            )
            return
        }
        synchronized(this@OfflineEntitlementsManager) {
            val alreadyProcessing = offlineCustomerInfoCallbackCache.containsKey(appUserId)
            val callbacks = offlineCustomerInfoCallbackCache[appUserId] ?: emptyList()
            offlineCustomerInfoCallbackCache[appUserId] = callbacks + listOf(onSuccess to onError)
            if (alreadyProcessing) {
                debugLog { OfflineEntitlementsStrings.ALREADY_CALCULATING_OFFLINE_CUSTOMER_INFO.format(appUserId) }
                return
            }
        }
        offlineCustomerInfoCalculator.computeOfflineCustomerInfo(
            appUserId,
            onSuccess = { customerInfo ->
                synchronized(this@OfflineEntitlementsManager) {
                    warnLog { OfflineEntitlementsStrings.USING_OFFLINE_ENTITLEMENTS_CUSTOMER_INFO }
                    diagnosticsTracker?.trackEnteredOfflineEntitlementsMode()
                    _offlineCustomerInfo = customerInfo
                    deviceCache.getCachedAppUserID()?.let { deviceCache.clearCustomerInfoCache(it) }
                    val callbacks = offlineCustomerInfoCallbackCache.remove(appUserId)
                    callbacks?.forEach { (onSuccess, _) ->
                        onSuccess(customerInfo)
                    }
                }
            },
            onError = {
                synchronized(this@OfflineEntitlementsManager) {
                    val callbacks = offlineCustomerInfoCallbackCache.remove(appUserId)
                    callbacks?.forEach { (_, onError) ->
                        onError(it)
                    }
                }
            },
        )
    }

    fun updateProductEntitlementMappingCacheIfStale(completion: ((PurchasesError?) -> Unit)? = null) {
        if (isOfflineEntitlementsEnabled() && deviceCache.isProductEntitlementMappingCacheStale()) {
            debugLog { OfflineEntitlementsStrings.UPDATING_PRODUCT_ENTITLEMENT_MAPPING }
            val topicProvider = productEntitlementMappingTopicProvider
            if (topicProvider == null) {
                fetchLegacyProductEntitlementMapping(completion)
                return
            }

            val updateJob = synchronized(productEntitlementMappingUpdateLock) {
                completion?.let(productEntitlementMappingUpdateCompletions::add)
                if (productEntitlementMappingUpdateJob != null) {
                    null
                } else {
                    lateinit var newUpdateJob: Job
                    newUpdateJob = scope.launch(start = CoroutineStart.LAZY) {
                        var waitingForLegacyResult = false
                        try {
                            val result = topicProvider.getProductEntitlementMapping()
                            if (result != null &&
                                result.cacheIfCurrent(deviceCache::cacheProductEntitlementMapping)
                            ) {
                                debugLog { OfflineEntitlementsStrings.SUCCESSFULLY_UPDATED_PRODUCT_ENTITLEMENTS }
                                finishProductEntitlementMappingUpdate(newUpdateJob, null)
                            } else {
                                waitingForLegacyResult = true
                                fetchLegacyProductEntitlementMapping {
                                    finishProductEntitlementMappingUpdate(newUpdateJob, it)
                                }
                            }
                        } finally {
                            if (!waitingForLegacyResult) {
                                finishProductEntitlementMappingUpdate(newUpdateJob, null, notifyCompletions = false)
                            }
                        }
                    }
                    productEntitlementMappingUpdateJob = newUpdateJob
                    newUpdateJob
                }
            }
            updateJob?.start()
        } else {
            completion?.invoke(null)
        }
    }

    fun close() {
        synchronized(productEntitlementMappingUpdateLock) {
            productEntitlementMappingUpdateJob = null
            productEntitlementMappingUpdateCompletions.clear()
        }
        scope.cancel()
    }

    private fun finishProductEntitlementMappingUpdate(
        updateJob: Job,
        error: PurchasesError?,
        notifyCompletions: Boolean = true,
    ) {
        val completions = synchronized(productEntitlementMappingUpdateLock) {
            if (productEntitlementMappingUpdateJob !== updateJob) {
                return@synchronized emptyList()
            }
            productEntitlementMappingUpdateJob = null
            productEntitlementMappingUpdateCompletions
                .takeIf { notifyCompletions }
                .orEmpty()
                .toList()
                .also {
                    productEntitlementMappingUpdateCompletions.clear()
                }
        }
        completions.forEach { it(error) }
    }

    private fun fetchLegacyProductEntitlementMapping(completion: ((PurchasesError?) -> Unit)?) {
        backend.getProductEntitlementMapping(
            onSuccessHandler = { productEntitlementMapping ->
                cacheProductEntitlementMapping(productEntitlementMapping, completion)
            },
            onErrorHandler = { e ->
                errorLog { OfflineEntitlementsStrings.ERROR_UPDATING_PRODUCT_ENTITLEMENTS.format(e) }
                completion?.invoke(e)
            },
        )
    }

    private fun cacheProductEntitlementMapping(
        productEntitlementMapping: ProductEntitlementMapping,
        completion: ((PurchasesError?) -> Unit)?,
    ) {
        deviceCache.cacheProductEntitlementMapping(productEntitlementMapping)
        debugLog { OfflineEntitlementsStrings.SUCCESSFULLY_UPDATED_PRODUCT_ENTITLEMENTS }
        completion?.invoke(null)
    }

    // We disable offline entitlements in observer mode (finishTransactions = false) since it doesn't
    // provide any value and simplifies operations in that mode. Also on test store, since we don't have a store
    // to store purchases in the client.
    private fun isOfflineEntitlementsEnabled() = appConfig.finishTransactions &&
        appConfig.enableOfflineEntitlements &&
        !appConfig.customEntitlementComputation &&
        appConfig.store != Store.TEST_STORE
}

private typealias OfflineCustomerInfoCallback = Pair<(CustomerInfo) -> Unit, (PurchasesError) -> Unit>
