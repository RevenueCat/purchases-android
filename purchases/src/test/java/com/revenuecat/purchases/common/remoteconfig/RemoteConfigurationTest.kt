package com.revenuecat.purchases.common.remoteconfig

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.nio.ByteBuffer

class RemoteConfigurationTest {

    @Test
    fun `parses a full first response`() {
        // language=json
        val payload = """
            {
              "domain": "app",
              "subdomains": ["app_workflows"],
              "app_uuid": "1a2b3c4d",
              "manifest": "v1.1710000100.sources:Jc83RzcK1LqA,product_entitlement_mapping:9v1DnUu6rXbE",
              "active_topics": ["sources", "product_entitlement_mapping"],
              "prefetch_blobs": ["blobRefA", "blobRefB"],
              "topics": {
                "product_entitlement_mapping": {
                  "default": { "blob_ref": "blobRefA", "prefetch": true }
                }
              },
              "state_hash": "x3R7YvQw2NfM"
            }
        """

        val response = RemoteConfiguration.parse(payload.trimIndent().toByteArray())

        assertThat(response.domain).isEqualTo("app")
        assertThat(response.subdomains).containsExactly("app_workflows")
        assertThat(response.appUuid).isEqualTo("1a2b3c4d")
        assertThat(response.stateHash).isEqualTo("x3R7YvQw2NfM")

        // The manifest is opaque: stored verbatim, never parsed.
        assertThat(response.manifest)
            .isEqualTo("v1.1710000100.sources:Jc83RzcK1LqA,product_entitlement_mapping:9v1DnUu6rXbE")
        assertThat(response.activeTopics).containsExactly("sources", "product_entitlement_mapping")
        assertThat(response.prefetchBlobs).containsExactly("blobRefA", "blobRefB")

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
              "manifest": "v1.1710000200.sources:etag1,product_entitlement_mapping:etag2",
              "active_topics": ["sources", "product_entitlement_mapping"],
              "topics": {
                "sources": { "blob": { "blob_ref": "sourcesBlob" } }
              }
            }
        """

        val response = RemoteConfiguration.parse(payload.trimIndent().toByteArray())

        // active_topics lists every topic; topics carries only the changed one.
        assertThat(response.activeTopics)
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
              "manifest": "v1.1710000300.sources:etag1",
              "active_topics": ["sources"],
              "prefetch_blobs": [],
              "topics": {}
            }
        """

        val response = RemoteConfiguration.parse(payload.trimIndent().toByteArray())

        assertThat(response.topics).isEmpty()
        assertThat(response.activeTopics).containsExactly("sources")
        assertThat(response.prefetchBlobs).isEmpty()
    }

    @Test
    fun `keeps unknown topic names so the SDK stays forward-compatible`() {
        // language=json
        val payload = """
            {
              "domain": "app",
              "manifest": "v1.1710000400.future_topic:etagF",
              "active_topics": ["future_topic"],
              "topics": {
                "future_topic": { "default": { "blob_ref": "futureBlob" } }
              }
            }
        """

        val response = RemoteConfiguration.parse(payload.trimIndent().toByteArray())

        assertThat(response.activeTopics).contains("future_topic")
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
              "manifest": "v1.1710000500.sources:etag1",
              "active_topics": ["sources"],
              "topics": {
                "sources": {
                  "blob": { "blob_ref": "sourcesBlob", "prefetch": true, "future_field": "kept" }
                }
              }
            }
        """

        val response = RemoteConfiguration.parse(payload.trimIndent().toByteArray())

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
              "manifest": "v1.1710000600.sources:etag1",
              "active_topics": ["sources"],
              "topics": {
                "sources": {
                  "api": { "id": "primary", "url": "https://api.revenuecat.com", "priority": 100, "weight": 100 }
                }
              }
            }
        """

        val response = RemoteConfiguration.parse(payload.trimIndent().toByteArray())

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
              "manifest": "v1.1710000700."
            }
        """

        val response = RemoteConfiguration.parse(payload.trimIndent().toByteArray())

        assertThat(response.subdomains).isEmpty()
        assertThat(response.appUuid).isNull()
        assertThat(response.stateHash).isNull()
        assertThat(response.topics).isEmpty()
        assertThat(response.activeTopics).isEmpty()
        assertThat(response.prefetchBlobs).isEmpty()
    }

    @Test
    fun `item without blob_ref parses with null ref and prefetch false`() {
        // language=json
        val payload = """
            {
              "domain": "app",
              "manifest": "v1.1710000800.sources:etag1",
              "active_topics": ["sources"],
              "topics": { "sources": { "api": {} } }
            }
        """

        val response = RemoteConfiguration.parse(payload.trimIndent().toByteArray())

        val item = response.topics.getValue("sources").getValue("api")
        assertThat(item.blobRef).isNull()
        assertThat(item.prefetch).isFalse
    }

    @Test
    fun `fails to parse when the required domain is missing`() {
        // language=json
        val payload = """
            {
              "manifest": "v1.1710000900.sources:etag1"
            }
        """

        assertThatThrownBy { RemoteConfiguration.parse(payload.trimIndent().toByteArray()) }
            .isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `fails to parse when the required manifest is missing`() {
        // language=json
        val payload = """
            {
              "domain": "app"
            }
        """

        assertThatThrownBy { RemoteConfiguration.parse(payload.trimIndent().toByteArray()) }
            .isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `fails to parse malformed JSON`() {
        // language=json
        val payload = """
            {
              "domain": "app",
              "manifest": "v1.123
        """

        assertThatThrownBy { RemoteConfiguration.parse(payload.trimIndent().toByteArray()) }
            .isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `fails to parse when a field has the wrong type`() {
        // manifest must be a string, not an object.
        // language=json
        val payload = """
            {
              "domain": "app",
              "manifest": { "topics": {} }
            }
        """

        assertThatThrownBy { RemoteConfiguration.parse(payload.trimIndent().toByteArray()) }
            .isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `parses from a ByteBuffer without consuming the caller's buffer`() {
        val payload = """
            {
              "domain": "app",
              "manifest": { "domain": "app", "topics": { "sources": "etag1" } }
            }
        """.trimIndent()
        val buffer = ByteBuffer.wrap(payload.toByteArray())

        val response = RemoteConfiguration.parse(buffer)

        assertThat(response.domain).isEqualTo("app")
        assertThat(response.manifest.topics).containsExactlyEntriesOf(mapOf("sources" to "etag1"))
        // The overload duplicates the buffer, so the caller's position is untouched.
        assertThat(buffer.position()).isEqualTo(0)
    }
}
