package com.revenuecat.purchases.subscriberattributes

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.subscriberattributes.caching.SubscriberAttributesCache
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

/**
 * [SubscriberAttributesCache.mergeRemoteAttributes] against real `SharedPreferences`, so what's asserted is what
 * a later read of the cache, or a fresh SDK instance, would find.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SubscriberAttributesCacheMergeTests {

    private val apiKey = "api_key"
    private val now = Date(1_700_000_000_000)
    private val earlier = Date(now.time - 60_000)
    private val later = Date(now.time + 60_000)

    private lateinit var preferences: SharedPreferences
    private lateinit var deviceCache: DeviceCache
    private lateinit var underTest: SubscriberAttributesCache

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = context.getSharedPreferences("subscriber_attributes_merge_test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        deviceCache = spyk(DeviceCache(preferences, apiKey))
        underTest = SubscriberAttributesCache(deviceCache)
    }

    @Test
    fun `merging into an empty cache stores every remote attribute`() {
        val email = attribute("\$email", "user@example.com", synced = true, at = now)
        val name = attribute("\$displayName", "Dave", synced = true, at = now)

        underTest.mergeRemoteAttributes("waldo", remote(email, name))

        assertThat(underTest.getAllStoredSubscriberAttributes("waldo"))
            .isEqualTo(mapOf("\$email" to email, "\$displayName" to name))
    }

    @Test
    fun `merged attributes survive a fresh cache instance`() {
        val band = attribute("band", "Rush", synced = true, at = now)

        underTest.mergeRemoteAttributes("waldo", remote(band))

        val reloaded = SubscriberAttributesCache(DeviceCache(preferences, apiKey))
        assertThat(reloaded.getAllStoredSubscriberAttributes("waldo")).isEqualTo(mapOf("band" to band))
    }

    @Test
    fun `merged remote attributes are not reported as unsynced`() {
        underTest.mergeRemoteAttributes("waldo", remote(attribute("band", "Rush", synced = true, at = now)))

        assertThat(underTest.getUnsyncedSubscriberAttributes("waldo")).isEmpty()
        assertThat(underTest.getUnsyncedSubscriberAttributes()).isEmpty()
    }

    @Test
    fun `a newer remote attribute replaces an older unsynced local one, so it's no longer posted`() {
        underTest.setAttributes("waldo", mapOf("band" to attribute("band", "Rush", synced = false, at = earlier)))
        val remote = attribute("band", "Yes", synced = true, at = now)

        underTest.mergeRemoteAttributes("waldo", remote(remote))

        assertThat(underTest.getAllStoredSubscriberAttributes("waldo")).isEqualTo(mapOf("band" to remote))
        assertThat(underTest.getUnsyncedSubscriberAttributes("waldo")).isEmpty()
    }

    @Test
    fun `a newer unsynced local attribute survives the merge and is still posted`() {
        val local = attribute("band", "Rush", synced = false, at = later)
        underTest.setAttributes("waldo", mapOf("band" to local))

        underTest.mergeRemoteAttributes("waldo", remote(attribute("band", "Yes", synced = true, at = now)))

        assertThat(underTest.getUnsyncedSubscriberAttributes("waldo")).isEqualTo(mapOf("band" to local))
    }

    @Test
    fun `a synced attribute the server no longer has is removed from the cache`() {
        underTest.setAttributes("waldo", mapOf("song" to attribute("song", "YYZ", synced = true, at = now)))
        val band = attribute("band", "Rush", synced = true, at = now)

        underTest.mergeRemoteAttributes("waldo", remote(band))

        assertThat(underTest.getAllStoredSubscriberAttributes("waldo")).isEqualTo(mapOf("band" to band))
    }

    @Test
    fun `merging an empty remote set removes the user once nothing is left`() {
        underTest.setAttributes("waldo", mapOf("song" to attribute("song", "YYZ", synced = true, at = now)))

        underTest.mergeRemoteAttributes("waldo", remote())

        assertThat(underTest.getAllStoredSubscriberAttributes()).doesNotContainKey("waldo")
    }

    @Test
    fun `merging only touches the given user`() {
        val carmens = attribute("band", "Rush", synced = false, at = now)
        val carmensSynced = attribute("song", "YYZ", synced = true, at = now)
        underTest.setAttributes("carmen", mapOf("band" to carmens, "song" to carmensSynced))
        val remote = attribute("band", "Yes", synced = true, at = later)

        underTest.mergeRemoteAttributes("waldo", remote(remote))

        assertThat(underTest.getAllStoredSubscriberAttributes("carmen"))
            .isEqualTo(mapOf("band" to carmens, "song" to carmensSynced))
        assertThat(underTest.getAllStoredSubscriberAttributes("waldo")).isEqualTo(mapOf("band" to remote))
    }

    @Test
    fun `merging doesn't write when nothing changed`() {
        val band = attribute("band", "Rush", synced = true, at = now)
        underTest.setAttributes("waldo", mapOf("band" to band))

        underTest.mergeRemoteAttributes("waldo", remote(band))

        verify(exactly = 1) { deviceCache.putString(underTest.subscriberAttributesCacheKey, any()) }
    }

    @Test
    fun `merging an empty remote set into an empty cache doesn't write`() {
        underTest.mergeRemoteAttributes("waldo", remote())

        verify(exactly = 0) { deviceCache.putString(underTest.subscriberAttributesCacheKey, any()) }
        assertThat(underTest.getAllStoredSubscriberAttributes("waldo")).isEmpty()
    }

    @Test
    fun `local attributes with unreadable remote entries are left as they were`() {
        val synced = attribute("band", "Rush", synced = true, at = now)
        val unsynced = attribute("song", "YYZ", synced = false, at = now)
        underTest.setAttributes("waldo", mapOf("band" to synced, "song" to unsynced))

        underTest.mergeRemoteAttributes(
            "waldo",
            RemoteSubscriberAttributes(emptyMap(), unreadableKeys = setOf("band", "song")),
        )

        assertThat(underTest.getAllStoredSubscriberAttributes("waldo"))
            .isEqualTo(mapOf("band" to synced, "song" to unsynced))
    }

    @Test
    fun `a cache that isn't valid JSON is replaced by the merge`() {
        preferences.edit().putString(underTest.subscriberAttributesCacheKey, "{not json").commit()
        val band = attribute("band", "Rush", synced = true, at = now)

        underTest.mergeRemoteAttributes("waldo", remote(band))

        assertThat(underTest.getAllStoredSubscriberAttributes("waldo")).isEqualTo(mapOf("band" to band))
    }

    private fun remote(vararg attributes: SubscriberAttribute) =
        RemoteSubscriberAttributes(attributes.associateBy { it.key.backendKey })
}
