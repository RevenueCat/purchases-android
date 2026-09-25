package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.isSuccessful
import java.net.URL

/**
 * Performs the low-level `/auth/token` refresh call.
 *
 * Deliberately **not** routed through [com.revenuecat.purchases.common.Dispatcher.AsyncCall] the way
 * [TokenLoginOperation]/`TokenLogoutOperation` are: its only caller is `HTTPClient`'s own 401 handling
 * (step 22), which already runs synchronously on a background thread (inside another
 * [com.revenuecat.purchases.common.Dispatcher.AsyncCall]'s `call()`, or -- once step 22 lands -- inline in
 * `HTTPClient.performRequest` itself), not inside a coroutine and not as a fresh dispatched unit of work of
 * its own. A small directly-callable, blocking function is what that caller actually needs.
 *
 * This function only performs the HTTP call; it does not touch [TokenManager]'s refresh state machine
 * ([TokenManager.tokenRefreshRequest]/[TokenManager.handleTokenRefreshResponse]) at all -- step 22's own
 * description assigns feeding this function's result into `handleTokenRefreshResponse` to the caller, not
 * to this operation, so a caller can decide *when* (e.g. only once, after this returns) rather than this
 * function reaching into `TokenManager` on its own.
 */
internal object TokenRefreshOperation {

    private const val GRANT_TYPE_KEY = "grant_type"
    private const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token"
    private const val REFRESH_TOKEN_KEY = "refresh_token"
    private const val ACCESS_TOKEN_KEY = "access_token"
    private const val ID_TOKEN_KEY = "id_token"

    /**
     * Returns the refreshed [TokenManager.TokenSet] on success, or `null` for any failure -- an
     * unsuccessful HTTP response, a malformed response body, or any exception [HTTPClient.performRequest]
     * throws, expected ([org.json.JSONException], [java.io.IOException]) or not. This is caught broadly,
     * on purpose: the caller (step 22) has already registered a refresh waiter in [TokenManager] before
     * calling this, and only clears it -- via [TokenManager.handleTokenRefreshResponseSync] -- once this
     * returns one way or the other; anything this function lets escape instead leaves that waiter (and
     * every later 401 for the same [TokenManager.TokenRefreshRequest.appUserID], which piles onto the same
     * pending refresh rather than starting a new one) stuck timing out on the caller's own wait, forever,
     * since nothing would ever resolve it. There's no caller-facing
     * [com.revenuecat.purchases.PurchasesError] channel here: a refresh either silently succeeds, or the
     * caller falls back to whatever triggered it (the original 401).
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
            // Broad on purpose -- see this function's own doc for why letting anything escape here is
            // worse than a stray catch-all: JSONException/IOException are the expected failure modes, but
            // anything else (e.g. SecurityException, SignatureVerificationException) must still resolve
            // TokenManager's pending refresh rather than leak it.
            errorLog(e) { "/auth/token refresh threw for ${request.appUserID}." }
            null
        }
    }
}
