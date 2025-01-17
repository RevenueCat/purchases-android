package com.revenuecat.purchases.identity

import android.content.SharedPreferences.Editor
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.offerings.OfferingsCache
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.utils.SyncDispatcher
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
    private lateinit var mockOfferingsCache: OfferingsCache
    private lateinit var mockBackend: Backend
    private lateinit var mockOfflineEntitlementsManager: OfflineEntitlementsManager
    private lateinit var identityManager: IdentityManager
    private lateinit var mockEditor: Editor
    private val stubAnonymousID = "\$RCAnonymousID:ff68f26e432648369a713849a9f93b58"

    @Before
    fun setup() {
        cachedAppUserIDSlot = slot()
        mockEditor = mockk<Editor>().apply {
            every { apply() } just Runs
        }
        mockDeviceCache = mockk<DeviceCache>().apply {
            every { cacheAppUserID(capture(cachedAppUserIDSlot)) } answers {
                every { mockDeviceCache.getCachedAppUserID() } returns cachedAppUserIDSlot.captured
            }
            every { cacheAppUserID(capture(cachedAppUserIDSlot), mockEditor) } answers {
                every { mockDeviceCache.getCachedAppUserID() } returns cachedAppUserIDSlot.captured
                mockEditor
            }
            every { cleanupOldAttributionData() } just Runs
            every { getCachedCustomerInfo(any()) } returns null
            every { startEditing() } returns mockEditor
        }
        mockSubscriberAttributesCache = mockk<SubscriberAttributesCache>().apply {
            every {
                cleanUpSubscriberAttributeCache(capture(cachedAppUserIDSlot), mockEditor)
            } just Runs
        }
        mockSubscriberAttributesManager = mockk()
        mockOfferingsCache = mockk<OfferingsCache>().apply {
            every { clearCache() } just Runs
        }

        mockBackend = mockk<Backend>().apply {
            every { verificationMode } returns SignatureVerificationMode.Disabled
        }
        mockOfflineEntitlementsManager = mockk<OfflineEntitlementsManager>().apply {
            every { resetOfflineCustomerInfoCache() } just Runs
        }
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
        every { mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache("cesar", any()) } just Runs
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
        verify(exactly = 1) { mockOfferingsCache.clearCache() }
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
    fun `login resets offline customer info cache on success`() {
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

        verify(exactly = 1) { mockOfflineEntitlementsManager.resetOfflineCustomerInfoCache() }
    }

    @Test
    fun `login does not reset offline customer info cache on error`() {
        mockCachedAnonymousUser()
        val oldAppUserID = stubAnonymousID
        val newAppUserID = "new"
        every {
            mockBackend.logIn(oldAppUserID, newAppUserID, any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(
                PurchasesError(PurchasesErrorCode.InvalidCredentialsError)
            )
        }
        mockSubscriberAttributesManagerSynchronize(newAppUserID)

        identityManager.logIn(newAppUserID, { _, _ -> }, { })

        verify(exactly = 0) { mockOfflineEntitlementsManager.resetOfflineCustomerInfoCache() }
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
        verify(exactly = 1) { mockOfferingsCache.clearCache() }
    }

    @Test
    fun `logOut clears backend caches`() {
        val identifiedUserID = "Waldo"
        every { mockDeviceCache.cleanupOldAttributionData() } just Runs
        mockIdentifiedUser(identifiedUserID)
        mockSubscriberAttributesManagerSynchronize(identifiedUserID)

        var error: PurchasesError? = null
        identityManager.logOut { error = it }

        assertThat(error).isNull()
        verify(exactly = 1) { mockBackend.clearCaches() }
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
    fun `logOut resets offline customer info cache`() {
        val identifiedUserID = "Waldo"
        mockIdentifiedUser(identifiedUserID)
        mockSubscriberAttributesManagerSynchronize(identifiedUserID)
        every { mockDeviceCache.cleanupOldAttributionData() } just Runs

        var completionCallCount = 0
        identityManager.logOut { completionCallCount++ }

        assertThat(completionCallCount).isEqualTo(1)
        verify(exactly = 1) { mockOfflineEntitlementsManager.resetOfflineCustomerInfoCache() }
    }

    @Test
    fun testMigrationFromRandomIDConfiguringAnonymously() {
        every { mockDeviceCache.getCachedAppUserID() } returns null
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns "an_old_random"
        every { mockDeviceCache.clearCachesForAppUserID("an_old_random") } just Runs
        every { mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache("an_old_random", any()) } just Runs
        identityManager.configure(null)
        assertCorrectlyIdentifiedWithAnonymous(oldID = "an_old_random")
    }

    @Test
    fun testMigrationFromRandomIDConfiguringWithUser() {
        every { mockDeviceCache.getCachedAppUserID() } returns null
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns "an_old_random"
        every { mockDeviceCache.clearCachesForAppUserID("an_old_random") } just Runs
        every { mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache("cesar", any()) } just Runs
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
            mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache("cesar", any())
        }
    }

    @Test
    fun `when configuring with an anonymous user, subscriber attributes are cleaned up`() {
        mockCleanCaches()
        identityManager.configure(null)
        assertThat(cachedAppUserIDSlot.captured).isNotNull
        verify {
            mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache(cachedAppUserIDSlot.captured, any())
        }
    }

    @Test
    fun `when configuring with a specific user, cache is cleaned up`() {
        mockCleanCaches()
        identityManager.configure("cesar")
        verify {
            mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache("cesar", any())
        }
    }

    @Test
    fun `when configuring with an anonymous user, cache is cleaned up`() {
        mockCleanCaches()
        identityManager.configure(null)
        assertThat(cachedAppUserIDSlot.captured).isNotNull
        verify {
            mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache(cachedAppUserIDSlot.captured, any())
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
    fun `we invalidate etag caches if verification is informational and cached customer info is not requested`() {
        val userId = "test-app-user-id"
        setupCustomerInfoCacheInvalidationTest(
            userId,
            VerificationResult.NOT_REQUESTED,
            SignatureVerificationMode.Informational(mockk()),
            true
        )
        identityManager.configure(userId)
        verify(exactly = 0) {
            mockDeviceCache.clearCustomerInfoCache(userId, mockEditor)
        }
        verify(exactly = 1) {
            mockBackend.clearCaches()
        }
    }

    @Test
    fun `we invalidate etag caches if verification is enforced and cached customer info is not requested`() {
        val userId = "test-app-user-id"
        setupCustomerInfoCacheInvalidationTest(
            userId,
            VerificationResult.NOT_REQUESTED,
            SignatureVerificationMode.Enforced(mockk()),
            true
        )
        identityManager.configure(userId)
        verify(exactly = 0) {
            mockDeviceCache.clearCustomerInfoCache(userId, mockEditor)
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

    // region switch user
    @Test
    fun `switching users clears all caches`() {
        val oldAppUserID = "cesar"
        mockIdentifiedUser(oldAppUserID)
        val newAppUserID = "new"
        every { mockDeviceCache.cacheCustomerInfo(any(), any()) } just Runs
        mockSubscriberAttributesManagerSynchronize(newAppUserID)
        mockSubscriberAttributesManagerCopyAttributes(oldAppUserID, newAppUserID)

        identityManager.switchUser(newAppUserID)

        verify(exactly = 1) { mockDeviceCache.clearCachesForAppUserID(oldAppUserID) }
        verify(exactly = 1) { mockOfferingsCache.clearCache() }
        verify(exactly = 1) {
            mockSubscriberAttributesCache.clearSubscriberAttributesIfSyncedForSubscriber(oldAppUserID)
        }
        verify(exactly = 1) { mockOfflineEntitlementsManager.resetOfflineCustomerInfoCache() }
        verify(exactly = 1) { mockBackend.clearCaches() }
    }
    @Test
    fun `switching users saves the new user`() {
        val oldAppUserID = "cesar"
        mockIdentifiedUser(oldAppUserID)

        val newAppUserID = "new"
        identityManager.switchUser(newAppUserID)

        verify(exactly = 1) { mockDeviceCache.cacheAppUserID(newAppUserID) }
    }
    // endregion

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
            every { mockDeviceCache.clearCustomerInfoCache(userId, mockEditor) } just Runs
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
        every { mockSubscriberAttributesCache.cleanUpSubscriberAttributeCache(identifiedUserID, any()) } just Runs
        every { mockBackend.clearCaches() } just Runs
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
        every { mockBackend.clearCaches() } just Runs
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
        offeringsCache: OfferingsCache = mockOfferingsCache,
        backend: Backend = mockBackend,
        offlineEntitlementsManager: OfflineEntitlementsManager = mockOfflineEntitlementsManager
    ): IdentityManager {
        return IdentityManager(
            deviceCache,
            subscriberAttributesCache,
            subscriberAttributesManager,
            offeringsCache,
            backend,
            offlineEntitlementsManager,
            SyncDispatcher()
        )
    }

    // endregion
}
