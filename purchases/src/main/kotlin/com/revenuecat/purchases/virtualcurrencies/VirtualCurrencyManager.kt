package com.revenuecat.purchases.virtualcurrencies

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.GetVirtualCurrenciesCallback
import com.revenuecat.purchases.strings.VirtualCurrencyStrings

@OptIn(InternalRevenueCatAPI::class)
internal class VirtualCurrencyManager(
    private val identityManager: IdentityManager,
    private val deviceCache: DeviceCache,
    private val backend: Backend,
    private val appConfig: AppConfig,
    private val spendQueue: PerUserSerialQueue = PerUserSerialQueue(),
) {
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
            callback = handleVirtualCurrenciesRequestResult(callback, appUserID),
        )
    }

    // Mirrors iOS: invalid entries are dropped, and a spend with nothing left returns the current balances.
    fun spendVirtualCurrencies(
        amounts: Map<String, Int>,
        reference: String?,
        callback: GetVirtualCurrenciesCallback,
    ) {
        val resolvedAmounts = amounts.filter { (code, amount) -> isValidSpend(code, amount) }
        val emptyOrTooManyMessage = when {
            resolvedAmounts.isEmpty() -> VirtualCurrencyStrings.EMPTY_SPEND_AMOUNT
            resolvedAmounts.size >= MAX_SPEND_CODES -> VirtualCurrencyStrings.TOO_MANY_CODES
            else -> null
        }
        if (emptyOrTooManyMessage != null) {
            log(LogIntent.WARNING) { emptyOrTooManyMessage }
            virtualCurrencies(callback)
            return
        }

        val appUserID = identityManager.currentAppUserID
        // One spend in flight per user. With a multithreaded executor, responses could otherwise arrive out of
        // order, and an older balance would overwrite a newer one in the cache.
        spendQueue.enqueue(appUserID) { done ->
            backend.spendVirtualCurrencies(
                appUserID = appUserID,
                amounts = resolvedAmounts,
                reference = reference,
                onSuccessHandler = { virtualCurrencies ->
                    log(LogIntent.RC_SUCCESS) { VirtualCurrencyStrings.VIRTUAL_CURRENCIES_SPENT_ON_NETWORK }
                    cacheVirtualCurrencies(virtualCurrencies, appUserID)
                    done()
                    callback.onReceived(virtualCurrencies)
                },
                onErrorHandler = { error ->
                    log(LogIntent.RC_ERROR) {
                        VirtualCurrencyStrings.VIRTUAL_CURRENCIES_SPENT_ON_NETWORK_ERROR.format(error)
                    }
                    done()
                    callback.onError(error)
                },
            )
        }
    }

    private fun isValidSpend(code: String, amount: Int): Boolean = when {
        // The server caps codes far below this; it only rejects obvious garbage.
        code.length > MAX_CODE_LENGTH -> {
            log(LogIntent.WARNING) { VirtualCurrencyStrings.INVALID_CODE.format(code) }
            false
        }
        amount <= 0 -> {
            log(LogIntent.WARNING) { VirtualCurrencyStrings.INVALID_SPEND_AMOUNT.format(amount, code) }
            false
        }
        else -> true
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
        }
        return cachedVirtualCurrencies
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
        if (cachedVirtualCurrencies == null) {
            log(LogIntent.DEBUG) {
                VirtualCurrencyStrings.NO_CACHED_VIRTUAL_CURRENCIES
            }
        }

        return cachedVirtualCurrencies
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

    private fun handleVirtualCurrenciesRequestResult(
        completion: GetVirtualCurrenciesCallback,
        appUserID: String,
    ): GetVirtualCurrenciesCallback = object : GetVirtualCurrenciesCallback {
        override fun onReceived(virtualCurrencies: VirtualCurrencies) {
            log(LogIntent.RC_SUCCESS) {
                VirtualCurrencyStrings.VIRTUAL_CURRENCIES_UPDATED_FROM_NETWORK
            }

            cacheVirtualCurrencies(virtualCurrencies, appUserID)
            completion.onReceived(virtualCurrencies)
        }
        override fun onError(error: PurchasesError) {
            log(LogIntent.RC_ERROR) {
                VirtualCurrencyStrings.VIRTUAL_CURRENCIES_UPDATED_FROM_NETWORK_ERROR.format(error)
            }
            completion.onError(error)
        }
    }

    private companion object {
        const val MAX_CODE_LENGTH = 100
        const val MAX_SPEND_CODES = 1000
    }
}
