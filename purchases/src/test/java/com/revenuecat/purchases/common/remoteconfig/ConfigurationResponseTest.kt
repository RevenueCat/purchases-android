package com.revenuecat.purchases.common.remoteconfig

import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConfigurationResponseTest {

    @Test
    fun `parses a full first response`() {
        // language=json
        val payload = """
            {
              "domain": "app",
              "subdomains": ["app_workflows"],
              "app_uuid": "1a2b3c4d",
              "manifest": {
                "domain": "app",
                "topics": { "sources": "Jc83RzcK1LqA", "product_entitlement_mapping": "9v1DnUu6rXbE" },
                "prefetch_blobs": ["blobRefA", "blobRefB"],
                "last_refresh_at": 1710000100
              },
              "topics": {
                "product_entitlement_mapping": {
                  "default": { "blob_ref": "blobRefA", "prefetch": true }
                }
              },
              "state_hash": "x3R7YvQw2NfM"
            }
        """

        val response = ConfigurationResponse.parse(payload.trimIndent().toByteArray())

        assertThat(response.domain).isEqualTo("app")
        assertThat(response.subdomains).containsExactly("app_workflows")
        assertThat(response.appUuid).isEqualTo("1a2b3c4d")
        assertThat(response.stateHash).isEqualTo("x3R7YvQw2NfM")

        assertThat(response.manifest.domain).isEqualTo("app")
        assertThat(response.manifest.topics)
            .containsEntry("sources", "Jc83RzcK1LqA")
            .containsEntry("product_entitlement_mapping", "9v1DnUu6rXbE")
        assertThat(response.manifest.prefetchBlobs).containsExactly("blobRefA", "blobRefB")
        assertThat(response.manifest.prefetchedBlobs).isEmpty()
        assertThat(response.manifest.lastRefreshAt).isEqualTo(1710000100L)

        val item = response.topics.getValue("product_entitlement_mapping").getValue("default")
        assertThat(item.blobRef).isEqualTo("blobRefA")
        assertThat(item.prefetch).isTrue
    }

    @Test
    fun `parses a changed-topics-only response`() {
        // language=json
        val payload = """
            {
              "domain": "app",
              "manifest": {
                "domain": "app",
                "topics": { "sources": "etag1", "product_entitlement_mapping": "etag2" }
              },
              "topics": {
                "sources": { "blob": { "blob_ref": "sourcesBlob" } }
              }
            }
        """

        val response = ConfigurationResponse.parse(payload.trimIndent().toByteArray())

        // manifest lists every topic; topics carries only the changed one.
        assertThat(response.manifest.topics.keys)
            .containsExactlyInAnyOrder("sources", "product_entitlement_mapping")
        assertThat(response.topics.keys).containsExactly("sources")
        assertThat(response.topics.getValue("sources").getValue("blob").blobRef).isEqualTo("sourcesBlob")
    }

    @Test
    fun `parses a no-changed-topics response (204-equivalent body)`() {
        // language=json
        val payload = """
            {
              "domain": "app",
              "manifest": {
                "domain": "app",
                "topics": { "sources": "etag1" },
                "prefetch_blobs": []
              },
              "topics": {}
            }
        """

        val response = ConfigurationResponse.parse(payload.trimIndent().toByteArray())

        assertThat(response.topics).isEmpty()
        assertThat(response.manifest.topics).containsExactlyEntriesOf(mapOf("sources" to "etag1"))
        assertThat(response.manifest.prefetchBlobs).isEmpty()
    }

    @Test
    fun `keeps unknown topic names so the SDK stays forward-compatible`() {
        // language=json
        val payload = """
            {
              "domain": "app",
              "manifest": {
                "domain": "app",
                "topics": { "future_topic": "etagF" }
              },
              "topics": {
                "future_topic": { "default": { "blob_ref": "futureBlob" } }
              }
            }
        """

        val response = ConfigurationResponse.parse(payload.trimIndent().toByteArray())

        assertThat(response.manifest.topics).containsKey("future_topic")
        assertThat(response.topics).containsKey("future_topic")
        assertThat(response.topics.getValue("future_topic").getValue("default").blobRef)
            .isEqualTo("futureBlob")
    }

    @Test
    fun `preserves arbitrary item content alongside the reserved keys`() {
        // language=json
        val payload = """
            {
              "domain": "app",
              "manifest": { "domain": "app", "topics": { "sources": "etag1" } },
              "topics": {
                "sources": {
                  "blob": { "blob_ref": "sourcesBlob", "prefetch": true, "future_field": "kept" }
                }
              }
            }
        """

        val response = ConfigurationResponse.parse(payload.trimIndent().toByteArray())

        val item = response.topics.getValue("sources").getValue("blob")
        assertThat(item.blobRef).isEqualTo("sourcesBlob")
        assertThat(item.prefetch).isTrue
        // Non-reserved keys are kept in content (not dropped).
        assertThat(item.content["future_field"]?.jsonPrimitive?.content).isEqualTo("kept")
        // Reserved keys are not duplicated into content.
        assertThat(item.content).doesNotContainKey("blob_ref")
        assertThat(item.content).doesNotContainKey("prefetch")
    }

    @Test
    fun `preserves a fully inline item with no blob ref`() {
        // language=json
        val payload = """
            {
              "domain": "app",
              "manifest": { "domain": "app", "topics": { "sources": "etag1" } },
              "topics": {
                "sources": {
                  "api": { "id": "primary", "url": "https://api.revenuecat.com", "priority": 100, "weight": 100 }
                }
              }
            }
        """

        val response = ConfigurationResponse.parse(payload.trimIndent().toByteArray())

        val item = response.topics.getValue("sources").getValue("api")
        assertThat(item.blobRef).isNull()
        assertThat(item.prefetch).isFalse
        assertThat(item.content["id"]?.jsonPrimitive?.content).isEqualTo("primary")
        assertThat(item.content["url"]?.jsonPrimitive?.content).isEqualTo("https://api.revenuecat.com")
        assertThat(item.content["priority"]?.jsonPrimitive?.int).isEqualTo(100)
        assertThat(item.content["weight"]?.jsonPrimitive?.int).isEqualTo(100)
    }

    @Test
    fun `applies defaults when optional fields are absent`() {
        // language=json
        val payload = """
            {
              "domain": "app",
              "manifest": { "domain": "app" }
            }
        """

        val response = ConfigurationResponse.parse(payload.trimIndent().toByteArray())

        assertThat(response.subdomains).isEmpty()
        assertThat(response.appUuid).isNull()
        assertThat(response.stateHash).isNull()
        assertThat(response.topics).isEmpty()
        assertThat(response.manifest.topics).isEmpty()
        assertThat(response.manifest.prefetchBlobs).isEmpty()
        assertThat(response.manifest.prefetchedBlobs).isEmpty()
        assertThat(response.manifest.lastRefreshAt).isEqualTo(0L)
    }

    @Test
    fun `item without blob_ref parses with null ref and prefetch false`() {
        // language=json
        val payload = """
            {
              "domain": "app",
              "manifest": { "domain": "app", "topics": { "sources": "etag1" } },
              "topics": { "sources": { "api": {} } }
            }
        """

        val response = ConfigurationResponse.parse(payload.trimIndent().toByteArray())

        val item = response.topics.getValue("sources").getValue("api")
        assertThat(item.blobRef).isNull()
        assertThat(item.prefetch).isFalse
    }
}
