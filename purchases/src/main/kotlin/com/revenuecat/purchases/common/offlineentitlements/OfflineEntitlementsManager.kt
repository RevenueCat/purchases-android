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
        if (!isOfflineEntitlementsEnabled() || !deviceCache.isProductEntitlementMappingCacheStale()) {
            completion?.invoke(null)
            return
        }

        debugLog { OfflineEntitlementsStrings.UPDATING_PRODUCT_ENTITLEMENT_MAPPING }
        val topicProvider = productEntitlementMappingTopicProvider
            ?: return fetchLegacyProductEntitlementMapping(completion)

        startProductEntitlementMappingUpdate(topicProvider, completion)
    }

    fun close() {
        synchronized(productEntitlementMappingUpdateLock) {
            productEntitlementMappingUpdateJob = null
        }
        scope.cancel()
    }

    private fun startProductEntitlementMappingUpdate(
        topicProvider: EntitlementMappingTopicProvider,
        completion: ((PurchasesError?) -> Unit)?,
    ) {
        fun finishUpdate(updateJob: Job): Boolean =
            synchronized(productEntitlementMappingUpdateLock) {
                if (productEntitlementMappingUpdateJob !== updateJob) {
                    false
                } else {
                    productEntitlementMappingUpdateJob = null
                    true
                }
            }

        val completeUpdate: (Job, PurchasesError?) -> Unit = { updateJob, error ->
            if (finishUpdate(updateJob)) {
                completion?.invoke(error)
            }
        }
        val updateJob = synchronized(productEntitlementMappingUpdateLock) {
            if (productEntitlementMappingUpdateJob != null) {
                return
            }

            scope.launch(start = CoroutineStart.LAZY) {
                val currentJob = coroutineContext[Job] ?: return@launch
                var waitingForLegacyResult = false
                try {
                    val result = topicProvider.getProductEntitlementMapping()
                    if (result?.cacheIfCurrent(deviceCache::cacheProductEntitlementMapping) == true) {
                        debugLog { OfflineEntitlementsStrings.SUCCESSFULLY_UPDATED_PRODUCT_ENTITLEMENTS }
                        completeUpdate(currentJob, null)
                    } else {
                        waitingForLegacyResult = true
                        fetchLegacyProductEntitlementMapping { completeUpdate(currentJob, it) }
                    }
                } finally {
                    if (!waitingForLegacyResult) {
                        finishUpdate(currentJob)
                    }
                }
            }.also {
                productEntitlementMappingUpdateJob = it
            }
        }
        updateJob.start()
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
