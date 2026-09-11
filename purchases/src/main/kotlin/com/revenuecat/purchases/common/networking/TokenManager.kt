package com.revenuecat.purchases.common.networking

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.security.EncryptedItemStorage
import com.revenuecat.purchases.common.security.SecureItemStorage
import com.revenuecat.purchases.common.security.derivePassword
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
 * @param context the application context; the same direct-boot-aware context other file-backed singletons in
 *   this SDK use.
 * @param apiKey the SDK's configured API key, used to derive the storage's encryption password.
 * @param enabled whether IAM login is enabled for this SDK configuration (`AppConfig.iamEnabled`). When `false`,
 *   storage is never constructed and every operation below is a no-op.
 * @param scope the [CoroutineScope] storage construction runs on; constructor-injectable so tests can control
 *   or await it. Defaults to a dedicated [SupervisorJob] + [Dispatchers.IO] scope, cancelled by [close].
 */
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

    // region Per-token access

    /** The current access token stored for [appUserID], or `null` if none is stored (or storage isn't ready). */
    fun currentAccessToken(appUserID: String): String? = readToken(accessTokenKey(appUserID))

    /** The current refresh token stored for [appUserID], or `null` if none is stored (or storage isn't ready). */
    fun currentRefreshToken(appUserID: String): String? = readToken(refreshTokenKey(appUserID))

    /** The current ID token stored for [appUserID], or `null` if none is stored (or storage isn't ready). */
    fun currentIDToken(appUserID: String): String? = readToken(idTokenKey(appUserID))

    /** Sets (or, if `null`, clears) the access token stored for [appUserID]. No-op if storage isn't ready. */
    fun setCurrentAccessToken(appUserID: String, accessToken: String?) {
        writeToken(accessTokenKey(appUserID), accessToken)
    }

    /** Sets (or, if `null`, clears) the refresh token stored for [appUserID]. No-op if storage isn't ready. */
    fun setCurrentRefreshToken(appUserID: String, refreshToken: String?) {
        writeToken(refreshTokenKey(appUserID), refreshToken)
    }

    /** Sets (or, if `null`, clears) the ID token stored for [appUserID]. No-op if storage isn't ready. */
    fun setCurrentIDToken(appUserID: String, idToken: String?) {
        writeToken(idTokenKey(appUserID), idToken)
    }

    /**
     * Whether an access token is currently stored for [appUserID]. `false` if storage isn't ready, independent
     * of whether one would otherwise be present.
     */
    fun hasCurrentAccessToken(appUserID: String): Boolean =
        storage?.containsItem(accessTokenKey(appUserID)) == true

    // endregion

    // region Bulk operations

    /**
     * Saves all three tokens for [appUserID] in one call, e.g. after a successful login or token refresh.
     * No-op if storage isn't ready.
     */
    fun saveTokens(appUserID: String, accessToken: String, refreshToken: String, idToken: String) {
        setCurrentAccessToken(appUserID, accessToken)
        setCurrentRefreshToken(appUserID, refreshToken)
        setCurrentIDToken(appUserID, idToken)
    }

    /**
     * Clears all three token slots for [appUserID], e.g. for a full logout. No-op if storage isn't ready.
     */
    fun deleteTokens(appUserID: String) {
        setCurrentAccessToken(appUserID, null)
        setCurrentRefreshToken(appUserID, null)
        setCurrentIDToken(appUserID, null)
    }

    /**
     * Clears only the access-token slot for [appUserID], leaving its refresh and ID tokens untouched. Local-only
     * — never triggers a network call. Used where the SDK needs to invalidate a cached access token without a
     * full logout, e.g. forcing a refresh on the next request. No-op if storage isn't ready.
     */
    fun deleteAccessToken(appUserID: String) {
        setCurrentAccessToken(appUserID, null)
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
        storage?.readItem(identifier)?.toString(Charsets.UTF_8)

    private fun writeToken(identifier: String, value: String?) {
        storage?.modifyItem(identifier, value?.toByteArray(Charsets.UTF_8))
    }

    private companion object {
        fun accessTokenKey(appUserID: String) = "RC-access-$appUserID"
        fun refreshTokenKey(appUserID: String) = "RC-refresh-$appUserID"
        fun idTokenKey(appUserID: String) = "RC-id-$appUserID"
    }
}
