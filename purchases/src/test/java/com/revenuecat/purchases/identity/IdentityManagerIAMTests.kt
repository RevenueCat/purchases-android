@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.identity

import android.content.SharedPreferences
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.createCustomerInfo
import com.revenuecat.purchases.common.networking.TokenAPI
import com.revenuecat.purchases.common.networking.TokenManager
import com.revenuecat.purchases.common.offerings.OfferingsCache
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.paywalls.PaywallAssetWarming
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.utils.Responses
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
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** [IdentityManager]'s IAM behavior, against a real [TokenManager] and a mocked [TokenAPI]. */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@Suppress("LargeClass")
class IdentityManagerIAMTests {

    private val anonymousID = "\$RCAnonymousID:ff68f26e432648369a713849a9f93b58"
    private val identifiedID = "identified-user"
    private val serverID = "server-assigned-user"
    private val googleIdentity = Identity.google("google-token".toByteArray())
    private val customerInfo = mockk<CustomerInfo>()
    private val editor = mockk<SharedPreferences.Editor>().apply { every { apply() } just Runs }

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
            every { getCachedCustomerInfo(any()) } returns null
            every { startEditing() } returns editor
            every { cacheAppUserID(any(), editor) } answers {
                cachedAppUserID = firstArg()
                editor
            }
            every { cleanupOldAttributionData() } just Runs
        }
        subscriberAttributesCache = mockk<SubscriberAttributesCache>().apply {
            every { clearSubscriberAttributesIfSyncedForSubscriber(any()) } just Runs
            every { cleanUpSubscriberAttributeCache(any(), editor) } just Runs
        }
        subscriberAttributesManager = mockk<SubscriberAttributesManager>().apply {
            every { synchronizeSubscriberAttributesForAllUsers(any(), any(), any()) } answers {
                thirdArg<(() -> Unit)?>()?.invoke()
            }
            every { copyUnsyncedSubscriberAttributes(any(), any()) } just Runs
            every { mergeAttributes(any(), any()) } just Runs
        }
        offeringsCache = mockk<OfferingsCache>().apply {
            every { clearCache() } just Runs
        }
        remoteConfigManager = mockk<RemoteConfigManager>().apply {
            every { clearCache(any()) } just Runs
        }
        backend = mockk<Backend>().apply {
            every { clearCaches() } just Runs
            every { verificationMode } returns SignatureVerificationMode.Disabled
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

    // region checkIAMLoginNeeded

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
    fun `IAM login is never needed when IAM is disabled, and that's reported right away`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager(iamEnabled = false, loaded = false)
        var needed: Boolean? = null

        identityManager.checkIAMLoginNeeded { needed = it }

        assertThat(needed).isFalse()
    }

    @Test
    fun `IAM login is not needed when token storage is unavailable`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager(apiKey = "   ")

        assertThat(iamLoginNeeded()).isFalse()
    }

    @Test
    fun `IAM login need is evaluated only after the token cache loads`() = runTest {
        newTokenManager(iamEnabled = true)
            .saveTokens(anonymousID, accessToken = "stored", refreshToken = null, idToken = null)
        advanceUntilIdle()
        cachedAppUserID = anonymousID
        createIdentityManager(loaded = false)
        val results = mutableListOf<Boolean>()

        identityManager.checkIAMLoginNeeded { results.add(it) }
        assertThat(results).isEmpty()
        advanceUntilIdle()

        // Evaluated before the load, the stored token would have been invisible.
        assertThat(results).containsExactly(false)
    }

    // endregion

    // region logIn(identity)

    @Test
    fun `IAM logIn switches to the server-assigned user and returns fresh CustomerInfo`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        stubTokenLogIn(succeedWith = serverID)

        val result = logIn(googleIdentity)

        assertThat(result).isEqualTo(customerInfo to serverID)
        assertThat(identityManager.currentAppUserID).isEqualTo(serverID)
        verify(exactly = 1) { tokenAPI.logIn(anonymousID, googleIdentity, any(), any()) }
        verify(exactly = 1) { backend.getCustomerInfo(serverID, false, any(), any()) }
        verify(exactly = 1) { deviceCache.cacheCustomerInfo(serverID, customerInfo) }
        verify(exactly = 0) { backend.logIn(any(), any(), any(), any()) }
    }

    @Test
    fun `IAM logIn merges the fresh CustomerInfo's subscriber attributes for the server-assigned user`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        stubTokenLogIn(succeedWith = serverID)

        logIn(googleIdentity)

        verifyOrder {
            deviceCache.cacheCustomerInfo(serverID, customerInfo)
            subscriberAttributesManager.mergeAttributes(customerInfo, serverID)
        }
        verify(exactly = 0) { subscriberAttributesManager.mergeAttributes(any(), anonymousID) }
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
    fun `IAM logIn from a server-assigned anonymous user copies attributes and keeps paywall web storage`() =
        runTest {
            cachedAppUserID = serverID
            createIdentityManager()
            saveIDToken(serverID, amr = listOf("anonymous"))
            stubTokenLogIn(succeedWith = identifiedID)

            logIn(googleIdentity)

            verify(exactly = 1) { subscriberAttributesManager.copyUnsyncedSubscriberAttributes(serverID, identifiedID) }
            verify(exactly = 0) { paywallAssetWarming.clearWebViewStorage() }
        }

    @Test
    fun `IAM logIn from a server-assigned identified user copies nothing and clears paywall web storage`() =
        runTest {
            cachedAppUserID = serverID
            createIdentityManager()
            saveIDToken(serverID, amr = listOf("anonymous", "google"))
            stubTokenLogIn(succeedWith = identifiedID)

            logIn(googleIdentity)

            verify(exactly = 0) { subscriberAttributesManager.copyUnsyncedSubscriberAttributes(any(), any()) }
            verify(exactly = 1) { paywallAssetWarming.clearWebViewStorage() }
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

    @Test
    fun `a backend request-already-in-progress failure is not mistaken for an identity change`() {
        val backendError = PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError)

        assertThat(IdentityManager.isIdentityChangedError(backendError)).isFalse()
        assertThat(IdentityManager.isIdentityChangedError(IdentityManager.identityChangedError)).isTrue()
    }

    // region currentUserIsAnonymous

    @Test
    fun `a regex-anonymous app user ID is anonymous without an ID token`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()

        assertThat(identityManager.currentUserIsAnonymous()).isTrue()
    }

    @Test
    fun `an identified app user ID is not anonymous without an ID token`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager()

        assertThat(identityManager.currentUserIsAnonymous()).isFalse()
    }

    @Test
    fun `an anonymous ID token makes a server-assigned app user ID anonymous`() = runTest {
        cachedAppUserID = serverID
        createIdentityManager()
        saveIDToken(serverID, amr = listOf("anonymous"))

        assertThat(identityManager.currentUserIsAnonymous()).isTrue()
    }

    @Test
    fun `a regex-anonymous app user ID stays anonymous even with a non-anonymous ID token`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        saveIDToken(anonymousID, amr = listOf("google"))

        assertThat(identityManager.currentUserIsAnonymous()).isTrue()
    }

    @Test
    fun `a non-anonymous ID token leaves a server-assigned app user ID identified`() = runTest {
        cachedAppUserID = serverID
        createIdentityManager()
        saveIDToken(serverID, amr = listOf("anonymous", "google"))

        assertThat(identityManager.currentUserIsAnonymous()).isFalse()
    }

    @Test
    fun `a cached CustomerInfo with only anonymous identities makes a server-assigned app user ID anonymous`() =
        runTest {
            cachedAppUserID = serverID
            every { deviceCache.getCachedCustomerInfo(serverID) } returns customerInfoWithAmr("anonymous")
            createIdentityManager()

            assertThat(identityManager.currentUserIsAnonymous()).isTrue()
        }

    @Test
    fun `a cached CustomerInfo with a non-anonymous identity leaves a server-assigned app user ID identified`() =
        runTest {
            cachedAppUserID = serverID
            every { deviceCache.getCachedCustomerInfo(serverID) } returns customerInfoWithAmr("anonymous", "google")
            createIdentityManager()

            assertThat(identityManager.currentUserIsAnonymous()).isFalse()
        }

    @Test
    fun `an identified ID token wins over a stale anonymous cached CustomerInfo`() = runTest {
        cachedAppUserID = serverID
        every { deviceCache.getCachedCustomerInfo(serverID) } returns customerInfoWithAmr("anonymous")
        createIdentityManager()
        saveIDToken(serverID, amr = listOf("anonymous", "google"))

        assertThat(identityManager.currentUserIsAnonymous()).isFalse()
    }

    @Test
    fun `the cached CustomerInfo isn't read when there's an ID token`() = runTest {
        cachedAppUserID = serverID
        createIdentityManager()
        saveIDToken(serverID, amr = listOf("google"))

        identityManager.currentUserIsAnonymous()

        verify(exactly = 0) { deviceCache.getCachedCustomerInfo(any()) }
    }

    @Test
    fun `the cached CustomerInfo is not consulted when IAM is disabled`() = runTest {
        cachedAppUserID = serverID
        every { deviceCache.getCachedCustomerInfo(serverID) } returns customerInfoWithAmr("anonymous")
        createIdentityManager(iamEnabled = false)

        assertThat(identityManager.currentUserIsAnonymous()).isFalse()
        verify(exactly = 0) { deviceCache.getCachedCustomerInfo(any()) }
    }

    @Test
    fun `the ID token is not consulted before the token cache loads`() = runTest {
        val idToken = fakeIDToken(listOf("anonymous"))
        newTokenManager(iamEnabled = true).saveTokens(serverID, "access", refreshToken = null, idToken = idToken)
        advanceUntilIdle()
        cachedAppUserID = serverID
        createIdentityManager(loaded = false)

        assertThat(identityManager.currentUserIsAnonymous()).isFalse()
        advanceUntilIdle()
        assertThat(identityManager.currentUserIsAnonymous()).isTrue()
    }

    @Test
    fun `logOut fails for a server-assigned anonymous user`() = runTest {
        cachedAppUserID = serverID
        createIdentityManager()
        saveIDToken(serverID, amr = listOf("anonymous"))

        val error = logOut()

        assertThat(error?.code).isEqualTo(PurchasesErrorCode.LogOutWithAnonymousUserError)
        verify(exactly = 0) { tokenAPI.revokeTokens(any(), any(), any()) }
    }

    // endregion

    // region logOut

    @Test
    fun `IAM logOut revokes, then switches to a server-assigned anonymous user with tokens`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager()
        stubRevoke(fail = null)
        stubTokenLogIn(succeedWith = serverID)

        val error = logOut()

        assertThat(error).isNull()
        assertThat(identityManager.currentAppUserID).isEqualTo(serverID)
        assertThat(tokenManager.hasCurrentAccessToken(serverID)).isTrue()
        verifyOrder {
            tokenAPI.revokeTokens(identifiedID, any(), any())
            tokenAPI.logIn(identifiedID, Identity.anonymous, any(), any())
            deviceCache.clearCachesForAppUserID(identifiedID)
            deviceCache.cacheAppUserID(serverID)
        }
        verify(exactly = 1) { backend.clearCaches() }
        verify(exactly = 0) { backend.getCustomerInfo(any(), any(), any(), any()) }
    }

    @Test
    fun `IAM logOut fails for an anonymous user without touching TokenAPI`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()

        val error = logOut()

        assertThat(error?.code).isEqualTo(PurchasesErrorCode.LogOutWithAnonymousUserError)
        verify(exactly = 0) { tokenAPI.revokeTokens(any(), any(), any()) }
    }

    @Test
    fun `IAM logOut revocation failure leaves the identity untouched`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager()
        val revokeError = PurchasesError(PurchasesErrorCode.NetworkError)
        stubRevoke(fail = revokeError)

        val error = logOut()

        assertThat(error).isEqualTo(revokeError)
        assertThat(identityManager.currentAppUserID).isEqualTo(identifiedID)
        verify(exactly = 0) { tokenAPI.logIn(any(), any(), any(), any()) }
        verify(exactly = 0) { deviceCache.clearCachesForAppUserID(any()) }
        verify(exactly = 0) { backend.clearCaches() }
    }

    @Test
    fun `IAM logOut succeeds as a local anonymous user when the anonymous login fails`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager()
        stubRevoke(fail = null)
        stubTokenLogIn(failWith = PurchasesError(PurchasesErrorCode.NetworkError))

        val error = logOut()

        // The identity changed, so callers must run their logout handling.
        assertThat(error).isNull()
        val newAppUserID = identityManager.currentAppUserID
        assertThat(IdentityManager.isUserIDAnonymous(newAppUserID)).isTrue()
        assertThat(tokenManager.hasCurrentAccessToken(newAppUserID)).isFalse()
        verify(exactly = 1) { deviceCache.clearCachesForAppUserID(identifiedID) }
        verify(exactly = 1) { backend.clearCaches() }
        assertThat(iamLoginNeeded()).isTrue()
    }

    @Test
    fun `logOut does not touch TokenAPI when IAM is disabled`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager(iamEnabled = false)

        val error = logOut()

        assertThat(error).isNull()
        assertThat(IdentityManager.isUserIDAnonymous(identityManager.currentAppUserID)).isTrue()
        verify(exactly = 0) { tokenAPI.revokeTokens(any(), any(), any()) }
        verify(exactly = 0) { tokenAPI.logIn(any(), any(), any(), any()) }
    }

    // endregion

    // region configure

    @Test
    fun `configure leaves the bootstrap login to its caller`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()

        identityManager.configure(null)
        advanceUntilIdle()

        verify(exactly = 0) { tokenAPI.logIn(any(), any(), any(), any()) }
    }

    // endregion

    // region before the token cache loads

    @Test
    fun `logOut waits for the token cache, so a server-assigned anonymous user is still rejected`() = runTest {
        val anonymousIDToken = fakeIDToken(listOf("anonymous"))
        newTokenManager(iamEnabled = true).saveTokens(serverID, "access", "refresh", anonymousIDToken)
        advanceUntilIdle()
        cachedAppUserID = serverID
        createIdentityManager(loaded = false)
        var error: PurchasesError? = null

        identityManager.logOut { error = it }
        assertThat(error).isNull()
        advanceUntilIdle()

        assertThat(error?.code).isEqualTo(PurchasesErrorCode.LogOutWithAnonymousUserError)
        verify(exactly = 0) { tokenAPI.revokeTokens(any(), any(), any()) }
        // The stored tokens were restored, not shadowed by deletions made before the load.
        assertThat(tokenManager.currentAccessToken(serverID)).isEqualTo("access")
        assertThat(tokenManager.currentRefreshToken(serverID)).isEqualTo("refresh")
    }

    @Test
    fun `logOut revokes only once the token cache has loaded`() = runTest {
        newTokenManager(iamEnabled = true).saveTokens(identifiedID, "access", "refresh", null)
        advanceUntilIdle()
        cachedAppUserID = identifiedID
        createIdentityManager(loaded = false)
        stubRevoke(fail = null)
        stubTokenLogIn(succeedWith = serverID)

        identityManager.logOut { }
        verify(exactly = 0) { tokenAPI.revokeTokens(any(), any(), any()) }
        advanceUntilIdle()

        verify(exactly = 1) { tokenAPI.revokeTokens(identifiedID, any(), any()) }
    }

    @Test
    fun `IAM logIn waits for the token cache before calling TokenAPI`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager(loaded = false)
        stubTokenLogIn(succeedWith = serverID)

        logIn(googleIdentity)
        verify(exactly = 0) { tokenAPI.logIn(any(), any(), any(), any()) }
        advanceUntilIdle()

        verify(exactly = 1) { tokenAPI.logIn(anonymousID, googleIdentity, any(), any()) }
    }

    @Test
    fun `logOut and IAM logIn fail when token storage is unavailable`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager(apiKey = "   ")

        val logOutError = logOut()
        val logInResult = logIn(googleIdentity)

        assertThat(logOutError?.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat((logInResult as PurchasesError).code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(identityManager.currentAppUserID).isEqualTo(identifiedID)
        verify(exactly = 0) { tokenAPI.revokeTokens(any(), any(), any()) }
        verify(exactly = 0) { tokenAPI.logIn(any(), any(), any(), any()) }
    }

    // endregion

    // region identity change listener

    @Test
    fun `the listener hears an IAM switch even when the CustomerInfo fetch then fails`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        var changes = 0
        identityManager.setIdentityChangeListener { changes++ }
        stubTokenLogIn(succeedWith = serverID)
        every { backend.getCustomerInfo(any(), any(), any(), any()) } answers {
            arg<(PurchasesError, Boolean) -> Unit>(3)(PurchasesError(PurchasesErrorCode.NetworkError), false)
        }

        logIn(googleIdentity)

        assertThat(identityManager.currentAppUserID).isEqualTo(serverID)
        assertThat(changes).isEqualTo(1)
    }

    @Test
    fun `the listener hears switchUser`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        var changes = 0
        identityManager.setIdentityChangeListener { changes++ }

        identityManager.switchUser(identifiedID)

        assertThat(changes).isEqualTo(1)
    }

    @Test
    fun `the listener doesn't hear a login dropped because the identity changed meanwhile`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        val pending = deferTokenLogIn()
        identityManager.logIn(googleIdentity, { _, _ -> }, { })
        identityManager.switchUser(identifiedID)
        var changes = 0
        identityManager.setIdentityChangeListener { changes++ }

        pending.onSuccess(serverID)

        assertThat(changes).isEqualTo(0)
    }

    // endregion

    // region identity changes while a request is in flight

    @Test
    fun `a late IAM logIn success does not undo a switchUser made meanwhile`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        val pending = deferTokenLogIn()
        var result: Any? = null
        identityManager.logIn(googleIdentity, { info, appUserID -> result = info to appUserID }, { result = it })

        identityManager.switchUser(identifiedID)
        pending.onSuccess(serverID)

        assertThat(identityManager.currentAppUserID).isEqualTo(identifiedID)
        assertThat(IdentityManager.isIdentityChangedError(result as PurchasesError)).isTrue()
        verify(exactly = 0) { deviceCache.cacheAppUserID(serverID) }
        verify(exactly = 0) { subscriberAttributesManager.copyUnsyncedSubscriberAttributes(any(), any()) }
        verify(exactly = 0) { backend.getCustomerInfo(any(), any(), any(), any()) }
    }

    @Test
    fun `a late anonymous IAM logIn success does not undo an identified logIn made meanwhile`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        val pending = deferTokenLogIn()
        identityManager.logIn(Identity.anonymous, { _, _ -> }, { })

        every { backend.logIn(anonymousID, identifiedID, any(), any()) } answers {
            thirdArg<(CustomerInfo, Boolean) -> Unit>()(customerInfo, false)
        }
        identityManager.logIn(identifiedID, { _, _ -> }, { })
        pending.onSuccess(serverID)

        assertThat(identityManager.currentAppUserID).isEqualTo(identifiedID)
        verify(exactly = 0) { deviceCache.cacheAppUserID(serverID) }
    }

    @Test
    fun `a CustomerInfo fetch that lands after an identity change has no effect`() = runTest {
        cachedAppUserID = anonymousID
        createIdentityManager()
        stubTokenLogIn(succeedWith = serverID)
        lateinit var deliverCustomerInfo: () -> Unit
        every { backend.getCustomerInfo(any(), any(), any(), any()) } answers {
            deliverCustomerInfo = { thirdArg<(CustomerInfo) -> Unit>()(customerInfo) }
        }
        var result: Any? = null
        identityManager.logIn(googleIdentity, { info, appUserID -> result = info to appUserID }, { result = it })

        identityManager.switchUser(identifiedID)
        deliverCustomerInfo()

        // The fetch itself went out for the server-assigned user, pinned to that user's token.
        verify(exactly = 1) { backend.getCustomerInfo(serverID, any(), any(), any()) }
        verify(exactly = 0) { deviceCache.cacheCustomerInfo(any(), any()) }
        verify(exactly = 0) { subscriberAttributesManager.mergeAttributes(any(), any()) }
        assertThat(IdentityManager.isIdentityChangedError(result as PurchasesError)).isTrue()
        assertThat(identityManager.currentAppUserID).isEqualTo(identifiedID)
    }

    @Test
    fun `IAM logOut leaves alone a user who became current during the attribute sync`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager()
        lateinit var finishSync: () -> Unit
        every { subscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(any(), any(), any()) } answers {
            finishSync = { thirdArg<(() -> Unit)?>()?.invoke() }
        }
        var error: PurchasesError? = null
        identityManager.logOut { error = it }

        identityManager.switchUser("another-user")
        finishSync()

        assertThat(identityManager.currentAppUserID).isEqualTo("another-user")
        assertThat(IdentityManager.isIdentityChangedError(error!!)).isTrue()
        verify(exactly = 1) {
            subscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(identifiedID, any(), any())
        }
        verify(exactly = 0) { tokenAPI.revokeTokens(any(), any(), any()) }
        verify(exactly = 0) { tokenAPI.logIn(any(), any(), any(), any()) }
    }

    @Test
    fun `a late logOut anonymous login does not undo an identity change made meanwhile`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager()
        stubRevoke(fail = null)
        val pending = deferTokenLogIn()
        var error: PurchasesError? = null
        identityManager.logOut { error = it }

        identityManager.switchUser("another-user")
        pending.onSuccess(serverID)

        assertThat(identityManager.currentAppUserID).isEqualTo("another-user")
        assertThat(IdentityManager.isIdentityChangedError(error!!)).isTrue()
    }

    @Test
    fun `a late logOut anonymous login failure does not reset an identity changed meanwhile`() = runTest {
        cachedAppUserID = identifiedID
        createIdentityManager()
        stubRevoke(fail = null)
        val pending = deferTokenLogIn()
        var error: PurchasesError? = null
        identityManager.logOut { error = it }

        identityManager.switchUser("another-user")
        pending.onError(PurchasesError(PurchasesErrorCode.NetworkError))

        assertThat(identityManager.currentAppUserID).isEqualTo("another-user")
        assertThat(IdentityManager.isIdentityChangedError(error!!)).isTrue()
    }

    // endregion

    private fun logOut(): PurchasesError? {
        var result: PurchasesError? = null
        var completed = false
        identityManager.logOut {
            result = it
            completed = true
        }
        assertThat(completed).isTrue()
        return result
    }

    private fun saveIDToken(appUserID: String, amr: List<String>) {
        tokenManager.saveTokens(appUserID, accessToken = "access", refreshToken = null, idToken = fakeIDToken(amr))
    }

    // Unsigned; only the amr claim is ever read.
    private fun fakeIDToken(amr: List<String>): String {
        fun base64Url(value: String) = Base64.encodeToString(
            value.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        val payload = JSONObject().put("amr", JSONArray(amr)).toString()
        val header = base64Url("{\"alg\":\"none\"}")
        return "$header.${base64Url(payload)}.${base64Url("")}"
    }

    private fun stubRevoke(fail: PurchasesError?) {
        every { tokenAPI.revokeTokens(any(), any(), any()) } answers {
            if (fail == null) secondArg<() -> Unit>()() else thirdArg<(PurchasesError) -> Unit>()(fail)
        }
    }

    // Returns (CustomerInfo, appUserID) on success, or the PurchasesError.
    private fun logIn(identity: Identity): Any? {
        var result: Any? = null
        identityManager.logIn(identity, { info, appUserID -> result = info to appUserID }, { result = it })
        return result
    }

    // Holds TokenAPI.logIn's callbacks so a test can change identity while the request is in flight.
    private class PendingLogIn {
        lateinit var onSuccess: (String) -> Unit
        lateinit var onError: (PurchasesError) -> Unit
    }

    private fun deferTokenLogIn(): PendingLogIn {
        val pending = PendingLogIn()
        every { tokenAPI.logIn(any(), any(), any(), any()) } answers {
            pending.onSuccess = thirdArg()
            pending.onError = arg(3)
        }
        return pending
    }

    private fun stubTokenLogIn(succeedWith: String? = null, failWith: PurchasesError? = null) {
        every { tokenAPI.logIn(any(), any(), any(), any()) } answers {
            if (succeedWith != null) {
                tokenManager.saveTokens(succeedWith, accessToken = "access", refreshToken = "refresh", idToken = null)
                thirdArg<(String) -> Unit>()(succeedWith)
            }
            if (failWith != null) arg<(PurchasesError) -> Unit>(3)(failWith)
        }
    }

    // Null if the check never answered.
    private fun TestScope.iamLoginNeeded(): Boolean? {
        var needed: Boolean? = null
        identityManager.checkIAMLoginNeeded { needed = it }
        advanceUntilIdle()
        return needed
    }

    private fun customerInfoWithAmr(vararg amr: String): CustomerInfo =
        createCustomerInfo(
            JSONObject(Responses.validEmptyPurchaserResponse)
                .put("user", JSONObject().put("id", serverID).put("amr", JSONArray(amr.toList()))),
        )

    private fun TestScope.createIdentityManager(
        iamEnabled: Boolean = true,
        loaded: Boolean = true,
        apiKey: String = API_KEY,
    ) {
        tokenManager = newTokenManager(iamEnabled, apiKey)
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

    private fun TestScope.newTokenManager(iamEnabled: Boolean, apiKey: String = API_KEY): TokenManager {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return TokenManager(
            ApplicationProvider.getApplicationContext(),
            apiKey,
            enabled = iamEnabled,
            computationDispatcher = dispatcher,
            ioDispatcher = dispatcher,
        )
    }

    private companion object {
        const val API_KEY = "test_api_key"
    }
}
