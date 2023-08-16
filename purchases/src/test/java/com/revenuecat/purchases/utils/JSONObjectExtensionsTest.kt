package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class JSONObjectExtensionsTest {
    private val fromMap = mapOf(
        "int" to 5,
        "double" to 5.5,
        "boolean" to true,
        "string" to "five",
        "array" to listOf(
            "five",
            mapOf(
                "map" to "deep"
            ),
        ),
        "dictionary" to mapOf(
            "string" to "five",
            "more_dictionary" to mapOf(
                "map" to "deep"
            )
        )
    )

    @Test
    fun `test toMap`() {
        val jsonObject = JSONObject(fromMap)

        val toMap = jsonObject.toMap<Any>()!!

        assertThat(toMap["int"]).isEqualTo(5)
        assertThat(toMap["double"]).isEqualTo(5.5)
        assertThat(toMap["boolean"]).isEqualTo(true)
        assertThat(toMap["string"]).isEqualTo("five")

        assertThat(toMap["array"]).isExactlyInstanceOf(JSONArray::class.java)
        assertThat(toMap["dictionary"]).isExactlyInstanceOf(JSONObject::class.java)
    }

    @Test
    fun `test toMap with deep`() {
        val jsonObject = JSONObject(fromMap)

        val toMap = jsonObject.toMap<Any>(deep=true)!!

        assertThat(toMap["int"]).isEqualTo(5)
        assertThat(toMap["double"]).isEqualTo(5.5)
        assertThat(toMap["boolean"]).isEqualTo(true)
        assertThat(toMap["string"]).isEqualTo("five")

        assertThat(toMap["array"]).isEqualTo(
            listOf(
                "five",
                mapOf(
                    "map" to "deep"
                ),
            )
        )
        assertThat(toMap["dictionary"]).isEqualTo(
            mapOf(
                "string" to "five",
                "more_dictionary" to mapOf(
                    "map" to "deep",
                ),
            ),
        )
    }
}