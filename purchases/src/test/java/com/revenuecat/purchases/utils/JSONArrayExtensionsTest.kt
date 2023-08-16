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
}