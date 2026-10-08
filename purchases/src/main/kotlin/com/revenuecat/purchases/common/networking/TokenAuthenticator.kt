@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.errorLog
import org.json.JSONException
import java.io.IOException

/**
 * IAM authentication for [com.revenuecat.purchases.common.HTTPClient]: the current user's access token in
 * place of the API key, IAM paths, and refreshing tokens after a 401. Inert unless [tokenManager] is enabled.
 */
internal class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val currentAppUserID: () -> String,
) {

    data class RequestAuthentication(
        val appUserID: String,
        val sentAuthorizationHeaders: Map<String, String>,
    )

    /** Whether requests should use their IAM paths. */
    val usesIAMPaths: Boolean
        get() = tokenManager.enabled

    /** The user whose IAM credentials should stay attached to one logical request and all of its retries. */
    fun appUserIDForRequest(): String = currentAppUserID()

    /** The current user's `Authorization` header for [endpoint], or empty to keep the caller's API key. */
    fun authorizationHeaders(endpoint: Endpoint): Map<String, String> =
        authorizationHeaders(endpoint, appUserIDForRequest())

    /** [appUserID]'s `Authorization` header for [endpoint], or empty to keep the caller's API key. */
    fun authorizationHeaders(endpoint: Endpoint, appUserID: String): Map<String, String> =
        tokenManager.authorizationHeaders(appUserID, endpoint.isIAMEndpoint)

    // Serializes refreshes, so concurrent 401s for the same token cause one /auth/token call.
    private val refreshLock = Any()

    /**
     * Whether a request to [endpoint] that got [responseCode], sent with [sentAuthorizationHeaders], should be
     * retried with refreshed tokens. If another request refreshed while this one was in flight, it just
     * retries; otherwise it posts [Endpoint.TokenRefresh] with [postTokenRefresh]. Blocks while a refresh is in
     * progress, so only call it from a background thread.
     */
    fun refreshTokensIfNeeded(
        endpoint: Endpoint,
        responseCode: Int,
        alreadyRetried: Boolean,
        sentAuthorizationHeaders: Map<String, String>,
        postTokenRefresh: (body: Map<String, Any?>) -> HTTPResult,
    ): Boolean = refreshTokensIfNeeded(
        endpoint,
        responseCode,
        alreadyRetried,
        RequestAuthentication(appUserIDForRequest(), sentAuthorizationHeaders),
        postTokenRefresh,
    )

    /** As above, bound to the user whose credentials were used for the original request. */
    fun refreshTokensIfNeeded(
        endpoint: Endpoint,
        responseCode: Int,
        alreadyRetried: Boolean,
        requestAuthentication: RequestAuthentication,
        postTokenRefresh: (body: Map<String, Any?>) -> HTTPResult,
    ): Boolean {
        if (responseCode != RCHTTPStatusCodes.UNAUTHORIZED || endpoint.isIAMEndpoint || alreadyRetried) return false
        return synchronized(refreshLock) {
            val currentHeaders = tokenManager.authorizationHeaders(
                requestAuthentication.appUserID,
                isIAMEndpoint = false,
            )
            if (currentHeaders.isNotEmpty() && currentHeaders != requestAuthentication.sentAuthorizationHeaders) {
                true // Already refreshed by another request.
            } else {
                val tokens = tokenManager.currentRefreshToken(requestAuthentication.appUserID)
                    ?.let { refresh(it, postTokenRefresh) }
                tokens?.let {
                    tokenManager.saveTokens(
                        requestAuthentication.appUserID,
                        it.accessToken,
                        it.refreshToken,
                        it.idToken,
                    )
                } != null
            }
        }
    }

    private fun refresh(
        refreshToken: String,
        postTokenRefresh: (body: Map<String, Any?>) -> HTTPResult,
    ): TokenManager.TokenSet? =
        try {
            TokenRefreshOperation.handleResponse(postTokenRefresh(TokenRefreshOperation.body(refreshToken)))
        } catch (e: IOException) {
            errorLog(e) { "IAM token refresh failed." }
            null
        } catch (e: JSONException) {
            errorLog(e) { "IAM token refresh failed." }
            null
        }
}
