package com.revenuecat.purchases.subscriberattributes

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfoOriginalSource
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * [SubscriberAttributesManager.mergeAttributes] and [SubscriberAttributesManager.getStoredAttributes] over a real
 * cache, including the ways the response or the cache can be broken.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SubscriberAttributesManagerMergeTests {

    private val appUserID = "waldo"
    private val now = Date(REMOTE_SET_TIME_MS)
    private val later = Date(REMOTE_SET_TIME_MS + 60_000)

    private lateinit var preferences: SharedPreferences
    private lateinit var attributesCache: SubscriberAttributesCache
    private lateinit var underTest: SubscriberAttributesManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = context.getSharedPreferences("subscriber_attributes_manager_merge_test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        attributesCache = spyk(SubscriberAttributesCache(DeviceCache(preferences, apiKey = "api_key")))
        underTest = SubscriberAttributesManager(
            attributesCache,
            backend = mockk(),
            deviceIdentifiersFetcher = mockk(),
            automaticDeviceIdentifierCollectionEnabled = false,
        )
    }

    // region mergeAttributes

    @Test
    fun `merges the customer info's attributes as synced`() {
        val info = customerInfoWithSubscriberAttributes(
            remoteAttributesJSON("\$email" to remoteEntry("user@example.com"), "band" to remoteEntry("Rush")),
        )

        underTest.mergeAttributes(info, appUserID)

        assertThat(underTest.getStoredAttributes(appUserID)).isEqualTo(
            mapOf(
                "\$email" to attribute("\$email", "user@example.com", synced = true, at = remoteSetTime),
                "band" to attribute("band", "Rush", synced = true, at = remoteSetTime),
            ),
        )
    }

    @Test
    fun `merges only for the given app user ID`() {
        underTest.mergeAttributes(
            customerInfoWithSubscriberAttributes(remoteAttributesJSON("band" to remoteEntry("Rush"))),
            appUserID,
        )

        assertThat(underTest.getStoredAttributes("another_user")).isEmpty()
    }

    @Test
    fun `customer info without subscriber attributes leaves the cache alone`() {
        val synced = attribute("band", "Rush", synced = true, at = now)
        val unsynced = attribute("song", "YYZ", synced = false, at = now)
        attributesCache.setAttributes(appUserID, mapOf("band" to synced, "song" to unsynced))

        underTest.mergeAttributes(customerInfoWithSubscriberAttributes(null), appUserID)

        assertThat(underTest.getStoredAttributes(appUserID)).isEqualTo(mapOf("band" to synced, "song" to unsynced))
    }

    @Test
    fun `empty subscriber attributes keep unsynced local attributes`() {
        val unsynced = attribute("song", "YYZ", synced = false, at = now)
        attributesCache.setAttributes(appUserID, mapOf("song" to unsynced))

        underTest.mergeAttributes(customerInfoWithSubscriberAttributes(JSONObject()), appUserID)

        assertThat(underTest.getStoredAttributes(appUserID)).isEqualTo(mapOf("song" to unsynced))
    }

    @Test
    fun `keeps a newer unsynced local attribute`() {
        val local = attribute("band", "Yes", synced = false, at = later)
        attributesCache.setAttributes(appUserID, mapOf("band" to local))

        underTest.mergeAttributes(
            customerInfoWithSubscriberAttributes(remoteAttributesJSON("band" to remoteEntry("Rush"))),
            appUserID,
        )

        assertThat(underTest.getStoredAttributes(appUserID)).isEqualTo(mapOf("band" to local))
    }

    @Test
    fun `overrides an older local attribute`() {
        attributesCache.setAttributes(
            appUserID,
            mapOf("band" to attribute("band", "Yes", synced = false, at = Date(REMOTE_SET_TIME_MS - 60_000))),
        )

        underTest.mergeAttributes(
            customerInfoWithSubscriberAttributes(remoteAttributesJSON("band" to remoteEntry("Rush"))),
            appUserID,
        )

        assertThat(underTest.getStoredAttributes(appUserID))
            .isEqualTo(mapOf("band" to attribute("band", "Rush", synced = true, at = remoteSetTime)))
    }

    @Test
    fun `ignores offline customer info`() {
        val info = customerInfoWithSubscriberAttributes(
            remoteAttributesJSON("band" to remoteEntry("Rush")),
            originalSource = CustomerInfoOriginalSource.OFFLINE_ENTITLEMENTS,
        )

        underTest.mergeAttributes(info, appUserID)

        assertThat(underTest.getStoredAttributes(appUserID)).isEmpty()
    }

    @Test
    fun `merges load shedder customer info`() {
        val info = customerInfoWithSubscriberAttributes(
            remoteAttributesJSON("band" to remoteEntry("Rush")),
            originalSource = CustomerInfoOriginalSource.LOAD_SHEDDER,
        )

        underTest.mergeAttributes(info, appUserID)

        assertThat(underTest.getStoredAttributes(appUserID).keys).containsExactly("band")
    }

    @Test
    fun `malformed subscriber attributes leave the cache alone`() {
        val synced = attribute("band", "Rush", synced = true, at = now)
        attributesCache.setAttributes(appUserID, mapOf("band" to synced))

        listOf(JSONArray(), "band", 42).forEach { malformed ->
            underTest.mergeAttributes(customerInfoWithSubscriberAttributes(malformed), appUserID)
        }

        assertThat(underTest.getStoredAttributes(appUserID)).isEqualTo(mapOf("band" to synced))
    }

    @Test
    fun `a malformed entry keeps its local attribute while the rest merge`() {
        val synced = attribute("band", "Rush", synced = true, at = now)
        attributesCache.setAttributes(appUserID, mapOf("band" to synced))

        underTest.mergeAttributes(
            customerInfoWithSubscriberAttributes(
                remoteAttributesJSON("band" to "garbage", "song" to remoteEntry("YYZ")),
            ),
            appUserID,
        )

        assertThat(underTest.getStoredAttributes(appUserID)).isEqualTo(
            mapOf(
                "band" to synced,
                "song" to attribute("song", "YYZ", synced = true, at = remoteSetTime),
            ),
        )
    }

    @Test
    fun `a cache in an unexpected shape isn't thrown or overwritten`() {
        val corrupt = JSONObject().put("attributes", JSONObject().put(appUserID, "not an object")).toString()
        preferences.edit().putString(attributesCache.subscriberAttributesCacheKey, corrupt).commit()

        assertThatCode {
            underTest.mergeAttributes(
                customerInfoWithSubscriberAttributes(remoteAttributesJSON("band" to remoteEntry("Rush"))),
                appUserID,
            )
        }.doesNotThrowAnyException()

        assertThat(preferences.getString(attributesCache.subscriberAttributesCacheKey, null)).isEqualTo(corrupt)
    }

    @Test
    fun `an exception from the cache is logged, not thrown`() {
        every { attributesCache.mergeRemoteAttributes(any(), any()) } throws IllegalStateException("boom")

        assertThatCode {
            underTest.mergeAttributes(
                customerInfoWithSubscriberAttributes(remoteAttributesJSON("band" to remoteEntry("Rush"))),
                appUserID,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `concurrent local writes and merges don't lose unsynced attributes`() {
        val iterations = 200
        val start = CountDownLatch(1)
        val failures = ConcurrentLinkedQueue<Throwable>()
        fun worker(block: () -> Unit) = thread {
            try {
                start.await()
                block()
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                failures += e
            }
        }
        val setter = worker {
            repeat(iterations) { underTest.setAttributes(mapOf("local_$it" to "value_$it"), appUserID) }
        }
        val merger = worker {
            repeat(iterations) {
                underTest.mergeAttributes(
                    customerInfoWithSubscriberAttributes(
                        remoteAttributesJSON("remote" to remoteEntry("value_$it", updatedAtMs = it.toLong())),
                    ),
                    appUserID,
                )
            }
        }
        start.countDown()
        setter.join(TimeUnit.SECONDS.toMillis(30))
        merger.join(TimeUnit.SECONDS.toMillis(30))

        assertThat(failures).isEmpty()
        assertThat(setter.isAlive || merger.isAlive).isFalse()
        val stored = underTest.getStoredAttributes(appUserID)
        val unsynced = stored.filterValues { !it.isSynced }
        assertThat(unsynced.keys).containsExactlyInAnyOrderElementsOf((0 until iterations).map { "local_$it" })
        assertThat(unsynced.mapValues { it.value.value })
            .isEqualTo((0 until iterations).associate { "local_$it" to "value_$it" })
        assertThat(stored.getValue("remote").value).isEqualTo("value_${iterations - 1}")
    }

    // endregion mergeAttributes

    // region getStoredAttributes

    @Test
    fun `stored attributes include synced, unsynced and deleted attributes`() {
        val synced = attribute("band", "Rush", synced = true, at = now)
        val unsynced = attribute("song", "YYZ", synced = false, at = now)
        val deleted = attribute("\$email", null, synced = false, at = now)
        attributesCache.setAttributes(appUserID, mapOf("band" to synced, "song" to unsynced, "\$email" to deleted))

        assertThat(underTest.getStoredAttributes(appUserID))
            .isEqualTo(mapOf("band" to synced, "song" to unsynced, "\$email" to deleted))
    }

    @Test
    fun `stored attributes are empty, not thrown, when the cache is in an unexpected shape`() {
        val corrupt = JSONObject().put("attributes", JSONObject().put(appUserID, JSONArray())).toString()
        preferences.edit().putString(attributesCache.subscriberAttributesCacheKey, corrupt).commit()

        assertThat(underTest.getStoredAttributes(appUserID)).isEmpty()
    }

    @Test
    fun `stored attributes are empty, not thrown, when the cache has no attributes object`() {
        preferences.edit().putString(attributesCache.subscriberAttributesCacheKey, "{}").commit()

        assertThat(underTest.getStoredAttributes(appUserID)).isEmpty()
    }

    // endregion getStoredAttributes
}
