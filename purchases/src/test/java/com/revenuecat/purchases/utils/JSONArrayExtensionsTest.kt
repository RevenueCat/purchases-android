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
class JSONArrayExtensionsTest {
    private val fromList = listOf(
        5,
        5.5,
        true,
        "five",
        listOf(
            "six",
            "seven",
            mapOf(
                "map" to "deep"
            ),
        ),
        mapOf(
            "string" to "five",
            "more_dictionary" to mapOf(
                "map" to "deep"
            ),
        )
    )

    @Test
    fun `test toList`() {
        val jsonArray = JSONArray(fromList)

        val toList = jsonArray.toList<Any>()!!

        assertThat(toList[0]).isEqualTo(5)
        assertThat(toList[1]).isEqualTo(5.5)
        assertThat(toList[2]).isEqualTo(true)
        assertThat(toList[3]).isEqualTo("five")

        assertThat(toList[4]).isEqualTo(
            listOf(
                "six",
                "seven",
                mapOf(
                    "map" to "deep"
                ),
            )
        )
        assertThat(toList[5]).isEqualTo(
            mapOf(
                "string" to "five",
                "more_dictionary" to mapOf(
                    "map" to "deep"
                ),
            )
        )
    }

    // region asSequence

    @Test
    fun `asSequence empty array yields empty sequence`() {
        val arr = JSONArray()
        assertThat(arr.asSequence().toList()).isEmpty()
    }

    @Test
    fun `asSequence yields primitives and null in order`() {
        val arr = JSONArray().apply {
            put(1)
            put("a")
            put(JSONObject.NULL)
            put(true)
        }
        val list = arr.asSequence().toList()
        assertThat(list).hasSize(4)
        assertThat(list[0]).isEqualTo(1)
        assertThat(list[1]).isEqualTo("a")
        assertThat(list[2]).isEqualTo(JSONObject.NULL)
        assertThat(list[3]).isEqualTo(true)
    }

    @Test
    fun `asSequence yields nested JSONObject and JSONArray`() {
        val obj = JSONObject().apply { put("key", "value") }
        val nestedArr = JSONArray().apply { put(1) }
        val arr = JSONArray().apply {
            put(obj)
            put(nestedArr)
        }
        val list = arr.asSequence().toList()
        assertThat(list).hasSize(2)
        assertThat(list[0]).isSameAs(obj)
        assertThat((list[0] as JSONObject).getString("key")).isEqualTo("value")
        assertThat(list[1]).isSameAs(nestedArr)
        assertThat((list[1] as JSONArray).getInt(0)).isEqualTo(1)
    }

    // endregion

    // region objects

    @Test
    fun `objects returns JSONObject elements in order`() {
        val obj1 = JSONObject().apply { put("id", "a") }
        val obj2 = JSONObject().apply { put("id", "b") }
        val arr = JSONArray().apply {
            put(obj1)
            put(obj2)
        }
        val list = arr.objects().toList()
        assertThat(list).hasSize(2)
        assertThat(list[0].getString("id")).isEqualTo("a")
        assertThat(list[1].getString("id")).isEqualTo("b")
    }

    @Test
    fun `objects filters out non JSONObject and preserves order`() {
        val obj1 = JSONObject().apply { put("id", "first") }
        val obj2 = JSONObject().apply { put("id", "second") }
        val arr = JSONArray().apply {
            put(obj1)
            put("string")
            put(42)
            put(obj2)
        }
        val list = arr.objects().toList()
        assertThat(list).hasSize(2)
        assertThat(list[0].getString("id")).isEqualTo("first")
        assertThat(list[1].getString("id")).isEqualTo("second")
    }

    @Test
    fun `objects empty array returns empty sequence`() {
        val arr = JSONArray()
        assertThat(arr.objects().toList()).isEmpty()
    }

    @Test
    fun `objects array of primitives returns empty sequence`() {
        val arr = JSONArray().apply {
            put(1)
            put("a")
            put(true)
        }
        assertThat(arr.objects().toList()).isEmpty()
    }

    // endregion
}