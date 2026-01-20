package com.revenuecat.purchases.subscriberattributes

import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributeMap
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesPerAppUserIDMap
import com.revenuecat.purchases.subscriberattributes.caching.legacySubscriberAttributesCacheKey
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class SubscriberAttributesDeviceCacheTests {
    private val apiKey = "api_key"
    private val appUserID = "app_user_id"

    private lateinit var cache: DeviceCache
    private lateinit var underTest: SubscriberAttributesCache
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private val putStringSlot = slot<String>()

    @Before
    fun setup() {
        mockEditor = mockk<SharedPreferences.Editor>().apply {
            every {
                putString("com.revenuecat.purchases.$apiKey.subscriberAttributes", capture(putStringSlot))
            } answers {
                every {
                    mockPrefs.getString("com.revenuecat.purchases.$apiKey.subscriberAttributes", any())
                } returns putStringSlot.captured
                mockEditor
            }
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

        cache = DeviceCache(mockPrefs, apiKey)
        underTest = SubscriberAttributesCache(cache)

        every {
            mockPrefs.getString(cache.appUserIDCacheKey, isNull())
        } returns "appUserID"
        every {
            mockPrefs.getString(cache.legacyAppUserIDCacheKey, isNull())
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

        assertCapturedEqualsExpected(mapOf(appUserID to expectedAttributes))
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

        assertCapturedEqualsExpected(mapOf(appUserID to expectedAttributes))
    }

    @Test
    fun `setting attribute already existing but marking as synced`() {
        mockNotEmptyCache(
            mapOf(
                "tshirtsize" to SubscriberAttribute("tshirtsize", "L")
            )
        )

        val expectedAttributes: Map<String, SubscriberAttribute> = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = true)
        )
        underTest.setAttributes(appUserID, expectedAttributes)

        assertCapturedEqualsExpected(mapOf(appUserID to expectedAttributes))
    }

    @Test
    fun `setting existing attribute as null`() {
        mockNotEmptyCache(
            mapOf(
                "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = true)
            )
        )

        val expectedAttributes: Map<String, SubscriberAttribute> = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", null)
        )
        underTest.setAttributes(appUserID, expectedAttributes)

        assertCapturedEqualsExpected(mapOf(appUserID to expectedAttributes))
    }

    @Test
    fun `setting existing attribute`() {
        mockNotEmptyCache(
            mapOf(
                "tshirtsize" to SubscriberAttribute("tshirtsize", "L")
            )
        )

        val expectedAttributes: Map<String, SubscriberAttribute> = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "M")
        )
        underTest.setAttributes(appUserID, expectedAttributes)

        assertCapturedEqualsExpected(mapOf(appUserID to expectedAttributes))
    }

    @Test
    fun `setting non existing attribute`() {
        val expectedAttributes: Map<String, SubscriberAttribute> = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", setTime = Date(1)),
            "shoesize" to SubscriberAttribute("shoesize", "32", setTime = Date(2))
        )
        mockNotEmptyCache(mapOf(expectedAttributes.keys.toList()[0] to expectedAttributes.values.toList()[0]))

        underTest.setAttributes(
            appUserID,
            mapOf(expectedAttributes.keys.toList()[1] to expectedAttributes.values.toList()[1])
        )

        assertCapturedEqualsExpected(mapOf(appUserID to expectedAttributes))
    }

    @Test
    fun `clearAllSubscriberAttributesFromUser clears the synced subscriber attributes`() {
        val expectedAttributes = mapOf(
            "shoesize" to SubscriberAttribute("shoesize", "9", isSynced = true),
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = true)
        )
        mockNotEmptyCache(expectedAttributes)
        underTest.clearAllSubscriberAttributesFromUser(appUserID)
        verify(exactly = 1) {
            mockEditor.putString(
                "com.revenuecat.purchases.$apiKey.subscriberAttributes",
                JSONObject().also {
                    it.put("attributes", JSONObject())
                }.toString()
            )
        }
    }

    @Test
    fun `clearAllSubscriberAttributesFromUser also clears the unsynced subscriber attributes`() {
        val expectedAttributes = mapOf(
            "shoesize" to SubscriberAttribute("shoesize", "9", isSynced = false),
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = false)
        )
        mockNotEmptyCache(expectedAttributes)
        underTest.clearAllSubscriberAttributesFromUser(appUserID)
        verify(exactly = 1) {
            mockEditor.putString(
                "com.revenuecat.purchases.$apiKey.subscriberAttributes",
                JSONObject().also {
                    it.put("attributes", JSONObject())
                }.toString()
            )
        }
    }


    @Test
    fun `clearSubscriberAttributesIfSyncedForSubscriber also clears the synced subscriber attributes`() {
        val expectedAttributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = true)
        )
        mockNotEmptyCache(expectedAttributes)
        underTest.clearSubscriberAttributesIfSyncedForSubscriber(appUserID)
        verify {
            mockEditor.putString(
                "com.revenuecat.purchases.$apiKey.subscriberAttributes",
                JSONObject().also {
                    it.put("attributes", JSONObject())
                }.toString()
            )
        }
    }

    @Test
    fun `clearSubscriberAttributesIfSyncedForSubscriber doesn't clear the unsynced subscriber attributes`() {
        val expectedAttributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = false)
        )
        mockNotEmptyCache(expectedAttributes)
        underTest.clearSubscriberAttributesIfSyncedForSubscriber(appUserID)
        verify(exactly = 0) {
            mockEditor.putString("com.revenuecat.purchases.$apiKey.subscriberAttributes", any())
        }
    }

    @Test
    fun `Given there are two user IDs with unsynced attributes, getUnsyncedSubscriberAttributes returns a map with two users and only the attributes that are not found`() {
        val attributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L"),
            "age" to SubscriberAttribute("age", "L", isSynced = true)
        )
        val cacheContents = mapOf(
            appUserID to attributes,
            "user2" to attributes
        )
        mockNotEmptyCacheMultipleUsers(cacheContents)
        val unsyncedSubscriberAttributes = underTest.getUnsyncedSubscriberAttributes()
        unsyncedSubscriberAttributes.forEach { (_, subscriberAttributesForUser) ->
            assertThat(subscriberAttributesForUser.size).isEqualTo(1)
            assertThat(subscriberAttributesForUser["tshirtsize"]).isEqualTo(attributes["tshirtsize"])
        }

        assertThat(unsyncedSubscriberAttributes.size).isEqualTo(cacheContents.size)
        unsyncedSubscriberAttributes.keys.containsAll(cacheContents.keys)
    }

    @Test
    fun `Given there are two user IDs, but only one of them has unsynced attributes, getUnsyncedSubscriberAttributes returns a map with only the unsynced attributes for that user`() {
        val expectedAttribute = SubscriberAttribute("tshirtsize", "L")
        mockNotEmptyCacheMultipleUsers(
            mapOf(
                appUserID to mapOf(
                    "tshirtsize" to expectedAttribute,
                    "age" to SubscriberAttribute("age", "L", isSynced = true)
                ),
                "user2" to mapOf(
                    "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = true),
                    "age" to SubscriberAttribute("age", "L", isSynced = true)
                )
            )
        )
        val unsyncedSubscriberAttributes = underTest.getUnsyncedSubscriberAttributes()
        assertThat(unsyncedSubscriberAttributes.size).isEqualTo(1)
        unsyncedSubscriberAttributes.forEach { (_, subscriberAttributesForUser) ->
            assertThat(subscriberAttributesForUser.size).isEqualTo(1)
            assertThat(subscriberAttributesForUser["tshirtsize"]).isEqualTo(expectedAttribute)
        }
    }

    @Test
    fun `Given there are two user IDs, and none of them have unsynced attributes, getUnsyncedSubscriberAttributes returns an empty map`() {
        val attributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = true),
            "age" to SubscriberAttribute("age", "L", isSynced = true)
        )
        mockNotEmptyCacheMultipleUsers(
            mapOf(
                appUserID to attributes,
                "user2" to attributes
            )
        )
        val unsyncedSubscriberAttributes = underTest.getUnsyncedSubscriberAttributes()
        assertThat(unsyncedSubscriberAttributes.size).isEqualTo(0)
    }

    @Test
    fun `Given there are no legacy subscriber attributes, cleanup doesn't migrate anything`() {
        mockLegacyCacheMultipleUsers(emptyMap())
        val expectedAttributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L"),
            "age" to SubscriberAttribute("age", "L"),
            "removeThis" to SubscriberAttribute("removeThis", null)
        )

        mockNotEmptyCache(expectedAttributes)

        underTest.cleanUpSubscriberAttributeCache(appUserID, mockEditor)

        verify(exactly = 0) { mockEditor.remove(any()) }
        assertCapturedEqualsExpected(mapOf(appUserID to expectedAttributes))
    }

    @Test
    fun `Given there are legacy subscriber attributes, migration happens`() {
        val userOne = "${Random.nextInt(0, 100)}"
        val userThree = "another$userOne"

        val legacyAttributesForAppUserID = createMapOfUnsyncedAttributes(
            extraAttribute = SubscriberAttribute("attribute", "value")
        )
        val legacyCacheContents = mapOf(
            userOne to createMapOfUnsyncedAttributes(),
            appUserID to legacyAttributesForAppUserID
        )
        legacyCacheContents.keys.forEach {
            every {
                mockEditor.remove("com.revenuecat.purchases.$apiKey.subscriberAttributes.$it")
            } returns mockEditor
        }
        mockLegacyCacheMultipleUsers(legacyCacheContents)

        val newCacheAttributesForAppUserID =
            mapOf("attribute" to SubscriberAttribute("attribute", "new_value"))
        val cacheContents = mapOf(
            appUserID to newCacheAttributesForAppUserID,
            userThree to createMapOfUnsyncedAttributes()
        )
        mockNotEmptyCacheMultipleUsers(cacheContents)
        underTest.cleanUpSubscriberAttributeCache(appUserID, mockEditor)

        verify(exactly = 1) {
            mockEditor.remove(underTest.legacySubscriberAttributesCacheKey(userOne))
        }
        verify(exactly = 1) {
            mockEditor.remove(underTest.legacySubscriberAttributesCacheKey(appUserID))
        }
        val expectedAttributesForAppUserID =
            legacyAttributesForAppUserID + newCacheAttributesForAppUserID
        assertCapturedEqualsExpected(
            mapOf(
                userOne to (legacyCacheContents[userOne] ?: error("legacyCacheContents has been modified")),
                appUserID to expectedAttributesForAppUserID,
                userThree to (cacheContents[userThree] ?: error("cacheContents has been modified"))
            )
        )
    }

    @Test
    fun `Given there are legacy subscriber attributes, clean up removes already synced attributes`() {
        val userOne = "${Random.nextInt(0, 100)}"
        val userThree = "another$userOne"

        val legacyAttributesForAppUserID = createMapOfUnsyncedAttributes(
            extraAttribute = SubscriberAttribute(
                key = "attribute",
                value = "value"
            )
        )
        val legacyCacheContents = mapOf(
            userOne to createMapOfUnsyncedAttributes(allSynced = true),
            appUserID to legacyAttributesForAppUserID
        )
        legacyCacheContents.keys.forEach {
            every {
                mockEditor.remove("com.revenuecat.purchases.$apiKey.subscriberAttributes.$it")
            } returns mockEditor
        }
        mockLegacyCacheMultipleUsers(legacyCacheContents)

        val newCacheAttributesForAppUserID =
            mapOf("attribute" to SubscriberAttribute("attribute", "new_value"))
        val cacheContents = mapOf(
            appUserID to newCacheAttributesForAppUserID,
            userThree to createMapOfUnsyncedAttributes(allSynced = true)
        )
        mockNotEmptyCacheMultipleUsers(cacheContents)

        underTest.cleanUpSubscriberAttributeCache(appUserID, mockEditor)

        verify(exactly = 1) {
            mockEditor.remove(underTest.legacySubscriberAttributesCacheKey(userOne))
        }
        verify(exactly = 1) {
            mockEditor.remove(underTest.legacySubscriberAttributesCacheKey(appUserID))
        }
        assertCapturedEqualsExpected(
            mapOf(
                appUserID to newCacheAttributesForAppUserID
            )
        )
    }

    private fun createMapOfUnsyncedAttributes(
        extraAttribute: SubscriberAttribute? = null,
        allSynced: Boolean = false
    ): Map<String, SubscriberAttribute> {
        val attributesMap = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "attr${Random.nextInt(0, 100)}", isSynced = allSynced),
            "age" to SubscriberAttribute("age", "attr${Random.nextInt(0, 100)}", isSynced = allSynced)
        )
        if (extraAttribute != null) {
            return attributesMap + (extraAttribute.key.backendKey to extraAttribute)
        }
        return attributesMap
    }

    @Test
    fun `Given there are old synced attributes, they get cleaned`() {
        val expectedAttributes = mapOf(
            "tshirtsize" to SubscriberAttribute("tshirtsize", "L", isSynced = true),
            "age" to SubscriberAttribute("age", "L", isSynced = true)
        )
        val cacheContents = mapOf(
            appUserID to expectedAttributes,
            "pedro" to expectedAttributes
        )
        mockEmptyLegacyCache()
        mockNotEmptyCacheMultipleUsers(cacheContents)
        underTest.cleanUpSubscriberAttributeCache(appUserID, mockEditor)
        assertCapturedEqualsExpected(
            mapOf(
                appUserID to expectedAttributes
            )
        )
    }

    private fun mockEmptyLegacyCache() {
        every {
            mockPrefs.all
        } returns emptyMap<String, Any>().toMutableMap()
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

    private fun mockLegacyCacheMultipleUsers(cacheContents: SubscriberAttributesPerAppUserIDMap) {
        val preferencesContent = cacheContents.map { (userID, subscriberAttributeMap) ->
            val json = JSONObject().put("attributes", JSONObject().also {
                subscriberAttributeMap.forEach { (key, subscriberAttribute) ->
                    it.put(key, subscriberAttribute.toJSONObject())
                }
            }).toString()
            "com.revenuecat.purchases.$apiKey.subscriberAttributes.$userID" to json
        }.toMap()

        preferencesContent.forEach { (key, contents) ->
            every {
                mockPrefs.getString(
                    key,
                    any()
                )
            } returns contents
        }

        every {
            mockPrefs.all
        } returns preferencesContent.toMutableMap()
    }

    private fun assertCapturedEqualsExpected(
        expectedAttributes: SubscriberAttributesPerAppUserIDMap
    ) {
        assertThat(putStringSlot.isCaptured)
        val receivedAttributes =
            JSONObject(putStringSlot.captured).buildSubscriberAttributesMapPerUser()
        assertThat(receivedAttributes).isNotNull
        assertThat(receivedAttributes).hasSameSizeAs(expectedAttributes)
        expectedAttributes.forEach { (userID, expectedAttributesForUser) ->
            expectedAttributesForUser.values.map {
                val receivedAttributesForUser = receivedAttributes[userID] ?: fail("Didn't match")
                it to receivedAttributesForUser[it.key.backendKey]
            }.forEach { (expected, received) ->
                assertThat(received).isNotNull
                assertThat(expected).isEqualTo(received)
            }
        }
    }
}
