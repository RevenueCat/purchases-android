package com.revenuecat.purchases.attributes

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.buildSubscriberAttributesMapPerUser
import com.revenuecat.purchases.caching.DeviceCache
import com.revenuecat.purchases.caching.SubscriberAttributeMap
import com.revenuecat.purchases.caching.SubscriberAttributesPerAppUserIDMap
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class SubscriberAttributesDeviceCacheTests {
    private val apiKey = "api_key"
    private val appUserID = "app_user_id"

    private lateinit var underTest: DeviceCache
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private val putStringSlot = slot<String>()

    @Before
    fun setup() {
        mockEditor = mockk<SharedPreferences.Editor>().apply {
            every {
                putString("com.revenuecat.purchases.$apiKey.subscriberAttributes", capture(putStringSlot))
            } returns this
            every {
                remove("com.revenuecat.purchases.$apiKey.subscriberAttributes")
            } returns this
            every {
                remove("com.revenuecat.purchases.$apiKey.appUserID")
            } returns this
            every {
                remove("com.revenuecat.purchases.$apiKey.legacyAppUserID")
            } returns this
            every {
                remove("com.revenuecat.purchases.$apiKey.new")
            } returns this
            every {
                remove("com.revenuecat.purchases.$apiKey")
            } returns this
            every {
                apply()
            } just runs
        }

        mockPrefs = mockk<SharedPreferences>().apply {
            every {
                edit()
            } returns mockEditor
        }

        underTest = DeviceCache(mockPrefs, apiKey)

        every {
            mockPrefs.getString(underTest.appUserIDCacheKey, isNull())
        } returns "appUserID"
        every {
            mockPrefs.getString(underTest.legacyAppUserIDCacheKey, isNull())
        } returns "legacyAppUserID"
    }

    @Test
    fun `getting attributes on empty cache returns empty map`() {
        mockEmptyCache()
        val allStoredSubscriberAttributes = underTest.getAllStoredSubscriberAttributes(appUserID)
        assertThat(allStoredSubscriberAttributes.size).isZero()
    }

    @Test
    fun `getting attributes on not empty cache returns map of attributes`() {
        val expectedAttributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L"),
            "age" to SubscriberAttribute("age", "L"),
            "removeThis" to SubscriberAttribute("removeThis", null)
        )

        mockNotEmptyCache(expectedAttributes)
        val receivedAttributes = underTest.getAllStoredSubscriberAttributes(appUserID)

        assertThat(receivedAttributes.size).isEqualTo(expectedAttributes.size)
        expectedAttributes.values.map { it to receivedAttributes[it.key.backendKey] }.forEach { (expected, received) ->
            assertThat(received).isNotNull
            assertThat(expected).isEqualTo(received)
        }
    }

    @Test
    fun `setting attributes on empty cache`() {
        mockEmptyCache()

        val expectedAttributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L"),
            "age" to SubscriberAttribute("age", "L"),
            "removeThis" to SubscriberAttribute("removeThis", null)
        )

        underTest.setAttributes(appUserID, expectedAttributes)

        assertCapturedEqualsExpected(expectedAttributes)
    }

    @Test
    fun `setting attributes on not empty cache`() {
        val expectedAttributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L"),
            "age" to SubscriberAttribute("age", "L"),
            "removeThis" to SubscriberAttribute("removeThis", null)
        )
        mockNotEmptyCache(expectedAttributes)

        underTest.setAttributes(appUserID, expectedAttributes)

        assertCapturedEqualsExpected(expectedAttributes)
    }

    @Test
    fun `setting attribute already existing but marking as synced`() {
        mockNotEmptyCache(mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L")
        ))

        val expectedAttributes: Map<String, SubscriberAttribute> = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = true)
        )
        underTest.setAttributes(appUserID, expectedAttributes)

        assertCapturedEqualsExpected(expectedAttributes)
    }

    @Test
    fun `setting existing attribute as null`() {
        mockNotEmptyCache(mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = true)
        ))

        val expectedAttributes: Map<String, SubscriberAttribute> = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", null)
        )
        underTest.setAttributes(appUserID, expectedAttributes)

        assertCapturedEqualsExpected(expectedAttributes)
    }

    @Test
    fun `setting existing attribute`() {
        mockNotEmptyCache(mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L")
        ))

        val expectedAttributes: Map<String, SubscriberAttribute> = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "M")
        )
        underTest.setAttributes(appUserID, expectedAttributes)

        assertCapturedEqualsExpected(expectedAttributes)
    }

    @Test
    fun `setting non existing attribute`() {
        val expectedAttributes: Map<String, SubscriberAttribute> = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", setTime = Date(1)),
            "shoesize" to SubscriberAttribute("shoesize", "32", setTime = Date(2))
        )
        mockNotEmptyCache(mapOf(expectedAttributes.keys.toList()[0] to expectedAttributes.values.toList()[0]))

        underTest.setAttributes(appUserID, mapOf(expectedAttributes.keys.toList()[1] to expectedAttributes.values.toList()[1]))

        assertCapturedEqualsExpected(expectedAttributes)
    }

    @Test
    fun `clearing caches also clears the subscriber attributes`() {
        val expectedAttributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = true)
        )
        mockNotEmptyCache(expectedAttributes)
        underTest.clearCachesForAppUserID(appUserID)
        verify {
            mockEditor.putString(
                "com.revenuecat.purchases.$apiKey.subscriberAttributes",
                JSONObject().also {
                    it.put("attributes", JSONObject())
                }.toString()
            )
        }
    }

    private fun mockEmptyCache() {
        every {
            mockPrefs.getString(
                "com.revenuecat.purchases.$apiKey.subscriberAttributes",
                any()
            )
        } returns null
    }

    private fun mockNotEmptyCache(cacheContents: SubscriberAttributeMap) {
        mockNotEmptyCacheMultipleUsers(mapOf(appUserID to cacheContents))
    }

    private fun mockNotEmptyCacheMultipleUsers(cacheContents: SubscriberAttributesPerAppUserIDMap) {
        every {
            mockPrefs.getString(
                "com.revenuecat.purchases.$apiKey.subscriberAttributes",
                any()
            )
        } returns JSONObject().put("attributes", JSONObject().also { attributesJSONObject ->
            cacheContents.forEach { (appUserID, subscriberAttributeMap) ->
                attributesJSONObject.put(appUserID, JSONObject().also { appUserIDJSONObject ->
                    subscriberAttributeMap.forEach { (key, subscriberAttribute) ->
                        appUserIDJSONObject.put(key, subscriberAttribute.toJSONObject())
                    }
                })
            }
        }).toString()
    }

    private fun assertCapturedEqualsExpected(expectedAttributes: Map<String, SubscriberAttribute>) {
        assertThat(putStringSlot.isCaptured)
        val receivedAttributes =
            JSONObject(putStringSlot.captured).buildSubscriberAttributesMapPerUser()
                .getValue(appUserID)
        assertThat(receivedAttributes).isNotNull
        assertThat(receivedAttributes.size).isEqualTo(expectedAttributes.size)
        expectedAttributes.values.map { it to receivedAttributes[it.key.backendKey] }
            .forEach { (expected, received) ->
                assertThat(received).isNotNull
                assertThat(expected).isEqualTo(received)
            }
    }
}