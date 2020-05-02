package com.revenuecat.purchases.attributes

import com.revenuecat.purchases.Backend
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.SubscriberAttributeError
import com.revenuecat.purchases.caching.DeviceCache
import com.revenuecat.purchases.caching.SubscriberAttributesPerAppUserIDMap
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class SubscriberAttributesManagerTests {

    private val mockDeviceCache: DeviceCache = mockk()
    private val mockBackend: Backend = mockk()
    private val appUserID: String = "appUserID"
    private lateinit var underTest: SubscriberAttributesManager

    @Before
    fun setup() {
        underTest = SubscriberAttributesManager(
            mockDeviceCache,
            mockBackend
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
        val slot = slot<Map<String, SubscriberAttribute>>()
        every {
            mockDeviceCache.setAttributes(appUserID, capture(slot))
        } just Runs
        every {
            mockDeviceCache.getAllStoredSubscriberAttributes(appUserID)
        } returns emptyMap()
        val attributes = mapOf(
            "tshirtsize" to "L",
            "age" to "34",
            "removeThis" to null
        )
        underTest.setAttributes(attributes, appUserID)
        val captured = slot.captured
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

        underTest.setAttributes(mapOf(
            "tshirtsize" to "L",
            "removeThis" to null
        ), appUserID)

        verify (exactly = 0) {
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

        val slotOfPostedAttributes = slot<Map<String, SubscriberAttribute>>()
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

        every {
            mockDeviceCache.clearSyncedSubscriberAttributesForOtherAppUserIDs(appUserID)
        } just Runs

        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID)

        verify(exactly = 1) {
            mockBackend.postSubscriberAttributes(any(), any(), any(), any())
        }
        val capturedPosted = slotOfPostedAttributes.captured
        assertThat(capturedPosted).isNotNull
        assertThat(capturedPosted.size).isEqualTo(1)
        val capturedPostedSubscriberAttribute = capturedPosted["key"]
        assertThat(capturedPostedSubscriberAttribute).isNotNull
        assertThat(capturedPostedSubscriberAttribute!!.value).isEqualTo(null)
        assertThat(capturedPostedSubscriberAttribute.setTime).isEqualTo(subscriberAttribute.setTime)

        val capturedSet = slotOfSetAttributes.captured
        assertThat(capturedSet).isNotNull
        assertThat(capturedSet.size).isEqualTo(2)

        val capturedSetSubscriberAttribute = capturedSet["key"]
        assertThat(capturedSetSubscriberAttribute).isNotNull
        assertThat(capturedSetSubscriberAttribute!!.value).isEqualTo(null)
        assertThat(capturedSetSubscriberAttribute.setTime).isEqualTo(subscriberAttribute.setTime)
        assertThat(capturedSetSubscriberAttribute.isSynced).isTrue()

        val capturedSetSubscriberAttribute2 = capturedSet["key2"]
        assertThat(capturedSetSubscriberAttribute2).isNotNull
        assertThat(capturedSetSubscriberAttribute2!!.value).isEqualTo("value2")
        assertThat(capturedSetSubscriberAttribute2.setTime).isEqualTo(subscriberAttribute2.setTime)
        assertThat(capturedSetSubscriberAttribute2.isSynced).isTrue()
    }

    @Test
    fun `getting unsynchronized attributes`() {
        val subscriberAttribute = SubscriberAttribute("key", null, isSynced = false)
        val expected = mapOf(
            "key" to subscriberAttribute
        )
        every {
            mockDeviceCache.getUnsyncedSubscriberAttributes(appUserID)
        } returns expected

        val actual = underTest.getUnsyncedSubscriberAttributes(appUserID)

        assertThat(actual).isEqualTo(expected)
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

        val slotOfPostedAttributes = slot<Map<String, SubscriberAttribute>>()
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
        assertThat(capturedSetSubscriberAttribute.isSynced).isTrue()
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

        val slotOfPostedAttributes = slot<Map<String, SubscriberAttribute>>()
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
            mockBackend.postSubscriberAttributes(attributes, appUserID, any(), any())
        }
        verify(exactly = 1) {
            mockBackend.postSubscriberAttributes(attributes, "user2", any(), any())
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
            mockDeviceCache.clearSyncedSubscriberAttributesForOtherAppUserIDs(appUserID)
        } just Runs
        every {
            mockDeviceCache.clearSyncedSubscriberAttributesForSubscriber("user2")
        } just Runs

        underTest.synchronizeSubscriberAttributesForAllUsers(appUserID)

        verify(exactly = 1) {
            mockDeviceCache.clearSyncedSubscriberAttributesForSubscriber("user2")
        }
        verify(exactly = 0) {
            mockDeviceCache.clearSyncedSubscriberAttributesForSubscriber(appUserID)
        }
        verify(exactly = 0) {
            mockDeviceCache.clearSyncedSubscriberAttributesForOtherAppUserIDs("user2")
        }
        verify(exactly = 1) {
            mockDeviceCache.clearSyncedSubscriberAttributesForOtherAppUserIDs(appUserID)
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
                    mapOfAttributes,
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
                user to list.map {
                    it.key.backendKey to it
                }.toMap()
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
}