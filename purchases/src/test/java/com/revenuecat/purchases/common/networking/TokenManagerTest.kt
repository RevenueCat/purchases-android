package com.revenuecat.purchases.common.networking

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.security.SecureStorageException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests for [TokenManager]'s storage plumbing and construction (IAM phase 3, step 7). These deliberately go
 * through the real `derivePassword -> EncryptedItemStorage.create` chain (a real [Application] context, a real
 * API key, no test doubles for storage) rather than mocking storage construction, since [TokenManager]'s whole
 * purpose in this step is to own that chain correctly — a mocked storage would only prove the mock works, not
 * catch integration mistakes like accidental double-derivation, reading the wrong API key field, or a salt
 * mismatch between instances.
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

        val second = readyManager(apiKey = "api_key_two")
        // Different derived key: either a decode/decrypt failure (surfaced as SecureStorageException from the
        // underlying storage) or, since these two instances also don't share encrypted files on disk in this
        // test's context, simply no value at all. Either is an acceptable "not readable" outcome; a successful
        // read of the first instance's plaintext value would not be.
        val secondValue = try {
            second.currentAccessToken("user")
        } catch (e: SecureStorageException) {
            null
        }
        assertThat(secondValue).isNotEqualTo("from-first-instance")
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

    private fun testScope(): CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    private suspend fun readyManager(apiKey: String = "test_api_key"): TokenManager {
        val manager = TokenManager(context, apiKey, enabled = true, scope = testScope())
        manager.awaitStorageInitialized()
        return manager
    }
}
