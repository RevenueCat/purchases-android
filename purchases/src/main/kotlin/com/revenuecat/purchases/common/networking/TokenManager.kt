@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.JWT
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.security.EncryptedItemStorage
import com.revenuecat.purchases.common.security.SecureItemStorage
import com.revenuecat.purchases.common.security.SecureStorageException
import com.revenuecat.purchases.common.security.derivePassword
import com.revenuecat.purchases.identity.IdentitySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.security.GeneralSecurityException

/**
 * Owns IAM login's persisted token state: the access/refresh/ID tokens for each app user, kept in a
 * [SecureItemStorage] this class constructs and owns for its entire lifetime.
 *
 * ## Construction
 *
 * `TokenManager` is built from the same raw ingredients [EncryptedItemStorage.create] itself needs — the
 * application [Context], the SDK's configured API key, and whether IAM is enabled — rather than a pre-built
 * [SecureItemStorage]. This class is the only thing that ever touches this storage, and it's what
 * `PurchasesOrchestrator` ends up holding a reference to regardless of whether anything has called into it yet,
 * so it's the natural owner of the construction lifecycle rather than parking that state somewhere ahead of a
 * real consumer.
 *
 * Construction is async: when [enabled], this class owns a [CoroutineScope] (mirroring the
 * `UiConfigProvider`/`RemoteConfigManager` pattern already used elsewhere in this codebase) and launches
 * [EncryptedItemStorage.create] on it as soon as it's constructed. This keeps `create()`'s `suspend` signature
 * intact and never blocks the thread that constructs a `TokenManager` — nothing needs the result before
 * `Purchases.configure()` returns. The resolved storage lands in a `@Volatile`-guarded holder; until it resolves, every
 * token operation below treats storage as unavailable the same way it already does when [enabled] is `false` —
 * not a new failure mode, just an existing one with a second cause.
 *
 * A blank API key ([derivePassword] returning `null`) or a [GeneralSecurityException] during key derivation
 * both leave storage permanently unavailable for this instance's lifetime (already logged by [derivePassword]
 * or this class, respectively) rather than crashing configuration.
 *
 * Once storage is ready, a [SecureStorageException] from an individual read or write is handled the same
 * way rather than escaping as a crash: a read reports the value as absent and a write is a no-op (both
 * logged). The most common cause isn't a hardware/IO failure but a decrypt failure -- a value on disk was
 * encrypted under a *different* derived key than this instance's, e.g. leftover data from a previous API
 * key sharing the same on-disk storage file (reconfiguring from a sandbox to a production key, say).
 *
 * @param context the application context; the same direct-boot-aware context other file-backed singletons in
 *   this SDK use.
 * @param apiKey the SDK's configured API key, used to derive the storage's encryption password.
 * @param enabled whether IAM login is enabled for this SDK configuration (`AppConfig.iamEnabled`). When `false`,
 *   storage is never constructed and every operation below is a no-op.
 * @param scope the [CoroutineScope] storage construction runs on; constructor-injectable so tests can control
 *   or await it. Defaults to a dedicated [SupervisorJob] + [Dispatchers.IO] scope, cancelled by [close].
 */

