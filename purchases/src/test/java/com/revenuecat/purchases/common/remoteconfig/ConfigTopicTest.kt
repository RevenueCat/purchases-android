package com.revenuecat.purchases.common.remoteconfig

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConfigTopicTest {

    @Test
    fun `two identical topics have the same hash`() {
        val a = topic("""{ "api": { "url": "https://a.com", "priority": 100, "weight": 50 } }""")
        val b = topic("""{ "api": { "url": "https://a.com", "priority": 100, "weight": 50 } }""")

        assertThat(a.hash).isEqualTo(b.hash)
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

        assertThat(a.hash).isEqualTo(b.hash)
    }

    @Test
    fun `reordering keys inside an item does not change the hash`() {
        val a = topic("""{ "api": { "url": "https://a.com", "priority": 100, "weight": 50 } }""")
        val b = topic("""{ "api": { "weight": 50, "url": "https://a.com", "priority": 100 } }""")

        assertThat(a.hash).isEqualTo(b.hash)
    }

    @Test
    fun `reordering nested object keys does not change the hash`() {
        val a = topic("""{ "api": { "meta": { "region": "us", "tier": "primary" } } }""")
        val b = topic("""{ "api": { "meta": { "tier": "primary", "region": "us" } } }""")

        assertThat(a.hash).isEqualTo(b.hash)
    }

    @Test
    fun `different item values produce a different hash`() {
        val a = topic("""{ "api": { "url": "https://a.com" } }""")
        val b = topic("""{ "api": { "url": "https://b.com" } }""")

        assertThat(a.hash).isNotEqualTo(b.hash)
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

        assertThat(a.hash).isNotEqualTo(b.hash)
    }

    @Test
    fun `array order is significant`() {
        val a = topic("""{ "api": { "hosts": ["a", "b"] } }""")
        val b = topic("""{ "api": { "hosts": ["b", "a"] } }""")

        assertThat(a.hash).isNotEqualTo(b.hash)
    }

    @Test
    fun `reserved keys are part of the hash`() {
        val a = topic("""{ "default": { "blob_ref": "blobA" } }""")
        val b = topic("""{ "default": { "blob_ref": "blobB" } }""")

        assertThat(a.hash).isNotEqualTo(b.hash)
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
