package com.revenuecat.purchases.virtualcurrencies

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
    fun virtualCurrencies(
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

        // TODO: Handle errors from network request
        val virtualCurrenciesFromNetwork = fetchVirtualCurrenciesFromBackend(
            appUserID = appUserID,
            isAppBackgrounded = isAppBackgrounded,
            callback = callback,
        )
        cacheVirtualCurrencies(
            virtualCurrencies = virtualCurrenciesFromNetwork,
            appUserID = appUserID,
        )
        callback.onReceived(virtualCurrenciesFromNetwork)
    }

    @Synchronized
    fun cachedVirtualCurrencies(): VirtualCurrencies? {
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

    @Suppress("UnusedParameter", "ForbiddenComment")
    private fun fetchVirtualCurrenciesFromBackend(
        appUserID: String,
        isAppBackgrounded: Boolean,
        callback: GetVirtualCurrenciesCallback,
    ): VirtualCurrencies {
        // TODO: Implement this
        return VirtualCurrencies(all = emptyMap())
    }
}