@Suppress("TooManyFunctions")
internal class TokenManager(
    context: Context,
    apiKey: String,
    val enabled: Boolean,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    @Volatile
    private var storage: SecureItemStorage? = null

    // Non-null only when `enabled`, so `awaitStorageInitialized` is a no-op for a disabled instance.
    private val storageInitJob: Job? = if (enabled) {
        scope.launch {
            val password = derivePassword(apiKey) ?: return@launch
            try {
                storage = EncryptedItemStorage.create(context, password)
            } catch (e: GeneralSecurityException) {
                errorLog(e) {
                    "Failed to initialize IAM secure storage; IAM login will be unavailable this session."
                }
            } finally {
                // EncryptedItemStorage.create's KDoc makes zeroing the caller's responsibility once the
                // password has been consumed; this is the only call site, so it's done unconditionally here.
                password.fill('\u0000')
            }
        }
    } else {
        null
    }

    // region Read access

    /** The current access token stored for [appUserID], or `null` if none is stored (or storage isn't ready). */
    fun currentAccessToken(appUserID: String): String? = readToken(accessTokenKey(appUserID))

    /** The current refresh token stored for [appUserID], or `null` if none is stored (or storage isn't ready). */
    fun currentRefreshToken(appUserID: String): String? = readToken(refreshTokenKey(appUserID))

    /** The current ID token stored for [appUserID], or `null` if none is stored (or storage isn't ready). */
    fun currentIDToken(appUserID: String): String? = readToken(idTokenKey(appUserID))

    /**
     * Whether an access token is currently stored *and readable* for [appUserID]. Deliberately computed from
     * [currentAccessToken] rather than a raw on-disk presence check: a value can exist on disk left over from
     * a *previous* API key's storage (e.g. reconfiguring the SDK from a sandbox to a production key, which
     * shares the same on-disk storage file) without there being any way to actually decrypt it back, and this
     * should never report `true` for a token that [currentAccessToken] can't actually produce. `false` if
     * storage isn't ready or the stored value can't be read, independent of whether one would otherwise be
     * present.
     */
    fun hasCurrentAccessToken(appUserID: String): Boolean = currentAccessToken(appUserID) != null

    // endregion

    // region Identity introspection

    /**
     * The identity provider(s) listed in the `amr` (Authentication Methods References) claim of the ID
     * token currently stored for [appUserID], as their raw wire values (e.g. `"anonymous"`, `"google"`) --
     * deliberately *not* mapped through [IdentitySource.fromRawValue] here. A value this SDK version
     * doesn't recognize yet (e.g. a newer wire value) is kept rather than silently dropped: dropping it
     * would let [isCurrentIdentityAnonymous] see only the *recognized* remainder of the list, which can
     * make an identity that is actually linked to an unrecognized, non-anonymous provider look anonymous
     * once the one entry that would have said otherwise has been filtered out. Raw values are converted to
     * [IdentitySource] only where a single one is actually consumed, e.g. [currentIdentitySource].
     *
     * `null` if there's no ID token stored for [appUserID] (including because storage isn't ready) or the
     * stored value isn't a well-formed JWT -- never because the `amr` claim happens to be empty; an empty
     * `amr` claim yields an empty list, not `null`.
     */
    fun currentIdentitySources(appUserID: String): List<String>? =
        currentIDToken(appUserID)?.let { JWT.decode(it) }?.amr

    /**
     * Whether [appUserID]'s currently stored identity is anonymous: every raw value in
     * [currentIdentitySources] equals [IdentitySource.ANONYMOUS]'s raw wire value, and at least one source
     * is listed at all. Comparing raw strings here (rather than mapping each one through
     * [IdentitySource.fromRawValue] first, the way [currentIdentitySource] does) is what makes this safe
     * against an unrecognized, *not*-actually-anonymous entry sitting alongside `"anonymous"` in the `amr`
     * claim -- that entry can't quietly be dropped and produce a false positive here, since nothing is ever
     * dropped from [currentIdentitySources] in the first place.
     *
     * `false` when there's no readable identity to introspect in the first place ([currentIdentitySources]
     * returns `null`), same as when it returns an empty list.
     */
    fun isCurrentIdentityAnonymous(appUserID: String): Boolean {
        val sources = currentIdentitySources(appUserID)
        return !sources.isNullOrEmpty() && sources.all { it == IdentitySource.ANONYMOUS.rawValue }
    }

    /**
     * The most recently listed entry in [currentIdentitySources] for [appUserID] -- the last raw value in
     * the stored ID token's `amr` claim -- mapped through [IdentitySource.fromRawValue]. `null` if there's
     * no readable identity to introspect at all, or if that last entry isn't a source this SDK version
     * recognizes yet.
     */
    fun currentIdentitySource(appUserID: String): IdentitySource? =
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
    fun authorizationHeaders(appUserID: String, isIAMEndpoint: Boolean): Map<String, String> {
        if (!enabled || isIAMEndpoint) return emptyMap()
        val token = currentAccessToken(appUserID) ?: return emptyMap()
        return mapOf("Authorization" to "Bearer $token")
    }

    // endregion

    // region Bulk operations

    /**
     * Saves all three tokens for [appUserID] in one call, e.g. after a successful login or token refresh. The
     * only way to write a token into this storage — there's no per-token setter, so a caller can never update
     * one slot without the other two. No-op if storage isn't ready.
     */
    fun saveTokens(appUserID: String, accessToken: String, refreshToken: String, idToken: String) {
        writeToken(accessTokenKey(appUserID), accessToken)
        writeToken(refreshTokenKey(appUserID), refreshToken)
        writeToken(idTokenKey(appUserID), idToken)
    }

    /**
     * Clears all three token slots for [appUserID], e.g. for a full logout. No-op if storage isn't ready.
     */
    fun deleteTokens(appUserID: String) {
        writeToken(accessTokenKey(appUserID), null)
        writeToken(refreshTokenKey(appUserID), null)
        writeToken(idTokenKey(appUserID), null)
    }

    /**
     * Clears only the access-token slot for [appUserID], leaving its refresh and ID tokens untouched. Local-only
     * — never triggers a network call. Used where the SDK needs to invalidate a cached access token without a
     * full logout, e.g. forcing a refresh on the next request. No-op if storage isn't ready.
     */
    fun deleteAccessToken(appUserID: String) {
        writeToken(accessTokenKey(appUserID), null)
    }

    // endregion

    /** Cancels this instance's [scope], including any in-flight storage construction. */
    fun close() {
        scope.cancel()
    }

    /**
     * Suspends until this instance's async storage construction has finished (successfully or not). A no-op
     * when [enabled] is `false`, since no construction was ever started. For tests only — production callers
     * are designed to tolerate "storage not ready yet" rather than wait for it.
     */
    @VisibleForTesting
    internal suspend fun awaitStorageInitialized() {
        storageInitJob?.join()
    }

    private fun readToken(identifier: String): String? =
        try {
            storage?.readItem(identifier)?.toString(Charsets.UTF_8)
        } catch (e: SecureStorageException) {
            errorLog(e) { "Failed to read IAM token '$identifier'; treating it as absent." }
            null
        }

    private fun writeToken(identifier: String, value: String?) {
        try {
            storage?.modifyItem(identifier, value?.toByteArray(Charsets.UTF_8))
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
