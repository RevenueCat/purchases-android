package com.revenuecat.purchases.virtualcurrencies

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.GetVirtualCurrenciesCallback
import com.revenuecat.purchases.strings.CustomerInfoStrings
import com.revenuecat.purchases.strings.VirtualCurrencyStrings

@Suppress("UnusedPrivateProperty")
internal class VirtualCurrencyManager(
    private val identityManager: IdentityManager,
    private val deviceCache: DeviceCache,
    private val backend: Backend,
    private val appConfig: AppConfig,
) {

    // region Public functions

    @Synchronized
    fun virtualCurrencies(
        callback: GetVirtualCurrenciesCallback,
    ) {
        val appUserID = identityManager.currentAppUserID
        val isAppBackgrounded = appConfig.isAppBackgrounded

        val cachedVirtualCurrencies = fetchCachedVirtualCurrencies(
            appUserID = appUserID,
            isAppBackgrounded = isAppBackgrounded,
            allowStaleCache = false,
        )
        if (cachedVirtualCurrencies != null) {
            log(LogIntent.DEBUG) {
                VirtualCurrencyStrings.VENDING_FROM_CACHE
            }
            callback.onReceived(cachedVirtualCurrencies)
            return
        }

        log(LogIntent.DEBUG) {
            VirtualCurrencyStrings.VIRTUAL_CURRENCIES_STALE_UPDATING_FROM_NETWORK
        }
        fetchVirtualCurrenciesFromBackend(
            appUserID = appUserID,
            isAppBackgrounded = isAppBackgrounded,
            callback = fetchedVirtualCurrenciesFromBackendCallback(callback, appUserID),
        )
    }

    @Synchronized
    fun cachedVirtualCurrencies(): VirtualCurrencies? {
        val appUserID = identityManager.currentAppUserID
        val isAppBackgrounded = appConfig.isAppBackgrounded

        val cachedVirtualCurrencies: VirtualCurrencies? = fetchCachedVirtualCurrencies(
            appUserID = appUserID,
            isAppBackgrounded = isAppBackgrounded,
            allowStaleCache = true,
        )

        if (cachedVirtualCurrencies != null) {
            log(LogIntent.DEBUG) {
                VirtualCurrencyStrings.VENDING_FROM_CACHE
            }
            return cachedVirtualCurrencies
        } else {
            return null
        }
    }

    @Synchronized
    fun invalidateVirtualCurrenciesCache() {
        val appUserID = identityManager.currentAppUserID

        log(LogIntent.DEBUG) {
            VirtualCurrencyStrings.INVALIDATING_VIRTUAL_CURRENCIES_CACHE
        }
        deviceCache.clearVirtualCurrenciesCache(appUserID = appUserID)
    }

    private fun cacheVirtualCurrencies(
        virtualCurrencies: VirtualCurrencies,
        appUserID: String,
    ) {
        deviceCache.cacheVirtualCurrencies(
            appUserID = appUserID,
            virtualCurrencies = virtualCurrencies,
        )
    }

    private fun fetchCachedVirtualCurrencies(
        appUserID: String,
        isAppBackgrounded: Boolean,
        allowStaleCache: Boolean,
    ): VirtualCurrencies? {
        if (!allowStaleCache && deviceCache.isVirtualCurrenciesCacheStale(appUserID, isAppBackgrounded)) {
            return null
        }

        val cachedVirtualCurrencies: VirtualCurrencies? = deviceCache.getCachedVirtualCurrencies(appUserID = appUserID)
        if (cachedVirtualCurrencies != null) {
            return cachedVirtualCurrencies
        } else {
            log(LogIntent.DEBUG) {
                VirtualCurrencyStrings.NO_CACHED_VIRTUAL_CURRENCIES
            }
            return null
        }
    }

    private fun fetchVirtualCurrenciesFromBackend(
        appUserID: String,
        isAppBackgrounded: Boolean,
        callback: GetVirtualCurrenciesCallback,
    ) {
        backend.getVirtualCurrencies(
            appUserID = appUserID,
            appInBackground = isAppBackgrounded,
            onSuccess = { callback.onReceived(it) },
            onError = { callback.onError(it) },
        )
    }

    private fun fetchedVirtualCurrenciesFromBackendCallback(
        originalCallback: GetVirtualCurrenciesCallback,
        appUserID: String,
    ): GetVirtualCurrenciesCallback = object : GetVirtualCurrenciesCallback {
        override fun onReceived(virtualCurrencies: VirtualCurrencies) {
            log(LogIntent.RC_SUCCESS) {
                VirtualCurrencyStrings.VIRTUAL_CURRENCIES_UPDATED_FROM_NETWORK
            }

            cacheVirtualCurrencies(virtualCurrencies, appUserID)
            originalCallback.onReceived(virtualCurrencies)
        }
        override fun onError(error: PurchasesError) {
            log(LogIntent.RC_SUCCESS) {
                VirtualCurrencyStrings.VIRTUAL_CURRENCIES_UPDATED_FROM_NETWORK_ERROR.format(error)
            }
            originalCallback.onError(error)
        }
    }
}
