package com.revenuecat.purchases.attributes

import com.revenuecat.purchases.Backend
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.SubscriberAttributeError
import com.revenuecat.purchases.caching.DeviceCache
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
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
    fun `do not synchronize with backend if cache is empty`() {
        every {
            mockDeviceCache.getAllStoredSubscriberAttributes(appUserID)
        } returns emptyMap()

        var successCalled = false
        underTest.synchronizeSubscriberAttributesIfNeeded(appUserID, {
            successCalled = true
        }, {
            fail("should be success")
        })

        assertThat(successCalled).isTrue()
        verify(exactly = 0) {
            mockBackend.postSubscriberAttributes(any(), any(), any(), any())
        }
    }

    @Test
    fun `do not synchronize with backend if cache does not have unsynchronized attributes`() {
        every {
            mockDeviceCache.getAllStoredSubscriberAttributes(appUserID)
        } returns mapOf("key" to SubscriberAttribute("key", null, isSynced = true))

        var successCalled = false
        underTest.synchronizeSubscriberAttributesIfNeeded(appUserID, {
            successCalled = true
        }, {
            fail("should be success")
        })

        assertThat(successCalled).isTrue()
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

        var successCalled = false
        underTest.synchronizeSubscriberAttributesIfNeeded(
            appUserID,
            {
                successCalled = true
            },
            {
                fail("should be success")
            }
        )

        assertThat(successCalled).isTrue()
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
        val subscriberAttribute2 = SubscriberAttribute("key2", "value2", isSynced = true)

        val expected = mapOf(
            "key" to subscriberAttribute
        )
        every {
            mockDeviceCache.getAllStoredSubscriberAttributes(appUserID)
        } returns (expected.toMutableMap() + mapOf("key2" to subscriberAttribute2))

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
            mockDeviceCache.getAllStoredSubscriberAttributes(appUserID)
        } returns mapOf("key" to subscriberAttribute)
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

        var failureCalled = false
        underTest.synchronizeSubscriberAttributesIfNeeded(
            appUserID,
            {
                fail("should be failure")
            },
            {
                failureCalled = true
            }
        )

        assertThat(failureCalled).isTrue()
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
            mockDeviceCache.getAllStoredSubscriberAttributes(appUserID)
        } returns mapOf(
            "key" to subscriberAttribute
        )
        every {
            mockDeviceCache.getAllStoredSubscriberAttributes(appUserID)
        } returns mapOf("key" to subscriberAttribute)
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

        var failureCalled = false
        underTest.synchronizeSubscriberAttributesIfNeeded(
            appUserID,
            {
                fail("should be failure")
            },
            {
                failureCalled = true
            }
        )

        assertThat(failureCalled).isTrue()
        verify(exactly = 1) {
            mockBackend.postSubscriberAttributes(any(), any(), any(), any())
        }
        verify(exactly = 0) {
            mockDeviceCache.setAttributes(appUserID, any())
        }
    }
}