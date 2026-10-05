@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.identity

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.networking.TokenAPI
import com.revenuecat.purchases.common.networking.TokenManager
import com.revenuecat.purchases.common.offerings.OfferingsCache
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.paywalls.PaywallAssetWarming
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.utils.SyncDispatcher
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
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

/** [IdentityManager]'s IAM behavior, against a real [TokenManager] and a mocked [TokenAPI]. */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class IdentityManagerIAMTests {

    private val anonymousID = "\$RCAnonymousID:ff68f26e432648369a713849a9f93b58"
    private val identifiedID = "identified-user"
    private val serverID = "server-assigned-user"
    private val googleIdentity = Identity.google("google-token".toByteArray())
    private val customerInfo = mockk<CustomerInfo>()

    private var cachedAppUserID: String? = null
    private lateinit var appConfig: AppConfig
    private lateinit var deviceCache: DeviceCache
    private lateinit var subscriberAttributesCache: SubscriberAttributesCache
    private lateinit var subscriberAttributesManager: SubscriberAttributesManager
    private lateinit var offeringsCache: OfferingsCache
    private lateinit var remoteConfigManager: RemoteConfigManager
    private lateinit var backend: Backend
    private lateinit var offlineEntitlementsManager: OfflineEntitlementsManager
    private lateinit var paywallAssetWarming: PaywallAssetWarming
    private lateinit var tokenAPI: TokenAPI
    private lateinit var tokenManager: TokenManager
    private lateinit var identityManager: IdentityManager

    @Before
    fun setup() {
        appConfig = mockk<AppConfig>().apply {
            every { isAppBackgrounded } returns false
        }
        deviceCache = mockk<DeviceCache>().apply {
            every { getCachedAppUserID() } answers { cachedAppUserID }
            every { getLegacyCachedAppUserID() } returns null
            every { cacheAppUserID(any()) } answers { cachedAppUserID = firstArg() }
            every { clearCachesForAppUserID(any()) } just Runs
            every { cacheCustomerInfo(any(), any()) } just Runs
        }
        subscriberAttributesCache = mockk<SubscriberAttributesCache>().apply {
            every { clearSubscriberAttributesIfSyncedForSubscriber(any()) } just Runs
        }
        subscriberAttributesManager = mockk<SubscriberAttributesManager>().apply {
            every { synchronizeSubscriberAttributesForAllUsers(any(), any(), any()) } answers {
                thirdArg<(() -> Unit)?>()?.invoke()
            }
            every { copyUnsyncedSubscriberAttributes(any(), any()) } just Runs
        }
        offeringsCache = mockk<OfferingsCache>().apply {
            every { clearCache() } just Runs
        }
        remoteConfigManager = mockk<RemoteConfigManager>().apply {
            every { clearCache(any()) } just Runs
        }
        backend = mockk<Backend>().apply {
            every { clearCaches() } just Runs
            every { getCustomerInfo(any(), any(), any(), any()) } answers {
                thirdArg<(CustomerInfo) -> Unit>()(customerInfo)
            }
        }
        offlineEntitlementsManager = mockk<OfflineEntitlementsManager>().apply {
            every { resetOfflineCustomerInfoCache() } just Runs
        }
        paywallAssetWarming = mockk<PaywallAssetWarming>().apply {
            every { clearWebViewStorage() } just Runs
        }
        tokenAPI = mockk()
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

    // region logIn(identity)

    @Test
    fun `IAM logIn switches to the server-assigned user and returns fresh CustomerInfo`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        stubTokenLogIn(succeedWith = serverID)

        val result = logIn(googleIdentity)

        assertThat(result).isEqualTo(customerInfo to false)
        assertThat(identityManager.currentAppUserID).isEqualTo(serverID)
        verify(exactly = 1) { tokenAPI.logIn(anonymousID, googleIdentity, any(), any()) }
        verify(exactly = 1) { backend.getCustomerInfo(serverID, false, any(), any()) }
        verify(exactly = 1) { deviceCache.cacheCustomerInfo(serverID, customerInfo) }
        verify(exactly = 0) { backend.logIn(any(), any(), any(), any()) }
    }

    @Test
    fun `IAM logIn clears the old user's caches, including the ETag cache`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager()
        stubTokenLogIn(succeedWith = serverID)

        logIn(googleIdentity)

        verifyOrder {
            deviceCache.clearCachesForAppUserID(identifiedID)
            remoteConfigManager.clearCache(serverID)
            offeringsCache.clearCache()
            subscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(identifiedID)
            deviceCache.cacheAppUserID(serverID)
        }
        verify(exactly = 1) { offlineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 1) { paywallAssetWarming.clearWebViewStorage() }
        verify(exactly = 1) { backend.clearCaches() }
    }

    @Test
    fun `IAM logIn syncs the current user's attributes before logging in`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        stubTokenLogIn(succeedWith = serverID)

        logIn(googleIdentity)

        verifyOrder {
            subscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(anonymousID, any(), any())
            tokenAPI.logIn(anonymousID, googleIdentity, any(), any())
        }
    }

    @Test
    fun `IAM logIn copies attributes from an anonymous user`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        stubTokenLogIn(succeedWith = serverID)

        logIn(googleIdentity)

        verify(exactly = 1) { subscriberAttributesManager.copyUnsyncedSubscriberAttributes(anonymousID, serverID) }
    }

    @Test
    fun `IAM logIn does not copy attributes from an identified user`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager()
        stubTokenLogIn(succeedWith = serverID)

        logIn(googleIdentity)

        verify(exactly = 0) { subscriberAttributesManager.copyUnsyncedSubscriberAttributes(any(), any()) }
    }

    @Test
    fun `IAM logIn error leaves the current identity untouched`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        val error = PurchasesError(PurchasesErrorCode.InvalidCredentialsError)
        stubTokenLogIn(failWith = error)

        val result = logIn(googleIdentity)

        assertThat(result).isEqualTo(error)
        assertThat(identityManager.currentAppUserID).isEqualTo(anonymousID)
        verify(exactly = 0) { deviceCache.cacheAppUserID(any()) }
        verify(exactly = 0) { deviceCache.clearCachesForAppUserID(any()) }
        verify(exactly = 0) { backend.clearCaches() }
        verify(exactly = 0) { backend.getCustomerInfo(any(), any(), any(), any()) }
    }

    @Test
    fun `IAM logIn reports a CustomerInfo fetch failure after switching users`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        stubTokenLogIn(succeedWith = serverID)
        val error = PurchasesError(PurchasesErrorCode.NetworkError)
        every { backend.getCustomerInfo(any(), any(), any(), any()) } answers {
            arg<(PurchasesError, Boolean) -> Unit>(3)(error, false)
        }

        val result = logIn(googleIdentity)

        assertThat(result).isEqualTo(error)
        assertThat(identityManager.currentAppUserID).isEqualTo(serverID)
    }

    @Test
    fun `IAM logIn fails without touching TokenAPI when IAM is disabled`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager(iamEnabled = false)

        val result = logIn(googleIdentity)

        assertThat((result as PurchasesError).code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(identityManager.currentAppUserID).isEqualTo(anonymousID)
        verify(exactly = 0) { tokenAPI.logIn(any(), any(), any(), any()) }
    }

    @Test
    fun `IAM logIn is unsupported in UI preview mode`() = runTest {
        cachedAppUserID = IdentityManager.UI_PREVIEW_MODE_APP_USER_ID
        createIdentityManager()

        val result = logIn(googleIdentity)

        assertThat((result as PurchasesError).code).isEqualTo(PurchasesErrorCode.UnsupportedError)
        verify(exactly = 0) { tokenAPI.logIn(any(), any(), any(), any()) }
    }

    // endregion

    // Returns (CustomerInfo, created) on success, or the PurchasesError.
    private fun logIn(identity: Identity): Any? {
        var result: Any? = null
        identityManager.logIn(identity, { info, created -> result = info to created }, { result = it })
        return result
    }

    private fun stubTokenLogIn(succeedWith: String? = null, failWith: PurchasesError? = null) {
        every { tokenAPI.logIn(any(), any(), any(), any()) } answers {
            if (succeedWith != null) thirdArg<(String) -> Unit>()(succeedWith)
            if (failWith != null) arg<(PurchasesError) -> Unit>(3)(failWith)
        }
    }

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
            appConfig,
            deviceCache,
            subscriberAttributesCache,
            subscriberAttributesManager,
            offeringsCache,
            remoteConfigManager,
            backend,
            offlineEntitlementsManager,
            SyncDispatcher(),
            paywallAssetWarming,
            tokenManager,
            tokenAPI,
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
