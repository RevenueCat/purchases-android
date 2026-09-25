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
 * The IAM login façade: combines [TokenManager] with the raw `/auth`-family operations (steps 16-18) into the
 * two calls the rest of the SDK actually needs -- [logIn] and [revokeOrLogout].
 *
 * Concurrent identical calls are deduped the same way `Backend.kt` dedupes its own (a cache-key-keyed map
 * guarded by `synchronized(this)`, mirroring `Backend.identifyCallbacks`/`addCallback`) -- only the first
 * caller for a given key actually triggers the underlying operation; every caller folded into it gets
 * notified once that call resolves.
 */
internal class TokenAPI(
    private val appConfig: AppConfig,
    private val dispatcher: Dispatcher,
    private val httpClient: HTTPClient,
    private val backendHelper: BackendHelper,
    private val tokenManager: TokenManager,
) {

    // Keyed by (identity credential, linkToID) -- IdentityAuthToken.cacheIdentifier already mixes the
    // identity source into its hash, so two different providers' tokens never collide here.
    private val loginCallbacks = mutableMapOf<List<String>, MutableList<TokenLoginCallback>>()

    // Keyed by appUserID.
    private val logoutCallbacks = mutableMapOf<List<String>, MutableList<TokenLogoutCallback>>()

    /**
     * Logs in with [identity], persisting the returned tokens via [TokenManager.saveTokensSync] on success
     * before notifying every caller deduped into this call. See [TokenLoginRequestBody.from] for why
     * [linkToID] is taken as an already-resolved value rather than computed here.
     */
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
                )
                synchronized(this) { loginCallbacks.remove(cacheKey) }.orEmpty().forEach { (onSuccess, _) ->
                    onSuccess(result)
                }
            },
            onErrorHandler = { error ->
                synchronized(this) { loginCallbacks.remove(cacheKey) }.orEmpty().forEach { (_, onError) ->
                    onError(error)
                }
            },
        )
    }

    /**
     * Revokes (or, with no refresh token stored, simply clears) [appUserID]'s tokens, notifying every
     * caller deduped into this call once it resolves.
     */
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
