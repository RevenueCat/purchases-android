@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.JWT
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.security.EncryptedItemStorage
import com.revenuecat.purchases.common.security.SecureItemStorage
import com.revenuecat.purchases.common.security.SecureStorageException
import com.revenuecat.purchases.common.security.derivePassword
import com.revenuecat.purchases.identity.IdentitySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import java.security.GeneralSecurityException

/**
 * Owns IAM login's persisted access/refresh/ID tokens, backed by a [SecureItemStorage] this class
 * constructs and owns for its lifetime.
 *
 * Construction is async and coroutine-driven: [EncryptedItemStorage.create] is kicked off on [scope] at
 * construction time, and every operation below suspends on that result via [storage] before doing
 * anything else -- callers never see a "not ready yet" state, they just wait. A blank API key or a
 * [GeneralSecurityException] deriving the password leaves storage permanently unavailable (logged, not
 * thrown); a [SecureStorageException] from an individual read or write is handled the same way -- a read
 * reports the value as absent, a write is a no-op -- most often caused by data left over from a previous
 * API key sharing the same on-disk file.
 *
 * Every public operation is `suspend`; callers integrate through coroutines, or a callback-based bridge
 * if they can't.
 *
 * @param context application context used to construct the underlying storage.
 * @param apiKey the SDK's configured API key; derives the storage's encryption password.
 * @param enabled whether IAM login is enabled. When `false`, storage is never constructed and every
 *   operation is a no-op.
 * @param scope owns storage construction; constructor-injectable for tests, cancelled by [close].
 */
