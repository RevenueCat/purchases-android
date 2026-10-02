@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.errorLog
import org.json.JSONException
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * IAM authentication for [com.revenuecat.purchases.common.HTTPClient]: the current user's access token in
 * place of the API key, IAM paths, and refreshing tokens after a 401. Inert unless [tokenManager] is enabled.
 */
internal class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val currentAppUserID: () -> String,
) {

    /** Whether requests should use their IAM paths. */
    val usesIAMPaths: Boolean
        get() = tokenManager.enabled

    /** The current user's `Authorization` header for [endpoint], or empty to keep the caller's API key. */
    fun authorizationHeaders(endpoint: Endpoint): Map<String, String> =
        tokenManager.authorizationHeaders(currentAppUserID(), endpoint.isIAMEndpoint)

    /**
     * Whether a request to [endpoint] that got [responseCode] should be retried because the current user's
     * tokens were refreshed -- by posting [Endpoint.TokenRefresh] with [postTokenRefresh], or by waiting for a
     * refresh already in flight. Blocks the calling thread until then, so only call it from a background thread.
     */
    fun refreshTokensIfNeeded(
        endpoint: Endpoint,
        responseCode: Int,
        alreadyRetried: Boolean,
        postTokenRefresh: (body: Map<String, Any?>) -> HTTPResult,
    ): Boolean {
        if (responseCode != RCHTTPStatusCodes.UNAUTHORIZED || endpoint.isIAMEndpoint) return false
        val appUserID = currentAppUserID()

        val refreshedTokens = AtomicReference<TokenManager.TokenSet?>()
        val refreshCompleted = CountDownLatch(1)
        val action = tokenManager.tokenRefreshRequest(appUserID, responseCode, alreadyRetried) {
            refreshedTokens.set(it)
            refreshCompleted.countDown()
        }
        val completed = when (action) {
            TokenManager.TokenRefreshAction.NoAction -> false
            is TokenManager.TokenRefreshAction.Refresh -> {
                var tokens: TokenManager.TokenSet? = null
                try {
                    tokens = refresh(action.request, postTokenRefresh)
                } finally {
                    // Always resolve the refresh, or later 401s for this user would wait on it.
                    tokenManager.handleTokenRefreshResponse(appUserID, tokens)
                }
                true
            }
            TokenManager.TokenRefreshAction.WaitingForOtherRequest ->
                refreshCompleted.await(REFRESH_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }
        return completed && refreshedTokens.get() != null
    }

    private fun refresh(
        request: TokenManager.TokenRefreshRequest,
        postTokenRefresh: (body: Map<String, Any?>) -> HTTPResult,
    ): TokenManager.TokenSet? =
        try {
            TokenRefreshOperation.handleResponse(postTokenRefresh(TokenRefreshOperation.body(request.refreshToken)))
        } catch (e: IOException) {
            errorLog(e) { "IAM token refresh failed." }
            null
        } catch (e: JSONException) {
            errorLog(e) { "IAM token refresh failed." }
            null
        }

    private companion object {
        // How long to wait on another request's refresh; matches HTTPClient's default request timeout.
        const val REFRESH_WAIT_TIMEOUT_MS = 30_000L
    }
}
