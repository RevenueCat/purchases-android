@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.identity

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.networking.TokenManager
import com.revenuecat.purchases.common.offerings.OfferingsCache
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.paywalls.PaywallAssetWarming
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.utils.SyncDispatcher
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** [IdentityManager]'s IAM behavior, against a real [TokenManager]. */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class IdentityManagerIAMTests {

    private val anonymousID = "\$RCAnonymousID:ff68f26e432648369a713849a9f93b58"
    private val identifiedID = "identified-user"

    private var cachedAppUserID: String? = null
    private lateinit var deviceCache: DeviceCache
    private lateinit var tokenManager: TokenManager
    private lateinit var identityManager: IdentityManager

    @Before
    fun setup() {
        deviceCache = mockk<DeviceCache>().apply {
            every { getCachedAppUserID() } answers { cachedAppUserID }
            every { getLegacyCachedAppUserID() } returns null
        }
    }

    // region whenIAMLoginNeeded

    @Test
    fun `IAM login is needed for an anonymous user without an access token`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()

        assertThat(iamLoginNeeded()).isTrue()
    }

    @Test
    fun `IAM login is not needed for an anonymous user with an access token`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        tokenManager.saveTokens(anonymousID, accessToken = "access", refreshToken = null, idToken = null)

        assertThat(iamLoginNeeded()).isFalse()
    }

    @Test
    fun `IAM login is not needed for an identified user without an access token`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager()

        assertThat(iamLoginNeeded()).isFalse()
    }

    @Test
    fun `IAM login is not needed for an identified user with an access token`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager()
        tokenManager.saveTokens(identifiedID, accessToken = "access", refreshToken = null, idToken = null)

        assertThat(iamLoginNeeded()).isFalse()
    }

    @Test
    fun `IAM login is never needed when IAM is disabled`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager(iamEnabled = false)

        assertThat(iamLoginNeeded()).isFalse()
    }

    @Test
    fun `IAM login need is evaluated only after the token cache loads`() = runTest {
        newTokenManager(iamEnabled = true)
            .saveTokens(anonymousID, accessToken = "stored", refreshToken = null, idToken = null)
        advanceUntilIdle()
        cachedAppUserID = anonymousID
        createIdentityManager(loaded = false)
        var calls = 0

        identityManager.whenIAMLoginNeeded { calls++ }
        advanceUntilIdle()

        // Evaluated before the load, the stored token would have been invisible.
        assertThat(calls).isZero()
    }

    // endregion

    private fun TestScope.iamLoginNeeded(): Boolean {
        var needed = false
        identityManager.whenIAMLoginNeeded { needed = true }
        advanceUntilIdle()
        return needed
    }

    private fun TestScope.createIdentityManager(iamEnabled: Boolean = true, loaded: Boolean = true) {
        tokenManager = newTokenManager(iamEnabled)
        if (loaded) advanceUntilIdle()
        identityManager = IdentityManager(
            mockk<AppConfig>(),
            deviceCache,
            mockk<SubscriberAttributesCache>(),
            mockk<SubscriberAttributesManager>(),
            mockk<OfferingsCache>(),
            mockk<RemoteConfigManager>(),
            mockk<Backend>(),
            mockk<OfflineEntitlementsManager>(),
            SyncDispatcher(),
            mockk<PaywallAssetWarming>(),
            tokenManager,
        )
    }

    private fun TestScope.newTokenManager(iamEnabled: Boolean): TokenManager {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return TokenManager(
            ApplicationProvider.getApplicationContext(),
            API_KEY,
            enabled = iamEnabled,
            computationDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        )
    }

    private companion object {
        const val API_KEY = "test_api_key"
    }
}
