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
import kotlinx.coroutines.runBlocking
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
 * Every read/write operation has two forms: a `suspend` one for coroutine callers, and a synchronous twin
 * (its name suffixed `Sync`) for the SDK's own callback-based `Dispatcher.AsyncCall` call chains
 * (`HTTPClient`, `TokenAPI`, `TokenLogoutOperation`), which can't themselves suspend. Both do the same
 * underlying work -- the synchronous twins are thin wrappers around the suspend ones, bridged with a
 * contained [runBlocking] -- but a synchronous twin first consults [cachedTokens], an in-memory mirror of
 * every token slot this process has touched. A write (`saveTokens`, `deleteTokens`, `deleteAccessToken`)
 * always keeps its slot(s) in [cachedTokens] current, since it's authoritative about what it just
 * persisted; a plain read only ever *fills* a slot the first time it's asked for (see
 * [cacheAccessTokenIfUnknown] and its siblings) and never overwrites one a write -- or an earlier read --
 * has already populated, so a read that's slow to resolve can never clobber a newer value with a stale
 * one once it finally does. So only the very first ever access for a given `appUserID` each process
 * actually blocks on storage; every later one is served straight from the cache, whichever form asks for
 * it.
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
    suspend fun currentAccessToken(appUserID: String): String? =
        readToken(accessTokenKey(appUserID)).also { cacheAccessTokenIfUnknown(appUserID, it) }

    /** The current refresh token stored for [appUserID], or `null` if none is stored. */
    suspend fun currentRefreshToken(appUserID: String): String? =
        readToken(refreshTokenKey(appUserID)).also { cacheRefreshTokenIfUnknown(appUserID, it) }

    /** The current ID token stored for [appUserID], or `null` if none is stored. */
    suspend fun currentIDToken(appUserID: String): String? =
        readToken(idTokenKey(appUserID)).also { cacheIDTokenIfUnknown(appUserID, it) }

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
        setCachedTokens(appUserID, accessToken, refreshToken, idToken)
    }

    /** Clears all three token slots for [appUserID], e.g. for a full logout. */
    suspend fun deleteTokens(appUserID: String) {
        writeToken(accessTokenKey(appUserID), null)
        writeToken(refreshTokenKey(appUserID), null)
        writeToken(idTokenKey(appUserID), null)
        setCachedTokens(appUserID, null, null, null)
    }

    /**
     * Clears only the access-token slot for [appUserID]. Local-only, no network call -- used to force a
     * refresh on the next request without a full logout.
     */
    suspend fun deleteAccessToken(appUserID: String) {
        writeToken(accessTokenKey(appUserID), null)
        setCachedAccessToken(appUserID, null)
    }

    // endregion

    // region Token refresh state machine

    /**
     * The tokens a successful `/auth/token` refresh call produced, in the shape [handleTokenRefreshResponse]
     * expects them back -- the same three values [saveTokens] takes, since a refresh writes all three slots
     * the same way a fresh login does.
     */
    data class TokenSet(val accessToken: String, val refreshToken: String, val idToken: String)

    /**
     * A plain description of the `/auth/token` call [tokenRefreshRequest] wants performed on behalf of
     * [appUserID], using [refreshToken] -- everything a caller needs to build that request, and nothing
     * about how to actually make it. Pass the eventual result back to [handleTokenRefreshResponse].
     */
    data class TokenRefreshRequest(val appUserID: String, val refreshToken: String)

    // Keyed by appUserID; a present entry means a refresh is currently in flight for that user, and its
    // list is every reportTokenUpdate callback still waiting on it (the caller that's actually performing
    // the network call, plus any concurrent callers that were folded into it instead of starting their own).
    private val pendingRefreshes = mutableMapOf<String, MutableList<(TokenSet?) -> Unit>>()

    /**
     * Whether a failed request for [appUserID] that got back [statusCode] -- and has, or hasn't,
     * [alreadyRetriedRefresh] -- should attempt a token refresh, and if so, a plain description of the
     * `/auth/token` call to make. Returns `null` (no refresh) when [statusCode] isn't 401, when
     * [alreadyRetriedRefresh] is already `true` (this request already went through one refresh-and-retry
     * cycle; a second one would risk looping forever against a refresh token the server keeps rejecting),
     * or when there's no refresh token currently stored for [appUserID] to attempt with. None of these
     * no-op cases register [reportTokenUpdate] -- there would be nothing for it to ever be called back with.
     *
     * A refresh already in flight for [appUserID] is de-duplicated: this call still registers
     * [reportTokenUpdate] to be replayed once that existing refresh resolves (see [handleTokenRefreshResponse]),
     * but returns `null` rather than a second [TokenRefreshRequest] -- only the caller whose request is
     * already in flight should actually perform a network call.
     */
    suspend fun tokenRefreshRequest(
        appUserID: String,
        statusCode: Int,
        alreadyRetriedRefresh: Boolean,
        reportTokenUpdate: (TokenSet?) -> Unit,
    ): TokenRefreshRequest? {
        val refreshToken = if (statusCode == RCHTTPStatusCodes.UNAUTHORIZED && !alreadyRetriedRefresh) {
            currentRefreshToken(appUserID)
        } else {
            null
        }
        if (refreshToken == null) return null

        return if (registerRefreshWaiter(appUserID, reportTokenUpdate)) {
            TokenRefreshRequest(appUserID, refreshToken)
        } else {
            null
        }
    }

    /**
     * Resolves the in-flight refresh for [appUserID] with [tokens] -- the `/auth/token` response mapped to
     * [TokenSet], or `null` for a failed refresh -- replaying it to every `reportTokenUpdate` callback
     * registered via [tokenRefreshRequest] since this refresh started, including the caller whose
     * [TokenRefreshRequest] actually performed it. A non-null [tokens] is saved the same way [saveTokens]
     * does, before anyone is notified. Either way, [appUserID]'s in-flight state is cleared once every
     * waiter has been notified, so the next eligible call to [tokenRefreshRequest] starts a fresh refresh
     * rather than folding into this one.
     */
    suspend fun handleTokenRefreshResponse(appUserID: String, tokens: TokenSet?) {
        if (tokens != null) {
            saveTokens(appUserID, tokens.accessToken, tokens.refreshToken, tokens.idToken)
        }
        resolveRefreshWaiters(appUserID).forEach { it(tokens) }
    }

    // endregion

    // region Synchronous access

    /**
     * The synchronous twin of [currentAccessToken] -- see this class's own doc for why one exists, and
     * what backs it. `null` immediately when [enabled] is `false`, the same as the suspend version, without
     * even consulting [cachedTokens].
     */
    fun currentAccessTokenSync(appUserID: String): String? {
        if (!enabled) return null
        return when (val cached = cachedAccessToken(appUserID)) {
            is Cached.Known -> cached.value
            Cached.Unknown -> runBlocking { currentAccessToken(appUserID) }
        }
    }

    /** The synchronous twin of [currentRefreshToken] -- see [currentAccessTokenSync]. */
    fun currentRefreshTokenSync(appUserID: String): String? {
        if (!enabled) return null
        return when (val cached = cachedRefreshToken(appUserID)) {
            is Cached.Known -> cached.value
            Cached.Unknown -> runBlocking { currentRefreshToken(appUserID) }
        }
    }

    /** The synchronous twin of [saveTokens] -- see [currentAccessTokenSync]. */
    fun saveTokensSync(appUserID: String, accessToken: String, refreshToken: String, idToken: String) {
        runBlocking { saveTokens(appUserID, accessToken, refreshToken, idToken) }
    }

    /** The synchronous twin of [deleteTokens] -- see [currentAccessTokenSync]. */
    fun deleteTokensSync(appUserID: String) {
        runBlocking { deleteTokens(appUserID) }
    }

    /** The synchronous twin of [authorizationHeaders] -- see [currentAccessTokenSync]. */
    fun authorizationHeadersSync(appUserID: String, isIAMEndpoint: Boolean): Map<String, String> {
        val token = if (enabled && !isIAMEndpoint) currentAccessTokenSync(appUserID) else null
        return token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
    }

    /** The synchronous twin of [tokenRefreshRequest] -- see [currentAccessTokenSync]. */
    fun tokenRefreshRequestSync(
        appUserID: String,
        statusCode: Int,
        alreadyRetriedRefresh: Boolean,
        reportTokenUpdate: (TokenSet?) -> Unit,
    ): TokenRefreshRequest? {
        val refreshToken = if (statusCode == RCHTTPStatusCodes.UNAUTHORIZED && !alreadyRetriedRefresh) {
            currentRefreshTokenSync(appUserID)
        } else {
            null
        }
        if (refreshToken == null) return null

        return if (registerRefreshWaiter(appUserID, reportTokenUpdate)) {
            TokenRefreshRequest(appUserID, refreshToken)
        } else {
            null
        }
    }

    /** The synchronous twin of [handleTokenRefreshResponse] -- see [currentAccessTokenSync]. */
    fun handleTokenRefreshResponseSync(appUserID: String, tokens: TokenSet?) {
        if (tokens != null) {
            saveTokensSync(appUserID, tokens.accessToken, tokens.refreshToken, tokens.idToken)
        }
        resolveRefreshWaiters(appUserID).forEach { it(tokens) }
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

    // Registers callback as a waiter on appUserID's in-flight refresh, starting one if none is already
    // running. Returns whether this call is the one that should actually perform the refresh (true iff it
    // was the first to register) -- @Synchronized so concurrent callers for the same appUserID can never
    // both see themselves as first.
    @Synchronized
    private fun registerRefreshWaiter(appUserID: String, callback: (TokenSet?) -> Unit): Boolean {
        val isFirstWaiter = !pendingRefreshes.containsKey(appUserID)
        pendingRefreshes.getOrPut(appUserID) { mutableListOf() }.add(callback)
        return isFirstWaiter
    }

    // Clears and returns every waiter registered for appUserID since its refresh started (empty if there
    // were none), so a caller can replay the result to each of them exactly once.
    @Synchronized
    private fun resolveRefreshWaiters(appUserID: String): List<(TokenSet?) -> Unit> =
        pendingRefreshes.remove(appUserID).orEmpty()

    // A cache slot that's never been read from or written to this process (Unknown) reads as "go ask
    // storage"; one that has (Known) is authoritative even when its value is null -- that's the only way
    // to tell "no token" apart from "haven't checked yet" without going back to storage on every call.
    private sealed class Cached {
        object Unknown : Cached()
        data class Known(val value: String?) : Cached()
    }

    private data class CachedTokens(
        val accessToken: Cached = Cached.Unknown,
        val refreshToken: Cached = Cached.Unknown,
        val idToken: Cached = Cached.Unknown,
    )

    // In-memory mirror of every token slot this process has read from or written to storage, keyed by
    // appUserID and guarded by this class's own monitor (the same lock @Synchronized already uses above) --
    // what lets a synchronous caller read or write a token without a fresh bridge into a coroutine each
    // time, as long as this appUserID's been touched at least once this process. A synchronous call for a
    // slot this process hasn't touched yet (Cached.Unknown) does a one-time, contained [runBlocking] read to
    // fill it, exactly the bridge every caller here used to do individually before this cache existed, just
    // paid once instead of on every request. Two concurrent first-ever synchronous calls for the same,
    // not-yet-cached appUserID can each independently read storage rather than one waiting on the other;
    // that's a deliberately accepted, one-time-only redundant read, not a correctness issue -- both reads see
    // the same on-disk value, and the lock still makes the final cache write itself race-free.
    //
    // A write (`saveTokens`/`deleteTokens`/`deleteAccessToken`) always overwrites its slot(s) here
    // unconditionally, since it's authoritative about what it just persisted. A plain read, though, only
    // ever fills a slot the first time (`cacheAccessTokenIfUnknown` and its siblings below) -- once a slot
    // is Known, by a write or an earlier read, a later read's result is discarded rather than applied. That's
    // what stops a read that's slow to resolve (e.g. one already in flight when a write for the same slot
    // lands) from clobbering a newer value with a stale one once it finally does.
    //
    // This still leaves one gap this cache alone doesn't close: two overlapping *writes* for the same
    // appUserID (e.g. an in-flight token refresh's `saveTokens` landing after a concurrent `deleteTokens`
    // from a logout) aren't ordered against each other here at all -- whichever one's own write physically
    // completes last simply wins, which isn't always the one that's semantically newest. Closing that fully
    // would need a per-appUserID write-fencing mechanism (a generation/epoch check spanning the disk write
    // too, not just this cache); tracked separately rather than folded into this fix.
    private val cachedTokens = mutableMapOf<String, CachedTokens>()

    @Synchronized
    private fun cachedAccessToken(appUserID: String): Cached =
        cachedTokens[appUserID]?.accessToken ?: Cached.Unknown

    @Synchronized
    private fun cachedRefreshToken(appUserID: String): Cached =
        cachedTokens[appUserID]?.refreshToken ?: Cached.Unknown

    @Synchronized
    private fun setCachedAccessToken(appUserID: String, value: String?) {
        val current = cachedTokens[appUserID] ?: CachedTokens()
        cachedTokens[appUserID] = current.copy(accessToken = Cached.Known(value))
    }

    /** Fills the access-token slot for [appUserID] only if nothing has claimed it yet -- see [cachedTokens]. */
    @Synchronized
    private fun cacheAccessTokenIfUnknown(appUserID: String, value: String?) {
        val current = cachedTokens[appUserID] ?: CachedTokens()
        if (current.accessToken == Cached.Unknown) {
            cachedTokens[appUserID] = current.copy(accessToken = Cached.Known(value))
        }
    }

    /** Fills the refresh-token slot for [appUserID] only if nothing has claimed it yet -- see [cachedTokens]. */
    @Synchronized
    private fun cacheRefreshTokenIfUnknown(appUserID: String, value: String?) {
        val current = cachedTokens[appUserID] ?: CachedTokens()
        if (current.refreshToken == Cached.Unknown) {
            cachedTokens[appUserID] = current.copy(refreshToken = Cached.Known(value))
        }
    }

    /** Fills the ID-token slot for [appUserID] only if nothing has claimed it yet -- see [cachedTokens]. */
    @Synchronized
    private fun cacheIDTokenIfUnknown(appUserID: String, value: String?) {
        val current = cachedTokens[appUserID] ?: CachedTokens()
        if (current.idToken == Cached.Unknown) {
            cachedTokens[appUserID] = current.copy(idToken = Cached.Known(value))
        }
    }

    @Synchronized
    private fun setCachedTokens(appUserID: String, accessToken: String?, refreshToken: String?, idToken: String?) {
        cachedTokens[appUserID] = CachedTokens(
            accessToken = Cached.Known(accessToken),
            refreshToken = Cached.Known(refreshToken),
            idToken = Cached.Known(idToken),
        )
    }

    private companion object {
        fun accessTokenKey(appUserID: String) = "RC-access-$appUserID"
        fun refreshTokenKey(appUserID: String) = "RC-refresh-$appUserID"
        fun idTokenKey(appUserID: String) = "RC-id-$appUserID"
    }
}
