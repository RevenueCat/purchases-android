package com.revenuecat.purchases.common.remoteconfig

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class RemoteConfigResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserializes full payload`() {
        val payload = """
            {
              "api_sources": [
                {
                  "id": "primary",
                  "url": "https://api.revenuecat.com/",
                  "priority": 0,
                  "weight": 100
                }
              ],
              "blob_sources": [
                {
                  "id": "cloudfront-primary",
                  "url_format": "https://assets.revenuecat.com/rc_app_1234/{blob_ref}",
                  "priority": 0,
                  "weight": 100
                }
              ],
              "manifest": {
                "topics": {
                  "product_entitlement_mapping": {
                    "DEFAULT": {
                      "blob_ref": "abc123"
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<RemoteConfigResponse>(payload)

        assertThat(response.apiSources).containsExactly(
            ApiSource(
                id = "primary",
                url = "https://api.revenuecat.com/",
                priority = 0,
                weight = 100,
            ),
        )
        assertThat(response.blobSources).containsExactly(
            BlobSource(
                id = "cloudfront-primary",
                urlFormat = "https://assets.revenuecat.com/rc_app_1234/{blob_ref}",
                priority = 0,
                weight = 100,
            ),
        )
        assertThat(response.manifest.topics).containsExactly(
            entry(
                Topic.PRODUCT_ENTITLEMENT_MAPPING,
                mapOf("DEFAULT" to TopicEntry(blobRef = "abc123")),
            ),
        )
    }

    @Test
    fun `missing sources and manifest fall back to defaults`() {
        val response = json.decodeFromString<RemoteConfigResponse>("""{}""")

        assertThat(response.apiSources).isEmpty()
        assertThat(response.blobSources).isEmpty()
        assertThat(response.manifest.topics).isEmpty()
    }

    @Test
    fun `unknown sibling fields anywhere are ignored`() {
        val payload = """
            {
              "future_top_level": true,
              "api_sources": [
                {
                  "id": "primary",
                  "url": "https://api.revenuecat.com/",
                  "priority": 0,
                  "weight": 100,
                  "future_field": "ignored"
                }
              ],
              "blob_sources": [
                {
                  "id": "primary",
                  "url_format": "https://assets.example/{blob_ref}",
                  "priority": 0,
                  "weight": 100,
                  "future_field": "ignored"
                }
              ],
              "manifest": {
                "future_manifest_field": [],
                "topics": {
                  "product_entitlement_mapping": {
                    "DEFAULT": {
                      "blob_ref": "abc",
                      "future_per_entry": 7
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<RemoteConfigResponse>(payload)

        assertThat(response.apiSources).hasSize(1)
        assertThat(response.apiSources[0].id).isEqualTo("primary")
        assertThat(response.blobSources).hasSize(1)
        assertThat(response.blobSources[0].id).isEqualTo("primary")
        val pem = response.manifest.topics[Topic.PRODUCT_ENTITLEMENT_MAPPING]
        assertThat(pem?.get("DEFAULT")?.blobRef).isEqualTo("abc")
    }

    @Test
    fun `wrong type for blob_sources is rejected`() {
        val payload = """{"blob_sources": "not-an-array"}"""

        assertThatThrownBy { json.decodeFromString<RemoteConfigResponse>(payload) }
            .isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `wrong type for api_sources is rejected`() {
        val payload = """{"api_sources": "not-an-array"}"""

        assertThatThrownBy { json.decodeFromString<RemoteConfigResponse>(payload) }
            .isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `TopicsMapSerializer drops unknown topic names but keeps known ones`() {
        val payload = """
            {
              "manifest": {
                "topics": {
                  "product_entitlement_mapping": {
                    "DEFAULT": {
                      "blob_ref": "abc"
                    }
                  },
                  "future_topic": {
                    "DEFAULT": {
                      "blob_ref": "def"
                    }
                  },
                  "another_unknown": {
                    "DEFAULT": {
                      "blob_ref": "ghi"
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<RemoteConfigResponse>(payload)

        assertThat(response.manifest.topics.keys).containsExactly(Topic.PRODUCT_ENTITLEMENT_MAPPING)
    }

    @Test
    fun `TopicsMapSerializer returns empty map when no topics are recognized`() {
        val payload = """
            {
              "manifest": {
                "topics": {
                  "future_topic": {
                    "DEFAULT": {
                      "blob_ref": "abc"
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<RemoteConfigResponse>(payload)

        assertThat(response.manifest.topics).isEmpty()
    }

    @Test
    fun `TopicsMapSerializer handles empty topics object`() {
        val payload = """
            {
              "manifest": {"topics": {}}
            }
        """.trimIndent()

        val response = json.decodeFromString<RemoteConfigResponse>(payload)

        assertThat(response.manifest.topics).isEmpty()
    }

    @Test
    fun `TopicsMapSerializer preserves multiple entryId keys for a known topic`() {
        val payload = """
            {
              "manifest": {
                "topics": {
                  "product_entitlement_mapping": {
                    "DEFAULT": {
                      "blob_ref": "default-blob"
                    },
                    "EXPERIMENT_A": {
                      "blob_ref": "experiment-blob"
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<RemoteConfigResponse>(payload)

        val entries = response.manifest.topics[Topic.PRODUCT_ENTITLEMENT_MAPPING]
        assertThat(entries?.keys).containsExactlyInAnyOrder("DEFAULT", "EXPERIMENT_A")
        assertThat(entries?.get("DEFAULT")?.blobRef).isEqualTo("default-blob")
        assertThat(entries?.get("EXPERIMENT_A")?.blobRef).isEqualTo("experiment-blob")
    }

    @Test
    fun `TopicsMapSerializer encodes Topic enum back to its wire key`() {
        val manifest = Manifest(
            topics = mapOf(
                Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf(
                    "DEFAULT" to TopicEntry(blobRef = "abc"),
                ),
            ),
        )

        val encoded = json.encodeToString(Manifest.serializer(), manifest)

        assertThat(encoded).contains("\"product_entitlement_mapping\"")
        assertThat(encoded).doesNotContain("PRODUCT_ENTITLEMENT_MAPPING")
    }

    @Test
    fun `round trip preserves known topics`() {
        val original = RemoteConfigResponse(
            apiSources = listOf(
                ApiSource(
                    id = "primary",
                    url = "https://api.revenuecat.com/",
                    priority = 0,
                    weight = 100,
                ),
            ),
            blobSources = listOf(
                BlobSource(
                    id = "cdn",
                    urlFormat = "https://assets.example/{blob_ref}",
                    priority = 0,
                    weight = 100,
                ),
            ),
            manifest = Manifest(
                topics = mapOf(
                    Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf(
                        "DEFAULT" to TopicEntry(blobRef = "abc"),
                    ),
                ),
            ),
        )

        val encoded = json.encodeToString(RemoteConfigResponse.serializer(), original)
        val decoded = json.decodeFromString<RemoteConfigResponse>(encoded)

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `Topic fromKey returns matching enum for known wire key`() {
        assertThat(Topic.fromKey("product_entitlement_mapping"))
            .isEqualTo(Topic.PRODUCT_ENTITLEMENT_MAPPING)
    }

    @Test
    fun `Topic fromKey returns null for unknown wire key`() {
        assertThat(Topic.fromKey("future_topic")).isNull()
        assertThat(Topic.fromKey("PRODUCT_ENTITLEMENT_MAPPING")).isNull()
        assertThat(Topic.fromKey("")).isNull()
    }

    private fun <K, V> entry(key: K, value: V): Map.Entry<K, V> =
        java.util.AbstractMap.SimpleEntry(key, value)
}
