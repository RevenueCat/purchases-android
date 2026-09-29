package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.isSuccessful
import com.revenuecat.purchases.common.toPurchasesError
import java.net.URL

/**
 * Performs the low-level `/auth/revoke` call for [appUserID] via [Dispatcher.AsyncCall].
 *
 * There's no bare access-token revoke -- only a refresh token can be revoked -- so with nothing stored for
 * [appUserID] this skips the network call and clears local tokens directly. The whole body, including the
 * initial [TokenManager.currentRefreshTokenSync] check, runs inside a single [Dispatcher] job.
 *
 * A successful revoke only clears tokens it actually revoked: [TokenManager.currentRefreshTokenSync] is
 * re-checked against the sent refresh token, leaving local tokens alone if they've since changed (e.g. a
 * login/refresh that completed while this revoke was in flight).
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
                tokenManager.currentRefreshTokenSync(appUserID) { refreshToken ->
                    if (refreshToken == null) {
                        tokenManager.deleteTokensSync(appUserID) {
                            onSuccessHandler()
                        }
                    } else {
                        val body = mapOf(
                            TOKEN_KEY to refreshToken,
                            TOKEN_TYPE_HINT_KEY to TOKEN_TYPE_HINT_REFRESH_TOKEN,
                        )
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
                                    tokenManager.currentRefreshTokenSync(appUserID) { latestRefreshToken ->
                                        if (latestRefreshToken == refreshToken) {
                                            tokenManager.deleteTokensSync(appUserID) {
                                                onSuccessHandler()
                                            }
                                        } else {
                                            onSuccessHandler()
                                        }
                                    }
                                } else {
                                    // Does not clear local tokens on failure -- the session stays as valid as before.
                                    onErrorHandler(result.toPurchasesError().also { errorLog(it) })
                                }
                            }
                        }
                        call.run()
                    }
                }
            },
        )
    }
}
