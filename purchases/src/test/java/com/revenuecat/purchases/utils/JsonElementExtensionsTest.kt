package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JsonElementExtensionsTest {
    @Test
    fun `can convert map with different types of elements`() {
        val mapOfElements: JsonElement = JsonObject(
            mapOf(
                "int" to JsonPrimitive(123),
                "bool" to JsonPrimitive(true),
                "float" to JsonPrimitive(1234.4f),
                "string" to JsonPrimitive("my_text"),
                "list" to JsonArray(listOf(JsonPrimitive(123), JsonPrimitive(456), JsonPrimitive(789))),
                "map" to JsonObject(mapOf("key1" to JsonPrimitive(123), "key2" to JsonPrimitive(true)))
            )
        )
        assertThat(mapOfElements.asMap()).isEqualTo(
            mapOf(
                "int" to 123,
                "bool" to true,
                "float" to 1234.4f,
                "string" to "my_text",
                "list" to listOf(123, 456, 789),
                "map" to mapOf("key1" to 123, "key2" to true)
            )
        )
    }
}
