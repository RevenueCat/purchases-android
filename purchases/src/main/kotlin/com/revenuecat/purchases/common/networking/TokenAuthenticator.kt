@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.isSuccessful
import com.revenuecat.purchases.common.toPurchasesError
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
                // No refresh token: nothing to refresh with, and nothing to report (iOS does the same).
                val refreshToken = tokenManager.currentRefreshToken(requestAuthentication.appUserID)
                when (val tokens = refreshToken?.let { refresh(it, postTokenRefresh) }) {
                    null -> false
                    is RefreshResult.Failure -> {
                        tokenManager.reportRefreshFailure(tokens.error)
                        false
                    }
                    is RefreshResult.Success -> {
                        val set = tokens.tokens
                        val appUserID = requestAuthentication.appUserID
                        tokenManager.saveTokens(appUserID, set.accessToken, set.refreshToken, set.idToken)
                        true
                    }
                }
            }
        }
    }

    private sealed interface RefreshResult {
        class Success(val tokens: TokenManager.TokenSet) : RefreshResult
        class Failure(val error: PurchasesError) : RefreshResult
    }

    private fun refresh(
        refreshToken: String,
        postTokenRefresh: (body: Map<String, Any?>) -> HTTPResult,
    ): RefreshResult =
        try {
            val result = postTokenRefresh(TokenRefreshOperation.body(refreshToken))
            TokenRefreshOperation.handleResponse(result)?.let { RefreshResult.Success(it) }
                ?: RefreshResult.Failure(
                    if (result.isSuccessful()) {
                        PurchasesError(PurchasesErrorCode.UnexpectedBackendResponseError, "No access token in refresh")
                    } else {
                        result.toPurchasesError()
                    },
                )
        } catch (e: IOException) {
            errorLog(e) { "IAM token refresh failed." }
            RefreshResult.Failure(e.toPurchasesError())
        } catch (e: JSONException) {
            errorLog(e) { "IAM token refresh failed." }
            RefreshResult.Failure(e.toPurchasesError())
        }
}
