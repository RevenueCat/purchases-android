package com.revenuecat.purchases.common.networking

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
              "config_version": "v1",
              "api_sources": [
                {
                  "id": "primary",
                  "url_prefix": "https://api.revenuecat.com",
                  "priority": 0,
                  "weight": 100,
                  "blacklist_time_seconds": 300
                }
              ],
              "asset_sources": [
                {
                  "id": "cloudfront-primary",
                  "url_format": "https://assets.revenuecat.com/rc_app_1234/{blob_ref}",
                  "priority": 0,
                  "weight": 100,
                  "blacklist_time_seconds": 600,
                  "test_url": "/health"
                }
              ],
              "manifest": {
                "topics": {
                  "product_entitlement_mapping": {
                    "DEFAULT": {
                      "asset_blob_ref": "abc123",
                      "content_type": "application/json",
                      "prefetch": true
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<RemoteConfigResponse>(payload)

        assertThat(response.configVersion).isEqualTo("v1")
        assertThat(response.apiSources).containsExactly(
            ApiSource(
                id = "primary",
                urlPrefix = "https://api.revenuecat.com",
                priority = 0,
                weight = 100,
                blacklistTimeSeconds = 300L,
            ),
        )
        assertThat(response.assetSources).containsExactly(
            AssetSource(
                id = "cloudfront-primary",
                urlFormat = "https://assets.revenuecat.com/rc_app_1234/{blob_ref}",
                priority = 0,
                weight = 100,
                blacklistTimeSeconds = 600L,
                testUrl = "/health",
            ),
        )
        assertThat(response.manifest.topics).containsExactly(
            entry(
                Topic.PRODUCT_ENTITLEMENT_MAPPING,
                mapOf(
                    "DEFAULT" to TopicEntry(
                        assetBlobRef = "abc123",
                        contentType = "application/json",
                        prefetch = true,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `AssetSource testUrl is optional and defaults to null`() {
        val payload = """
            {
              "config_version": "v1",
              "asset_sources": [
                {
                  "id": "primary",
                  "url_format": "https://assets.example/{blob_ref}",
                  "priority": 0,
                  "weight": 100,
                  "blacklist_time_seconds": 60
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<RemoteConfigResponse>(payload)

        assertThat(response.assetSources).hasSize(1)
        assertThat(response.assetSources[0].testUrl).isNull()
    }

    @Test
    fun `TopicEntry prefetch defaults to false when missing`() {
        val payload = """
            {
              "config_version": "v1",
              "manifest": {
                "topics": {
                  "product_entitlement_mapping": {
                    "DEFAULT": {
                      "asset_blob_ref": "xyz",
                      "content_type": "application/json"
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<RemoteConfigResponse>(payload)

        val entry = response.manifest.topics[Topic.PRODUCT_ENTITLEMENT_MAPPING]?.get("DEFAULT")
        assertThat(entry?.prefetch).isFalse
    }

    @Test
    fun `missing config_version is rejected`() {
        val payload = """
            {
              "api_sources": [],
              "asset_sources": []
            }
        """.trimIndent()

        assertThatThrownBy { json.decodeFromString<RemoteConfigResponse>(payload) }
            .isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `wrong type for config_version is rejected`() {
        val payload = """{"config_version": 12345}"""

        assertThatThrownBy { json.decodeFromString<RemoteConfigResponse>(payload) }
            .isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `missing api_sources, asset_sources, manifest fall back to defaults`() {
        val response = json.decodeFromString<RemoteConfigResponse>("""{"config_version": "v1"}""")

        assertThat(response.apiSources).isEmpty()
        assertThat(response.assetSources).isEmpty()
        assertThat(response.manifest.topics).isEmpty()
    }

    @Test
    fun `unknown sibling fields anywhere are ignored`() {
        val payload = """
            {
              "config_version": "v1",
              "future_top_level": true,
              "api_sources": [
                {
                  "id": "primary",
                  "url_prefix": "https://api.example",
                  "priority": 0,
                  "weight": 100,
                  "blacklist_time_seconds": 300,
                  "future_field": "ignored"
                }
              ],
              "asset_sources": [],
              "manifest": {
                "topics": {
                  "product_entitlement_mapping": {
                    "DEFAULT": {
                      "asset_blob_ref": "abc",
                      "content_type": "application/json",
                      "future_per_entry": 7
                    }
                  }
                },
                "future_manifest_field": []
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<RemoteConfigResponse>(payload)

        assertThat(response.configVersion).isEqualTo("v1")
        assertThat(response.apiSources).hasSize(1)
        val pem = response.manifest.topics[Topic.PRODUCT_ENTITLEMENT_MAPPING]
        assertThat(pem?.get("DEFAULT")?.assetBlobRef).isEqualTo("abc")
    }

    @Test
    fun `TopicsMapSerializer drops unknown topic names but keeps known ones`() {
        val payload = """
            {
              "config_version": "v1",
              "manifest": {
                "topics": {
                  "product_entitlement_mapping": {
                    "DEFAULT": {
                      "asset_blob_ref": "abc",
                      "content_type": "application/json"
                    }
                  },
                  "future_topic": {
                    "DEFAULT": {
                      "asset_blob_ref": "def",
                      "content_type": "application/json"
                    }
                  },
                  "another_unknown": {
                    "DEFAULT": {
                      "asset_blob_ref": "ghi",
                      "content_type": "application/json"
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
              "config_version": "v1",
              "manifest": {
                "topics": {
                  "future_topic": {
                    "DEFAULT": {
                      "asset_blob_ref": "abc",
                      "content_type": "application/json"
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
              "config_version": "v1",
              "manifest": {"topics": {}}
            }
        """.trimIndent()

        val response = json.decodeFromString<RemoteConfigResponse>(payload)

        assertThat(response.manifest.topics).isEmpty()
    }

    @Test
    fun `TopicsMapSerializer preserves multiple variant keys for a known topic`() {
        val payload = """
            {
              "config_version": "v1",
              "manifest": {
                "topics": {
                  "product_entitlement_mapping": {
                    "DEFAULT": {
                      "asset_blob_ref": "default-blob",
                      "content_type": "application/json"
                    },
                    "EXPERIMENT_A": {
                      "asset_blob_ref": "experiment-blob",
                      "content_type": "application/json",
                      "prefetch": true
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<RemoteConfigResponse>(payload)

        val variants = response.manifest.topics[Topic.PRODUCT_ENTITLEMENT_MAPPING]
        assertThat(variants?.keys).containsExactlyInAnyOrder("DEFAULT", "EXPERIMENT_A")
        assertThat(variants?.get("EXPERIMENT_A")?.prefetch).isTrue
    }

    @Test
    fun `TopicsMapSerializer encodes Topic enum back to its wire key`() {
        val manifest = Manifest(
            topics = mapOf(
                Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf(
                    "DEFAULT" to TopicEntry(
                        assetBlobRef = "abc",
                        contentType = "application/json",
                        prefetch = true,
                    ),
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
            configVersion = "v1",
            apiSources = listOf(
                ApiSource(
                    id = "primary",
                    urlPrefix = "https://api.example",
                    priority = 0,
                    weight = 100,
                    blacklistTimeSeconds = 300L,
                ),
            ),
            assetSources = listOf(
                AssetSource(
                    id = "cdn",
                    urlFormat = "https://assets.example/{blob_ref}",
                    priority = 0,
                    weight = 100,
                    blacklistTimeSeconds = 600L,
                    testUrl = "/health",
                ),
            ),
            manifest = Manifest(
                topics = mapOf(
                    Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf(
                        "DEFAULT" to TopicEntry(
                            assetBlobRef = "abc",
                            contentType = "application/json",
                            prefetch = true,
                        ),
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
