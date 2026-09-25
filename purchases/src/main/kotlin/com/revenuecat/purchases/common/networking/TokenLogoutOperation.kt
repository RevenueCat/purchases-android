package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.isSuccessful
import com.revenuecat.purchases.common.toPurchasesError
import java.net.URL

/**
 * Performs the low-level `/auth/revoke` call for [appUserID], via the existing [Dispatcher.AsyncCall]
 * pattern (see `Backend.logIn`).
 *
 * There is no bare access-token revoke -- the backend only supports revoking a refresh token (see
 * [TOKEN_TYPE_HINT_REFRESH_TOKEN]), so when [TokenManager.currentRefreshTokenSync] has nothing stored for
 * [appUserID] this makes no network call at all and just clears the local tokens directly, matching the
 * plan's "no refresh token → skip the call, clear local tokens directly."
 *
 * The whole body -- including that initial [TokenManager.currentRefreshTokenSync] check, which can block on
 * encrypted storage the same as any other synchronous twin -- runs inside a single [Dispatcher] job, never on
 * the caller's own thread. [TokenLoginOperation]/[TokenRefreshOperation] get that for free from
 * `Dispatcher.AsyncCall`'s own `call()`; a revoke's fast path (no refresh token, so no network call at all)
 * has nothing to hang that check on unless it's wrapped explicitly like this.
 *
 * A successful revoke only clears the tokens it actually revoked: [TokenManager.currentRefreshTokenSync] is
 * re-checked against the refresh token this call sent, and the local tokens are left alone if they've since
 * changed -- e.g. a login or another refresh that completed while this revoke was in flight -- rather than
 * deleting whatever a newer operation has since stored.
 */
internal object TokenLogoutOperation {

    private const val TOKEN_KEY = "token"
    private const val TOKEN_TYPE_HINT_KEY = "token_type_hint"
    private const val TOKEN_TYPE_HINT_REFRESH_TOKEN = "refresh_token"

    @Suppress("LongParameterList")
    fun logout(
        baseURL: URL,
        httpClient: HTTPClient,
        dispatcher: Dispatcher,
        authenticationHeaders: Map<String, String>,
        tokenManager: TokenManager,
        appUserID: String,
        fallbackBaseURLs: List<URL> = emptyList(),
        onSuccessHandler: () -> Unit,
        onErrorHandler: (PurchasesError) -> Unit,
    ) {
        dispatcher.enqueue(
            Runnable {
                val refreshToken = tokenManager.currentRefreshTokenSync(appUserID)
                if (refreshToken == null) {
                    tokenManager.deleteTokensSync(appUserID)
                    onSuccessHandler()
                    return@Runnable
                }

                val body = mapOf(TOKEN_KEY to refreshToken, TOKEN_TYPE_HINT_KEY to TOKEN_TYPE_HINT_REFRESH_TOKEN)
                val call = object : Dispatcher.AsyncCall() {
                    override fun call(): HTTPResult {
                        return httpClient.performRequest(
                            baseURL,
                            Endpoint.TokenLogout,
                            body,
                            null,
                            authenticationHeaders,
                            fallbackBaseURLs = fallbackBaseURLs,
                        )
                    }

                    override fun onError(error: PurchasesError) {
                        onErrorHandler(error)
                    }

                    override fun onCompletion(result: HTTPResult) {
                        if (result.isSuccessful()) {
                            if (tokenManager.currentRefreshTokenSync(appUserID) == refreshToken) {
                                tokenManager.deleteTokensSync(appUserID)
                            }
                            onSuccessHandler()
                        } else {
                            // Deliberately does NOT clear local tokens on failure -- a revoke the backend
                            // rejected (or never received) leaves the session as valid as it was before this
                            // call, matching the plan's "failure leaves tokens intact and propagates the error."
                            onErrorHandler(result.toPurchasesError().also { errorLog(it) })
                        }
                    }
                }
                call.run()
            },
        )
    }
}
