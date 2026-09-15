@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import android.app.Application
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.identity.IdentitySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests for [TokenManager]'s storage plumbing, construction, and identity introspection (IAM phase 3, steps
 * 7-8). These deliberately go through the real `derivePassword -> EncryptedItemStorage.create` chain (a real
 * [Application] context, a real API key, no test doubles for storage) rather than mocking storage
 * construction, since [TokenManager]'s whole purpose in this step is to own that chain correctly — a mocked
 * storage would only prove the mock works, not catch integration mistakes like accidental double-derivation,
 * reading the wrong API key field, or a salt mismatch between instances.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TokenManagerTest {

    private val context: Application = ApplicationProvider.getApplicationContext()

    // region disabled

    @Test
    fun `construction is skipped entirely when disabled`() = runTest {
        val manager = TokenManager(context, "test_api_key", enabled = false, scope = testScope())
        manager.awaitStorageInitialized() // no-op: nothing was ever launched

        assertThat(manager.currentAccessToken("user")).isNull()
        assertThat(manager.currentRefreshToken("user")).isNull()
        assertThat(manager.currentIDToken("user")).isNull()
        assertThat(manager.hasCurrentAccessToken("user")).isFalse()
    }

    @Test
    fun `save-delete calls are no-ops when disabled, not crashes`() = runTest {
        val manager = TokenManager(context, "test_api_key", enabled = false, scope = testScope())

        manager.saveTokens("user", "access", "refresh", "id")
        manager.deleteTokens("user")
        manager.deleteAccessToken("user")

        assertThat(manager.currentAccessToken("user")).isNull()
    }

    @Test
    fun `close on a disabled instance does not throw`() = runTest {
        val scope = testScope()
        val manager = TokenManager(context, "test_api_key", enabled = false, scope = scope)

        manager.close()

        assertThat(scope.isActive).isFalse()
    }

    // endregion

    // region still constructing

    @Test
    fun `all operations are no-ops while storage is still constructing`() = runTest {
        val manager = TokenManager(context, "test_api_key", enabled = true, scope = testScope())

        // Storage construction was launched but genuinely hasn't resolved yet - EncryptedItemStorage.create
        // always hops onto a real background dispatcher, so this is never a race against this assertion.
        assertThat(manager.currentAccessToken("user")).isNull()
        assertThat(manager.currentRefreshToken("user")).isNull()
        assertThat(manager.currentIDToken("user")).isNull()
        assertThat(manager.hasCurrentAccessToken("user")).isFalse()

        // A write attempted before storage is ready is silently dropped, not queued.
        manager.saveTokens("user", "access", "refresh", "id")
        manager.awaitStorageInitialized()
        assertThat(manager.currentAccessToken("user")).isNull()
    }

    @Test
    fun `a blank API key leaves storage permanently unavailable`() = runTest {
        val manager = TokenManager(context, "   ", enabled = true, scope = testScope())
        manager.awaitStorageInitialized()

        assertThat(manager.currentAccessToken("user")).isNull()
        manager.saveTokens("user", "access", "refresh", "id")
        assertThat(manager.currentAccessToken("user")).isNull()
    }

    // endregion

    // region real construction chain

    @Test
    fun `storage eventually becomes readable and writable when enabled`() = runTest {
        val manager = readyManager()

        assertThat(manager.currentAccessToken("user")).isNull()
        manager.saveTokens("user", accessToken = "access-token-value", refreshToken = "refresh", idToken = "id")
        assertThat(manager.currentAccessToken("user")).isEqualTo("access-token-value")
    }

    @Test
    fun `two instances built from the same API key derive the same storage key`() = runTest {
        val first = readyManager(apiKey = "shared_api_key")
        first.saveTokens("user", accessToken = "from-first-instance", refreshToken = "refresh", idToken = "id")

        // A second instance, built independently from the same API key and context, must derive the exact
        // same encryption key to be able to read what the first instance wrote.
        val second = readyManager(apiKey = "shared_api_key")
        assertThat(second.currentAccessToken("user")).isEqualTo("from-first-instance")
    }

    @Test
    fun `two instances built from different API keys do not share readable storage`() = runTest {
        val first = readyManager(apiKey = "api_key_one")
        first.saveTokens("user", accessToken = "from-first-instance", refreshToken = "refresh", idToken = "id")

        // Different derived key: the underlying storage shares its on-disk files across TokenManager
        // instances built from the same context regardless of API key, so this hits a genuine decrypt
        // failure rather than simply finding nothing - and currentAccessToken must swallow that rather
        // than let it escape as a crash (see the dedicated tests below for that guarantee on its own).
        val second = readyManager(apiKey = "api_key_two")
        assertThat(second.currentAccessToken("user")).isNull()
    }

    // endregion

    // region read access

    @Test
    fun `tokens are isolated between users`() = runTest {
        val manager = readyManager()
        manager.saveTokens("user-a", accessToken = "a-access", refreshToken = "a-refresh", idToken = "a-id")
        manager.saveTokens("user-b", accessToken = "b-access", refreshToken = "b-refresh", idToken = "b-id")

        assertThat(manager.currentAccessToken("user-a")).isEqualTo("a-access")
        assertThat(manager.currentRefreshToken("user-a")).isEqualTo("a-refresh")
        assertThat(manager.currentAccessToken("user-b")).isEqualTo("b-access")
        assertThat(manager.currentRefreshToken("user-b")).isEqualTo("b-refresh")
    }

    @Test
    fun `hasCurrentAccessToken reflects whether an access token is stored`() = runTest {
        val manager = readyManager()
        assertThat(manager.hasCurrentAccessToken("user")).isFalse()

        manager.saveTokens("user", accessToken = "access-token", refreshToken = "refresh", idToken = "id")
        assertThat(manager.hasCurrentAccessToken("user")).isTrue()

        manager.deleteAccessToken("user")
        assertThat(manager.hasCurrentAccessToken("user")).isFalse()
    }

    // endregion

    // region resilience to unreadable stored data

    @Test
    fun `a value that can't be decrypted is treated as absent for every token, not as a crash`() = runTest {
        val first = readyManager(apiKey = "api_key_one")
        first.saveTokens("user", accessToken = "from-first-instance", refreshToken = "refresh", idToken = "id")

        // Same on-disk storage file, different derived key - e.g. reconfiguring the SDK from a sandbox to a
        // production API key. The stored ciphertext genuinely exists under this identifier; it just can't be
        // decrypted with this instance's key.
        val second = readyManager(apiKey = "api_key_two")

        assertThat(second.currentAccessToken("user")).isNull()
        assertThat(second.currentRefreshToken("user")).isNull()
        assertThat(second.currentIDToken("user")).isNull()
    }

    @Test
    fun `hasCurrentAccessToken is false, not true, for a value left over from a different API key`() = runTest {
        val first = readyManager(apiKey = "api_key_one")
        first.saveTokens("user", accessToken = "from-first-instance", refreshToken = "refresh", idToken = "id")

        val second = readyManager(apiKey = "api_key_two")

        // The identifier genuinely exists on disk (the first instance wrote it) - hasCurrentAccessToken must
        // still report false here rather than true, since currentAccessToken can never actually produce a
        // value for it under this instance's key.
        assertThat(second.hasCurrentAccessToken("user")).isFalse()
    }

    @Test
    fun `a fresh write cleanly overwrites a leftover undecryptable value for the same identifier`() = runTest {
        val first = readyManager(apiKey = "api_key_one")
        first.saveTokens("user", accessToken = "from-first-instance", refreshToken = "refresh", idToken = "id")

        val second = readyManager(apiKey = "api_key_two")
        second.saveTokens("user", accessToken = "from-second-instance", refreshToken = "refresh", idToken = "id")

        // Writing doesn't need to decrypt the existing ciphertext first, so this recovers cleanly even though
        // the identifier was previously unreadable under this instance's key.
        assertThat(second.currentAccessToken("user")).isEqualTo("from-second-instance")
        assertThat(second.hasCurrentAccessToken("user")).isTrue()
    }

    // endregion

    // region identity introspection

    @Test
    fun `currentIdentitySources, isCurrentIdentityAnonymous and currentIdentitySource are null-ish with no ID token`() =
        runTest {
            val manager = readyManager()

            assertThat(manager.currentIdentitySources("user")).isNull()
            assertThat(manager.isCurrentIdentityAnonymous("user")).isFalse()
            assertThat(manager.currentIdentitySource("user")).isNull()
        }

    @Test
    fun `a malformed ID token is handled gracefully, not as a crash`() = runTest {
        val manager = readyManager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "not-a-jwt")

        assertThat(manager.currentIdentitySources("user")).isNull()
        assertThat(manager.isCurrentIdentityAnonymous("user")).isFalse()
        assertThat(manager.currentIdentitySource("user")).isNull()
    }

    @Test
    fun `a single anonymous source is reported as anonymous`() = runTest {
        val manager = readyManager()
        manager.saveTokens(
            "user",
            accessToken = "access",
            refreshToken = "refresh",
            idToken = fakeIDToken(amr = listOf("anonymous")),
        )

        assertThat(manager.currentIdentitySources("user")).containsExactly(IdentitySource.ANONYMOUS.rawValue)
        assertThat(manager.isCurrentIdentityAnonymous("user")).isTrue()
        assertThat(manager.currentIdentitySource("user")).isEqualTo(IdentitySource.ANONYMOUS)
    }

    @Test
    fun `mixed sources - a linked identity - are not reported as anonymous`() = runTest {
        val manager = readyManager()
        manager.saveTokens(
            "user",
            accessToken = "access",
            refreshToken = "refresh",
            idToken = fakeIDToken(amr = listOf("anonymous", "google")),
        )

        assertThat(manager.currentIdentitySources("user"))
            .containsExactly(IdentitySource.ANONYMOUS.rawValue, IdentitySource.GOOGLE.rawValue)
        assertThat(manager.isCurrentIdentityAnonymous("user")).isFalse()
        assertThat(manager.currentIdentitySource("user")).isEqualTo(IdentitySource.GOOGLE)
    }

    @Test
    fun `an empty amr list yields an empty, not null, list of sources - and is not anonymous`() = runTest {
        val manager = readyManager()
        manager.saveTokens(
            "user",
            accessToken = "access",
            refreshToken = "refresh",
            idToken = fakeIDToken(amr = emptyList()),
        )

        assertThat(manager.currentIdentitySources("user")).isEmpty()
        assertThat(manager.isCurrentIdentityAnonymous("user")).isFalse()
        assertThat(manager.currentIdentitySource("user")).isNull()
    }

    @Test
    fun `an unrecognized amr entry is kept as a raw string, not dropped`() = runTest {
        val manager = readyManager()
        manager.saveTokens(
            "user",
            accessToken = "access",
            refreshToken = "refresh",
            idToken = fakeIDToken(amr = listOf("google", "some_future_provider")),
        )

        assertThat(manager.currentIdentitySources("user")).containsExactly("google", "some_future_provider")
    }

    @Test
    fun `an unrecognized, non-anonymous source alongside anonymous is not reported as anonymous`() = runTest {
        // The regression this guards against: if an unrecognized amr entry were silently dropped before
        // the anonymous check ran, this would incorrectly look anonymous once "some_future_provider" (a
        // real, non-anonymous linked identity this SDK version just doesn't have a name for yet) had been
        // filtered out of the list.
        val manager = readyManager()
        manager.saveTokens(
            "user",
            accessToken = "access",
            refreshToken = "refresh",
            idToken = fakeIDToken(amr = listOf("anonymous", "some_future_provider")),
        )

        assertThat(manager.currentIdentitySources("user")).containsExactly("anonymous", "some_future_provider")
        assertThat(manager.isCurrentIdentityAnonymous("user")).isFalse()
    }

    @Test
    fun `currentIdentitySource is the last listed source, not the first`() = runTest {
        val manager = readyManager()
        manager.saveTokens(
            "user",
            accessToken = "access",
            refreshToken = "refresh",
            idToken = fakeIDToken(amr = listOf("google", "apple")),
        )

        assertThat(manager.currentIdentitySource("user")).isEqualTo(IdentitySource.SIGN_IN_WITH_APPLE)
    }

    @Test
    fun `currentIdentitySource is null, not a stale earlier value, when the last entry is unrecognized`() = runTest {
        // Mirrors the isCurrentIdentityAnonymous regression above: the last raw entry is the one that
        // matters here, so an unrecognized one must report null rather than falling back to the last
        // *recognized* entry ("google") from earlier in the list.
        val manager = readyManager()
        manager.saveTokens(
            "user",
            accessToken = "access",
            refreshToken = "refresh",
            idToken = fakeIDToken(amr = listOf("google", "some_future_provider")),
        )

        assertThat(manager.currentIdentitySource("user")).isNull()
    }

    // endregion

    // region bulk operations

    @Test
    fun `saveTokens writes all three slots for a user`() = runTest {
        val manager = readyManager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "id")

        assertThat(manager.currentAccessToken("user")).isEqualTo("access")
        assertThat(manager.currentRefreshToken("user")).isEqualTo("refresh")
        assertThat(manager.currentIDToken("user")).isEqualTo("id")
    }

    @Test
    fun `deleteTokens clears all three slots for a user`() = runTest {
        val manager = readyManager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "id")

        manager.deleteTokens("user")

        assertThat(manager.currentAccessToken("user")).isNull()
        assertThat(manager.currentRefreshToken("user")).isNull()
        assertThat(manager.currentIDToken("user")).isNull()
        assertThat(manager.hasCurrentAccessToken("user")).isFalse()
    }

    @Test
    fun `deleteTokens for one user does not affect another user's tokens`() = runTest {
        val manager = readyManager()
        manager.saveTokens("user-a", accessToken = "a-access", refreshToken = "a-refresh", idToken = "a-id")
        manager.saveTokens("user-b", accessToken = "b-access", refreshToken = "b-refresh", idToken = "b-id")

        manager.deleteTokens("user-a")

        assertThat(manager.currentAccessToken("user-a")).isNull()
        assertThat(manager.currentRefreshToken("user-a")).isNull()
        assertThat(manager.currentIDToken("user-a")).isNull()
        assertThat(manager.currentAccessToken("user-b")).isEqualTo("b-access")
        assertThat(manager.currentRefreshToken("user-b")).isEqualTo("b-refresh")
        assertThat(manager.currentIDToken("user-b")).isEqualTo("b-id")
    }

    @Test
    fun `deleteAccessToken clears only the access-token slot`() = runTest {
        val manager = readyManager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "id")

        manager.deleteAccessToken("user")

        assertThat(manager.currentAccessToken("user")).isNull()
        assertThat(manager.currentRefreshToken("user")).isEqualTo("refresh")
        assertThat(manager.currentIDToken("user")).isEqualTo("id")
        assertThat(manager.hasCurrentAccessToken("user")).isFalse()
    }

    @Test
    fun `deleteAccessToken never touches the network - it is a purely local operation`() = runTest {
        // There is no network dependency anywhere in TokenManager to begin with; this test locks in that
        // deleteAccessToken's contract (local-only, no network call) by construction rather than by mocking:
        // TokenManager is built here with no Backend/HTTPClient/Dispatcher collaborator at all, so a call that
        // somehow tried to reach the network would fail loudly (a compile error, since none is even wired in)
        // rather than this test needing to assert a mock was never invoked.
        val manager = readyManager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "id")

        manager.deleteAccessToken("user")

        assertThat(manager.currentRefreshToken("user")).isEqualTo("refresh")
    }

    // endregion

    // region close

    @Test
    fun `close cancels the scope`() = runTest {
        val scope = testScope()
        val manager = TokenManager(context, "test_api_key", enabled = true, scope = scope)
        manager.awaitStorageInitialized()

        manager.close()

        assertThat(scope.isActive).isFalse()
    }

    @Test
    fun `close cancels in-flight storage construction`() = runTest {
        val scope = testScope()
        val manager = TokenManager(context, "test_api_key", enabled = true, scope = scope)

        // Cancel before construction has had a chance to resolve.
        manager.close()

        assertThat(scope.isActive).isFalse()
    }

    // endregion

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun testScope(): CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    private suspend fun readyManager(apiKey: String = "test_api_key"): TokenManager {
        val manager = TokenManager(context, apiKey, enabled = true, scope = testScope())
        manager.awaitStorageInitialized()
        return manager
    }

    /**
     * A syntactically-valid (but unsigned and unverified) JWT with the given [amr] claim - or no `amr`
     * claim at all when [amr] is `null` - suitable for exercising [TokenManager]'s identity-introspection
     * methods, which only ever read that one claim out of a stored ID token.
     */
    private fun fakeIDToken(amr: List<String>?): String {
        val payload = JSONObject().apply {
            if (amr != null) put("amr", JSONArray(amr))
        }
        val header = base64Url("""{"alg":"none"}""")
        val body = base64Url(payload.toString())
        val signature = base64Url("")
        return "$header.$body.$signature"
    }

    private fun base64Url(value: String): String =
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
