@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.JWT
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.isSuccessful
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.identity.Identity
import java.net.URL

/**
 * Performs the low-level `/auth/login` call via [Dispatcher.AsyncCall], and on success extracts the new
 * app user id from the access token's JWT claims. Deliberately thin: doesn't dedupe concurrent calls,
 * decide whether to link an identity, or persist tokens -- all `TokenAPI`'s job.
 */
internal object TokenLoginOperation {

    private const val ACCESS_TOKEN_KEY = "access_token"
    private const val REFRESH_TOKEN_KEY = "refresh_token"
    private const val ID_TOKEN_KEY = "id_token"

    /** A successful login's app user id (from the access token's `rc.app_user_id` claim) and its tokens. */
    internal data class Result(val appUserID: String, val tokens: TokenManager.TokenSet)

    /** @param linkToID the already-resolved app user id to link to, or `null` for none. */
    @Suppress("LongParameterList")
    fun logIn(
        baseURL: URL,
        httpClient: HTTPClient,
        dispatcher: Dispatcher,
        authenticationHeaders: Map<String, String>,
        identity: Identity,
        linkToID: String?,
        fallbackBaseURLs: List<URL> = emptyList(),
        onSuccessHandler: (Result) -> Unit,
        onErrorHandler: (PurchasesError) -> Unit,
    ) {
        if (!identity.authToken.validate()) {
            onErrorHandler(
                PurchasesError(
                    PurchasesErrorCode.InvalidCredentialsError,
                    "Cannot log in with an empty ${identity.identitySource} identity credential.",
                ).also { errorLog(it) },
            )
            return
        }

        val body = TokenLoginRequestBody.from(identity, linkToID)

        val call = object : Dispatcher.AsyncCall() {
            override fun call(): HTTPResult {
                return httpClient.performRequest(
                    baseURL,
                    Endpoint.TokenLogin,
                    body.toMap(),
                    null,
                    authenticationHeaders,
                    fallbackBaseURLs = fallbackBaseURLs,
                )
            }

            override fun onError(error: PurchasesError) {
                onErrorHandler(error)
            }

            override fun onCompletion(result: HTTPResult) {
                if (!result.isSuccessful()) {
                    onErrorHandler(result.toPurchasesError().also { errorLog(it) })
                    return
                }
                completeSuccessfulLogin(result, onSuccessHandler, onErrorHandler)
            }
        }
        dispatcher.enqueue(call)
    }

    /** Parses a successful `/auth/login` response and reports [onSuccessHandler]/[onErrorHandler]. */
    private fun completeSuccessfulLogin(
        result: HTTPResult,
        onSuccessHandler: (Result) -> Unit,
        onErrorHandler: (PurchasesError) -> Unit,
    ) {
        val accessToken = result.body.optString(ACCESS_TOKEN_KEY).takeIf { it.isNotEmpty() }
        val refreshToken = result.body.optString(REFRESH_TOKEN_KEY).takeIf { it.isNotEmpty() }
        val idToken = result.body.optString(ID_TOKEN_KEY).takeIf { it.isNotEmpty() }
        if (accessToken == null || refreshToken == null || idToken == null) {
            onErrorHandler(
                PurchasesError(
                    PurchasesErrorCode.UnexpectedBackendResponseError,
                    "/auth/login response is missing one or more of " +
                        "access_token/refresh_token/id_token.",
                ).also { errorLog(it) },
            )
            return
        }

        val appUserID = JWT.decode(accessToken)?.appUserId
        if (appUserID == null) {
            onErrorHandler(
                PurchasesError(
                    PurchasesErrorCode.UnexpectedBackendResponseError,
                    "/auth/login's access token is not a well-formed JWT, or is missing its " +
                        "rc.app_user_id claim.",
                ).also { errorLog(it) },
            )
            return
        }

        onSuccessHandler(Result(appUserID, TokenManager.TokenSet(accessToken, refreshToken, idToken)))
    }
}
