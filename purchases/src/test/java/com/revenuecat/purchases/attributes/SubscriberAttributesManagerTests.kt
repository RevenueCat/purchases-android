package com.revenuecat.purchases.attributes

import com.revenuecat.purchases.Backend
import com.revenuecat.purchases.caching.DeviceCache
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
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
            assertThat(subscriberAttribute.key.serverValue).isEqualTo(key)
            assertThat(subscriberAttribute.value).isEqualTo(value)
        }
    }

}