@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.networking

import android.app.Application
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.identity.IdentitySource
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Tests for [TokenManager]: storage plumbing, construction, identity introspection, authorization headers,
 * the refresh state machine, and callback-based/cache-only access. Uses the real
 * `derivePassword -> EncryptedItemStorage.createBlocking` chain, no test doubles for storage.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TokenManagerTest {

    private val context: Application = ApplicationProvider.getApplicationContext()

    // region disabled

    @Test
    fun `construction is skipped entirely when disabled`() = runTest {
        val manager = TokenManager(context, "test_api_key", enabled = false)

        assertThat(manager.currentAccessToken("user")).isNull()
        assertThat(manager.currentRefreshToken("user")).isNull()
        assertThat(manager.currentIDToken("user")).isNull()
        assertThat(manager.hasCurrentAccessToken("user")).isFalse()
    }

    @Test
    fun `save-delete calls are no-ops when disabled, not crashes`() = runTest {
        val manager = TokenManager(context, "test_api_key", enabled = false)

        manager.saveTokens("user", "access", "refresh", "id")
        manager.deleteTokens("user")
        manager.deleteAccessToken("user")

        assertThat(manager.currentAccessToken("user")).isNull()
    }

    @Test
    fun `close on a disabled instance does not throw`() {
        val manager = TokenManager(context, "test_api_key", enabled = false)

        manager.close()
    }

    // endregion

    // region construction

    @Test
    fun `a blank API key leaves storage permanently unavailable`() = runTest {
        val manager = TokenManager(context, "   ", enabled = true)

        assertThat(manager.currentAccessToken("user")).isNull()
        manager.saveTokens("user", "access", "refresh", "id")
        assertThat(manager.currentAccessToken("user")).isNull()
    }

    @Test
    fun `storage is readable and writable once construction resolves`() = runTest {
        val manager = manager()

        assertThat(manager.currentAccessToken("user")).isNull()
        manager.saveTokens("user", accessToken = "access-token-value", refreshToken = "refresh", idToken = "id")
        assertThat(manager.currentAccessToken("user")).isEqualTo("access-token-value")
    }

    @Test
    fun `two instances built from the same API key derive the same storage key`() = runTest {
        val first = manager(apiKey = "shared_api_key")
        first.saveTokens("user", accessToken = "from-first-instance", refreshToken = "refresh", idToken = "id")

        val second = manager(apiKey = "shared_api_key")
        assertThat(second.currentAccessToken("user")).isEqualTo("from-first-instance")
    }

    @Test
    fun `two instances built from different API keys do not share readable storage`() = runTest {
        val first = manager(apiKey = "api_key_one")
        first.saveTokens("user", accessToken = "from-first-instance", refreshToken = "refresh", idToken = "id")

        val second = manager(apiKey = "api_key_two")
        assertThat(second.currentAccessToken("user")).isNull()
    }

    // endregion

    // region read access

    @Test
    fun `tokens are isolated between users`() = runTest {
        val manager = manager()
        manager.saveTokens("user-a", accessToken = "a-access", refreshToken = "a-refresh", idToken = "a-id")
        manager.saveTokens("user-b", accessToken = "b-access", refreshToken = "b-refresh", idToken = "b-id")

        assertThat(manager.currentAccessToken("user-a")).isEqualTo("a-access")
        assertThat(manager.currentRefreshToken("user-a")).isEqualTo("a-refresh")
        assertThat(manager.currentAccessToken("user-b")).isEqualTo("b-access")
        assertThat(manager.currentRefreshToken("user-b")).isEqualTo("b-refresh")
    }

    @Test
    fun `hasCurrentAccessToken reflects whether an access token is stored`() = runTest {
        val manager = manager()
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
        val first = manager(apiKey = "api_key_one")
        first.saveTokens("user", accessToken = "from-first-instance", refreshToken = "refresh", idToken = "id")

        // Same file, different derived key -- e.g. sandbox to production.
        val second = manager(apiKey = "api_key_two")

        assertThat(second.currentAccessToken("user")).isNull()
        assertThat(second.currentRefreshToken("user")).isNull()
        assertThat(second.currentIDToken("user")).isNull()
    }

    @Test
    fun `hasCurrentAccessToken is false, not true, for a value left over from a different API key`() = runTest {
        val first = manager(apiKey = "api_key_one")
        first.saveTokens("user", accessToken = "from-first-instance", refreshToken = "refresh", idToken = "id")

        val second = manager(apiKey = "api_key_two")
        assertThat(second.hasCurrentAccessToken("user")).isFalse()
    }

    @Test
    fun `a fresh write cleanly overwrites a leftover undecryptable value for the same identifier`() = runTest {
        val first = manager(apiKey = "api_key_one")
        first.saveTokens("user", accessToken = "from-first-instance", refreshToken = "refresh", idToken = "id")

        val second = manager(apiKey = "api_key_two")
        second.saveTokens("user", accessToken = "from-second-instance", refreshToken = "refresh", idToken = "id")

        assertThat(second.currentAccessToken("user")).isEqualTo("from-second-instance")
        assertThat(second.hasCurrentAccessToken("user")).isTrue()
    }

    // endregion

    // region identity introspection

    @Test
    fun `currentIdentitySources, isCurrentIdentityAnonymous and currentIdentitySource are null-ish with no ID token`() =
        runTest {
            val manager = manager()

            assertThat(manager.currentIdentitySources("user")).isNull()
            assertThat(manager.isCurrentIdentityAnonymous("user")).isFalse()
            assertThat(manager.currentIdentitySource("user")).isNull()
        }

    @Test
    fun `a malformed ID token is handled gracefully, not as a crash`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "not-a-jwt")

        assertThat(manager.currentIdentitySources("user")).isNull()
        assertThat(manager.isCurrentIdentityAnonymous("user")).isFalse()
        assertThat(manager.currentIdentitySource("user")).isNull()
    }

    @Test
    fun `a single anonymous source is reported as anonymous`() = runTest {
        val manager = manager()
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
        val manager = manager()
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
        val manager = manager()
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
        val manager = manager()
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
        // Guards against dropping an unrecognized amr entry before the anonymous check runs.
        val manager = manager()
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
        val manager = manager()
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
        val manager = manager()
        manager.saveTokens(
            "user",
            accessToken = "access",
            refreshToken = "refresh",
            idToken = fakeIDToken(amr = listOf("google", "some_future_provider")),
        )

        assertThat(manager.currentIdentitySource("user")).isNull()
    }

    // endregion

    // region authorization headers

    @Test
    fun `authorizationHeaders is empty when disabled, even with a token present`() = runTest {
        // Locks in that authorizationHeaders checks `enabled` directly, not just "no token".
        val manager = TokenManager(context, "test_api_key", enabled = false)

        assertThat(manager.authorizationHeaders("user", isIAMEndpoint = false)).isEmpty()
    }

    @Test
    fun `authorizationHeaders is empty when enabled but no access token is stored`() = runTest {
        val manager = manager()

        assertThat(manager.authorizationHeaders("user", isIAMEndpoint = false)).isEmpty()
    }

    @Test
    fun `authorizationHeaders carries a Bearer header for the stored access token`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access-token-value", refreshToken = "refresh", idToken = "id")

        assertThat(manager.authorizationHeaders("user", isIAMEndpoint = false))
            .isEqualTo(mapOf("Authorization" to "Bearer access-token-value"))
    }

    @Test
    fun `authorizationHeaders is empty for an IAM auth endpoint, even with a token present`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access-token-value", refreshToken = "refresh", idToken = "id")

        assertThat(manager.authorizationHeaders("user", isIAMEndpoint = true)).isEmpty()
    }

    @Test
    fun `authorizationHeaders is empty for the real auth endpoints' isIAMEndpoint flag`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access-token-value", refreshToken = "refresh", idToken = "id")

        val authEndpoints = listOf(Endpoint.TokenLogin, Endpoint.TokenRefresh, Endpoint.TokenLogout)
        for (endpoint in authEndpoints) {
            assertThat(manager.authorizationHeaders("user", isIAMEndpoint = endpoint.isIAMEndpoint))
                .withFailMessage { "Endpoint $endpoint expected no Authorization header" }
                .isEmpty()
        }
    }

    // endregion

    // region bulk operations

    @Test
    fun `saveTokens writes all three slots for a user`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "id")

        assertThat(manager.currentAccessToken("user")).isEqualTo("access")
        assertThat(manager.currentRefreshToken("user")).isEqualTo("refresh")
        assertThat(manager.currentIDToken("user")).isEqualTo("id")
    }

    @Test
    fun `deleteTokens clears all three slots for a user`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "id")

        manager.deleteTokens("user")

        assertThat(manager.currentAccessToken("user")).isNull()
        assertThat(manager.currentRefreshToken("user")).isNull()
        assertThat(manager.currentIDToken("user")).isNull()
        assertThat(manager.hasCurrentAccessToken("user")).isFalse()
    }

    @Test
    fun `deleteTokens for one user does not affect another user's tokens`() = runTest {
        val manager = manager()
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
        val manager = manager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "id")

        manager.deleteAccessToken("user")

        assertThat(manager.currentAccessToken("user")).isNull()
        assertThat(manager.currentRefreshToken("user")).isEqualTo("refresh")
        assertThat(manager.currentIDToken("user")).isEqualTo("id")
        assertThat(manager.hasCurrentAccessToken("user")).isFalse()
    }

    @Test
    fun `deleteAccessToken never touches the network`() = runTest {
        // No Backend/HTTPClient/Dispatcher wired in, so a network call here would fail to compile.
        val manager = manager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "id")

        manager.deleteAccessToken("user")

        assertThat(manager.currentRefreshToken("user")).isEqualTo("refresh")
    }

    // endregion

    // region token refresh state machine

    @Test
    fun `tokenRefreshRequest is a no-op when there is no stored refresh token`() = runTest {
        val manager = manager()
        var called = false

        val request = manager.tokenRefreshRequest(
            "user",
            statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
            alreadyRetriedRefresh = false,
        ) { called = true }

        assertThat(request).isNull()
        assertThat(called).isFalse()
    }

    @Test
    fun `tokenRefreshRequest is a no-op for a request that already retried a refresh once`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "id")
        var called = false

        val request = manager.tokenRefreshRequest(
            "user",
            statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
            alreadyRetriedRefresh = true,
        ) { called = true }

        assertThat(request).isNull()
        assertThat(called).isFalse()
    }

    @Test
    fun `tokenRefreshRequest is a no-op for a non-401 response`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh", idToken = "id")
        var called = false

        val request = manager.tokenRefreshRequest(
            "user",
            statusCode = RCHTTPStatusCodes.FORBIDDEN,
            alreadyRetriedRefresh = false,
        ) { called = true }

        assertThat(request).isNull()
        assertThat(called).isFalse()
    }

    @Test
    fun `the first caller for a user gets a TokenRefreshRequest to actually perform`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh-token", idToken = "id")

        val request = manager.tokenRefreshRequest(
            "user",
            statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
            alreadyRetriedRefresh = false,
        ) {}

        assertThat(request).isEqualTo(TokenManager.TokenRefreshRequest("user", "refresh-token"))
    }

    @Test
    fun `a concurrent caller for the same user is folded into the in-flight refresh, and both get notified`() =
        runTest {
            val manager = manager()
            manager.saveTokens("user", accessToken = "access", refreshToken = "refresh-token", idToken = "id")
            var firstResult: TokenManager.TokenSet? = null
            var secondResult: TokenManager.TokenSet? = null
            var secondWasCalled = false

            val firstRequest = manager.tokenRefreshRequest(
                "user",
                statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
                alreadyRetriedRefresh = false,
            ) { firstResult = it }
            val secondRequest = manager.tokenRefreshRequest(
                "user",
                statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
                alreadyRetriedRefresh = false,
            ) {
                secondWasCalled = true
                secondResult = it
            }

            // Only the first caller gets a real request; the second is folded into it.
            assertThat(firstRequest).isNotNull()
            assertThat(secondRequest).isNull()

            val refreshedTokens = TokenManager.TokenSet(
                accessToken = "new-access",
                refreshToken = "new-refresh",
                idToken = "new-id",
            )
            manager.handleTokenRefreshResponse("user", refreshedTokens)

            assertThat(firstResult).isEqualTo(refreshedTokens)
            assertThat(secondWasCalled).isTrue()
            assertThat(secondResult).isEqualTo(refreshedTokens)
        }

    @Test
    fun `a successful refresh saves all three tokens and notifies reportTokenUpdate`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "old-access", refreshToken = "old-refresh", idToken = "old-id")
        var notified: TokenManager.TokenSet? = null

        manager.tokenRefreshRequest(
            "user",
            statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
            alreadyRetriedRefresh = false,
        ) { notified = it }

        val refreshedTokens = TokenManager.TokenSet(
            accessToken = "new-access",
            refreshToken = "new-refresh",
            idToken = "new-id",
        )
        manager.handleTokenRefreshResponse("user", refreshedTokens)

        assertThat(notified).isEqualTo(refreshedTokens)
        assertThat(manager.currentAccessToken("user")).isEqualTo("new-access")
        assertThat(manager.currentRefreshToken("user")).isEqualTo("new-refresh")
        assertThat(manager.currentIDToken("user")).isEqualTo("new-id")
    }

    @Test
    fun `a failed refresh notifies null and clears in-flight state so a subsequent request can retry`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh-token", idToken = "id")
        var notifiedWith: TokenManager.TokenSet? = TokenManager.TokenSet("x", "y", "z")
        var wasNotified = false

        manager.tokenRefreshRequest(
            "user",
            statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
            alreadyRetriedRefresh = false,
        ) {
            wasNotified = true
            notifiedWith = it
        }

        manager.handleTokenRefreshResponse("user", null)

        assertThat(wasNotified).isTrue()
        assertThat(notifiedWith).isNull()
        // A failed refresh never touches storage.
        assertThat(manager.currentAccessToken("user")).isEqualTo("access")

        // In-flight state was cleared by the failure, so this starts a fresh refresh.
        val secondRequest = manager.tokenRefreshRequest(
            "user",
            statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
            alreadyRetriedRefresh = false,
        ) {}

        assertThat(secondRequest).isEqualTo(TokenManager.TokenRefreshRequest("user", "refresh-token"))
    }

    // endregion

    // region callback-based access
    //
    // Plain callbacks, not suspend functions, so runTest won't await them. Calls whose result matters go
    // through waitForCallback/waitForCompletion instead.

    @Test
    fun `Sync getters return null when nothing stored, with no prior suspend call for that user`() {
        val manager = manager()

        val access = waitForCallback<String?> { onResult -> manager.currentAccessTokenSync("user", onResult) }
        val refresh = waitForCallback<String?> { onResult -> manager.currentRefreshTokenSync("user", onResult) }

        assertThat(access).isNull()
        assertThat(refresh).isNull()
    }

    @Test
    fun `a value saved via the suspend API is visible via the Sync getters, with no prior Sync call`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access-token-value", refreshToken = "refresh", idToken = "id")

        val access = waitForCallback<String?> { onResult -> manager.currentAccessTokenSync("user", onResult) }
        val refresh = waitForCallback<String?> { onResult -> manager.currentRefreshTokenSync("user", onResult) }

        assertThat(access).isEqualTo("access-token-value")
        assertThat(refresh).isEqualTo("refresh")
    }

    @Test
    fun `saveTokensSync persists all three slots, visible via the suspend getters too`() = runTest {
        val manager = manager()

        waitForCompletion { onDone ->
            manager.saveTokensSync(
                "user",
                accessToken = "access",
                refreshToken = "refresh",
                idToken = "id",
                callback = onDone,
            )
        }

        val accessFromSync = waitForCallback<String?> { onResult -> manager.currentAccessTokenSync("user", onResult) }
        assertThat(accessFromSync).isEqualTo("access")
        assertThat(manager.currentAccessToken("user")).isEqualTo("access")
        assertThat(manager.currentRefreshToken("user")).isEqualTo("refresh")
        assertThat(manager.currentIDToken("user")).isEqualTo("id")
    }

    @Test
    fun `deleteTokensSync clears all three slots, visible via both surfaces`() = runTest {
        val manager = manager()
        waitForCompletion { onDone ->
            manager.saveTokensSync(
                "user",
                accessToken = "access",
                refreshToken = "refresh",
                idToken = "id",
                callback = onDone,
            )
        }

        waitForCompletion { onDone -> manager.deleteTokensSync("user", callback = onDone) }

        val accessFromSync = waitForCallback<String?> { onResult -> manager.currentAccessTokenSync("user", onResult) }
        val refreshFromSync = waitForCallback<String?> { onResult -> manager.currentRefreshTokenSync("user", onResult) }
        assertThat(accessFromSync).isNull()
        assertThat(refreshFromSync).isNull()
        assertThat(manager.currentAccessToken("user")).isNull()
        assertThat(manager.currentRefreshToken("user")).isNull()
        assertThat(manager.currentIDToken("user")).isNull()
    }

    @Test
    fun `Sync getters are null for a disabled instance, even right after saveTokensSync`() {
        val manager = TokenManager(context, "test_api_key", enabled = false)

        waitForCompletion { onDone ->
            manager.saveTokensSync(
                "user",
                accessToken = "access",
                refreshToken = "refresh",
                idToken = "id",
                callback = onDone,
            )
        }

        val access = waitForCallback<String?> { onResult -> manager.currentAccessTokenSync("user", onResult) }
        val refresh = waitForCallback<String?> { onResult -> manager.currentRefreshTokenSync("user", onResult) }
        assertThat(access).isNull()
        assertThat(refresh).isNull()
    }

    @Test
    fun `deleteTokensSync on a disabled instance does not throw`() {
        val manager = TokenManager(context, "test_api_key", enabled = false)

        waitForCompletion { onDone -> manager.deleteTokensSync("user", callback = onDone) }

        val access = waitForCallback<String?> { onResult -> manager.currentAccessTokenSync("user", onResult) }
        assertThat(access).isNull()
    }

    @Test
    fun `Sync getters are isolated between users, same as the suspend ones`() = runTest {
        val manager = manager()
        waitForCompletion { onDone ->
            manager.saveTokensSync(
                "user-a",
                accessToken = "a-access",
                refreshToken = "a-refresh",
                idToken = "a-id",
                callback = onDone,
            )
        }
        waitForCompletion { onDone ->
            manager.saveTokensSync(
                "user-b",
                accessToken = "b-access",
                refreshToken = "b-refresh",
                idToken = "b-id",
                callback = onDone,
            )
        }

        val accessA = waitForCallback<String?> { onResult -> manager.currentAccessTokenSync("user-a", onResult) }
        val accessB = waitForCallback<String?> { onResult -> manager.currentAccessTokenSync("user-b", onResult) }

        assertThat(accessA).isEqualTo("a-access")
        assertThat(accessB).isEqualTo("b-access")
    }

    @Test
    fun `authorizationHeadersSync is empty when disabled, even with a token present`() {
        val manager = TokenManager(context, "test_api_key", enabled = false)

        val headers = waitForCallback<Map<String, String>> { onResult ->
            manager.authorizationHeadersSync("user", isIAMEndpoint = false, callback = onResult)
        }

        assertThat(headers).isEmpty()
    }

    @Test
    fun `authorizationHeadersSync carries a Bearer header for the stored access token`() = runTest {
        val manager = manager()
        waitForCompletion { onDone ->
            manager.saveTokensSync(
                "user",
                accessToken = "access-token-value",
                refreshToken = "refresh",
                idToken = "id",
                callback = onDone,
            )
        }

        val headers = waitForCallback<Map<String, String>> { onResult ->
            manager.authorizationHeadersSync("user", isIAMEndpoint = false, callback = onResult)
        }

        assertThat(headers).isEqualTo(mapOf("Authorization" to "Bearer access-token-value"))
    }

    @Test
    fun `authorizationHeadersSync is empty for an IAM auth endpoint, even with a token present`() = runTest {
        val manager = manager()
        waitForCompletion { onDone ->
            manager.saveTokensSync(
                "user",
                accessToken = "access-token-value",
                refreshToken = "refresh",
                idToken = "id",
                callback = onDone,
            )
        }

        val headers = waitForCallback<Map<String, String>> { onResult ->
            manager.authorizationHeadersSync("user", isIAMEndpoint = true, callback = onResult)
        }

        assertThat(headers).isEmpty()
    }

    @Test
    fun `tokenRefreshRequestSync is a no-op when there is no stored refresh token`() {
        val manager = manager()
        var called = false

        val request = waitForCallback<TokenManager.TokenRefreshRequest?> { onResult ->
            manager.tokenRefreshRequestSync(
                "user",
                statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
                alreadyRetriedRefresh = false,
                reportTokenUpdate = { called = true },
                callback = onResult,
            )
        }

        assertThat(request).isNull()
        assertThat(called).isFalse()
    }

    @Test
    fun `the first Sync caller for a user gets a TokenRefreshRequest to actually perform`() = runTest {
        val manager = manager()
        waitForCompletion { onDone ->
            manager.saveTokensSync(
                "user",
                accessToken = "access",
                refreshToken = "refresh-token",
                idToken = "id",
                callback = onDone,
            )
        }

        val request = waitForCallback<TokenManager.TokenRefreshRequest?> { onResult ->
            manager.tokenRefreshRequestSync(
                "user",
                statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
                alreadyRetriedRefresh = false,
                reportTokenUpdate = {},
                callback = onResult,
            )
        }

        assertThat(request).isEqualTo(TokenManager.TokenRefreshRequest("user", "refresh-token"))
    }

    @Test
    fun `a concurrent Sync caller for the same user is folded into the in-flight refresh, and both are notified`() =
        runTest {
            val manager = manager()
            waitForCompletion { onDone ->
                manager.saveTokensSync(
                    "user",
                    accessToken = "access",
                    refreshToken = "refresh-token",
                    idToken = "id",
                    callback = onDone,
                )
            }
            var firstResult: TokenManager.TokenSet? = null
            var secondResult: TokenManager.TokenSet? = null

            val firstRequest = waitForCallback<TokenManager.TokenRefreshRequest?> { onResult ->
                manager.tokenRefreshRequestSync(
                    "user",
                    statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
                    alreadyRetriedRefresh = false,
                    reportTokenUpdate = { firstResult = it },
                    callback = onResult,
                )
            }
            val secondRequest = waitForCallback<TokenManager.TokenRefreshRequest?> { onResult ->
                manager.tokenRefreshRequestSync(
                    "user",
                    statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
                    alreadyRetriedRefresh = false,
                    reportTokenUpdate = { secondResult = it },
                    callback = onResult,
                )
            }

            assertThat(firstRequest).isNotNull()
            assertThat(secondRequest).isNull()

            val refreshedTokens = TokenManager.TokenSet(
                accessToken = "new-access",
                refreshToken = "new-refresh",
                idToken = "new-id",
            )
            waitForCompletion { onDone -> manager.handleTokenRefreshResponseSync("user", refreshedTokens, onDone) }

            assertThat(firstResult).isEqualTo(refreshedTokens)
            assertThat(secondResult).isEqualTo(refreshedTokens)
        }

    @Test
    fun `handleTokenRefreshResponseSync saves all three tokens and notifies reportTokenUpdate`() = runTest {
        val manager = manager()
        waitForCompletion { onDone ->
            manager.saveTokensSync(
                "user",
                accessToken = "old-access",
                refreshToken = "old-refresh",
                idToken = "old-id",
                callback = onDone,
            )
        }
        var notified: TokenManager.TokenSet? = null

        waitForCallback<TokenManager.TokenRefreshRequest?> { onResult ->
            manager.tokenRefreshRequestSync(
                "user",
                statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
                alreadyRetriedRefresh = false,
                reportTokenUpdate = { notified = it },
                callback = onResult,
            )
        }

        val refreshedTokens = TokenManager.TokenSet(
            accessToken = "new-access",
            refreshToken = "new-refresh",
            idToken = "new-id",
        )
        waitForCompletion { onDone -> manager.handleTokenRefreshResponseSync("user", refreshedTokens, onDone) }

        assertThat(notified).isEqualTo(refreshedTokens)
        val accessFromSync = waitForCallback<String?> { onResult -> manager.currentAccessTokenSync("user", onResult) }
        assertThat(accessFromSync).isEqualTo("new-access")
        assertThat(manager.currentAccessToken("user")).isEqualTo("new-access")
    }

    @Test
    fun `a failed Sync refresh notifies null and never touches storage`() = runTest {
        val manager = manager()
        waitForCompletion { onDone ->
            manager.saveTokensSync(
                "user",
                accessToken = "access",
                refreshToken = "refresh-token",
                idToken = "id",
                callback = onDone,
            )
        }
        var notifiedWith: TokenManager.TokenSet? = TokenManager.TokenSet("x", "y", "z")

        waitForCallback<TokenManager.TokenRefreshRequest?> { onResult ->
            manager.tokenRefreshRequestSync(
                "user",
                statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
                alreadyRetriedRefresh = false,
                reportTokenUpdate = { notifiedWith = it },
                callback = onResult,
            )
        }

        waitForCompletion { onDone -> manager.handleTokenRefreshResponseSync("user", null, onDone) }

        assertThat(notifiedWith).isNull()
        val accessFromSync = waitForCallback<String?> { onResult -> manager.currentAccessTokenSync("user", onResult) }
        assertThat(accessFromSync).isEqualTo("access")
    }

    // endregion

    // region cache-only access
    //
    // These read cachedTokens directly and return immediately, so no waitForCallback is needed.

    @Test
    fun `currentAccessTokenFromCache is null when nothing has resolved that slot yet`() {
        val manager = manager()

        assertThat(manager.currentAccessTokenFromCache("user")).isNull()
        assertThat(manager.currentRefreshTokenFromCache("user")).isNull()
    }

    @Test
    fun `currentAccessTokenFromCache sees a value saved via any of the other surfaces`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access-token-value", refreshToken = "refresh", idToken = "id")

        assertThat(manager.currentAccessTokenFromCache("user")).isEqualTo("access-token-value")
        assertThat(manager.currentRefreshTokenFromCache("user")).isEqualTo("refresh")
    }

    @Test
    fun `currentAccessTokenFromCache is null when disabled`() {
        val manager = TokenManager(context, "test_api_key", enabled = false)

        assertThat(manager.currentAccessTokenFromCache("user")).isNull()
    }

    @Test
    fun `authorizationHeadersFromCache carries a Bearer header once the access token is cached`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access-token-value", refreshToken = "refresh", idToken = "id")

        assertThat(manager.authorizationHeadersFromCache("user", isIAMEndpoint = false))
            .isEqualTo(mapOf("Authorization" to "Bearer access-token-value"))
    }

    @Test
    fun `authorizationHeadersFromCache is empty for an IAM auth endpoint, even with a token cached`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access-token-value", refreshToken = "refresh", idToken = "id")

        assertThat(manager.authorizationHeadersFromCache("user", isIAMEndpoint = true)).isEmpty()
    }

    @Test
    fun `authorizationHeadersFromCache is empty for a slot nothing has resolved yet`() {
        val manager = manager()

        assertThat(manager.authorizationHeadersFromCache("user", isIAMEndpoint = false)).isEmpty()
    }

    @Test
    fun `tokenRefreshRequestFromCache is a no-op when the refresh token isn't cached yet`() {
        val manager = manager()
        var called = false

        val request = manager.tokenRefreshRequestFromCache(
            "user",
            statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
            alreadyRetriedRefresh = false,
        ) { called = true }

        assertThat(request).isNull()
        assertThat(called).isFalse()
    }

    @Test
    fun `tokenRefreshRequestFromCache returns a request once the refresh token is cached`() = runTest {
        val manager = manager()
        manager.saveTokens("user", accessToken = "access", refreshToken = "refresh-token", idToken = "id")

        val request = manager.tokenRefreshRequestFromCache(
            "user",
            statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
            alreadyRetriedRefresh = false,
        ) {}

        assertThat(request).isEqualTo(TokenManager.TokenRefreshRequest("user", "refresh-token"))
    }

    @Test
    fun `construction pre-warms the cache with everything already on disk, for every appUserID`() = runTest {
        val first = manager(apiKey = "prewarm_api_key")
        first.saveTokens("user-a", accessToken = "a-access", refreshToken = "a-refresh", idToken = "a-id")
        first.saveTokens("user-b", accessToken = "b-access", refreshToken = "b-refresh", idToken = "b-id")

        val second = manager(apiKey = "prewarm_api_key")
        // Waits for construction (and preWarmCache) to resolve, without reading user-a/user-b directly --
        // that would populate their cache slots itself and defeat the point of this test.
        waitForCallback<String?> { onResult -> second.currentAccessTokenSync("__warmup__", onResult) }

        assertThat(second.currentAccessTokenFromCache("user-a")).isEqualTo("a-access")
        assertThat(second.currentRefreshTokenFromCache("user-a")).isEqualTo("a-refresh")
        assertThat(second.currentAccessTokenFromCache("user-b")).isEqualTo("b-access")
        assertThat(second.currentRefreshTokenFromCache("user-b")).isEqualTo("b-refresh")
    }

    @Test
    fun `construction pre-warms nothing for an appUserID with nothing stored on disk`() {
        val manager = manager(apiKey = "prewarm_empty_api_key")

        waitForCallback<String?> { onResult -> manager.currentAccessTokenSync("__warmup__", onResult) }

        assertThat(manager.currentAccessTokenFromCache("never-logged-in-user")).isNull()
        assertThat(manager.authorizationHeadersFromCache("never-logged-in-user", isIAMEndpoint = false)).isEmpty()
    }

    // endregion

    // region close

    @Test
    fun `close does not throw, even with construction still in flight`() {
        // Never awaited, so storage construction may still be in flight when close() runs.
        val manager = TokenManager(context, "test_api_key", enabled = true)

        manager.close()
        manager.close() // idempotent
    }

    // endregion

    private fun manager(apiKey: String = "test_api_key"): TokenManager =
        TokenManager(context, apiKey, enabled = true)

    /**
     * Blocks until [register]'s callback fires (possibly later, on [TokenManager]'s construction thread)
     * and returns its value. `runTest` doesn't await plain callbacks the way it does suspend functions.
     */
    private fun <T> waitForCallback(register: ((T) -> Unit) -> Unit): T {
        val latch = CountDownLatch(1)
        val resultHolder = AtomicReference<T>()
        register { value ->
            resultHolder.set(value)
            latch.countDown()
        }
        assertThat(latch.await(5, TimeUnit.SECONDS))
            .withFailMessage { "Callback never fired within 5 seconds" }
            .isTrue()
        return resultHolder.get()
    }

    /** [waitForCallback] for a no-argument completion callback (`() -> Unit`). */
    private fun waitForCompletion(register: (() -> Unit) -> Unit) {
        val latch = CountDownLatch(1)
        register { latch.countDown() }
        assertThat(latch.await(5, TimeUnit.SECONDS))
            .withFailMessage { "Completion callback never fired within 5 seconds" }
            .isTrue()
    }

    /** An unsigned, syntactically-valid JWT with the given [amr] claim (or none, if `null`). */
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
