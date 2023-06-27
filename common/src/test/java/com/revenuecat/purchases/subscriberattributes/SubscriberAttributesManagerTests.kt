package com.revenuecat.purchases.subscriberattributes

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.subscriberattributes.DeviceIdentifiersFetcher
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesPerAppUserIDMap
import com.revenuecat.purchases.utils.filterNotNullValues
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriberAttributesManagerTests {

    private val mockDeviceCache: SubscriberAttributesCache = mockk()
    private val mockBackend: SubscriberAttributesPoster = mockk()
    private val mockDeviceIdentifiersFetcher: DeviceIdentifiersFetcher = mockk()
    private val appUserID: String = "appUserID"
    private val androidID = "androidid"
    private lateinit var underTest: SubscriberAttributesManager

    @Before
    fun setup() {
        underTest = SubscriberAttributesManager(
            mockDeviceCache,
            mockBackend,
            mockDeviceIdentifiersFetcher
        )
    }

    @Test
    fun `can be created`() {
        assertThat(underTest).isNotNull
        assertThat(underTest.deviceCache).isNotNull
        assertThat(underTest.backend).isNotNull
    }

    @Test
    fun `setting dictionary of attributes`() {
        val capturingSlot = mockSettingAttributesOnEmptyCache()
        val attributes = mapOf(
            "tshirtsize" to "L",
            "age" to "34",
            "removeThis" to null
        )
        underTest.setAttributes(attributes, appUserID)
        val captured = capturingSlot.captured
        assertThat(captured).isNotNull
        attributes.forEach { (key, value) ->
            val subscriberAttribute = captured[key] ?: error("Should be there")
            assertThat(subscriberAttribute).isNotNull
            assertThat(subscriberAttribute.key.backendKey).isEqualTo(key)
            assertThat(subscriberAttribute.value).isEqualTo(value)
        }
    }

    @Test
    fun `setting dictionary of already set attributes`() {
        every {
            mockDeviceCache.setAttributes(appUserID, any())
        } just Runs
        every {
            mockDeviceCache.getAllStoredSubscriberAttributes(appUserID)
        } returns mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L"),
            "removeThis" to SubscriberAttribute("tshirtsize", null)
        )

        underTest.setAttributes(
            mapOf(
                "tshirtsize" to "L",
                "removeThis" to null
            ), appUserID
        )

        verify(exactly = 0) {
            mockDeviceCache.setAttributes(appUserID, any())
        }
    }

    @Test
    fun `do not synchronize with backend if cache does not have unsynchronized attributes`() {
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes()
        } returns emptyMap()

        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID)

        verify(exactly = 0) {
            mockBackend.postSubscriberAttributes(any(), any(), any(), any())
        }
    }

    @Test
    fun `calls completion if cache does not have unsynchronized attributes`() {
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes()
        } returns emptyMap()

        var completionCalled = false
        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID) {
            completionCalled = true
        }

        assertTrue(completionCalled)
    }

    @Test
    fun `calls completion if cache has unsynchronized attributes for single user that succeed synchronizing`() {
        val subscriberAttribute = SubscriberAttribute("key", null, isSynced = false)
        every {
            mockDeviceCache.getAllStoredSubscriberAttributes(appUserID)
        } returns mapOf(
            "key" to subscriberAttribute
        )

        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes()
        } returns mapOf(
            appUserID to mapOf("key" to subscriberAttribute)
        )

        every {
            mockDeviceCache.setAttributes(appUserID, any())
        } just Runs

        every {
            mockBackend.postSubscriberAttributes(
                any(),
                appUserID,
                captureLambda(),
                any()
            )
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        var completionCalled = false
        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID) {
            completionCalled = true
        }

        assertTrue(completionCalled)
    }

    @Test
    fun `calls completion if cache has unsynchronized attributes for single user that fail synchronizing`() {
        val subscriberAttribute = SubscriberAttribute("key", null, isSynced = false)
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes()
        } returns mapOf(
            appUserID to mapOf("key" to subscriberAttribute)
        )

        every {
            mockBackend.postSubscriberAttributes(
                any(),
                appUserID,
                any(),
                captureLambda()
            )
        } answers {
            lambda<(PurchasesError, Boolean, List<SubscriberAttributeError>) -> Unit>().captured.invoke(
                PurchasesError(PurchasesErrorCode.CustomerInfoError),
                false,
                emptyList()
            )
        }

        var completionCalled = false
        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID) {
            completionCalled = true
        }

        assertTrue(completionCalled)
    }

    @Test
    fun `calls completion if cache has unsynchronized attributes for multiple users some failing and some syncing`() {
        val userId2 = "appUserId2"
        val subscriberAttribute = SubscriberAttribute("key", null, isSynced = false)
        val subscriberAttribute2 = SubscriberAttribute("key2", "value2", isSynced = false)
        val subscriberAttribute3 = SubscriberAttribute("key3", "value3", isSynced = false)
        val subscriberAttribute4 = SubscriberAttribute("key4", "value4", isSynced = false)

        every {
            mockDeviceCache.getAllStoredSubscriberAttributes(userId2)
        } returns mapOf(
            "key3" to subscriberAttribute3,
            "key4" to subscriberAttribute4
        )
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes()
        } returns mapOf(
            appUserID to mapOf(
                "key" to subscriberAttribute,
                "key2" to subscriberAttribute2
            ),
            userId2 to mapOf(
                "key3" to subscriberAttribute3,
                "key4" to subscriberAttribute4
            )
        )
        every {
            mockDeviceCache.setAttributes(userId2, any())
        } just Runs
        every {
            mockBackend.postSubscriberAttributes(
                any(),
                appUserID,
                any(),
                captureLambda()
            )
        } answers {
            lambda<(PurchasesError, Boolean, List<SubscriberAttributeError>) -> Unit>().captured.invoke(
                PurchasesError(PurchasesErrorCode.CustomerInfoError), false, emptyList()
            )
        }
        every {
            mockBackend.postSubscriberAttributes(
                any(),
                userId2,
                captureLambda(),
                any()
            )
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }
        every {
            mockDeviceCache.clearSubscriberAttributesIfSyncedForSubscriber(userId2)
        } just Runs

        var completionCalled = false
        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID) {
            completionCalled = true
        }

        assertTrue(completionCalled)
    }

    @Test
    fun `synchronize with backend unsynchronized attributes`() {
        val subscriberAttribute = SubscriberAttribute("key", null, isSynced = false)
        val subscriberAttribute2 = SubscriberAttribute("key2", "value2", isSynced = true)

        val slotOfSetAttributes = slot<Map<String, SubscriberAttribute>>()
        every {
            mockDeviceCache.getAllStoredSubscriberAttributes(appUserID)
        } returns mapOf(
            "key" to subscriberAttribute,
            "key2" to subscriberAttribute2
        )
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes()
        } returns mapOf(
            appUserID to mapOf("key" to subscriberAttribute)
        )
        every {
            mockDeviceCache.setAttributes(appUserID, capture(slotOfSetAttributes))
        } just Runs

        val slotOfPostedAttributes = slot<Map<String, Map<String, Any?>>>()
        every {
            mockBackend.postSubscriberAttributes(
                capture(slotOfPostedAttributes),
                appUserID,
                captureLambda(),
                any()
            )
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID)

        verify(exactly = 1) {
            mockBackend.postSubscriberAttributes(any(), any(), any(), any())
        }
        val capturedPosted = slotOfPostedAttributes.captured
        assertThat(capturedPosted).isNotNull
        assertThat(capturedPosted.size).isEqualTo(1)
        val capturedPostedSubscriberAttribute = capturedPosted["key"]
        assertThat(capturedPostedSubscriberAttribute).isEqualTo(subscriberAttribute.toBackendMap())

        val capturedSet = slotOfSetAttributes.captured
        assertThat(capturedSet).isNotNull
        assertThat(capturedSet.size).isEqualTo(2)

        val capturedSetSubscriberAttribute = capturedSet["key"]
        assertThat(capturedSetSubscriberAttribute).isNotNull
        assertThat(capturedSetSubscriberAttribute!!.value).isEqualTo(null)
        assertThat(capturedSetSubscriberAttribute.setTime).isEqualTo(subscriberAttribute.setTime)
        assertThat(capturedSetSubscriberAttribute.isSynced).isTrue

        val capturedSetSubscriberAttribute2 = capturedSet["key2"]
        assertThat(capturedSetSubscriberAttribute2).isNotNull
        assertThat(capturedSetSubscriberAttribute2!!.value).isEqualTo("value2")
        assertThat(capturedSetSubscriberAttribute2.setTime).isEqualTo(subscriberAttribute2.setTime)
        assertThat(capturedSetSubscriberAttribute2.isSynced).isTrue
    }

    @Test
    fun `getting unsynchronized attributes finishes immediately if not currently getting device identifiers`() {
        val subscriberAttribute = SubscriberAttribute("key", null, isSynced = false)
        val expected = mapOf(
            "key" to subscriberAttribute
        )
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes(appUserID)
        } returns expected

        var unsyncedAttributes: Map<String, SubscriberAttribute>? = null
        underTest.getUnsyncedSubscriberAttributes(appUserID) {
            unsyncedAttributes = it
        }

        assertNotNull(unsyncedAttributes)
        assertThat(unsyncedAttributes).isEqualTo(expected)
    }

    @Test
    fun `getting unsynchronized attributes waits if currently getting device identifiers`() {
        val mockContext = mockk<Application>(relaxed = true)
        val subscriberAttribute = SubscriberAttribute("key", null, isSynced = false)
        val expected = mapOf(
            "key" to subscriberAttribute
        )
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes(appUserID)
        } returns expected

        mockSettingAttributesOnEmptyCache()

        var deviceIdentifiersLambda: ((Map<String, String>) -> Unit)? = null
        every {
            mockDeviceIdentifiersFetcher.getDeviceIdentifiers(mockContext, captureLambda())
        } answers {
            deviceIdentifiersLambda = lambda<(Map<String, String>) -> Unit>().captured
        }

        underTest.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Adjust,
            "test-adjust-id",
            appUserID,
            mockContext
        )

        var unsyncedAttributes: Map<String, SubscriberAttribute>? = null
        underTest.getUnsyncedSubscriberAttributes(appUserID) {
            unsyncedAttributes = it
        }

        assertNull(unsyncedAttributes)
        deviceIdentifiersLambda?.invoke(mapOf("random-key" to "random-value"))
        assertNotNull(unsyncedAttributes)
        assertThat(unsyncedAttributes).isEqualTo(expected)
    }

    @Test
    fun `getting unsynchronized attributes waits if currently getting device identifiers multiple times`() {
        val mockContext = mockk<Application>(relaxed = true)
        val subscriberAttribute = SubscriberAttribute("key", null, isSynced = false)
        val expected = mapOf(
            "key" to subscriberAttribute
        )
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes(appUserID)
        } returns expected

        mockSettingAttributesOnEmptyCache()

        var deviceIdentifiersFirstCallLambda: ((Map<String, String>) -> Unit)? = null
        var deviceIdentifiersSecondCallLambda: ((Map<String, String>) -> Unit)? = null
        var deviceIdentifiersFetchInvocationCount = 0
        every {
            mockDeviceIdentifiersFetcher.getDeviceIdentifiers(mockContext, captureLambda())
        } answers {
            deviceIdentifiersFetchInvocationCount++
            if (deviceIdentifiersFetchInvocationCount == 1) {
                deviceIdentifiersFirstCallLambda = lambda<(Map<String, String>) -> Unit>().captured
            } else if (deviceIdentifiersFetchInvocationCount == 2) {
                deviceIdentifiersSecondCallLambda = lambda<(Map<String, String>) -> Unit>().captured
            }
        }

        underTest.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Adjust,
            "test-adjust-id",
            appUserID,
            mockContext
        )

        underTest.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Facebook,
            "test-facebook-id",
            appUserID,
            mockContext
        )

        var unsyncedAttributes: Map<String, SubscriberAttribute>? = null
        underTest.getUnsyncedSubscriberAttributes(appUserID) {
            unsyncedAttributes = it
        }

        assertNull(unsyncedAttributes)
        deviceIdentifiersFirstCallLambda?.invoke(mapOf("random-key" to "random-value"))
        assertNull(unsyncedAttributes)
        deviceIdentifiersSecondCallLambda?.invoke(mapOf("random-key-2" to "random-value-2"))
        assertNotNull(unsyncedAttributes)
        assertThat(unsyncedAttributes).isEqualTo(expected)
    }

    @Test
    fun `getting unsynchronized attributes calls completion only once`() {
        val mockContext = mockk<Application>(relaxed = true)
        val subscriberAttribute = SubscriberAttribute("key", null, isSynced = false)
        val expected = mapOf(
            "key" to subscriberAttribute
        )
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes(appUserID)
        } returns expected

        mockSettingAttributesOnEmptyCache()

        var deviceIdentifiersCallLambda: ((Map<String, String>) -> Unit)? = null
        every {
            mockDeviceIdentifiersFetcher.getDeviceIdentifiers(mockContext, captureLambda())
        } answers {
            deviceIdentifiersCallLambda = lambda<(Map<String, String>) -> Unit>().captured
        }

        var callbackCallCount = 0
        underTest.getUnsyncedSubscriberAttributes(appUserID) {
            callbackCallCount += 1
        }

        underTest.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Facebook,
            "test-facebook-id",
            appUserID,
            mockContext
        )

        assertEquals(1, callbackCallCount)

        deviceIdentifiersCallLambda!!.invoke(emptyMap())

        assertEquals(1, callbackCallCount)
    }

    @Test
    fun `attribute errors when synchronizing with backend and backend got them`() {
        val subscriberAttribute = SubscriberAttribute("key", null, isSynced = false)

        val slotOfSetAttributes = slot<Map<String, SubscriberAttribute>>()
        every {
            mockDeviceCache.getAllStoredSubscriberAttributes(appUserID)
        } returns mapOf(
            "key" to subscriberAttribute
        )
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes()
        } returns mapOf(
            appUserID to mapOf(
                "key" to subscriberAttribute
            )
        )

        every {
            mockDeviceCache.setAttributes(appUserID, capture(slotOfSetAttributes))
        } just Runs

        val slotOfPostedAttributes = slot<Map<String, Map<String, Any?>>>()
        every {
            mockBackend.postSubscriberAttributes(
                capture(slotOfPostedAttributes),
                appUserID,
                any(),
                captureLambda()
            )
        } answers {
            lambda<(PurchasesError, Boolean, List<SubscriberAttributeError>) -> Unit>().captured.invoke(
                PurchasesError(
                    PurchasesErrorCode.InvalidSubscriberAttributesError,
                    "Error syncing"
                ),
                true,
                emptyList()
            )
        }

        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID)

        verify(exactly = 1) {
            mockBackend.postSubscriberAttributes(any(), any(), any(), any())
        }

        val capturedSet = slotOfSetAttributes.captured
        assertThat(capturedSet).isNotNull
        assertThat(capturedSet.size).isEqualTo(1)

        val capturedSetSubscriberAttribute = capturedSet["key"]
        assertThat(capturedSetSubscriberAttribute).isNotNull
        assertThat(capturedSetSubscriberAttribute!!.value).isEqualTo(null)
        assertThat(capturedSetSubscriberAttribute.setTime).isEqualTo(subscriberAttribute.setTime)
        assertThat(capturedSetSubscriberAttribute.isSynced).isTrue
    }

    @Test
    fun `attribute errors when synchronizing with backend and backend did not get them`() {
        val subscriberAttribute = SubscriberAttribute("key", null)

        val slotOfSetAttributes = slot<Map<String, SubscriberAttribute>>()
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes()
        } returns mapOf(
            appUserID to mapOf(
                "key" to subscriberAttribute
            )
        )
        every {
            mockDeviceCache.setAttributes(appUserID, capture(slotOfSetAttributes))
        } just Runs

        val slotOfPostedAttributes = slot<Map<String, Map<String, Any?>>>()
        every {
            mockBackend.postSubscriberAttributes(
                capture(slotOfPostedAttributes),
                appUserID,
                any(),
                captureLambda()
            )
        } answers {
            lambda<(PurchasesError, Boolean, List<SubscriberAttributeError>) -> Unit>().captured.invoke(
                PurchasesError(
                    PurchasesErrorCode.InvalidSubscriberAttributesError,
                    "Error syncing"
                ),
                false,
                emptyList()
            )
        }

        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID)

        verify(exactly = 1) {
            mockBackend.postSubscriberAttributes(any(), any(), any(), any())
        }
        verify(exactly = 0) {
            mockDeviceCache.setAttributes(appUserID, any())
        }
    }

    @Test
    fun `Sync unsynced attributes for all users`() {
        every {
            mockBackend.postSubscriberAttributes(any(), any(), any(), any())
        } just Runs

        val attributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L"),
            "removeThis" to SubscriberAttribute("tshirtsize", null)
        )
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes()
        } returns mapOf(
            appUserID to attributes,
            "user2" to attributes
        )
        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID)
        verify(exactly = 1) {
            mockBackend.postSubscriberAttributes(attributes.toBackendMap(), appUserID, any(), any())
        }
        verify(exactly = 1) {
            mockBackend.postSubscriberAttributes(attributes.toBackendMap(), "user2", any(), any())
        }
    }

    @Test
    fun `When syncing all users attributes, does not sync attributes for empty user IDs`() {
        every {
            mockBackend.postSubscriberAttributes(any(), any(), any(), any())
        } just Runs

        val attributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L"),
            "removeThis" to SubscriberAttribute("tshirtsize", null)
        )
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes()
        } returns mapOf(
            appUserID to attributes,
            "" to attributes
        )
        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID)
        verify(exactly = 1) {
            mockBackend.postSubscriberAttributes(attributes.toBackendMap(), appUserID, any(), any())
        }
        verify(exactly = 0) {
            mockBackend.postSubscriberAttributes(any(), "", any(), any())
        }
    }

    @Test
    fun `When syncing all users attributes, does not sync attributes for whitespaces user IDs`() {
        every {
            mockBackend.postSubscriberAttributes(any(), any(), any(), any())
        } just Runs

        val whitespacesUserId = "   "

        val attributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L"),
            "removeThis" to SubscriberAttribute("tshirtsize", null)
        )
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes()
        } returns mapOf(
            appUserID to attributes,
            whitespacesUserId to attributes
        )
        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID)
        verify(exactly = 1) {
            mockBackend.postSubscriberAttributes(attributes.toBackendMap(), appUserID, any(), any())
        }
        verify(exactly = 0) {
            mockBackend.postSubscriberAttributes(any(), eq(whitespacesUserId), any(), any())
        }
    }

    @Test
    fun `When syncing another user attributes, clear them when posted`() {
        val subscriberAttribute = SubscriberAttribute("key", null, isSynced = false)
        val subscriberAttribute2 = SubscriberAttribute("key2", "value2", isSynced = true)

        val listOfAttributes = listOf(subscriberAttribute, subscriberAttribute2)
        val (allStoredAttributes, unsyncedAttributes) =
            mockStoredAttributes(mapOf(appUserID to listOfAttributes, "user2" to listOfAttributes))

        mockPostingSubscriberAttributes(unsyncedAttributes)
        mockSettingUpdatedSyncedAttributes(allStoredAttributes)

        every {
            mockDeviceCache.clearSubscriberAttributesIfSyncedForSubscriber("user2")
        } just Runs

        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID)

        verify(exactly = 1) {
            mockDeviceCache.clearSubscriberAttributesIfSyncedForSubscriber("user2")
        }
        verify(exactly = 0) {
            mockDeviceCache.clearSubscriberAttributesIfSyncedForSubscriber(appUserID)
        }
    }

    @Test
    fun collectDeviceIdentifiers() {
        val capturingSlot = mockSettingAttributesOnEmptyCache()

        val mockContext = mockk<Application>(relaxed = true)
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = "12345"
        )

        underTest.collectDeviceIdentifiers(appUserID, mockContext)

        val captured = capturingSlot.captured
        assertThat(captured).isNotNull
        assertThat(captured.size).isEqualTo(3)

        val gpsAdIdSubscriberAttribute =
            captured[SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey]
        assertThat(gpsAdIdSubscriberAttribute).isNotNull
        assertThat(gpsAdIdSubscriberAttribute!!.value).isEqualTo("12345")

        val androidIDSubscriberAttribute =
            captured[SubscriberAttributeKey.DeviceIdentifiers.AndroidID.backendKey]
        assertThat(androidIDSubscriberAttribute).isNotNull
        assertThat(androidIDSubscriberAttribute!!.value).isEqualTo("androidid")

        val ipSubscriberAttribute =
            captured[SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey]
        assertThat(ipSubscriberAttribute).isNotNull
        assertThat(ipSubscriberAttribute!!.value).isEqualTo("true")
    }

    @Test
    fun `null adID when calling collectDeviceIdentifiers`() {
        val capturingSlot = mockSettingAttributesOnEmptyCache()

        val mockContext = mockk<Application>(relaxed = true)
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = null
        )

        underTest.collectDeviceIdentifiers(appUserID, mockContext)

        val captured = capturingSlot.captured
        assertThat(captured).isNotNull
        assertThat(captured.size).isEqualTo(2)

        val gpsAdIdSubscriberAttribute =
            captured[SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey]
        assertThat(gpsAdIdSubscriberAttribute).isNull()

        val androidIDSubscriberAttribute =
            captured[SubscriberAttributeKey.DeviceIdentifiers.AndroidID.backendKey]
        assertThat(androidIDSubscriberAttribute).isNotNull
        assertThat(androidIDSubscriberAttribute!!.value).isEqualTo("androidid")
    }

    @Test
    fun setAttributionID() {
        val capturingSlot = mockSettingAttributesOnEmptyCache()

        val mockContext = mockk<Application>(relaxed = true)
        mockAdvertisingInfo(
            mockContext = mockContext,
            expectedAdID = "12345"
        )

        underTest.setAttributionID(
            SubscriberAttributeKey.AttributionIds.Facebook,
            "facebook_id",
            appUserID,
            mockContext
        )

        val captured = capturingSlot.captured
        assertThat(captured).isNotNull
        assertThat(captured.size).isEqualTo(4)

        val gpsAdIdSubscriberAttribute =
            captured[SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey]
        assertThat(gpsAdIdSubscriberAttribute).isNotNull
        assertThat(gpsAdIdSubscriberAttribute!!.value).isEqualTo("12345")

        val androidIDSubscriberAttribute =
            captured[SubscriberAttributeKey.DeviceIdentifiers.AndroidID.backendKey]
        assertThat(androidIDSubscriberAttribute).isNotNull
        assertThat(androidIDSubscriberAttribute!!.value).isEqualTo("androidid")

        val ipSubscriberAttribute =
            captured[SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey]
        assertThat(ipSubscriberAttribute).isNotNull
        assertThat(ipSubscriberAttribute!!.value).isEqualTo("true")

        val facebookID =
            captured[SubscriberAttributeKey.AttributionIds.Facebook.backendKey]
        assertThat(facebookID).isNotNull
        assertThat(facebookID!!.value).isEqualTo("facebook_id")
    }

    // region copyUnsyncedSubscriberAttributes

    @Test
    fun `copyUnsyncedSubscriberAttributes does not do anything if no unsynced attributes to copy`() {
        every { mockDeviceCache.getUnsyncedSubscriberAttributes(appUserID) } returns emptyMap()

        underTest.copyUnsyncedSubscriberAttributes(appUserID, "new-app-user-id")

        verify(exactly = 0) { mockDeviceCache.setAttributes(any(), any()) }
        verify(exactly = 0) { mockDeviceCache.clearAllSubscriberAttributesFromUser(any()) }
    }

    @Test
    fun `copyUnsyncedSubscriberAttributes copies unsynced attributes to new user and deletes old ones`() {
        val unsyncedAttributes = mapOf("tshirtsize" to SubscriberAttribute("tshirtsize", "L"))
        val newAppUserId = "new-app-user-id"
        every { mockDeviceCache.getUnsyncedSubscriberAttributes(appUserID) } returns unsyncedAttributes
        every { mockDeviceCache.setAttributes(newAppUserId, unsyncedAttributes) } just Runs
        every { mockDeviceCache.clearAllSubscriberAttributesFromUser(appUserID) } just Runs

        underTest.copyUnsyncedSubscriberAttributes(appUserID, newAppUserId)

        verify(exactly = 1) { mockDeviceCache.setAttributes(newAppUserId, unsyncedAttributes) }
        verify(exactly = 1) { mockDeviceCache.clearAllSubscriberAttributesFromUser(appUserID) }
    }

    // endregion

    // region helper functions

    private fun mockAdvertisingInfo(
        mockContext: Application,
        expectedAdID: String?
    ) {
        every {
            mockDeviceIdentifiersFetcher.getDeviceIdentifiers(mockContext, captureLambda())
        } answers {
            lambda<(Map<String, String>) -> Unit>().captured.also {
                val deviceIdentifiers = mapOf(
                    SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey to expectedAdID,
                    SubscriberAttributeKey.DeviceIdentifiers.AndroidID.backendKey to androidID,
                    SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey to "true"
                ).filterNotNullValues()
                it.invoke(deviceIdentifiers)
            }
        }
    }

    private fun mockSettingUpdatedSyncedAttributes(
        allAttributes: SubscriberAttributesPerAppUserIDMap
    ) {
        val allMarkedSynced = allAttributes.map { (user, attributesMap) ->
            user to attributesMap.map { (key, attribute) ->
                key to attribute.copy(isSynced = true)
            }.toMap()
        }.toMap()
        allMarkedSynced.forEach { (user, attributes) ->
            every {
                mockDeviceCache.setAttributes(user, attributes)
            } just Runs
        }
    }

    private fun mockPostingSubscriberAttributes(
        unsyncedAttributes: SubscriberAttributesPerAppUserIDMap
    ) {
        unsyncedAttributes.forEach { (user, mapOfAttributes) ->
            every {
                mockBackend.postSubscriberAttributes(
                    mapOfAttributes.toBackendMap(),
                    user,
                    captureLambda(),
                    any()
                )
            } answers {
                lambda<() -> Unit>().captured.invoke()
            }
        }
    }

    private fun mockStoredAttributes(
        subscriberAttributes: Map<String, List<SubscriberAttribute>>
    ): Pair<SubscriberAttributesPerAppUserIDMap, SubscriberAttributesPerAppUserIDMap> {
        val allStoredAttributes =
            subscriberAttributes.map { (user, list) ->
                user to list.associateBy {
                    it.key.backendKey
                }
            }.toMap()

        subscriberAttributes.keys.forEach {
            every {
                mockDeviceCache.getAllStoredSubscriberAttributes(it)
            } returns (allStoredAttributes[it] ?: error("something broke"))
        }

        val unsyncedAttributes =
            allStoredAttributes.map { (user, attributes) ->
                user to attributes.filterValues { !it.isSynced }
            }.toMap()

        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes()
        } returns unsyncedAttributes

        return allStoredAttributes to unsyncedAttributes
    }

    private fun mockSettingAttributesOnEmptyCache(): CapturingSlot<Map<String, SubscriberAttribute>> {
        val slot = slot<Map<String, SubscriberAttribute>>()
        every {
            mockDeviceCache.setAttributes(appUserID, capture(slot))
        } just Runs
        every {
            mockDeviceCache.getAllStoredSubscriberAttributes(appUserID)
        } returns emptyMap()
        return slot
    }

    // endregion
}
