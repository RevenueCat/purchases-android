package com.revenuecat.purchases.identity

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
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
    private lateinit var mockSubscriberAttributesManager: SubscriberAttributesManager
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
            every { cleanupOldAttributionData() } just Runs
            every { getCachedCustomerInfo(any()) } returns null
        }
        mockSubscriberAttributesCache = mockk<SubscriberAttributesCache>().apply {
            every {
                cleanUpSubscriberAttributeCache(capture(cachedAppUserIDSlot))
            } just Runs
        }
        mockSubscriberAttributesManager = mockk()

        mockBackend = mockk()
        identityManager = createIdentityManager()
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
    fun `login synchronizes subscriber attributes`() {
        mockCachedAnonymousUser()
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers("test", any())
        } just Runs
        identityManager.logIn("test", { _, _ -> }, { })
        verify(exactly = 1) {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers("test", any())
        }
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
        val newAppUserID = "new"
        every {
            mockBackend.logIn(stubAnonymousID, newAppUserID, any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(
                    PurchasesError(PurchasesErrorCode.InvalidCredentialsError)
            )
        }
        var receivedError: PurchasesError? = null
        mockCachedAnonymousUser()
        mockSubscriberAttributesManagerSynchronize(newAppUserID)
        identityManager.logIn(newAppUserID, { _, _ -> }, { error -> receivedError = error })
        assertThat(receivedError).isNotNull
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.InvalidCredentialsError)
    }

    @Test
    fun `login passes received created and customerInfo from backend`() {
        val randomCreated: Boolean = Random.nextBoolean()
        val mockCustomerInfo: CustomerInfo = mockk()
        val newAppUserID = "new"
        every {
            mockBackend.logIn(stubAnonymousID, newAppUserID, captureLambda(), any())
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
        mockSubscriberAttributesManagerSynchronize(newAppUserID)
        mockSubscriberAttributesManagerCopyAttributes(stubAnonymousID, newAppUserID)

        identityManager.logIn(newAppUserID, { customerInfo, created ->
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
        val newAppUserID = "new"
        every {
            mockBackend.logIn(oldAppUserID, newAppUserID, captureLambda(), any())
        } answers {
            lambda<(CustomerInfo, Boolean) -> Unit>().captured.invoke(
                    mockCustomerInfo, randomCreated
            )
        }
        every { mockDeviceCache.cacheCustomerInfo(any(), any()) } just Runs
        mockSubscriberAttributesManagerSynchronize(newAppUserID)
        mockSubscriberAttributesManagerCopyAttributes(oldAppUserID, newAppUserID)

        identityManager.logIn(newAppUserID, { _, _ -> }, { })

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
        mockSubscriberAttributesManagerSynchronize(newAppUserID)
        mockSubscriberAttributesManagerCopyAttributes(oldAppUserID, newAppUserID)

        identityManager.logIn(newAppUserID, { _, _ -> }, { })

        verify(exactly = 1) { mockDeviceCache.cacheAppUserID(newAppUserID) }
        verify(exactly = 1) { mockDeviceCache.cacheCustomerInfo(newAppUserID, mockCustomerInfo) }
    }

    @Test
    fun `login copies unsynced attributes from old user to new one if old is anonymous on successful completion`() {
        val randomCreated: Boolean = Random.nextBoolean()
        val mockCustomerInfo: CustomerInfo = mockk()
        mockCachedAnonymousUser()
        val oldAppUserID = stubAnonymousID
        val newAppUserID = "new"
        every {
            mockBackend.logIn(oldAppUserID, newAppUserID, captureLambda(), any())
        } answers {
            lambda<(CustomerInfo, Boolean) -> Unit>().captured.invoke(mockCustomerInfo, randomCreated)
        }
        every { mockDeviceCache.cacheCustomerInfo(any(), any()) } just Runs
        mockSubscriberAttributesManagerSynchronize(newAppUserID)
        mockSubscriberAttributesManagerCopyAttributes(oldAppUserID, newAppUserID)

        identityManager.logIn(newAppUserID, { _, _ -> }, { })

        verify(exactly = 1) {
            mockSubscriberAttributesManager.copyUnsyncedSubscriberAttributes(stubAnonymousID, newAppUserID)
        }
    }

    @Test
    fun `login does not copy unsynced attributes from old user to new one if old is not anonymous`() {
        val randomCreated: Boolean = Random.nextBoolean()
        val mockCustomerInfo: CustomerInfo = mockk()
        val identifiedUserID = "Waldo"
        mockIdentifiedUser(identifiedUserID)
        val newAppUserID = "new"
        every {
            mockBackend.logIn(identifiedUserID, newAppUserID, captureLambda(), any())
        } answers {
            lambda<(CustomerInfo, Boolean) -> Unit>().captured.invoke(mockCustomerInfo, randomCreated)
        }
        every { mockDeviceCache.cacheCustomerInfo(any(), any()) } just Runs
        mockSubscriberAttributesManagerSynchronize(newAppUserID)

        identityManager.logIn(newAppUserID, { _, _ -> }, { })

        verify(exactly = 0) { mockSubscriberAttributesManager.copyUnsyncedSubscriberAttributes(any(), any()) }
    }

    @Test
    fun `logout synchronizes subscriber attributes`() {
        val identifiedUserID = "Waldo"
        mockIdentifiedUser(identifiedUserID)
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(identifiedUserID, any())
        } just Runs
        identityManager.logOut { }
        verify(exactly = 1) {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(identifiedUserID, any())
        }
    }

    @Test
    fun `logOut returns an error if the current user is anonymous`() {
        mockCachedAnonymousUser()
        mockSubscriberAttributesManagerSynchronize("")

        var error: PurchasesError? = null
        identityManager.logOut { error = it }

        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.LogOutWithAnonymousUserError)
    }

    @Test
    fun `logOut clears old caches`() {
        val identifiedUserID = "Waldo"
        every { mockDeviceCache.cleanupOldAttributionData() } just Runs
        mockIdentifiedUser(identifiedUserID)
        mockSubscriberAttributesManagerSynchronize(identifiedUserID)

        var error: PurchasesError? = null
        identityManager.logOut { error = it }

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
        mockSubscriberAttributesManagerSynchronize(identifiedUserID)
        every { mockDeviceCache.cleanupOldAttributionData() } just Runs

        var error: PurchasesError? = null
        identityManager.logOut { error = it }

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
        assertThat(cachedAppUserIDSlot.captured).isNotNull
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
        assertThat(cachedAppUserIDSlot.captured).isNotNull
        verify {
            mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache(cachedAppUserIDSlot.captured)
        }
    }

    @Test
    fun testConfigureCleansUpOldAttributionDataCacheForAnonymousUsers() {
        mockCleanCaches()
        identityManager.configure(null)
        verify(exactly = 1) { mockDeviceCache.cleanupOldAttributionData() }
    }

    @Test
    fun testConfigureCleansUpOldAttributionDataCacheForNonAnonymousUsers() {
        mockCleanCaches()
        identityManager.configure("cesar")
        verify(exactly = 1) { mockDeviceCache.cleanupOldAttributionData() }
    }

    @Test
    fun `we invalidate customer info and etag caches if verification is informational and cached customer info is not requested`() {
        val userId = "test-app-user-id"
        setupCustomerInfoCacheInvalidationTest(
            userId,
            VerificationResult.NOT_REQUESTED,
            SignatureVerificationMode.Informational(mockk()),
            true
        )
        identityManager.configure(userId)
        verify(exactly = 1) {
            mockDeviceCache.clearCustomerInfoCache(userId)
        }
        verify(exactly = 1) {
            mockBackend.clearCaches()
        }
    }

    @Test
    fun `we invalidate customer info and etag caches if verification is enforced and cached customer info is not requested`() {
        val userId = "test-app-user-id"
        setupCustomerInfoCacheInvalidationTest(
            userId,
            VerificationResult.NOT_REQUESTED,
            SignatureVerificationMode.Enforced(mockk()),
            true
        )
        identityManager.configure(userId)
        verify(exactly = 1) {
            mockDeviceCache.clearCustomerInfoCache(userId)
        }
        verify(exactly = 1) {
            mockBackend.clearCaches()
        }
    }

    @Test
    fun `we don't invalidate customer info and etag caches if verification is enabled and cached customer info is success`() {
        val userId = "test-app-user-id"
        setupCustomerInfoCacheInvalidationTest(
            userId,
            VerificationResult.VERIFIED,
            SignatureVerificationMode.Informational(mockk()),
            shouldClearCustomerInfoAndETagCaches = false
        )
        identityManager.configure(userId)
        verify(exactly = 0) {
            mockDeviceCache.clearCustomerInfoCache(userId)
        }
        verify(exactly = 0) {
            mockBackend.clearCaches()
        }
    }

    @Test
    fun `we don't invalidate customer info and etag caches if verification is disabled and cached customer info is not requested`() {
        val userId = "test-app-user-id"
        setupCustomerInfoCacheInvalidationTest(
            userId,
            VerificationResult.NOT_REQUESTED,
            SignatureVerificationMode.Disabled,
            shouldClearCustomerInfoAndETagCaches = false
        )
        identityManager.configure(userId)
        verify(exactly = 0) {
            mockDeviceCache.clearCustomerInfoCache(userId)
        }
        verify(exactly = 0) {
            mockBackend.clearCaches()
        }
    }

    @Test
    fun `we don't invalidate customer info and etag caches if no customer info cached`() {
        val userId = "test-app-user-id"
        every { mockDeviceCache.getCachedCustomerInfo(userId) } returns null
        every { mockBackend.verificationMode } returns SignatureVerificationMode.Informational(mockk())
        identityManager = createIdentityManager()
        identityManager.configure(userId)
        verify(exactly = 0) {
            mockDeviceCache.clearCustomerInfoCache(userId)
        }
        verify(exactly = 0) {
            mockBackend.clearCaches()
        }
    }

    // region helper functions

    private fun setupCustomerInfoCacheInvalidationTest(
        userId: String,
        verificationResult: VerificationResult,
        verificationMode: SignatureVerificationMode,
        shouldClearCustomerInfoAndETagCaches: Boolean
    ) {
        val mockCustomerInfo = mockk<CustomerInfo>().apply {
            every { entitlements } returns mockk<EntitlementInfos>().apply {
                every { verification } returns verificationResult
            }
        }
        every { mockDeviceCache.getCachedCustomerInfo(userId) } returns mockCustomerInfo
        if (shouldClearCustomerInfoAndETagCaches) {
            every { mockDeviceCache.clearCustomerInfoCache(userId) } just Runs
            every { mockBackend.clearCaches() } just Runs
        }
        every { mockBackend.verificationMode } returns verificationMode
        identityManager = createIdentityManager()
    }

    private fun mockIdentifiedUser(identifiedUserID: String) {
        every { mockDeviceCache.getCachedAppUserID() } returns identifiedUserID
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns null
        every { mockDeviceCache.clearCachesForAppUserID(identifiedUserID) } just Runs
        every { mockSubscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(identifiedUserID) } just Runs
        every { mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache(identifiedUserID) } just Runs
    }

    @Suppress("SameParameterValue")
    private fun assertCorrectlyIdentified(expectedAppUserID: String) {
        assertThat(cachedAppUserIDSlot.isCaptured).isTrue
        assertThat(cachedAppUserIDSlot.captured).isEqualTo(expectedAppUserID)
        assertThat(identityManager.currentUserIsAnonymous()).isFalse
    }

    private fun assertCorrectlyIdentifiedWithAnonymous(oldID: String? = null) {
        assertThat(cachedAppUserIDSlot.isCaptured).isTrue
        if (oldID == null) {
            assertThat(
                "^\\\$RCAnonymousID:([a-f0-9]{32})$".toRegex().matches(cachedAppUserIDSlot.captured)
            ).isTrue
        } else {
            assertThat(cachedAppUserIDSlot.captured).isEqualTo(oldID)
        }
        assertThat(identityManager.currentUserIsAnonymous()).isTrue
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

    private fun mockSubscriberAttributesManagerSynchronize(
        appUserId: String
    ) {
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserId, captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }
    }

    private fun mockSubscriberAttributesManagerCopyAttributes(
        oldAppUserId: String,
        newAppUserId: String
    ) {
        every {
            mockSubscriberAttributesManager.copyUnsyncedSubscriberAttributes(oldAppUserId, newAppUserId)
        } just Runs
    }

    private fun createIdentityManager(
        deviceCache: DeviceCache = mockDeviceCache,
        subscriberAttributesCache: SubscriberAttributesCache = mockSubscriberAttributesCache,
        subscriberAttributesManager: SubscriberAttributesManager = mockSubscriberAttributesManager,
        backend: Backend = mockBackend
    ): IdentityManager {
        return IdentityManager(
            deviceCache, subscriberAttributesCache, subscriberAttributesManager, backend
        )
    }

    // endregion
}
