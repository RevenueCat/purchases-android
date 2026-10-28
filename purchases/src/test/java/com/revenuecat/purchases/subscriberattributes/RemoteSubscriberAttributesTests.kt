package com.revenuecat.purchases.subscriberattributes

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteSubscriberAttributesTests {

    private val now = Date(1_700_000_000_000)
    private val earlier = Date(now.time - 60_000)
    private val later = Date(now.time + 60_000)

    // region Parsing

    @Test
    fun `no subscriber_attributes key means no information`() {
        assertThat(customerInfoWithSubscriberAttributes(null).remoteSubscriberAttributes()).isNull()
    }

    @Test
    fun `a null subscriber_attributes means no information`() {
        assertThat(customerInfoWithSubscriberAttributes(JSONObject.NULL).remoteSubscriberAttributes()).isNull()
    }

    @Test
    fun `a subscriber_attributes that isn't an object means no information`() {
        listOf(JSONArray().put("band"), "band", 42, true).forEach { malformed ->
            assertThat(customerInfoWithSubscriberAttributes(malformed).remoteSubscriberAttributes())
                .describedAs("subscriber_attributes = $malformed")
                .isNull()
        }
    }

    @Test
    fun `an empty subscriber_attributes means the server has no attributes`() {
        val remote = customerInfoWithSubscriberAttributes(JSONObject()).remoteSubscriberAttributes()

        assertThat(remote).isNotNull
        assertThat(remote!!.attributes).isEmpty()
        assertThat(remote.unreadableKeys).isEmpty()
    }

    @Test
    fun `entries are parsed as synced attributes set at updated_at_ms`() {
        val info = customerInfoWithSubscriberAttributes(
            remoteAttributesJSON(
                "\$email" to remoteEntry("user@example.com"),
                "band" to remoteEntry("Rush"),
            ),
        )

        val remote = info.remoteSubscriberAttributes()!!

        assertThat(remote.attributes).isEqualTo(
            mapOf(
                "\$email" to attribute("\$email", "user@example.com", synced = true, at = remoteSetTime),
                "band" to attribute("band", "Rush", synced = true, at = remoteSetTime),
            ),
        )
        assertThat(remote.attributes.getValue("\$email").key).isEqualTo(SubscriberAttributeKey.Email)
        assertThat(remote.unreadableKeys).isEmpty()
    }

    @Test
    fun `a null value is parsed as a deleted attribute`() {
        val info = customerInfoWithSubscriberAttributes(remoteAttributesJSON("band" to remoteEntry(null)))

        assertThat(info.remoteSubscriberAttributes()!!.attributes)
            .isEqualTo(mapOf("band" to attribute("band", null, synced = true, at = remoteSetTime)))
    }

    @Test
    fun `a missing value is parsed as a deleted attribute, like iOS`() {
        val info = customerInfoWithSubscriberAttributes(
            remoteAttributesJSON("band" to JSONObject().put("updated_at_ms", REMOTE_SET_TIME_MS)),
        )

        assertThat(info.remoteSubscriberAttributes()!!.attributes)
            .isEqualTo(mapOf("band" to attribute("band", null, synced = true, at = remoteSetTime)))
    }

    @Test
    fun `updated_at_ms accepts any JSON number`() {
        val info = customerInfoWithSubscriberAttributes(
            remoteAttributesJSON(
                "int" to remoteEntry("a", updatedAtMs = 1_000),
                "double" to remoteEntry("b", updatedAtMs = 1.6e12),
            ),
        )

        val attributes = info.remoteSubscriberAttributes()!!.attributes

        assertThat(attributes.getValue("int").setTime).isEqualTo(Date(1_000))
        assertThat(attributes.getValue("double").setTime).isEqualTo(Date(1_600_000_000_000))
    }

    @Test
    fun `unreadable entries are reported without dropping the readable ones`() {
        val info = customerInfoWithSubscriberAttributes(
            remoteAttributesJSON(
                "good" to remoteEntry("Rush"),
                "not_an_object" to "Rush",
                "null_entry" to JSONObject.NULL,
                "number_value" to remoteEntry(42),
                "bool_value" to remoteEntry(true),
                "object_value" to remoteEntry(JSONObject()),
                "missing_timestamp" to remoteEntry("Rush", updatedAtMs = null),
                "string_timestamp" to remoteEntry("Rush", updatedAtMs = "1600000000000"),
                "null_timestamp" to remoteEntry("Rush", updatedAtMs = JSONObject.NULL),
            ),
        )

        val remote = info.remoteSubscriberAttributes()!!

        assertThat(remote.attributes.keys).containsExactly("good")
        assertThat(remote.unreadableKeys).containsExactlyInAnyOrder(
            "not_an_object",
            "null_entry",
            "number_value",
            "bool_value",
            "object_value",
            "missing_timestamp",
            "string_timestamp",
            "null_timestamp",
        )
    }

    // endregion Parsing

    // region Merging

    @Test
    fun `merging into nothing keeps every remote attribute`() {
        val email = attribute("\$email", "user@example.com", synced = true, at = now)
        val name = attribute("\$displayName", "Dave", synced = true, at = now)

        val merged = remote(email, name).mergeIntoLocalMap(emptyMap())

        assertThat(merged).isEqualTo(mapOf(email.key.backendKey to email, name.key.backendKey to name))
    }

    @Test
    fun `a newer remote attribute overrides an older synced local one`() {
        val local = attribute("band", "Rush", synced = true, at = earlier)
        val remote = attribute("band", "Yes", synced = true, at = now)

        assertThat(remote(remote).mergeIntoLocalMap(local(local))).isEqualTo(mapOf("band" to remote))
    }

    @Test
    fun `a newer remote attribute overrides an older unsynced local one`() {
        val local = attribute("band", "Rush", synced = false, at = earlier)
        val remote = attribute("band", "Yes", synced = true, at = now)

        val merged = remote(remote).mergeIntoLocalMap(local(local))

        assertThat(merged).isEqualTo(mapOf("band" to remote))
        assertThat(merged.values.filterNot { it.isSynced }).isEmpty()
    }

    @Test
    fun `a newer unsynced local attribute is kept so it can still be posted`() {
        val local = attribute("band", "Rush", synced = false, at = now)
        val remote = attribute("band", "Yes", synced = true, at = earlier)

        assertThat(remote(remote).mergeIntoLocalMap(local(local))).isEqualTo(mapOf("band" to local))
    }

    @Test
    fun `a newer synced local attribute is kept`() {
        val local = attribute("band", "Rush", synced = true, at = now)
        val remote = attribute("band", "Yes", synced = true, at = earlier)

        assertThat(remote(remote).mergeIntoLocalMap(local(local))).isEqualTo(mapOf("band" to local))
    }

    @Test
    fun `the remote attribute wins when both were set at the same time`() {
        val local = attribute("band", "Rush", synced = false, at = now)
        val remote = attribute("band", "Yes", synced = true, at = now)

        assertThat(remote(remote).mergeIntoLocalMap(local(local))).isEqualTo(mapOf("band" to remote))
    }

    @Test
    fun `a newer local deletion is kept so it can still be posted`() {
        val local = attribute("band", null, synced = false, at = later)
        val remote = attribute("band", "Rush", synced = true, at = now)

        assertThat(remote(remote).mergeIntoLocalMap(local(local))).isEqualTo(mapOf("band" to local))
    }

    @Test
    fun `an unsynced local attribute the server doesn't have is kept`() {
        val local = attribute("song", "YYZ", synced = false, at = now)
        val remote = attribute("band", "Rush", synced = true, at = now)

        assertThat(remote(remote).mergeIntoLocalMap(local(local)))
            .isEqualTo(mapOf("song" to local, "band" to remote))
    }

    @Test
    fun `an empty remote set keeps unsynced local attributes`() {
        val local = attribute("song", "YYZ", synced = false, at = now)

        assertThat(remote().mergeIntoLocalMap(local(local))).isEqualTo(mapOf("song" to local))
    }

    @Test
    fun `a synced local attribute the server doesn't have was deleted there and is dropped`() {
        val deleted = attribute("song", "YYZ", synced = true, at = later)
        val remote = attribute("band", "Rush", synced = true, at = now)

        assertThat(remote(remote).mergeIntoLocalMap(local(deleted))).isEqualTo(mapOf("band" to remote))
    }

    @Test
    fun `an empty remote set drops every synced local attribute`() {
        val synced = attribute("song", "YYZ", synced = true, at = now)

        assertThat(remote().mergeIntoLocalMap(local(synced))).isEmpty()
    }

    @Test
    fun `local attributes whose remote entries were unreadable are kept, synced or not`() {
        val synced = attribute("band", "Rush", synced = true, at = now)
        val unsynced = attribute("song", "YYZ", synced = false, at = now)

        val merged = RemoteSubscriberAttributes(emptyMap(), unreadableKeys = setOf("band", "song"))
            .mergeIntoLocalMap(local(synced, unsynced))

        assertThat(merged).isEqualTo(mapOf("band" to synced, "song" to unsynced))
    }

    @Test
    fun `an unreadable remote entry with nothing local adds nothing`() {
        val merged = RemoteSubscriberAttributes(emptyMap(), unreadableKeys = setOf("band"))
            .mergeIntoLocalMap(emptyMap())

        assertThat(merged).isEmpty()
    }

    // endregion Merging

    private fun local(vararg attributes: SubscriberAttribute) = attributes.associateBy { it.key.backendKey }

    private fun remote(vararg attributes: SubscriberAttribute) =
        RemoteSubscriberAttributes(attributes.associateBy { it.key.backendKey })
}
