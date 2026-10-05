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
 * Tests for [TokenManager]'s storage plumbing, construction, identity introspection, authorization
 * headers, and the token refresh state machine (IAM phase 3, steps 7-10). These go through the real
 * `derivePassword -> EncryptedItemStorage.create` chain -- a real [Application] context, a real API key,
 * no test doubles for storage -- since this step's whole purpose is to own that chain correctly.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TokenManagerTest {

    private val context: Application = ApplicationProvider.getApplicationContext()

    // region disabled

    @Test
    fun `construction is skipped entirely when disabled`() = runTest {
        val manager = TokenManager(context, "test_api_key", enabled = false, scope = testScope())

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

    // region construction

    @Test
    fun `a blank API key leaves storage permanently unavailable`() = runTest {
        val manager = TokenManager(context, "   ", enabled = true, scope = testScope())

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

        // Same on-disk file, different derived key -- e.g. reconfiguring from a sandbox to a production key.
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
        // The regression this guards against: dropping an unrecognized amr entry before the anonymous check
        // ran would make this look anonymous once "some_future_provider" -- a real, non-anonymous linked
        // identity this SDK version just doesn't have a name for yet -- had been filtered out.
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
        // A disabled instance never has readable storage to begin with, so this also locks in that
        // authorizationHeaders checks `enabled` directly rather than only ever seeing "no token".
        val manager = TokenManager(context, "test_api_key", enabled = false, scope = testScope())

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
        // No Backend/HTTPClient/Dispatcher is wired into TokenManager, so a call that tried to reach the
        // network would fail to compile -- no mock needed to catch it.
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

            // Only the first caller gets a request to actually perform; the second is folded into it rather
            // than starting a redundant refresh of its own.
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

        // The in-flight state was cleared by the failure, so a fresh request is eligible to trigger a new
        // refresh rather than being folded into the (already-resolved) previous one.
        val secondRequest = manager.tokenRefreshRequest(
            "user",
            statusCode = RCHTTPStatusCodes.UNAUTHORIZED,
            alreadyRetriedRefresh = false,
        ) {}

        assertThat(secondRequest).isEqualTo(TokenManager.TokenRefreshRequest("user", "refresh-token"))
    }

    // endregion

    // region close

    @Test
    fun `close cancels the scope`() = runTest {
        val scope = testScope()
        // Never awaited, so storage construction may still be in flight when close() runs.
        val manager = TokenManager(context, "test_api_key", enabled = true, scope = scope)

        manager.close()

        assertThat(scope.isActive).isFalse()
    }

    // endregion

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun testScope(): CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    private fun manager(apiKey: String = "test_api_key"): TokenManager =
        TokenManager(context, apiKey, enabled = true, scope = testScope())

    /**
     * A syntactically-valid (but unsigned and unverified) JWT with the given [amr] claim -- or no `amr`
     * claim at all when [amr] is `null` -- suitable for exercising identity-introspection methods, which
     * only ever read that one claim out of a stored ID token.
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
