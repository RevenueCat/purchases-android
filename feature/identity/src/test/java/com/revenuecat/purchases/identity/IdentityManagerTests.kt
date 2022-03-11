package com.revenuecat.purchases.identity

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
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
import io.mockk.runs
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
            every { cleanupOldAttributionData(any()) } just Runs
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
    fun testConfigureWithEmptyUserIDGeneratesAnAnonymousAppUserID() {
        mockCleanCaches()
        identityManager.configure(" ")
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
    fun `login passes received created and customerInfo from backend`() {
        val randomCreated: Boolean = Random.nextBoolean()
        val mockCustomerInfo: CustomerInfo = mockk()
        every {
            mockBackend.logIn(stubAnonymousID, "new", captureLambda(), any())
        } answers {
            lambda<(CustomerInfo, Boolean) -> Unit>().captured.invoke(
                    mockCustomerInfo, randomCreated
            )
        }
        every { mockDeviceCache.cacheCustomerInfo(any(), any()) } just Runs
        var receivedError: PurchasesError? = null
        var receivedCustomerInfo: CustomerInfo? = null
        var receivedCreated: Boolean? = null
        mockCachedAnonymousUser()

        identityManager.logIn("new", { customerInfo, created ->
            receivedCustomerInfo = customerInfo
            receivedCreated = created
        }, { error -> receivedError = error })

        assertThat(receivedError).isNull()
        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedCustomerInfo).isEqualTo(mockCustomerInfo)
        assertThat(receivedCreated).isEqualTo(randomCreated)
    }

    @Test
    fun `login clears caches for old appUserID on successful completion`() {
        val randomCreated: Boolean = Random.nextBoolean()
        val mockCustomerInfo: CustomerInfo = mockk()
        mockCachedAnonymousUser()
        val oldAppUserID = stubAnonymousID
        every {
            mockBackend.logIn(oldAppUserID, "new", captureLambda(), any())
        } answers {
            lambda<(CustomerInfo, Boolean) -> Unit>().captured.invoke(
                    mockCustomerInfo, randomCreated
            )
        }
        every { mockDeviceCache.cacheCustomerInfo(any(), any()) } just Runs

        identityManager.logIn("new", { _, _ -> }, { _ -> })

        verify(exactly = 1) { mockDeviceCache.clearCachesForAppUserID(oldAppUserID) }
        verify(exactly = 1) {
            mockSubscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(oldAppUserID)
        }
    }

    @Test
    fun `login caches customerInfo and appUserID for new user on successful completion`() {
        val randomCreated: Boolean = Random.nextBoolean()
        val mockCustomerInfo: CustomerInfo = mockk()
        mockCachedAnonymousUser()
        val oldAppUserID = stubAnonymousID
        val newAppUserID = "new"
        every {
            mockBackend.logIn(oldAppUserID, newAppUserID, captureLambda(), any())
        } answers {
            lambda<(CustomerInfo, Boolean) -> Unit>().captured.invoke(
                    mockCustomerInfo, randomCreated
            )
        }
        every { mockDeviceCache.cacheCustomerInfo(any(), any()) } just Runs

        identityManager.logIn(newAppUserID, { _, _ -> }, { _ -> })

        verify(exactly = 1) { mockDeviceCache.cacheAppUserID(newAppUserID) }
        verify(exactly = 1) { mockDeviceCache.cacheCustomerInfo(newAppUserID, mockCustomerInfo) }
    }

    @Test
    fun `logOut returns an error if the current user is anonymous`() {
        mockCachedAnonymousUser()

        val error = identityManager.logOut()

        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.LogOutWithAnonymousUserError)
    }

    @Test
    fun `logOut clears old caches`() {
        val identifiedUserID = "Waldo"
        every { mockDeviceCache.cleanupOldAttributionData(identifiedUserID) } just Runs
        mockIdentifiedUser(identifiedUserID)

        val error = identityManager.logOut()

        assertThat(error).isNull()
        verify(exactly = 1) { mockDeviceCache.clearCachesForAppUserID(identifiedUserID) }
        verify(exactly = 1) {
            mockSubscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(
                identifiedUserID
            )
        }
    }

    @Test
    fun `logOut creates random ID and caches it`() {
        val identifiedUserID = "Waldo"
        mockIdentifiedUser(identifiedUserID)
        every { mockDeviceCache.cleanupOldAttributionData(identifiedUserID) } just Runs

        val error = identityManager.logOut()

        assertThat(error).isNull()
        assertCorrectlyIdentifiedWithAnonymous()
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

    @Test
    fun testConfigureCleansUpOldAttributionDataCacheForAnonymousUsers() {
        mockCleanCaches()
        identityManager.configure(null)
        verify(exactly = 1) { mockDeviceCache.cleanupOldAttributionData(cachedAppUserIDSlot.captured) }
    }

    @Test
    fun testConfigureCleansUpOldAttributionDataCacheForNonAnonymousUsers() {
        mockCleanCaches()
        identityManager.configure("cesar")
        verify(exactly = 1) { mockDeviceCache.cleanupOldAttributionData("cesar") }
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
