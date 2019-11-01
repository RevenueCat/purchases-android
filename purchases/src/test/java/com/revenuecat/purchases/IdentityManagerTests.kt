package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
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

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class IdentityManagerTests {

    private lateinit var cachedAppUserIDSlot: CapturingSlot<String>
    private lateinit var mockDeviceCache: DeviceCache
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
        mockBackend = mockk()
        identityManager = IdentityManager(
            mockDeviceCache,
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
        identityManager.configure("cesar")
        assertCorrectlyIdentified("cesar")
    }

    @Test
    fun testConfigureWithAnonymousUserSavesTheIDInTheCache() {
        mockAnonymousUser()
        every { mockDeviceCache.getCachedAppUserID() } returns null
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns null
        identityManager.configure(null)
        assertCorrectlyIdentifiedWithAnonymous()
    }

    @Test
    fun testIdentifyingClearsCaches() {
        mockIdentifiedUser()
        val newAppUserID = "new"
        identityManager.identify(newAppUserID, {}, {})
        assertThat(cachedAppUserIDSlot.captured).isEqualTo(newAppUserID)
        verify { mockDeviceCache.clearCachesForAppUserID() }
    }

    @Test
    fun testIdentifyingCorrectlyIdentifies() {
        mockIdentifiedUser()
        val newAppUserID = "cesar"
        identityManager.identify(newAppUserID, {}, {})
        assertCorrectlyIdentified(newAppUserID)
    }

    @Test
    fun testCreateAliasCallsBackend() {
        every { mockBackend.createAlias(stubAnonymousID, "new", any(), any()) } just Runs
        mockAnonymousUser()
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
        mockAnonymousUser()
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
        mockAnonymousUser()
        identityManager.createAlias("new", {}, {})
        verify { mockDeviceCache.clearCachesForAppUserID() }
    }

    @Test
    fun testCreateAliasForwardsErrors() {
        every {
            mockBackend.createAlias(stubAnonymousID, "new", any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(PurchasesError(PurchasesErrorCode.InvalidCredentialsError))
        }
        mockAnonymousUser()
        var expectedError: PurchasesError? = null
        identityManager.createAlias("new", {}, { error -> expectedError = error})
        assertThat(expectedError).isNotNull
    }

    @Test
    fun testResetClearsOldCaches() {
        mockAnonymousUser()
        identityManager.reset()
        verify { mockDeviceCache.clearCachesForAppUserID() }
    }

    @Test
    fun testResetCreatesRandomIDAndCachesIt() {
        mockAnonymousUser()
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
        mockAnonymousUser()
        identityManager.identify("cesar", {}, {})
        verify { mockBackend.createAlias(stubAnonymousID, "cesar", any(), any()) }
    }

    @Test
    fun testMigrationFromRandomIDConfiguringAnonymously() {
        every { mockDeviceCache.getCachedAppUserID() } returns null
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns "an_old_random"
        every { mockDeviceCache.clearCachesForAppUserID() } just Runs
        identityManager.configure(null)
        assertCorrectlyIdentifiedWithAnonymous(oldID = "an_old_random")
    }

    @Test
    fun testMigrationFromRandomIDConfiguringWithUser() {
        every { mockDeviceCache.getCachedAppUserID() } returns null
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns "an_old_random"
        every { mockDeviceCache.clearCachesForAppUserID() } just Runs
        identityManager.configure("cesar")
        assertCorrectlyIdentified("cesar")
    }

    @Test
    fun testConfiguringWithIdentifiedDoesntUseCaches() {
        mockIdentifiedUser()
        identityManager.configure("cesar")
        assertCorrectlyIdentified("cesar")
    }

    private fun mockIdentifiedUser() {
        every { mockDeviceCache.getCachedAppUserID() } returns "cesar"
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns null
        every { mockDeviceCache.clearCachesForAppUserID() } just Runs
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

    private fun mockAnonymousUser() {
        every { mockDeviceCache.getCachedAppUserID() } returns stubAnonymousID
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns null
        every { mockDeviceCache.clearCachesForAppUserID() } just Runs
    }


    private fun mockCleanCaches() {
        every { mockDeviceCache.getCachedAppUserID() } returns null
        every { mockDeviceCache.getLegacyCachedAppUserID() } returns null
    }
}