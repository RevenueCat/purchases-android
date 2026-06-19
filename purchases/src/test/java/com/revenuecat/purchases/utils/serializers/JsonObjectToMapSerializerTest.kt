package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JsonObjectToMapSerializerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Wrapper(
        @Serializable(with = JsonObjectToMapSerializer::class)
        val metadata: Map<String, @Contextual Any> = emptyMap(),
    )

    @Test
    fun `deserializes empty object`() {
        val result = json.decodeFromString<Wrapper>("""{"metadata": {}}""")
        assertThat(result.metadata).isEmpty()
    }

    @Test
    fun `deserializes string values`() {
        val result = json.decodeFromString<Wrapper>("""{"metadata": {"key": "value"}}""")
        assertThat(result.metadata["key"]).isEqualTo("value")
    }

    @Test
    fun `deserializes int values`() {
        val result = json.decodeFromString<Wrapper>("""{"metadata": {"count": 42}}""")
        assertThat(result.metadata["count"]).isEqualTo(42)
    }

    @Test
    fun `deserializes long values`() {
        val result = json.decodeFromString<Wrapper>("""{"metadata": {"big": 9999999999}}""")
        assertThat(result.metadata["big"]).isEqualTo(9999999999L)
    }

    @Test
    fun `deserializes double values`() {
        val result = json.decodeFromString<Wrapper>("""{"metadata": {"price": 9.99}}""")
        assertThat(result.metadata["price"]).isEqualTo(9.99)
    }

    @Test
    fun `deserializes boolean values`() {
        val result = json.decodeFromString<Wrapper>("""{"metadata": {"enabled": true, "disabled": false}}""")
        assertThat(result.metadata["enabled"]).isEqualTo(true)
        assertThat(result.metadata["disabled"]).isEqualTo(false)
    }

    @Test
    fun `deserializes nested objects`() {
        val result = json.decodeFromString<Wrapper>(
            """{"metadata": {"nested": {"inner_key": "inner_value"}}}""",
        )
        assertThat(result.metadata["nested"]).isEqualTo(mapOf("inner_key" to "inner_value"))
    }

    @Test
    fun `deserializes arrays`() {
        val result = json.decodeFromString<Wrapper>(
            """{"metadata": {"tags": ["a", "b", "c"]}}""",
        )
        assertThat(result.metadata["tags"]).isEqualTo(listOf("a", "b", "c"))
    }

    @Test
    fun `null values are excluded from map`() {
        val result = json.decodeFromString<Wrapper>(
            """{"metadata": {"present": "yes", "absent": null}}""",
        )
        assertThat(result.metadata).containsKey("present")
        assertThat(result.metadata).doesNotContainKey("absent")
    }

    @Test
    fun `missing metadata defaults to empty map`() {
        val result = json.decodeFromString<Wrapper>("""{}""")
        assertThat(result.metadata).isEmpty()
    }

    @Test
    fun `deserializes mixed types`() {
        val result = json.decodeFromString<Wrapper>(
            """
            {
                "metadata": {
                    "string": "hello",
                    "int": 1,
                    "double": 2.5,
                    "bool": true,
                    "list": [1, 2],
                    "map": {"a": "b"}
                }
            }
            """,
        )
        assertThat(result.metadata).hasSize(6)
        assertThat(result.metadata["string"]).isEqualTo("hello")
        assertThat(result.metadata["int"]).isEqualTo(1)
        assertThat(result.metadata["double"]).isEqualTo(2.5)
        assertThat(result.metadata["bool"]).isEqualTo(true)
        assertThat(result.metadata["list"]).isEqualTo(listOf(1, 2))
        assertThat(result.metadata["map"]).isEqualTo(mapOf("a" to "b"))
    }
}
