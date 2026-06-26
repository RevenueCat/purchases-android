package com.revenuecat.purchases.ui.revenuecatui

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

internal class PaywallWebViewValueTest {

    @Test
    fun `fromJson converts primitives`() {
        assertThat(PaywallWebViewValue.fromJson("hello", 8)).isEqualTo(PaywallWebViewValue.String("hello"))
        assertThat(PaywallWebViewValue.fromJson(true, 8)).isEqualTo(PaywallWebViewValue.Boolean(true))
        assertThat(PaywallWebViewValue.fromJson(42, 8)).isEqualTo(PaywallWebViewValue.Number(42))
        assertThat(PaywallWebViewValue.fromJson(3.5, 8)).isEqualTo(PaywallWebViewValue.Number(3.5))
        assertThat(PaywallWebViewValue.fromJson(null, 8)).isEqualTo(PaywallWebViewValue.Null)
        assertThat(PaywallWebViewValue.fromJson(JSONObject.NULL, 8)).isEqualTo(PaywallWebViewValue.Null)
    }

    @Test
    fun `fromJson converts nested objects and arrays`() {
        val json = JSONObject("""{"a":1,"b":["x",false],"c":{"d":null}}""")

        val value = PaywallWebViewValue.fromJson(json, 8)

        assertThat(value).isInstanceOf(PaywallWebViewValue.Object::class.java)
        val obj = (value as PaywallWebViewValue.Object).value
        assertThat(obj["a"]).isEqualTo(PaywallWebViewValue.Number(1))
        assertThat(obj["b"]).isEqualTo(
            PaywallWebViewValue.Array(listOf(PaywallWebViewValue.String("x"), PaywallWebViewValue.Boolean(false))),
        )
        assertThat(obj["c"]).isEqualTo(
            PaywallWebViewValue.Object(mapOf("d" to PaywallWebViewValue.Null)),
        )
    }

    @Test
    fun `fromJson returns null when too deeply nested`() {
        val json = JSONObject("""{"a":{"b":{"c":1}}}""")

        // Allow only 2 levels: top object (1) -> a (2) -> b would exceed.
        assertThat(PaywallWebViewValue.fromJson(json, 2)).isNull()
    }

    @Test
    fun `toJsonRepresentation emits whole numbers as integers`() {
        assertThat(PaywallWebViewValue.Number(100.0).toJsonRepresentation()).isEqualTo(100L)
        assertThat(PaywallWebViewValue.Number(99.99).toJsonRepresentation()).isEqualTo(99.99)
    }

    @Test
    fun `toJsonRepresentation keeps non-finite numbers as doubles`() {
        // Infinity/NaN are not whole numbers, so they must not be collapsed to Long.
        assertThat(PaywallWebViewValue.Number(Double.POSITIVE_INFINITY).toJsonRepresentation())
            .isEqualTo(Double.POSITIVE_INFINITY)
        assertThat(PaywallWebViewValue.Number(Double.NaN).toJsonRepresentation())
            .isEqualTo(Double.NaN)
    }

    @Test
    fun `toJsonObject round-trips a map`() {
        val map = mapOf(
            "locale" to PaywallWebViewValue.String("en-US"),
            "custom" to PaywallWebViewValue.Object(
                mapOf(
                    "plan" to PaywallWebViewValue.String("annual"),
                    "count" to PaywallWebViewValue.Number(3),
                    "flag" to PaywallWebViewValue.Boolean(true),
                    "list" to PaywallWebViewValue.Array(listOf(PaywallWebViewValue.Number(1))),
                    "nothing" to PaywallWebViewValue.Null,
                ),
            ),
        )

        val json = map.toJsonObject()

        assertThat(json.getString("locale")).isEqualTo("en-US")
        val custom = json.getJSONObject("custom")
        assertThat(custom.getString("plan")).isEqualTo("annual")
        assertThat(custom.getInt("count")).isEqualTo(3)
        assertThat(custom.getBoolean("flag")).isTrue()
        assertThat(custom.get("list")).isInstanceOf(JSONArray::class.java)
        assertThat(custom.isNull("nothing")).isTrue()
    }
}
