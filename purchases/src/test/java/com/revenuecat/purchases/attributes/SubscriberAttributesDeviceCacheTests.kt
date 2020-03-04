package com.revenuecat.purchases.attributes

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.caching.DeviceCache
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
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
                putString(any(), capture(putStringSlot))
            } returns this
            every {
                remove(any())
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
    }

    @Test
    fun `getting attributes on empty cache returns empty map`() {
        mockEmptyCache()
        val allStoredSubscriberAttributes = underTest.getAllStoredSubscriberAttributes(appUserID)
        assertThat(allStoredSubscriberAttributes.size).isZero()
    }

    @Test
    fun `getting attributes on not empty cache returns map of attributes`() {
        val attributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L"),
            "age" to SubscriberAttribute("age", "L"),
            "removeThis" to SubscriberAttribute("removeThis", null)
        )
        mockNotEmptyCache(attributes)

        val allStoredSubscriberAttributes = underTest.getAllStoredSubscriberAttributes(appUserID)
        assertThat(allStoredSubscriberAttributes.size).isEqualTo(3)
        assertThat(allStoredSubscriberAttributes.values).doesNotContainNull()
    }

    @Test
    fun `setting attributes on empty cache`() {
        mockEmptyCache()

        val attributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L"),
            "age" to SubscriberAttribute("age", "L"),
            "removeThis" to SubscriberAttribute("removeThis", null)
        )
        underTest.setAttributes(appUserID, attributes)
        assertThat(putStringSlot.isCaptured)
        val jsonObject = JSONObject(putStringSlot.captured)
        val attributesJSON = jsonObject.getJSONObject("attributes")
        assertThat(attributesJSON).isNotNull
        assertThat(attributesJSON.length()).isEqualTo(3)
        attributes.forEach { (key, _) ->
            assertThat(attributesJSON.getJSONObject(key)).isNotNull
        }
    }

    @Test
    fun `setting attributes on not empty cache`() {
        val attributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L"),
            "age" to SubscriberAttribute("age", "L"),
            "removeThis" to SubscriberAttribute("removeThis", null)
        )
        mockNotEmptyCache(attributes)

        underTest.setAttributes(appUserID, attributes)
        assertThat(putStringSlot.isCaptured)
        val jsonObject = JSONObject(putStringSlot.captured)
        val attributesJSON = jsonObject.getJSONObject("attributes")
        assertThat(attributesJSON).isNotNull
        assertThat(attributesJSON.length()).isEqualTo(3)
        attributes.forEach { (key, _) ->
            assertThat(attributesJSON.getJSONObject(key)).isNotNull
        }
    }

    @Test
    fun `setting attribute already existing but marking as synced`() {
        mockNotEmptyCache(mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L")
        ))
        val attributes: Map<String, SubscriberAttribute> = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = true)
        )
        underTest.setAttributes(appUserID, attributes)
        assertThat(putStringSlot.isCaptured)
        val jsonObject = JSONObject(putStringSlot.captured)
        val attributesJSON = jsonObject.getJSONObject("attributes")
        assertThat(attributesJSON).isNotNull
        assertThat(attributesJSON.length()).isEqualTo(1)
        attributes.forEach { (key, _) ->
            assertThat(attributesJSON.getJSONObject(key)).isNotNull
            assertThat(attributesJSON.getJSONObject(key).getBoolean(JSON_NAME_IS_SYNCED)).isTrue()
        }
    }

    @Test
    fun `setting existing attribute as null`() {
        mockNotEmptyCache(mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = true)
        ))
        val attributes: Map<String, SubscriberAttribute> = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", null)
        )
        underTest.setAttributes(appUserID, attributes)
        assertThat(putStringSlot.isCaptured)
        val jsonObject = JSONObject(putStringSlot.captured)
        val attributesJSON = jsonObject.getJSONObject("attributes")
        assertThat(attributesJSON).isNotNull
        assertThat(attributesJSON.length()).isEqualTo(1)
        attributes.forEach { (key, _) ->
            assertThat(attributesJSON.getJSONObject(key)).isNotNull
            assertThat(attributesJSON.getJSONObject(key).get(JSON_NAME_VALUE)).isEqualTo(null)
            assertThat(attributesJSON.getJSONObject(key).getBoolean(JSON_NAME_IS_SYNCED)).isFalse()
        }
    }

    @Test
    fun `setting existing attribute`() {
        mockNotEmptyCache(mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L")
        ))
        val attributes: Map<String, SubscriberAttribute> = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "M")
        )
        underTest.setAttributes(appUserID, attributes)
        assertThat(putStringSlot.isCaptured)
        val jsonObject = JSONObject(putStringSlot.captured)
        val attributesJSON = jsonObject.getJSONObject("attributes")
        assertThat(attributesJSON).isNotNull
        assertThat(attributesJSON.length()).isEqualTo(1)
        attributes.forEach { (key, _) ->
            assertThat(attributesJSON.getJSONObject(key)).isNotNull
            assertThat(attributesJSON.getJSONObject(key).get(JSON_NAME_VALUE)).isEqualTo("M")
            assertThat(attributesJSON.getJSONObject(key).getBoolean(JSON_NAME_IS_SYNCED)).isFalse()
        }
    }


    @Test
    fun `setting non existing attribute`() {
        val attributes: List<SubscriberAttribute> = listOf(
            SubscriberAttribute("tshirtsize", "L", setTime = Date(1)),
            SubscriberAttribute("shoesize", "32", setTime = Date(2))
        )
        mockNotEmptyCache(mapOf(attributes[0].key.serverValue to attributes[0]))
        underTest.setAttributes(appUserID, mapOf(attributes[1].key.serverValue to attributes[1]))
        assertThat(putStringSlot.isCaptured)
        val jsonObject = JSONObject(putStringSlot.captured)
        val attributesJSON = jsonObject.getJSONObject("attributes")
        assertThat(attributesJSON).isNotNull
        assertThat(attributesJSON.length()).isEqualTo(2)

        attributes.forEach {
            val attributeJSONObject = attributesJSON.getJSONObject(it.key.serverValue)
            assertThat(attributeJSONObject).isNotNull
            assertThat(attributeJSONObject.getBoolean(JSON_NAME_IS_SYNCED)).isFalse()
            assertThat(attributeJSONObject.getString(JSON_NAME_KEY)).isEqualTo(it.key.serverValue)
            assertThat(attributeJSONObject.getString(JSON_NAME_VALUE)).isEqualTo(it.value)
            assertThat(Date(attributeJSONObject.getLong(JSON_NAME_SET_TIME))).isEqualTo(it.setTime)

        }
    }

    private fun mockEmptyCache() {
        every {
            mockPrefs.getString(
                "com.revenuecat.purchases.$apiKey.subscriberAttributes.$appUserID",
                any()
            )
        } returns null
    }

    private fun mockNotEmptyCache(cacheContents: Map<String, SubscriberAttribute>) {
        every {
            mockPrefs.getString(
                "com.revenuecat.purchases.$apiKey.subscriberAttributes.$appUserID",
                any()
            )
        } returns JSONObject().put("attributes", JSONObject().also {
            cacheContents.forEach { (key, subscriberAttribute) ->
                it.put(key, subscriberAttribute.toJSONObject())
            }
        }).toString()
    }
}