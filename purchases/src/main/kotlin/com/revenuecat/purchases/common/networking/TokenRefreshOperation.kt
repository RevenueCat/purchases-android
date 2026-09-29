package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.isSuccessful
import java.net.URL

/**
 * Performs the low-level `/auth/token` refresh call. Not routed through
 * [com.revenuecat.purchases.common.Dispatcher.AsyncCall] like [TokenLoginOperation] -- its only caller
 * (`HTTPClient`'s 401 handling) already runs on a background thread, so a plain blocking function is
 * enough.
 *
 * Only performs the HTTP call; feeding the result into [TokenManager.handleTokenRefreshResponse] is the
 * caller's job, not this operation's.
 */
internal object TokenRefreshOperation {

    private const val GRANT_TYPE_KEY = "grant_type"
    private const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token"
    private const val REFRESH_TOKEN_KEY = "refresh_token"
    private const val ACCESS_TOKEN_KEY = "access_token"
    private const val ID_TOKEN_KEY = "id_token"

    /**
     * Returns the refreshed [TokenManager.TokenSet] on success, or `null` for any failure (bad response,
     * malformed body, or any exception). Caught broadly on purpose: the caller has already registered a
     * refresh waiter in [TokenManager], and anything left to escape here would leave it stuck forever. No
     * error channel -- the caller just falls back to whatever triggered the refresh.
     */
    fun refresh(
        baseURL: URL,
        httpClient: HTTPClient,
        authenticationHeaders: Map<String, String>,
        request: TokenManager.TokenRefreshRequest,
        fallbackBaseURLs: List<URL> = emptyList(),
    ): TokenManager.TokenSet? {
        val body = mapOf(
            GRANT_TYPE_KEY to GRANT_TYPE_REFRESH_TOKEN,
            REFRESH_TOKEN_KEY to request.refreshToken,
        )
        return try {
            val result = httpClient.performRequest(
                baseURL,
                Endpoint.TokenRefresh,
                body,
                null,
                authenticationHeaders,
                fallbackBaseURLs = fallbackBaseURLs,
            )
            if (!result.isSuccessful()) {
                errorLog { "/auth/token refresh failed for ${request.appUserID}: HTTP ${result.responseCode}." }
                null
            } else {
                val accessToken = result.body.optString(ACCESS_TOKEN_KEY).takeIf { it.isNotEmpty() }
                val refreshToken = result.body.optString(REFRESH_TOKEN_KEY).takeIf { it.isNotEmpty() }
                val idToken = result.body.optString(ID_TOKEN_KEY).takeIf { it.isNotEmpty() }
                if (accessToken == null || refreshToken == null || idToken == null) {
                    errorLog {
                        "/auth/token response for ${request.appUserID} is missing one or more of " +
                            "access_token/refresh_token/id_token."
                    }
                    null
                } else {
                    TokenManager.TokenSet(accessToken, refreshToken, idToken)
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Broad on purpose -- see this function's doc; anything escaping must still resolve the pending refresh.
            errorLog(e) { "/auth/token refresh threw for ${request.appUserID}." }
            null
        }
    }
}
