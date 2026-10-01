@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import android.util.Base64
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.JWT
import com.revenuecat.purchases.common.isSuccessful
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.identity.IdentityAuthToken
import com.revenuecat.purchases.utils.filterNotNullValues
import com.revenuecat.purchases.utils.optNullableString
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

// Request bodies and response handling for the `/auth` endpoints, mirroring iOS's TokenOperations.swift.
// Dispatching and token storage live with the callers.

/** `POST /auth/login`: exchanges an identity token for RevenueCat tokens. */
internal object TokenLoginOperation {

    sealed interface Result {
        class Success(val tokens: TokenManager.TokenSet, val appUserID: String) : Result
        class Error(val error: PurchasesError) : Result
    }

    /**
     * The request body for [token], linking the new identity to [linkToID] (the current user's ID token)
     * when present. Absent optional fields are omitted, as iOS's encoding does.
     */
    fun body(token: IdentityAuthToken, linkToID: String?): Map<String, Any?> {
        val method = token.authenticationMethod.rawValue
        val idToken = when (token) {
            IdentityAuthToken.Anonymous -> return mapOf(METHOD to method, SCOPE to SCOPE_VALUE)
            is IdentityAuthToken.Oidc -> token.identityToken
            is IdentityAuthToken.Google -> token.identityToken
            is IdentityAuthToken.SignInWithApple -> token.identityToken
            is IdentityAuthToken.Firebase -> token.identityToken
            is IdentityAuthToken.Facebook -> token.identityToken
        }
        return mapOf(
            METHOD to method,
            SCOPE to SCOPE_VALUE,
            ID_TOKEN to idTokenString(idToken),
            EMAIL to (token as? IdentityAuthToken.Facebook)?.email,
            LINK_TO_ID to linkToID,
        ).filterNotNullValues()
    }

    /** Maps an `/auth/login` response to the issued tokens and the app user ID they belong to. */
    fun handleResponse(result: HTTPResult): Result {
        if (!result.isSuccessful()) return Result.Error(result.toPurchasesError())
        val tokens = TokenResponse.parse(result.body)
        val appUserID = tokens?.let { JWT.decode(it.accessToken)?.appUserId }
        return when {
            tokens == null -> Result.Error(unexpectedResponse("Token response missing access token"))
            appUserID == null -> Result.Error(unexpectedResponse("JWT missing RC user ID"))
            else -> Result.Success(tokens, appUserID)
        }
    }

    // The raw token as UTF-8 text, or base64 if it isn't valid UTF-8 (as iOS does).
    private fun idTokenString(bytes: ByteArray): String =
        try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (@Suppress("SwallowedException") e: CharacterCodingException) {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

    private const val METHOD = "method"
    private const val SCOPE = "scope"
    private const val ID_TOKEN = "id_token"
    private const val EMAIL = "email"
    private const val LINK_TO_ID = "link_to_id"
    private const val SCOPE_VALUE = "openid offline_access"
}

/** The token fields shared by `/auth/login` and `/auth/token` responses. */
internal object TokenResponse {

    /** The tokens in [body], or `null` if it has no access token. */
    fun parse(body: JSONObject): TokenManager.TokenSet? {
        val accessToken = body.optNullableString(ACCESS_TOKEN)?.takeIf { it.isNotEmpty() } ?: return null
        return TokenManager.TokenSet(
            accessToken = accessToken,
            refreshToken = body.optNullableString(REFRESH_TOKEN),
            idToken = body.optNullableString(ID_TOKEN),
        )
    }

    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"
    private const val ID_TOKEN = "id_token"
}

private fun unexpectedResponse(message: String) =
    PurchasesError(PurchasesErrorCode.UnexpectedBackendResponseError, message)