@Suppress("TooManyFunctions")
internal class TokenManager(
    context: Context,
    apiKey: String,
    val enabled: Boolean,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    private val storageDeferred: Deferred<SecureItemStorage?>? = if (enabled) {
        scope.async {
            val password = derivePassword(apiKey) ?: return@async null
            try {
                EncryptedItemStorage.create(context, password)
            } catch (e: GeneralSecurityException) {
                errorLog(e) { "Failed to initialize IAM secure storage; IAM login will be unavailable." }
                null
            } finally {
                password.fill('\u0000') // caller's responsibility to zero once consumed
            }
        }
    } else {
        null
    }

    // region Read access

    /** The current access token stored for [appUserID], or `null` if none is stored. */
    suspend fun currentAccessToken(appUserID: String): String? = readToken(accessTokenKey(appUserID))

    /** The current refresh token stored for [appUserID], or `null` if none is stored. */
    suspend fun currentRefreshToken(appUserID: String): String? = readToken(refreshTokenKey(appUserID))

    /** The current ID token stored for [appUserID], or `null` if none is stored. */
    suspend fun currentIDToken(appUserID: String): String? = readToken(idTokenKey(appUserID))

    /**
     * Whether an access token is currently stored *and readable* for [appUserID]. Computed from
     * [currentAccessToken] rather than a raw presence check, so data left over from a different API key --
     * undecryptable under this instance's key -- never reports `true`.
     */
    suspend fun hasCurrentAccessToken(appUserID: String): Boolean = currentAccessToken(appUserID) != null

    // endregion

    // region Identity introspection

    /**
     * The `amr` claim of the ID token stored for [appUserID], as raw wire values (e.g. `"anonymous"`,
     * `"google"`) -- not mapped through [IdentitySource.fromRawValue] here, so an unrecognized value is
     * never silently dropped. [isCurrentIdentityAnonymous] needs every entry, recognized or not, to tell a
     * genuinely anonymous identity apart from one that only looks anonymous once an unrecognized,
     * non-anonymous source has been filtered out.
     *
     * `null` if there's no ID token stored, or it isn't a well-formed JWT; an empty `amr` claim yields an
     * empty list, not `null`.
     */
    suspend fun currentIdentitySources(appUserID: String): List<String>? =
        currentIDToken(appUserID)?.let { JWT.decode(it) }?.amr

    /**
     * Whether [appUserID]'s currently stored identity is anonymous: every raw value in
     * [currentIdentitySources] equals [IdentitySource.ANONYMOUS]'s raw value, and at least one is listed.
     * `false` if there's nothing to introspect ([currentIdentitySources] is `null` or empty).
     */
    suspend fun isCurrentIdentityAnonymous(appUserID: String): Boolean {
        val sources = currentIdentitySources(appUserID)
        return !sources.isNullOrEmpty() && sources.all { it == IdentitySource.ANONYMOUS.rawValue }
    }

    /**
     * The last raw value in [currentIdentitySources], mapped through [IdentitySource.fromRawValue]. `null`
     * if there's nothing to introspect, or that last entry isn't a source this SDK version recognizes.
     */
    suspend fun currentIdentitySource(appUserID: String): IdentitySource? =
        currentIdentitySources(appUserID)?.lastOrNull()?.let { IdentitySource.fromRawValue(it) }

    // endregion

    // region Authorization headers

    /**
     * The `Authorization` header(s) to attach to an IAM-authenticated request made on behalf of [appUserID]:
     * `mapOf("Authorization" to "Bearer <access token>")` when [enabled] and an access token is currently
     * readable for [appUserID], an empty map otherwise -- including when [isIAMEndpoint] is `true`, since the
     * token-issuing endpoints themselves (`/auth/login`, `/auth/token`, `/auth/revoke`) always authenticate
     * with the SDK's API key instead, never with a bearer token from a previous session (a caller must never
     * attach both).
     *
     * [isIAMEndpoint] stands in for `Endpoint.isIAMEndpoint`, added by a later step migrating IAM-aware
     * endpoints (see the IAM implementation plan's Step 14). It's threaded through as a plain parameter here,
     * rather than this method taking an `Endpoint` itself, so this behavior -- and its tests -- don't have to
     * wait on that migration landing first; callers pass the real check once it exists.
     */
    suspend fun authorizationHeaders(appUserID: String, isIAMEndpoint: Boolean): Map<String, String> {
        val token = if (enabled && !isIAMEndpoint) currentAccessToken(appUserID) else null
        return token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
    }

    // endregion

    // region Bulk operations

    /** Saves all three tokens for [appUserID], e.g. after login or a token refresh. */
    suspend fun saveTokens(appUserID: String, accessToken: String, refreshToken: String, idToken: String) {
        writeToken(accessTokenKey(appUserID), accessToken)
        writeToken(refreshTokenKey(appUserID), refreshToken)
        writeToken(idTokenKey(appUserID), idToken)
    }

    /** Clears all three token slots for [appUserID], e.g. for a full logout. */
    suspend fun deleteTokens(appUserID: String) {
        writeToken(accessTokenKey(appUserID), null)
        writeToken(refreshTokenKey(appUserID), null)
        writeToken(idTokenKey(appUserID), null)
    }

    /**
     * Clears only the access-token slot for [appUserID]. Local-only, no network call -- used to force a
     * refresh on the next request without a full logout.
     */
    suspend fun deleteAccessToken(appUserID: String) {
        writeToken(accessTokenKey(appUserID), null)
    }

    // endregion

    /** Cancels this instance's [scope], including any in-flight storage construction. */
    fun close() {
        scope.cancel()
    }

    private suspend fun storage(): SecureItemStorage? = storageDeferred?.await()

    private suspend fun readToken(identifier: String): String? =
        try {
            storage()?.readItem(identifier)?.toString(Charsets.UTF_8)
        } catch (e: SecureStorageException) {
            errorLog(e) { "Failed to read IAM token '$identifier'; treating it as absent." }
            null
        }

    private suspend fun writeToken(identifier: String, value: String?) {
        try {
            storage()?.modifyItem(identifier, value?.toByteArray(Charsets.UTF_8))
        } catch (e: SecureStorageException) {
            errorLog(e) { "Failed to write IAM token '$identifier'; treating it as a no-op." }
        }
    }

    private companion object {
        fun accessTokenKey(appUserID: String) = "RC-access-$appUserID"
        fun refreshTokenKey(appUserID: String) = "RC-refresh-$appUserID"
        fun idTokenKey(appUserID: String) = "RC-id-$appUserID"
    }
}
