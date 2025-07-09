package com.revenuecat.purchases.virtualcurrencies

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.GetVirtualCurrenciesCallback

@Suppress("UnusedPrivateProperty")
internal class VirtualCurrencyManager(
    private val identityManager: IdentityManager,
    private val deviceCache: DeviceCache,
    private val backend: Backend,
    private val appConfig: AppConfig,
) {

    // region Public functions

    @Suppress("ForbiddenComment")
    @Synchronized
    fun getVirtualCurrencies(
        callback: GetVirtualCurrenciesCallback,
    ) {
        // TODO: Add logging
        val appUserID = identityManager.currentAppUserID
        val isAppBackgrounded = appConfig.isAppBackgrounded

        val cachedVirtualCurrencies = fetchCachedVirtualCurrencies(
            appUserID = appUserID,
            isAppBackgrounded = isAppBackgrounded,
            allowStaleCache = false,
        )
        if (cachedVirtualCurrencies != null) {
            callback.onReceived(cachedVirtualCurrencies)
            return
        }

        fetchVirtualCurrenciesFromBackend(
            appUserID = appUserID,
            isAppBackgrounded = isAppBackgrounded,
            callback = fetchedVirtualCurrenciesFromBackendCallback(callback, appUserID),
        )
    }

    @Synchronized
    fun getCachedVirtualCurrencies(): VirtualCurrencies? {
        val appUserID = identityManager.currentAppUserID
        val isAppBackgrounded = appConfig.isAppBackgrounded

        return fetchCachedVirtualCurrencies(
            appUserID = appUserID,
            isAppBackgrounded = isAppBackgrounded,
            allowStaleCache = true,
        )
    }

    @Synchronized
    fun invalidateVirtualCurrenciesCache() {
        val appUserID = identityManager.currentAppUserID
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

        return deviceCache.getCachedVirtualCurrencies(appUserID = appUserID)
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
            cacheVirtualCurrencies(virtualCurrencies, appUserID)
            originalCallback.onReceived(virtualCurrencies)
        }
        override fun onError(error: PurchasesError) {
            originalCallback.onError(error)
        }
    }
}
