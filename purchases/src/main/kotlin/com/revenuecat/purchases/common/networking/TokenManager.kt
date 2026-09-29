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
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.GeneralSecurityException
import kotlin.coroutines.resume

/**
 * Owns IAM login's persisted access/refresh/ID tokens, backed by a [SecureItemStorage].
 *
 * Storage construction runs on a background thread via [EncryptedItemStorage.createBlocking], never on the
 * caller. A blank API key or security error leaves storage permanently unavailable (logged, not thrown);
 * other storage errors are logged and treated as absent/no-op. Construction also pre-warms [cachedTokens]
 * from whatever's already on disk (see [preWarmCache]).
 *
 * Every operation has three forms: **callback-based** (`Sync` suffix, queues until storage resolves),
 * **cache-only** (`FromCache` suffix, for callers like [HTTPClient] that can't wait on storage at all), and
 * **suspend** (a thin wrapper around the callback-based twin). Writes always overwrite [cachedTokens];
 * reads only fill an unknown slot, never clobbering a newer value.
 *
 * @param context application context used to construct the underlying storage.
 * @param apiKey the SDK's configured API key; derives the storage's encryption password.
 * @param enabled whether IAM login is enabled; when `false`, storage is never constructed.
 */
@Suppress("TooManyFunctions")
internal class TokenManager(
    context: Context,
    apiKey: String,
    val enabled: Boolean,
) {

    // Background thread doing one-time storage construction; interruptible via close().
    private var constructionThread: Thread? = null

    // Guarded by this class's monitor. storageResolved flips true exactly once; storage is only written then.
    private var storage: SecureItemStorage? = null
    private var storageResolved: Boolean = false
    private val pendingStorageCallbacks = mutableListOf<(SecureItemStorage?) -> Unit>()

    init {
        if (enabled) {
            constructionThread = Thread {
                val password = derivePassword(apiKey)
                val result = if (password == null) {
                    null
                } else {
                    try {
                        EncryptedItemStorage.createBlocking(context, password).also { preWarmCache(it) }
                    } catch (e: GeneralSecurityException) {
                        errorLog(e) { "Failed to initialize IAM secure storage; IAM login will be unavailable." }
                        null
                    } finally {
                        password.fill('\u0000') // zero the password once consumed
                    }
                }
                resolveStorage(result)
            }.apply {
                name = "RevenueCat-TokenManager-Init"
                isDaemon = true
                start()
            }
        } else {
            storageResolved = true
        }
    }

    // region Read access (suspend)

    /** The current access token stored for [appUserID], or `null` if none is stored. */
    suspend fun currentAccessToken(appUserID: String): String? =
        suspendCancellableCoroutine { cont -> currentAccessTokenSync(appUserID) { cont.resume(it) } }

    /** The current refresh token stored for [appUserID], or `null` if none is stored. */
    suspend fun currentRefreshToken(appUserID: String): String? =
        suspendCancellableCoroutine { cont -> currentRefreshTokenSync(appUserID) { cont.resume(it) } }

    /** The current ID token stored for [appUserID], or `null` if none is stored. */
    suspend fun currentIDToken(appUserID: String): String? =
        suspendCancellableCoroutine { cont -> currentIDTokenSync(appUserID) { cont.resume(it) } }

    /** Whether an access token is stored and decryptable for [appUserID]. */
    suspend fun hasCurrentAccessToken(appUserID: String): Boolean = currentAccessToken(appUserID) != null

    // endregion

    // region Identity introspection

    /**
     * The `amr` claim of [appUserID]'s ID token, as raw wire values (e.g. `"anonymous"`, `"google"`).
     * `null` if there's no ID token or it isn't a well-formed JWT.
     */
    suspend fun currentIdentitySources(appUserID: String): List<String>? =
        currentIDToken(appUserID)?.let { JWT.decode(it) }?.amr

    /** Whether every source in [currentIdentitySources] is anonymous; `false` if none are listed. */
    suspend fun isCurrentIdentityAnonymous(appUserID: String): Boolean {
        val sources = currentIdentitySources(appUserID)
        return !sources.isNullOrEmpty() && sources.all { it == IdentitySource.ANONYMOUS.rawValue }
    }

    /**
     * The last entry in [currentIdentitySources], mapped via [IdentitySource.fromRawValue]; `null` if
     * unrecognized or absent.
     */
    suspend fun currentIdentitySource(appUserID: String): IdentitySource? =
        currentIdentitySources(appUserID)?.lastOrNull()?.let { IdentitySource.fromRawValue(it) }

    // endregion

    // region Authorization headers (suspend)

    /**
     * The bearer `Authorization` header for [appUserID], or empty if disabled, no token, or
     * [isIAMEndpoint] is `true` (those endpoints authenticate with the API key instead).
     */
    suspend fun authorizationHeaders(appUserID: String, isIAMEndpoint: Boolean): Map<String, String> =
        suspendCancellableCoroutine { cont ->
            authorizationHeadersSync(appUserID, isIAMEndpoint) { cont.resume(it) }
        }

    // endregion

    // region Bulk operations (suspend)

    /** Saves all three tokens for [appUserID], e.g. after login or a token refresh. */
    suspend fun saveTokens(appUserID: String, accessToken: String, refreshToken: String, idToken: String) {
        suspendCancellableCoroutine<Unit> { cont ->
            saveTokensSync(appUserID, accessToken, refreshToken, idToken) { cont.resume(Unit) }
        }
    }

    /** Clears all three token slots for [appUserID], e.g. for a full logout. */
    suspend fun deleteTokens(appUserID: String) {
        suspendCancellableCoroutine<Unit> { cont ->
            deleteTokensSync(appUserID) { cont.resume(Unit) }
        }
    }

    /** Clears only the access-token slot for [appUserID], to force a refresh without a full logout. */
    suspend fun deleteAccessToken(appUserID: String) {
        suspendCancellableCoroutine<Unit> { cont ->
            if (!enabled) {
                cont.resume(Unit)
            } else {
                withStorage { storage ->
                    writeToken(storage, accessTokenKey(appUserID), null)
                    setCachedAccessToken(appUserID, null)
                    cont.resume(Unit)
                }
            }
        }
    }

    // endregion

    // region Token refresh state machine

    /** Tokens from a successful `/auth/token` refresh, passed to [handleTokenRefreshResponse]. */
    data class TokenSet(val accessToken: String, val refreshToken: String, val idToken: String)

    /** Describes the `/auth/token` call to make for [appUserID]; pass the result to [handleTokenRefreshResponse]. */
    data class TokenRefreshRequest(val appUserID: String, val refreshToken: String)

    // Keyed by appUserID; present = refresh in flight, value = waiters to notify when it resolves.
    private val pendingRefreshes = mutableMapOf<String, MutableList<(TokenSet?) -> Unit>>()

    /** Suspend twin of [tokenRefreshRequestSync]. */
    suspend fun tokenRefreshRequest(
        appUserID: String,
        statusCode: Int,
        alreadyRetriedRefresh: Boolean,
        reportTokenUpdate: (TokenSet?) -> Unit,
    ): TokenRefreshRequest? = suspendCancellableCoroutine { cont ->
        tokenRefreshRequestSync(appUserID, statusCode, alreadyRetriedRefresh, reportTokenUpdate) { cont.resume(it) }
    }

    /** Suspend twin of [handleTokenRefreshResponseSync]. */
    suspend fun handleTokenRefreshResponse(appUserID: String, tokens: TokenSet?) {
        suspendCancellableCoroutine<Unit> { cont ->
            handleTokenRefreshResponseSync(appUserID, tokens) { cont.resume(Unit) }
        }
    }

    // endregion

    // region Callback-based access

    /** Callback-based twin of [currentAccessToken]; never blocks the calling thread. */
    fun currentAccessTokenSync(appUserID: String, callback: (String?) -> Unit) {
        if (!enabled) {
            callback(null)
            return
        }
        when (val cached = cachedAccessToken(appUserID)) {
            is Cached.Known -> callback(cached.value)
            Cached.Unknown -> withStorage { storage ->
                val value = readToken(storage, accessTokenKey(appUserID))
                cacheAccessTokenIfUnknown(appUserID, value)
                callback(value)
            }
        }
    }

    /** The callback-based twin of [currentRefreshToken] -- see [currentAccessTokenSync]. */
    fun currentRefreshTokenSync(appUserID: String, callback: (String?) -> Unit) {
        if (!enabled) {
            callback(null)
            return
        }
        when (val cached = cachedRefreshToken(appUserID)) {
            is Cached.Known -> callback(cached.value)
            Cached.Unknown -> withStorage { storage ->
                val value = readToken(storage, refreshTokenKey(appUserID))
                cacheRefreshTokenIfUnknown(appUserID, value)
                callback(value)
            }
        }
    }

    /** The callback-based twin of [currentIDToken] -- see [currentAccessTokenSync]. */
    private fun currentIDTokenSync(appUserID: String, callback: (String?) -> Unit) {
        if (!enabled) {
            callback(null)
            return
        }
        when (val cached = cachedIDToken(appUserID)) {
            is Cached.Known -> callback(cached.value)
            Cached.Unknown -> withStorage { storage ->
                val value = readToken(storage, idTokenKey(appUserID))
                cacheIDTokenIfUnknown(appUserID, value)
                callback(value)
            }
        }
    }

    /** The callback-based twin of [saveTokens] -- see [currentAccessTokenSync]. */
    fun saveTokensSync(
        appUserID: String,
        accessToken: String,
        refreshToken: String,
        idToken: String,
        callback: () -> Unit = {},
    ) {
        if (!enabled) {
            callback()
            return
        }
        withStorage { storage ->
            // Null storage means nothing was persisted, so skip the cache update too.
            if (storage != null) {
                writeToken(storage, accessTokenKey(appUserID), accessToken)
                writeToken(storage, refreshTokenKey(appUserID), refreshToken)
                writeToken(storage, idTokenKey(appUserID), idToken)
                setCachedTokens(appUserID, accessToken, refreshToken, idToken)
            }
            callback()
        }
    }

    /** The callback-based twin of [deleteTokens] -- see [currentAccessTokenSync]. */
    fun deleteTokensSync(appUserID: String, callback: () -> Unit = {}) {
        if (!enabled) {
            callback()
            return
        }
        withStorage { storage ->
            // See saveTokensSync: null storage means nothing to delete, so leave the cache as-is.
            if (storage != null) {
                writeToken(storage, accessTokenKey(appUserID), null)
                writeToken(storage, refreshTokenKey(appUserID), null)
                writeToken(storage, idTokenKey(appUserID), null)
                setCachedTokens(appUserID, null, null, null)
            }
            callback()
        }
    }

    /** The callback-based twin of [authorizationHeaders] -- see [currentAccessTokenSync]. */
    fun authorizationHeadersSync(appUserID: String, isIAMEndpoint: Boolean, callback: (Map<String, String>) -> Unit) {
        if (!enabled || isIAMEndpoint) {
            callback(emptyMap())
            return
        }
        currentAccessTokenSync(appUserID) { token ->
            callback(token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap())
        }
    }

    /**
     * Callback-based twin of [tokenRefreshRequest]. Callback gets `null` when [statusCode] isn't 401,
     * [alreadyRetriedRefresh] is `true`, or no refresh token is stored. A refresh already in flight for
     * [appUserID] is deduped: [reportTokenUpdate] is still queued, but `null` is returned so only the
     * original caller performs the network call.
     */
    fun tokenRefreshRequestSync(
        appUserID: String,
        statusCode: Int,
        alreadyRetriedRefresh: Boolean,
        reportTokenUpdate: (TokenSet?) -> Unit,
        callback: (TokenRefreshRequest?) -> Unit,
    ) {
        if (statusCode != RCHTTPStatusCodes.UNAUTHORIZED || alreadyRetriedRefresh) {
            callback(null)
            return
        }
        currentRefreshTokenSync(appUserID) { refreshToken ->
            if (refreshToken == null) {
                callback(null)
            } else if (registerRefreshWaiter(appUserID, reportTokenUpdate)) {
                callback(TokenRefreshRequest(appUserID, refreshToken))
            } else {
                callback(null)
            }
        }
    }

    /**
     * Callback-based twin of [handleTokenRefreshResponse]. Saves [tokens] (if non-null) via
     * [saveTokensSync], then replays it to every waiter registered since this refresh started, clearing
     * the in-flight state.
     */
    fun handleTokenRefreshResponseSync(appUserID: String, tokens: TokenSet?, callback: () -> Unit = {}) {
        if (tokens != null) {
            saveTokensSync(appUserID, tokens.accessToken, tokens.refreshToken, tokens.idToken) {
                resolveRefreshWaiters(appUserID).forEach { it(tokens) }
                callback()
            }
        } else {
            resolveRefreshWaiters(appUserID).forEach { it(tokens) }
            callback()
        }
    }

    // endregion

    // region Cache-only access

    /**
     * The access token cached for [appUserID], never falling back to storage. For callers like
     * [HTTPClient] that run synchronously and can't wait on storage construction at all.
     *
     * [preWarmCache] means this only differs from [currentAccessTokenSync] during construction itself, or
     * for an `appUserID` genuinely new to this process.
     */
    fun currentAccessTokenFromCache(appUserID: String): String? {
        if (!enabled) return null
        return (cachedAccessToken(appUserID) as? Cached.Known)?.value
    }

    /** The cache-only twin of [currentRefreshTokenSync] -- see [currentAccessTokenFromCache]. */
    fun currentRefreshTokenFromCache(appUserID: String): String? {
        if (!enabled) return null
        return (cachedRefreshToken(appUserID) as? Cached.Known)?.value
    }

    /** The cache-only twin of [authorizationHeadersSync] -- see [currentAccessTokenFromCache]. */
    fun authorizationHeadersFromCache(appUserID: String, isIAMEndpoint: Boolean): Map<String, String> {
        val token = if (enabled && !isIAMEndpoint) currentAccessTokenFromCache(appUserID) else null
        return token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
    }

    /** Cache-only twin of [tokenRefreshRequestSync]; an unresolved refresh-token slot is treated as absent. */
    fun tokenRefreshRequestFromCache(
        appUserID: String,
        statusCode: Int,
        alreadyRetriedRefresh: Boolean,
        reportTokenUpdate: (TokenSet?) -> Unit,
    ): TokenRefreshRequest? {
        val refreshToken = if (statusCode == RCHTTPStatusCodes.UNAUTHORIZED && !alreadyRetriedRefresh) {
            currentRefreshTokenFromCache(appUserID)
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

    // endregion

    /** Best-effort interrupt of storage construction, if still in flight; advisory only. */
    fun close() {
        constructionThread?.interrupt()
    }

    // Invokes callback immediately if storage is already resolved, else queues it for resolveStorage.
    private fun withStorage(callback: (SecureItemStorage?) -> Unit) {
        var resolvedStorage: SecureItemStorage? = null
        var isResolved = false
        synchronized(this) {
            isResolved = storageResolved
            if (isResolved) {
                resolvedStorage = storage
            } else {
                pendingStorageCallbacks.add(callback)
            }
        }
        if (isResolved) callback(resolvedStorage)
    }

    // Called once construction resolves; drains and runs pendingStorageCallbacks.
    private fun resolveStorage(result: SecureItemStorage?) {
        val callbacksToRun: List<(SecureItemStorage?) -> Unit>
        synchronized(this) {
            storage = result
            storageResolved = true
            callbacksToRun = pendingStorageCallbacks.toList()
            pendingStorageCallbacks.clear()
        }
        callbacksToRun.forEach { it(result) }
    }

    /**
     * Fills [cachedTokens] with everything [storage] already holds on disk, for every `appUserID`.
     * Without this, [currentAccessTokenFromCache] and its siblings -- which never fall back to storage --
     * could see an empty cache for a returning user's very first request.
     *
     * Parses each [SecureItemStorage.allItemIdentifiers] entry back into an `(appUserID, slot)` pair via
     * the `*_PREFIX` constants, and fills each slot via the `-IfUnknown` setters so a concurrent write is
     * never clobbered. A [SecureStorageException] is logged and that slot is simply left for lazy fill.
     */
    private fun preWarmCache(storage: SecureItemStorage) {
        val identifiers = try {
            storage.allItemIdentifiers()
        } catch (e: SecureStorageException) {
            errorLog(e) { "Failed to enumerate stored IAM tokens; cache will warm up lazily instead." }
            return
        }
        for (identifier in identifiers) {
            when {
                identifier.startsWith(ACCESS_TOKEN_PREFIX) -> {
                    val appUserID = identifier.removePrefix(ACCESS_TOKEN_PREFIX)
                    cacheAccessTokenIfUnknown(appUserID, readToken(storage, identifier))
                }
                identifier.startsWith(REFRESH_TOKEN_PREFIX) -> {
                    val appUserID = identifier.removePrefix(REFRESH_TOKEN_PREFIX)
                    cacheRefreshTokenIfUnknown(appUserID, readToken(storage, identifier))
                }
                identifier.startsWith(ID_TOKEN_PREFIX) -> {
                    val appUserID = identifier.removePrefix(ID_TOKEN_PREFIX)
                    cacheIDTokenIfUnknown(appUserID, readToken(storage, identifier))
                }
                else -> Unit // Not one of this class's own keys.
            }
        }
    }

    private fun readToken(storage: SecureItemStorage?, identifier: String): String? =
        try {
            storage?.readItem(identifier)?.toString(Charsets.UTF_8)
        } catch (e: SecureStorageException) {
            errorLog(e) { "Failed to read IAM token '$identifier'; treating it as absent." }
            null
        }

    private fun writeToken(storage: SecureItemStorage?, identifier: String, value: String?) {
        try {
            storage?.modifyItem(identifier, value?.toByteArray(Charsets.UTF_8))
        } catch (e: SecureStorageException) {
            errorLog(e) { "Failed to write IAM token '$identifier'; treating it as a no-op." }
        }
    }

    // Registers callback as a waiter; returns true iff this is the first registration (should refresh).
    @Synchronized
    private fun registerRefreshWaiter(appUserID: String, callback: (TokenSet?) -> Unit): Boolean {
        val isFirstWaiter = !pendingRefreshes.containsKey(appUserID)
        pendingRefreshes.getOrPut(appUserID) { mutableListOf() }.add(callback)
        return isFirstWaiter
    }

    // Clears and returns every waiter registered for appUserID's in-flight refresh.
    @Synchronized
    private fun resolveRefreshWaiters(appUserID: String): List<(TokenSet?) -> Unit> =
        pendingRefreshes.remove(appUserID).orEmpty()

    // Unknown means "ask storage"; Known is authoritative even when its value is null.
    private sealed class Cached {
        object Unknown : Cached()
        data class Known(val value: String?) : Cached()
    }

    private data class CachedTokens(
        val accessToken: Cached = Cached.Unknown,
        val refreshToken: Cached = Cached.Unknown,
        val idToken: Cached = Cached.Unknown,
    )

    // In-memory mirror of tokens read from/written to storage, keyed by appUserID, guarded by this
    // class's monitor. Writes always overwrite; reads only fill an Unknown slot, so a slow read can't
    // clobber a newer value. Note: two overlapping *writes* for the same appUserID aren't ordered against
    // each other here (whichever completes last wins) -- a known gap, tracked separately.
    private val cachedTokens = mutableMapOf<String, CachedTokens>()

    @Synchronized
    private fun cachedAccessToken(appUserID: String): Cached =
        cachedTokens[appUserID]?.accessToken ?: Cached.Unknown

    @Synchronized
    private fun cachedRefreshToken(appUserID: String): Cached =
        cachedTokens[appUserID]?.refreshToken ?: Cached.Unknown

    @Synchronized
    private fun cachedIDToken(appUserID: String): Cached =
        cachedTokens[appUserID]?.idToken ?: Cached.Unknown

    @Synchronized
    private fun setCachedAccessToken(appUserID: String, value: String?) {
        val current = cachedTokens[appUserID] ?: CachedTokens()
        cachedTokens[appUserID] = current.copy(accessToken = Cached.Known(value))
    }

    /** Fills the access-token slot only if unclaimed -- see [cachedTokens]. */
    @Synchronized
    private fun cacheAccessTokenIfUnknown(appUserID: String, value: String?) {
        val current = cachedTokens[appUserID] ?: CachedTokens()
        if (current.accessToken == Cached.Unknown) {
            cachedTokens[appUserID] = current.copy(accessToken = Cached.Known(value))
        }
    }

    /** Fills the refresh-token slot only if unclaimed -- see [cachedTokens]. */
    @Synchronized
    private fun cacheRefreshTokenIfUnknown(appUserID: String, value: String?) {
        val current = cachedTokens[appUserID] ?: CachedTokens()
        if (current.refreshToken == Cached.Unknown) {
            cachedTokens[appUserID] = current.copy(refreshToken = Cached.Known(value))
        }
    }

    /** Fills the ID-token slot only if unclaimed -- see [cachedTokens]. */
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
        // Named so preWarmCache can strip them when parsing allItemIdentifiers() back into (appUserID, slot).
        const val ACCESS_TOKEN_PREFIX = "RC-access-"
        const val REFRESH_TOKEN_PREFIX = "RC-refresh-"
        const val ID_TOKEN_PREFIX = "RC-id-"

        fun accessTokenKey(appUserID: String) = "$ACCESS_TOKEN_PREFIX$appUserID"
        fun refreshTokenKey(appUserID: String) = "$REFRESH_TOKEN_PREFIX$appUserID"
        fun idTokenKey(appUserID: String) = "$ID_TOKEN_PREFIX$appUserID"
    }
}
