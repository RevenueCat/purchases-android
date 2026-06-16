package com.revenuecat.purchases.common.remoteconfig

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConfigurationResponseTest {

    @Test
    fun `parses a full first response`() {
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
        """.trimIndent()

        val response = ConfigurationResponse.parse(payload.toByteArray())

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
        """.trimIndent()

        val response = ConfigurationResponse.parse(payload.toByteArray())

        // manifest lists every topic; topics carries only the changed one.
        assertThat(response.manifest.topics.keys)
            .containsExactlyInAnyOrder("sources", "product_entitlement_mapping")
        assertThat(response.topics.keys).containsExactly("sources")
        assertThat(response.topics.getValue("sources").getValue("blob").blobRef).isEqualTo("sourcesBlob")
    }

    @Test
    fun `parses a no-changed-topics response (204-equivalent body)`() {
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
        """.trimIndent()

        val response = ConfigurationResponse.parse(payload.toByteArray())

        assertThat(response.topics).isEmpty()
        assertThat(response.manifest.topics).containsExactlyEntriesOf(mapOf("sources" to "etag1"))
        assertThat(response.manifest.prefetchBlobs).isEmpty()
    }

    @Test
    fun `keeps unknown topic names so the SDK stays forward-compatible`() {
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
        """.trimIndent()

        val response = ConfigurationResponse.parse(payload.toByteArray())

        assertThat(response.manifest.topics).containsKey("future_topic")
        assertThat(response.topics).containsKey("future_topic")
        assertThat(response.topics.getValue("future_topic").getValue("default").blobRef)
            .isEqualTo("futureBlob")
    }

    @Test
    fun `ignores unknown item metadata keys`() {
        val payload = """
            {
              "domain": "app",
              "manifest": { "domain": "app", "topics": { "sources": "etag1" } },
              "topics": {
                "sources": {
                  "blob": { "blob_ref": "sourcesBlob", "prefetch": true, "future_field": "ignored" }
                }
              }
            }
        """.trimIndent()

        val response = ConfigurationResponse.parse(payload.toByteArray())

        val item = response.topics.getValue("sources").getValue("blob")
        assertThat(item.blobRef).isEqualTo("sourcesBlob")
        assertThat(item.prefetch).isTrue
    }

    @Test
    fun `applies defaults when optional fields are absent`() {
        val payload = """
            {
              "domain": "app",
              "manifest": { "domain": "app" }
            }
        """.trimIndent()

        val response = ConfigurationResponse.parse(payload.toByteArray())

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
        val payload = """
            {
              "domain": "app",
              "manifest": { "domain": "app", "topics": { "sources": "etag1" } },
              "topics": { "sources": { "api": {} } }
            }
        """.trimIndent()

        val response = ConfigurationResponse.parse(payload.toByteArray())

        val item = response.topics.getValue("sources").getValue("api")
        assertThat(item.blobRef).isNull()
        assertThat(item.prefetch).isFalse
    }
}
