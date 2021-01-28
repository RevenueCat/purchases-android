package com.revenuecat.purchases.identity

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class IdentityManagerTests {

    private lateinit var cachedAppUserIDSlot: CapturingSlot<String>
    private lateinit var mockDeviceCache: DeviceCache
    private lateinit var mockSubscriberAttributesCache: SubscriberAttributesCache
    private lateinit var mockBackend: Backend
    private lateinit var identityManager: IdentityManager
    private val stubAnonymousID = "\$RCAnonymousID:ff68f26e432648369a713849a9f93b58"

    @Before
    fun setup() {
        cachedAppUserIDSlot = slot()
        mockDeviceCache = mockk<DeviceCache>().apply {
            every { cacheAppUserID(capture(cachedAppUserIDSlot)) } answers {
                every { mockDeviceCache.getCachedAppUserID() } returns cachedAppUserIDSlot.captured
            }
        }
        mockSubscriberAttributesCache = mockk<SubscriberAttributesCache>().apply {
            every {
                cleanUpSubscriberAttributeCache(capture(cachedAppUserIDSlot))
            } just Runs
        }

        mockBackend = mockk()
        identityManager = IdentityManager(
            mockDeviceCache,
            mockSubscriberAttributesCache,
            mockBackend
        )
    }

    @Test
    fun testConfigureWithAnonymousUserIDGeneratesAnAppUserID() {
        mockCleanCaches()
        identityManager.configure(null)
        assertCorrectlyIdentifiedWithAnonymous()
    }

    @Test
    fun testAnonymousIDsMatchesFormat() {
        mockCleanCaches()
        identityManager.configure(null)
        assertCorrectlyIdentifiedWithAnonymous()
    }

    @Test
    fun testConfigureSavesTheIDInTheCache() {
        every { mockDeviceCache.getCachedAppUserID() } returns null
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns null
        every { mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache("cesar") } just Runs
        identityManager.configure("cesar")
        assertCorrectlyIdentified("cesar")
    }

    @Test
    fun testConfigureWithAnonymousUserSavesTheIDInTheCache() {
        mockCleanCaches()
        identityManager.configure(null)
        assertCorrectlyIdentifiedWithAnonymous()
    }

    @Test
    fun testIdentifyingClearsCaches() {
        mockIdentifiedUser("cesar")
        val newAppUserID = "new"
        identityManager.identify(newAppUserID, {}, {})
        assertThat(cachedAppUserIDSlot.captured).isEqualTo(newAppUserID)
        verify { mockDeviceCache.clearCachesForAppUserID("cesar") }
        verify { mockSubscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber("cesar") }
    }

    @Test
    fun testIdentifyingCorrectlyIdentifies() {
        mockIdentifiedUser("cesar")
        val newAppUserID = "cesar"
        identityManager.identify(newAppUserID, {}, {})
        assertCorrectlyIdentified(newAppUserID)
    }

    @Test
    fun testCreateAliasCallsBackend() {
        every { mockBackend.createAlias(stubAnonymousID, "new", any(), any()) } just Runs
        mockCachedAnonymousUser()
        identityManager.createAlias("new", {}, {})
        verify { mockBackend.createAlias(stubAnonymousID, "new", any(), any()) }
    }

    @Test
    fun testCreateAliasIdentifiesWhenSuccessful() {
        every {
            mockBackend.createAlias(stubAnonymousID, "new", captureLambda(), any())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }
        mockCachedAnonymousUser()
        identityManager.createAlias("new", {}, {})
        assertCorrectlyIdentified("new")
    }

    @Test
    fun testCreateAliasClearsCachesForPreviousUser() {
        every {
            mockBackend.createAlias(stubAnonymousID, "new", captureLambda(), any())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }
        mockCachedAnonymousUser()
        identityManager.createAlias("new", {}, {})
        verify { mockDeviceCache.clearCachesForAppUserID(stubAnonymousID) }
        verify { mockSubscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(stubAnonymousID) }
    }

    @Test
    fun testCreateAliasForwardsErrors() {
        every {
            mockBackend.createAlias(stubAnonymousID, "new", any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(
                PurchasesError(PurchasesErrorCode.InvalidCredentialsError)
            )
        }
        mockCachedAnonymousUser()
        var expectedError: PurchasesError? = null
        identityManager.createAlias("new", {}, { error -> expectedError = error })
        assertThat(expectedError).isNotNull
    }

    @Test
    fun `login fails with error if the appUserID is empty`() {
        every {
            mockBackend.logIn(stubAnonymousID, "", any(), captureLambda())
        } just Runs
        var receivedError: PurchasesError? = null
        mockCachedAnonymousUser()
        identityManager.logIn("", { _, _ -> }, { error -> receivedError = error })
        assertThat(receivedError).isNotNull
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.InvalidAppUserIdError)
        verify(exactly = 0) {
            mockBackend.logIn(any(), any(), any(), any())
        }
    }

    @Test
    fun `login fails with error if the appUserID is blank`() {
        every {
            mockBackend.logIn(stubAnonymousID, "   ", any(), captureLambda())
        } just Runs
        var receivedError: PurchasesError? = null
        mockCachedAnonymousUser()
        identityManager.logIn("   ", { _, _ -> }, { error -> receivedError = error })
        assertThat(receivedError).isNotNull
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.InvalidAppUserIdError)
        verify(exactly = 0) {
            mockBackend.logIn(any(), any(), any(), any())
        }
    }

    @Test
    fun `login passes backend errors`() {
        every {
            mockBackend.logIn(stubAnonymousID, "new", any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(
                    PurchasesError(PurchasesErrorCode.InvalidCredentialsError)
            )
        }
        var receivedError: PurchasesError? = null
        mockCachedAnonymousUser()
        identityManager.logIn("new", { _, _ -> }, { error -> receivedError = error })
        assertThat(receivedError).isNotNull
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.InvalidCredentialsError)
    }

    @Test
    fun `login passes received created and purchaserInfo from backend`() {
        val randomCreated: Boolean = Random.nextBoolean()
        val mockPurchaserInfo: PurchaserInfo = mockk()
        every {
            mockBackend.logIn(stubAnonymousID, "new", captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo, Boolean) -> Unit>().captured.invoke(
                    mockPurchaserInfo, randomCreated
            )
        }
        every { mockDeviceCache.cachePurchaserInfo(any(), any()) } just Runs
        var receivedError: PurchasesError? = null
        var receivedPurchaserInfo: PurchaserInfo? = null
        var receivedCreated: Boolean? = null
        mockCachedAnonymousUser()

        identityManager.logIn("new", {
            purchaserInfo, created ->
            receivedPurchaserInfo = purchaserInfo
            receivedCreated = created
        }, { error -> receivedError = error })

        assertThat(receivedError).isNull()
        assertThat(receivedPurchaserInfo).isNotNull
        assertThat(receivedPurchaserInfo).isEqualTo(mockPurchaserInfo)
        assertThat(receivedCreated).isEqualTo(randomCreated)
    }

    @Test
    fun `login clears caches for old appUserID on successful completion`() {
        val randomCreated: Boolean = Random.nextBoolean()
        val mockPurchaserInfo: PurchaserInfo = mockk()
        mockCachedAnonymousUser()
        val oldAppUserID = stubAnonymousID
        every {
            mockBackend.logIn(oldAppUserID, "new", captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo, Boolean) -> Unit>().captured.invoke(
                    mockPurchaserInfo, randomCreated
            )
        }
        every { mockDeviceCache.cachePurchaserInfo(any(), any()) } just Runs

        identityManager.logIn("new", { _, _ -> }, { _ -> })

        verify(exactly = 1) { mockDeviceCache.clearCachesForAppUserID(oldAppUserID) }
        verify(exactly = 1) {
            mockSubscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(oldAppUserID)
        }
    }

    @Test
    fun `login caches purchaserInfo and appUserID for new user on successful completion`() {
        val randomCreated: Boolean = Random.nextBoolean()
        val mockPurchaserInfo: PurchaserInfo = mockk()
        mockCachedAnonymousUser()
        val oldAppUserID = stubAnonymousID
        val newAppUserID = "new"
        every {
            mockBackend.logIn(oldAppUserID, newAppUserID, captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo, Boolean) -> Unit>().captured.invoke(
                    mockPurchaserInfo, randomCreated
            )
        }
        every { mockDeviceCache.cachePurchaserInfo(any(), any()) } just Runs

        identityManager.logIn(newAppUserID, { _, _ -> }, { _ -> })

        verify(exactly = 1) { mockDeviceCache.cacheAppUserID(newAppUserID) }
        verify(exactly = 1) { mockDeviceCache.cachePurchaserInfo(newAppUserID, mockPurchaserInfo) }
    }

    @Test
    fun testResetClearsOldCaches() {
        mockCachedAnonymousUser()
        identityManager.reset()
        verify { mockDeviceCache.clearCachesForAppUserID(stubAnonymousID) }
        verify { mockSubscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(stubAnonymousID) }
    }

    @Test
    fun testResetCreatesRandomIDAndCachesIt() {
        mockCachedAnonymousUser()
        identityManager.reset()
        assertCorrectlyIdentifiedWithAnonymous()
    }

    @Test
    fun testIdentifyingWhenUserIsAnonymousCreatesAlias() {
        every {
            mockBackend.createAlias(stubAnonymousID, "cesar", captureLambda(), any())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }
        mockCachedAnonymousUser()
        identityManager.identify("cesar", {}, {})
        verify { mockBackend.createAlias(stubAnonymousID, "cesar", any(), any()) }
    }

    @Test
    fun testMigrationFromRandomIDConfiguringAnonymously() {
        every { mockDeviceCache.getCachedAppUserID() } returns null
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns "an_old_random"
        every { mockDeviceCache.clearCachesForAppUserID("an_old_random") } just Runs
        every { mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache("an_old_random") } just Runs
        identityManager.configure(null)
        assertCorrectlyIdentifiedWithAnonymous(oldID = "an_old_random")
    }

    @Test
    fun testMigrationFromRandomIDConfiguringWithUser() {
        every { mockDeviceCache.getCachedAppUserID() } returns null
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns "an_old_random"
        every { mockDeviceCache.clearCachesForAppUserID("an_old_random") } just Runs
        every { mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache("cesar") } just Runs
        identityManager.configure("cesar")
        assertCorrectlyIdentified("cesar")
    }

    @Test
    fun testConfiguringWithIdentifiedDoesntUseCaches() {
        mockIdentifiedUser("cesar")
        identityManager.configure("cesar")
        assertCorrectlyIdentified("cesar")
    }

    @Test
    fun `when configuring with a specific user, subscriber attributes are cleaned up`() {
        mockCleanCaches()
        identityManager.configure("cesar")
        verify {
            mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache("cesar")
        }
    }

    @Test
    fun `when configuring with an anonymous user, subscriber attributes are cleaned up`() {
        mockCleanCaches()
        identityManager.configure(null)
        assertThat(cachedAppUserIDSlot.captured).isNotNull()
        verify {
            mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache(cachedAppUserIDSlot.captured)
        }
    }

    @Test
    fun `when configuring with a specific user, cache is cleaned up`() {
        mockCleanCaches()
        identityManager.configure("cesar")
        verify {
            mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache("cesar")
        }
    }

    @Test
    fun `when configuring with an anonymous user, cache is cleaned up`() {
        mockCleanCaches()
        identityManager.configure(null)
        assertThat(cachedAppUserIDSlot.captured).isNotNull()
        verify {
            mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache(cachedAppUserIDSlot.captured)
        }
    }

    private fun mockIdentifiedUser(identifiedUserID: String) {
        every { mockDeviceCache.getCachedAppUserID() } returns identifiedUserID
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns null
        every { mockDeviceCache.clearCachesForAppUserID(identifiedUserID) } just Runs
        every { mockSubscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(identifiedUserID) } just Runs
        every { mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache(identifiedUserID) } just Runs
    }

    private fun assertCorrectlyIdentified(expectedAppUserID: String) {
        assertThat(cachedAppUserIDSlot.isCaptured).isTrue()
        assertThat(cachedAppUserIDSlot.captured).isEqualTo(expectedAppUserID)
        assertThat(identityManager.currentUserIsAnonymous()).isFalse()
    }

    private fun assertCorrectlyIdentifiedWithAnonymous(oldID: String? = null) {
        assertThat(cachedAppUserIDSlot.isCaptured).isTrue()
        if (oldID == null) {
            assertThat(
                "^\\\$RCAnonymousID:([a-f0-9]{32})$".toRegex().matches(cachedAppUserIDSlot.captured)
            ).isTrue()
        } else {
            assertThat(cachedAppUserIDSlot.captured).isEqualTo(oldID)
        }
        assertThat(identityManager.currentUserIsAnonymous()).isTrue()
    }

    private fun mockCachedAnonymousUser() {
        every { mockDeviceCache.getCachedAppUserID() } returns stubAnonymousID
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns null
        every { mockDeviceCache.clearCachesForAppUserID(stubAnonymousID) } just Runs
        every { mockSubscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(stubAnonymousID) } just Runs
    }

    private fun mockCleanCaches() {
        every { mockDeviceCache.getCachedAppUserID() } returns null
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns null
    }
}
