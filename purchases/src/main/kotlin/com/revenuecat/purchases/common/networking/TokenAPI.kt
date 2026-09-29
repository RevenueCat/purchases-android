@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.identity.Identity

/** @suppress */
internal typealias TokenLoginCallback = Pair<(TokenLoginOperation.Result) -> Unit, (PurchasesError) -> Unit>

/** @suppress */
internal typealias TokenLogoutCallback = Pair<() -> Unit, (PurchasesError) -> Unit>

/**
 * The IAM login façade: combines [TokenManager] with the raw `/auth` operations into [logIn] and
 * [revokeOrLogout]. Concurrent identical calls are deduped the same way `Backend.kt` dedupes its own (a
 * cache-key map guarded by `synchronized`).
 */
internal class TokenAPI(
    private val appConfig: AppConfig,
    private val dispatcher: Dispatcher,
    private val httpClient: HTTPClient,
    private val backendHelper: BackendHelper,
    private val tokenManager: TokenManager,
) {

    // Keyed by (identity credential, linkToID); cacheIdentifier already mixes in the identity source.
    private val loginCallbacks = mutableMapOf<List<String>, MutableList<TokenLoginCallback>>()

    // Keyed by appUserID.
    private val logoutCallbacks = mutableMapOf<List<String>, MutableList<TokenLogoutCallback>>()

    /** Logs in with [identity], saving tokens via [TokenManager.saveTokensSync], then notifies every deduped caller. */
    fun logIn(
        identity: Identity,
        linkToID: String?,
        onSuccessHandler: (TokenLoginOperation.Result) -> Unit,
        onErrorHandler: (PurchasesError) -> Unit,
    ) {
        val cacheKey = listOf(identity.authToken.cacheIdentifier, linkToID.orEmpty())
        synchronized(this) {
            val existing = loginCallbacks[cacheKey]
            if (existing != null) {
                existing.add(onSuccessHandler to onErrorHandler)
                return
            }
            loginCallbacks[cacheKey] = mutableListOf(onSuccessHandler to onErrorHandler)
        }

        TokenLoginOperation.logIn(
            appConfig.baseURL,
            httpClient,
            dispatcher,
            backendHelper.authenticationHeaders,
            identity,
            linkToID,
            appConfig.fallbackBaseURLs,
            onSuccessHandler = { result ->
                tokenManager.saveTokensSync(
                    result.appUserID,
                    result.tokens.accessToken,
                    result.tokens.refreshToken,
                    result.tokens.idToken,
                ) {
                    synchronized(this) { loginCallbacks.remove(cacheKey) }.orEmpty().forEach { (onSuccess, _) ->
                        onSuccess(result)
                    }
                }
            },
            onErrorHandler = { error ->
                synchronized(this) { loginCallbacks.remove(cacheKey) }.orEmpty().forEach { (_, onError) ->
                    onError(error)
                }
            },
        )
    }

    /** Revokes (or clears, if no refresh token) [appUserID]'s tokens, notifying every deduped caller. */
    fun revokeOrLogout(
        appUserID: String,
        onSuccessHandler: () -> Unit,
        onErrorHandler: (PurchasesError) -> Unit,
    ) {
        val cacheKey = listOf(appUserID)
        synchronized(this) {
            val existing = logoutCallbacks[cacheKey]
            if (existing != null) {
                existing.add(onSuccessHandler to onErrorHandler)
                return
            }
            logoutCallbacks[cacheKey] = mutableListOf(onSuccessHandler to onErrorHandler)
        }

        TokenLogoutOperation.logout(
            appConfig.baseURL,
            httpClient,
            dispatcher,
            backendHelper.authenticationHeaders,
            tokenManager,
            appUserID,
            appConfig.fallbackBaseURLs,
            onSuccessHandler = {
                synchronized(this) { logoutCallbacks.remove(cacheKey) }.orEmpty().forEach { (onSuccess, _) ->
                    onSuccess()
                }
            },
            onErrorHandler = { error ->
                synchronized(this) { logoutCallbacks.remove(cacheKey) }.orEmpty().forEach { (_, onError) ->
                    onError(error)
                }
            },
        )
    }
}
