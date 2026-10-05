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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.security.GeneralSecurityException

/**
 * Owns IAM login's persisted access/refresh/ID tokens.
 *
 * The API is synchronous and served from an in-memory cache. Construction builds the underlying
 * [SecureItemStorage] on [scope] and loads every stored token into the cache; until that finishes, reads
 * return `null`. Writes update the cache immediately and are persisted on [scope], in order.
 *
 * A blank API key or a [GeneralSecurityException] building storage leaves the cache unloaded for this
 * instance's lifetime (logged, not thrown). An item that can't be decrypted (e.g. left over from a different
 * API key) is treated as absent; a failed write is logged and stays in memory only.
 *
 * @param enabled whether IAM login is enabled. When `false`, storage is never constructed and every
 *   operation is a no-op.
 * @param computationDispatcher runs storage key derivation; passed to [EncryptedItemStorage.create].
 * @param ioDispatcher runs storage file I/O, and is [scope]'s default dispatcher.
 * @param scope builds storage and persists writes; cancelled by [close].
 */
@Suppress("TooManyFunctions")
internal class TokenManager(
    context: Context,
    apiKey: String,
    val enabled: Boolean,
    computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher),
) {

    // Identifier -> value. A null value is a deletion made before the load finished, so the load doesn't
    // resurrect it. Guarded by `this`, along with isLoaded and loadedCallbacks.
    private val cache = mutableMapOf<String, String?>()
    private var isLoaded = false
    private val loadedCallbacks = mutableListOf<() -> Unit>()

    // Drained in order by a single coroutine, once storage is built and loaded.
    private val pendingWrites = Channel<PendingWrite>(Channel.UNLIMITED)

    init {
        if (enabled) {
            scope.launch {
                val storage = createStorage(context, apiKey, computationDispatcher, ioDispatcher)
                if (storage != null) load(storage)
                for (write in pendingWrites) {
                    if (storage != null) persist(storage, write)
                }
            }
        }
    }

    /**
     * Runs [callback] once the cache has loaded: immediately on the calling thread if it already has, otherwise
     * on [scope] when the load finishes. Never runs when disabled or when storage is unavailable.
     */
    fun onLoaded(callback: () -> Unit) {
        if (!enabled) return
        val loaded = synchronized(this) {
            if (!isLoaded) loadedCallbacks.add(callback)
            isLoaded
        }
        if (loaded) callback()
    }

    // region Read access

    /** The current access token stored for [appUserID], or `null` if none is stored (or not yet loaded). */
    fun currentAccessToken(appUserID: String): String? = read(accessTokenKey(appUserID))

    /** The current refresh token stored for [appUserID], or `null` if none is stored (or not yet loaded). */
    fun currentRefreshToken(appUserID: String): String? = read(refreshTokenKey(appUserID))

    /** The current ID token stored for [appUserID], or `null` if none is stored (or not yet loaded). */
    fun currentIDToken(appUserID: String): String? = read(idTokenKey(appUserID))

    /** Whether an access token is currently available for [appUserID]. */
    fun hasCurrentAccessToken(appUserID: String): Boolean = currentAccessToken(appUserID) != null

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
    fun currentIdentitySources(appUserID: String): List<String>? =
        currentIDToken(appUserID)?.let { JWT.decode(it) }?.amr

    /**
     * Whether [appUserID]'s currently stored identity is anonymous: every raw value in
     * [currentIdentitySources] equals [IdentitySource.ANONYMOUS]'s raw value, and at least one is listed.
     * `false` if there's nothing to introspect ([currentIdentitySources] is `null` or empty).
     */
    fun isCurrentIdentityAnonymous(appUserID: String): Boolean {
        val sources = currentIdentitySources(appUserID)
        return !sources.isNullOrEmpty() && sources.all { it == IdentitySource.ANONYMOUS.rawValue }
    }

    /**
     * The last raw value in [currentIdentitySources], mapped through [IdentitySource.fromRawValue]. `null`
     * if there's nothing to introspect, or that last entry isn't a source this SDK version recognizes.
     */
    fun currentIdentitySource(appUserID: String): IdentitySource? =
        currentIdentitySources(appUserID)?.lastOrNull()?.let { IdentitySource.fromRawValue(it) }

    // endregion

    // region Authorization headers

    /**
     * The `Authorization` header to attach to a request made on behalf of [appUserID]: a Bearer header for
     * the current access token when [enabled] and one is available, an empty map otherwise -- including when
     * [isIAMEndpoint] is `true`, since the `/auth` endpoints always authenticate with the API key.
     */
    fun authorizationHeaders(appUserID: String, isIAMEndpoint: Boolean): Map<String, String> {
        val token = if (enabled && !isIAMEndpoint) currentAccessToken(appUserID) else null
        return token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
    }

    // endregion

    // region Bulk operations

    /**
     * Saves all three tokens for [appUserID], e.g. after login or a token refresh. A `null` [refreshToken]
     * or [idToken] clears that slot, mirroring iOS.
     */
    fun saveTokens(appUserID: String, accessToken: String, refreshToken: String?, idToken: String?) {
        write(accessTokenKey(appUserID), accessToken)
        write(refreshTokenKey(appUserID), refreshToken)
        write(idTokenKey(appUserID), idToken)
    }

    /** Clears all three token slots for [appUserID], e.g. for a full logout. */
    fun deleteTokens(appUserID: String) {
        write(accessTokenKey(appUserID), null)
        write(refreshTokenKey(appUserID), null)
        write(idTokenKey(appUserID), null)
    }

    /**
     * Clears only the access-token slot for [appUserID]. Local-only, no network call -- used to force a
     * refresh on the next request without a full logout.
     */
    fun deleteAccessToken(appUserID: String) {
        write(accessTokenKey(appUserID), null)
    }

    // endregion

    /** The tokens a successful `/auth/login` or `/auth/token` call produced. */
    data class TokenSet(val accessToken: String, val refreshToken: String?, val idToken: String?)

    /** Cancels [scope], including storage construction and any writes not yet persisted. */
    fun close() {
        pendingWrites.close()
        scope.cancel()
    }

    private fun read(identifier: String): String? =
        synchronized(this) { if (isLoaded) cache[identifier] else null }

    private fun write(identifier: String, value: String?) {
        if (!enabled) return
        synchronized(this) {
            cache[identifier] = value
            // Sent under the lock so persistence order matches cache order.
            pendingWrites.trySend(PendingWrite(identifier, value))
        }
    }

    private fun load(storage: SecureItemStorage) {
        val identifiers = try {
            storage.allItemIdentifiers()
        } catch (e: SecureStorageException) {
            errorLog(e) { "Failed to list IAM tokens; treating storage as empty." }
            emptyList()
        }
        val stored = identifiers.mapNotNull { identifier ->
            try {
                storage.readItem(identifier)?.let { identifier to it.toString(Charsets.UTF_8) }
            } catch (e: SecureStorageException) {
                errorLog(e) { "Failed to read IAM token '$identifier'; treating it as absent." }
                null
            }
        }
        val callbacks = synchronized(this) {
            // Writes made while loading are newer than what's on disk.
            stored.forEach { (identifier, value) -> if (!cache.containsKey(identifier)) cache[identifier] = value }
            isLoaded = true
            loadedCallbacks.toList().also { loadedCallbacks.clear() }
        }
        callbacks.forEach { it() }
    }

    private fun persist(storage: SecureItemStorage, write: PendingWrite) {
        try {
            storage.modifyItem(write.identifier, write.value?.toByteArray(Charsets.UTF_8))
        } catch (e: SecureStorageException) {
            errorLog(e) { "Failed to write IAM token '${write.identifier}'; it will not survive a restart." }
        }
    }

    private class PendingWrite(val identifier: String, val value: String?)

    private companion object {
        fun accessTokenKey(appUserID: String) = "RC-access-$appUserID"
        fun refreshTokenKey(appUserID: String) = "RC-refresh-$appUserID"
        fun idTokenKey(appUserID: String) = "RC-id-$appUserID"

        suspend fun createStorage(
            context: Context,
            apiKey: String,
            computationDispatcher: CoroutineDispatcher,
            ioDispatcher: CoroutineDispatcher,
        ): SecureItemStorage? {
            val password = derivePassword(apiKey) ?: return null
            return try {
                EncryptedItemStorage.create(
                    context,
                    password,
                    computationDispatcher = computationDispatcher,
                    ioDispatcher = ioDispatcher,
                )
            } catch (e: GeneralSecurityException) {
                errorLog(e) { "Failed to initialize IAM secure storage; IAM login will be unavailable." }
                null
            } finally {
                password.fill('\u0000') // caller's responsibility to zero once consumed
            }
        }
    }
}
