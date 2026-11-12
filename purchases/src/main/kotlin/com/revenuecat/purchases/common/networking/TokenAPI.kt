@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.CallbackCacheKey
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.identity.Identity
import com.revenuecat.purchases.strings.NetworkStrings

/**
 * IAM login and logout against the `/auth` endpoints, mirroring iOS's TokenAPI. Calls run on [dispatcher];
 * identical concurrent calls share one request, the way [com.revenuecat.purchases.common.Backend] does.
 * Tokens are saved to, or cleared from, [tokenManager] before callers are notified.
 */
internal class TokenAPI(
    private val tokenManager: TokenManager,
    private val httpClient: HTTPClient,
    private val backendHelper: BackendHelper,
    private val dispatcher: Dispatcher,
    private val appConfig: AppConfig,
) {

    private val logInCallbacks = mutableMapOf<CallbackCacheKey, MutableList<LogInCallback>>()
    private val revokeCallbacks = mutableMapOf<CallbackCacheKey, MutableList<RevokeCallback>>()

    /**
     * Exchanges [identity] for tokens, linking it to [currentAppUserID]'s identity when that user has an ID
     * token. [onSuccess] receives the app user ID the new tokens belong to.
     */
    fun logIn(
        currentAppUserID: String,
        identity: Identity,
        onSuccess: (appUserID: String) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        val token = identity.authToken
        if (!tokenManager.enabled) {
            onError(PurchasesError(PurchasesErrorCode.ConfigurationError, "IAM login is not enabled."))
            return
        }
        if (!token.validate()) {
            onError(PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "Invalid identity token."))
            return
        }
        val body = TokenLoginOperation.body(token, linkToID = tokenManager.currentIDToken(currentAppUserID))
        val cacheKey = listOf(currentAppUserID, token.cacheIdentifier)
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult = post(Endpoint.TokenLogin, body)

            override fun onError(error: PurchasesError) {
                takeCallbacks(logInCallbacks, cacheKey).forEach { it.onError(error) }
            }

            override fun onCompletion(result: HTTPResult) {
                when (val loginResult = TokenLoginOperation.handleResponse(result)) {
                    is TokenLoginOperation.Result.Success -> {
                        val tokens = loginResult.tokens
                        tokenManager.saveTokens(
                            loginResult.appUserID,
                            tokens.accessToken,
                            tokens.refreshToken,
                            tokens.idToken,
                        )
                        takeCallbacks(logInCallbacks, cacheKey).forEach { it.onSuccess(loginResult.appUserID) }
                    }
                    // `this.` -- the enclosing function's onError parameter would shadow it.
                    is TokenLoginOperation.Result.Error -> this.onError(loginResult.error)
                }
            }
        }
        enqueue(logInCallbacks, cacheKey, call, LogInCallback(onSuccess, onError))
    }

    /**
     * Revokes [appUserID]'s refresh token, then clears its local tokens. With no refresh token there's
     * nothing to revoke server-side, so the tokens are cleared without a network call. On failure, the
     * tokens are left in place.
     */
    fun revokeTokens(
        appUserID: String,
        onSuccess: () -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        val refreshToken = tokenManager.currentRefreshToken(appUserID)
        if (refreshToken == null) {
            tokenManager.deleteTokens(appUserID)
            onSuccess()
            return
        }
        val cacheKey = listOf(appUserID)
        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult = post(Endpoint.TokenLogout, TokenLogoutOperation.body(refreshToken))

            override fun onError(error: PurchasesError) {
                takeCallbacks(revokeCallbacks, cacheKey).forEach { it.onError(error) }
            }

            override fun onCompletion(result: HTTPResult) {
                val error = TokenLogoutOperation.handleResponse(result)
                if (error != null) {
                    this.onError(error)
                } else {
                    tokenManager.deleteTokens(appUserID)
                    takeCallbacks(revokeCallbacks, cacheKey).forEach { it.onSuccess() }
                }
            }
        }
        enqueue(revokeCallbacks, cacheKey, call, RevokeCallback(onSuccess, onError))
    }

    private fun post(endpoint: Endpoint, body: Map<String, Any?>): HTTPResult =
        httpClient.performRequest(
            appConfig.baseURL,
            endpoint,
            body,
            postFieldsToSign = null,
            backendHelper.authenticationHeaders,
        )

    private fun <T> enqueue(
        callbacks: MutableMap<CallbackCacheKey, MutableList<T>>,
        cacheKey: CallbackCacheKey,
        call: Dispatcher.AsyncCall,
        callback: T,
    ) {
        synchronized(this) {
            callbacks[cacheKey]?.let {
                debugLog { NetworkStrings.SAME_CALL_ALREADY_IN_PROGRESS.format(cacheKey) }
                it.add(callback)
                return
            }
            callbacks[cacheKey] = mutableListOf(callback)
        }
        backendHelper.enqueue(call, dispatcher)
    }

    private fun <T> takeCallbacks(
        callbacks: MutableMap<CallbackCacheKey, MutableList<T>>,
        cacheKey: CallbackCacheKey,
    ): List<T> = synchronized(this) { callbacks.remove(cacheKey).orEmpty() }

    private class LogInCallback(val onSuccess: (String) -> Unit, val onError: (PurchasesError) -> Unit)
    private class RevokeCallback(val onSuccess: () -> Unit, val onError: (PurchasesError) -> Unit)
}
