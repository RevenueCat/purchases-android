package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.spyk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class MapConverterTest {

    private lateinit var mapConverter: MapConverter

    @Before
    fun setUp() {
        mapConverter = MapConverter()
    }

    @Test
    fun `test convert to JSON`() {
        val inputMap = mapOf(
            "key1" to "value1",
            "key2" to listOf("value2", "value3"),
            "key3" to mapOf("nestedKey" to "nestedValue")
        )

        val expectedJson = JSONObject()
            .put("key1", "value1")
            .put("key2", JSONArray(listOf("value2", "value3")))
            .put("key3", JSONObject().put("nestedKey", "nestedValue"))

        val result = mapConverter.convertToJSON(inputMap)
        assertEquals(expectedJson.toString(), result.toString())
    }

    @Test
    fun `test convert to JSON with nested array of strings`() {
        val inputMap = mapOf(
            "key1" to "value1",
            "key2" to listOf("value2", "value3"),
            "key3" to mapOf("nestedKey" to "nestedValue"),
            "key4" to mapOf("nestedArray" to listOf("value4", "value5")),
        )

        val expectedJson = JSONObject()
            .put("key1", "value1")
            .put("key2", JSONArray(listOf("value2", "value3")))
            .put("key3", JSONObject().put("nestedKey", "nestedValue"))
            .put("key4", JSONObject().put("nestedArray", JSONArray(listOf("value4", "value5"))))

        val result = mapConverter.convertToJSON(inputMap)
        assertEquals(expectedJson.toString(), result.toString())
    }

    /**
     * This tests workaround for a bug in Android 4 , where a List<String> would be incorrectly converted into
     * a single string instead of a JSONArray of strings.
     * (i.e.: "[\"value1\", \"value2\"]" instead of "[value1, value2]")
     */
    @Test
    fun `test map conversion fixes wrong treatment of arrays of strings in JSON library`() {
        val mapConverterPartialMock = spyk<MapConverter>()

        val inputMap = mapOf(
            "product_ids" to listOf("product_1", "product_2")
        )

        val mapContainingInputMap = mapOf(
            "subscriber_info" to inputMap
        )

        val incorrectJsonArrayString = "[product_1,product_2]"
        val correctedJSONArray = JSONArray(listOf("product_1", "product_2"))

        every {
            mapConverterPartialMock.createJSONObject(match { it == inputMap })
        } returns JSONObject(mapOf("product_ids" to incorrectJsonArrayString))

        val resultJson = mapConverterPartialMock.convertToJSON(mapContainingInputMap)
        val resultArrayString = resultJson.optJSONObject("subscriber_info")?.optJSONArray("product_ids")

        assertEquals(correctedJSONArray, resultArrayString)
    }
}
