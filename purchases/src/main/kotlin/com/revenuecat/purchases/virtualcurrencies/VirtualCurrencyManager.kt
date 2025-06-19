package com.revenuecat.purchases.virtualcurrencies

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.GetVirtualCurrenciesCallback
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.VirtualCurrencyStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Date

internal class VirtualCurrencyManager(
    private val identityManager: IdentityManager,
    private val deviceCache: DeviceCache,
    private val backend: Backend,
    private val appConfig: AppConfig,
    private val dateProvider: DateProvider = DefaultDateProvider()
) {

    // region Public functions

    @Synchronized
    fun virtualCurrencies(
        callback: GetVirtualCurrenciesCallback
    ) {
        val appUserID = identityManager.currentAppUserID
        val isAppBackgrounded = appConfig.isAppBackgrounded

        val cachedVirtualCurrencies = fetchCachedVirtualCurrencies(
            appUserID = appUserID,
            isAppBackgrounded = isAppBackgrounded
        )
        if (cachedVirtualCurrencies != null) {
            callback.onReceived(cachedVirtualCurrencies)
            return
        }

        fetchVirtualCurrenciesFromBackend(
            appUserID = appUserID,
            isAppBackgrounded = isAppBackgrounded,
            callback = callback
        )
    }

    private fun cacheVirtualCurrencies(
        virtualCurrencies: VirtualCurrencies,
        appUserID: String
    ) {
//        try {
//            val virtualCurrenciesData = JSONObject().apply {
//                put("virtual_currencies", virtualCurrencies.rawData)
//            }.toString()
//            deviceCache.cacheVirtualCurrencies(virtualCurrenciesData, appUserID)
//        } catch (e: Exception) {
//            log(LogIntent.DEBUG, VirtualCurrencyStrings.ERROR_CACHING_VIRTUAL_CURRENCIES)
//        }
    }

    private fun fetchCachedVirtualCurrencies(
        appUserID: String,
        isAppBackgrounded: Boolean
    ): VirtualCurrencies? {
        // TODO: Implement device caching
        return null
//        if (deviceCache.isVirtualCurrenciesCacheStale(appUserID, isAppBackgrounded)) {
//            return null
//        }
//
//        val cachedVirtualCurrenciesData = deviceCache.cachedVirtualCurrenciesData(appUserID)
//            ?: return null
//
//        return try {
//            val json = JSONObject(cachedVirtualCurrenciesData)
//            VirtualCurrencies.fromJson(json)
//        } catch (e: Exception) {
//            null
//        }
    }

    private fun fetchVirtualCurrenciesFromBackend(
        appUserID: String,
        isAppBackgrounded: Boolean,
        callback: GetVirtualCurrenciesCallback
    ) {
        // TODO: Implement this
//        backend.getVirtualCurrencies(
//            appUserID = appUserID,
//            isAppBackgrounded = isAppBackgrounded,
//            onSuccess = { virtualCurrencies ->
//                cacheVirtualCurrencies(virtualCurrencies, appUserID)
//                callback.onReceived(virtualCurrencies)
//            },
//            onError = { error, _ ->
//                callback.onError(error)
//            }
//        )
    }
}