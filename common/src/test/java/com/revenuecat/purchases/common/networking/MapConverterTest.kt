package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
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
    fun testConvertToJSON() {
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
    fun testConvertToJSONWithNestedArrayOfStrings() {
        val inputMap = mapOf(
            "key1" to "value1",
            "key2" to listOf("value2", "value3"),
            "key3" to mapOf("nestedKey" to "nestedValue"),
            "key4" to listOf("value4", "value5")
        )

        val expectedJson = JSONObject()
            .put("key1", "value1")
            .put("key2", JSONArray(listOf("value2", "value3")))
            .put("key3", JSONObject().put("nestedKey", "nestedValue"))
            .put("key4", JSONArray(listOf("value4", "value5")))

        val result = mapConverter.convertToJSON(inputMap)
        assertEquals(expectedJson.toString(), result.toString())
    }

    @Test
    fun `test map conversion with mocked List of String conversion`() {
        val mapConverterMock = mockk<MapConverter>()

        val inputMap = mapOf(
            "key" to listOf("value1", "value2")
        )

        val expectedIncorrectJsonArrayString = "[\"value1\",\"value2\"]"

        every {
            mapConverterMock.convertToJSON(match { it == inputMap })
        } returns JSONObject(mapOf("key" to expectedIncorrectJsonArrayString))

        val resultJson = mapConverterMock.convertToJSON(inputMap)
        val resultArrayString = resultJson.optString("key")

        assertEquals(expectedIncorrectJsonArrayString, resultArrayString)
    }

}
