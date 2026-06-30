package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.common.JsonProvider
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConfigTopicTest {

    @Test
    fun `two identical topics have the same hash`() {
        val a = topic("""{ "api": { "url": "https://a.com", "priority": 100, "weight": 50 } }""")
        val b = topic("""{ "api": { "url": "https://a.com", "priority": 100, "weight": 50 } }""")

        assertThat(a.contentHash).isEqualTo(b.contentHash)
    }

    @Test
    fun `reordering item keys does not change the hash`() {
        val a = topic(
            """{
              "api": { "url": "https://a.com" },
              "blob": { "url": "https://b.com" }
            }""",
        )
        val b = topic(
            """{
              "blob": { "url": "https://b.com" },
              "api": { "url": "https://a.com" }
            }""",
        )

        assertThat(a.contentHash).isEqualTo(b.contentHash)
    }

    @Test
    fun `reordering keys inside an item does not change the hash`() {
        val a = topic("""{ "api": { "url": "https://a.com", "priority": 100, "weight": 50 } }""")
        val b = topic("""{ "api": { "weight": 50, "url": "https://a.com", "priority": 100 } }""")

        assertThat(a.contentHash).isEqualTo(b.contentHash)
    }

    @Test
    fun `reordering nested object keys does not change the hash`() {
        val a = topic("""{ "api": { "meta": { "region": "us", "tier": "primary" } } }""")
        val b = topic("""{ "api": { "meta": { "tier": "primary", "region": "us" } } }""")

        assertThat(a.contentHash).isEqualTo(b.contentHash)
    }

    @Test
    fun `different item values produce a different hash`() {
        val a = topic("""{ "api": { "url": "https://a.com" } }""")
        val b = topic("""{ "api": { "url": "https://b.com" } }""")

        assertThat(a.contentHash).isNotEqualTo(b.contentHash)
    }

    @Test
    fun `adding an item produces a different hash`() {
        val a = topic("""{ "api": { "url": "https://a.com" } }""")
        val b = topic(
            """{
              "api": { "url": "https://a.com" },
              "blob": { "url": "https://b.com" }
            }""",
        )

        assertThat(a.contentHash).isNotEqualTo(b.contentHash)
    }

    @Test
    fun `array order is significant`() {
        val a = topic("""{ "api": { "hosts": ["a", "b"] } }""")
        val b = topic("""{ "api": { "hosts": ["b", "a"] } }""")

        assertThat(a.contentHash).isNotEqualTo(b.contentHash)
    }

    @Test
    fun `reserved keys are part of the hash`() {
        val a = topic("""{ "default": { "blob_ref": "blobA" } }""")
        val b = topic("""{ "default": { "blob_ref": "blobB" } }""")

        assertThat(a.contentHash).isNotEqualTo(b.contentHash)
    }

    @Test
    fun `serializes as a bare item map with no wrapper or hash field`() {
        val topic = topic("""{ "api": { "url": "https://a.com", "priority": 100 } }""")

        val element = JsonProvider.defaultJson.encodeToJsonElement(ConfigTopicSerializer, topic).jsonObject

        // No wrapper object and no derived hash leaking into the serialized form.
        assertThat(element.keys).containsExactly("api")
        assertThat(element).doesNotContainKey("contentHash")
        assertThat(element).doesNotContainKey("items")
        assertThat(element.getValue("api").jsonObject["url"]?.jsonPrimitive?.content)
            .isEqualTo("https://a.com")
    }

    @Test
    fun `round-trips through serialization preserving items and hash`() {
        val original = topic(
            """{
              "api": { "url": "https://a.com", "priority": 100 },
              "blob": { "blob_ref": "b", "prefetch": true }
            }""",
        )

        val json = JsonProvider.defaultJson.encodeToString(ConfigTopicSerializer, original)
        val decoded = JsonProvider.defaultJson.decodeFromString(ConfigTopicSerializer, json)

        assertThat(decoded.items).isEqualTo(original.items)
        assertThat(decoded.contentHash).isEqualTo(original.contentHash)
    }

    private fun topic(sourcesJson: String): ConfigTopic {
        // language=json
        val payload = """
            {
              "domain": "app",
              "manifest": "v1.test.sources:etag1",
              "active_topics": ["sources"],
              "topics": { "sources": $sourcesJson }
            }
        """.trimIndent()
        return RemoteConfiguration.parse(payload.toByteArray()).topics.getValue("sources")
    }
}
