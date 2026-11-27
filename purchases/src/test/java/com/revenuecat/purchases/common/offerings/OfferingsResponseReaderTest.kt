package com.revenuecat.purchases.common.offerings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.utils.toMap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsResponseReaderTest {

    private val responseWithoutComponents = """
        {
          "current_offering_id": "offering_a",
          "offerings": [
            {
              "identifier": "offering_a",
              "description": "Line one\nLine \"two\" é中 😀 \/ \\",
              "metadata": {
                "int": 7,
                "negative": -3,
                "long": 9007199254740993,
                "double": 1.5,
                "exponent": 1e3,
                "true": true,
                "false": false,
                "null": null,
                "nested": {"list": [1, "two", 3.0, null, {"deep": []}]}
              },
              "packages": [
                {"identifier": "${'$'}rc_monthly", "platform_product_identifier": "monthly", "platform_product_plan_identifier": "p1m"}
              ],
              "paywall": {"template_name": "1", "config": {"packages": ["${'$'}rc_monthly"]}}
            },
            "not an offering"
          ],
          "placements": {"fallback_offering_id": "offering_a", "offering_ids_by_placement": {"home": null}},
          "targeting": {"revision": 2, "rule_id": "abc"},
          "ui_config": {"app": {"colors": {}, "fonts": {}}, "localizations": {"en_US": {"monthly": "Monthly"}}},
          "paywall_components": {"components_config": {"base": {"stack": {}}}}
        }
    """.trimIndent()

    @Test
    fun `a response without paywall components reads exactly as JSONObject parses it`() {
        val response = OfferingsResponseReader.read(responseWithoutComponents)

        assertThat(response.omittedPaywallComponents).isFalse
        assertThat(response.json.toString()).isEqualTo(JSONObject(responseWithoutComponents).toString())
    }

    @Test
    fun `values keep the types JSONObject gives them`() {
        fun metadataOf(json: JSONObject) =
            json.getJSONArray("offerings").getJSONObject(0).getJSONObject("metadata").toMap<Any>(deep = true)

        val metadata = metadataOf(OfferingsResponseReader.read(responseWithoutComponents).json)

        assertThat(metadata).isEqualTo(metadataOf(JSONObject(responseWithoutComponents)))
        assertThat(metadata["int"]).isEqualTo(7)
        assertThat(metadata["long"]).isEqualTo(9007199254740993L)
        assertThat(metadata["double"]).isEqualTo(1.5)
        assertThat(metadata["exponent"]).isEqualTo(1000.0)
    }

    @Test
    fun `nested contents of each offering's paywall components are omitted`() {
        val payload = OfferingsResponsePayloads.withHeavyPaywallComponents(targetBytes = 1)

        val response = OfferingsResponseReader.read(payload)

        assertThat(response.omittedPaywallComponents).isTrue
        val offerings = response.json.getJSONArray("offerings")
        assertThat(offerings.length()).isEqualTo(OfferingsResponsePayloads.OFFERING_COUNT)
        for (index in 0 until offerings.length()) {
            val components = offerings.getJSONObject(index).getJSONObject("paywall_components")
            assertThat(components.toString()).isEqualTo(
                """{"template_name":"components","asset_base_url":"https:\/\/assets.pawwalls.com",""" +
                    """"default_locale":"locale_0","revision":3,"components_config":{},""" +
                    """"components_localizations":{}}""",
            )
        }
    }

    @Test
    fun `everything outside paywall components is kept when they are omitted`() {
        val payload = OfferingsResponsePayloads.withHeavyPaywallComponents(targetBytes = 1)
        val expected = JSONObject(payload)
        val expectedOfferings = expected.getJSONArray("offerings")
        for (index in 0 until expectedOfferings.length()) {
            val components = expectedOfferings.getJSONObject(index).getJSONObject("paywall_components")
            components.put("components_config", JSONObject())
            components.put("components_localizations", JSONObject())
        }

        val response = OfferingsResponseReader.read(payload)

        assertThat(response.json.toString()).isEqualTo(expected.toString())
    }

    @Test
    fun `paywall components that are already empty are not reported as omitted`() {
        val payload = """
            {"offerings":[{"identifier":"a","paywall_components":{
              "template_name":"components","components_config":{},"components_localizations":{},"zero_states":[]
            }}]}
        """.trimIndent()

        val response = OfferingsResponseReader.read(payload)

        assertThat(response.omittedPaywallComponents).isFalse
        assertThat(response.json.toString()).isEqualTo(JSONObject(payload).toString())
    }

    @Test
    fun `serializing keeps the types of the values that are not left out`() {
        val payload = """
            {"offerings":[{"identifier":"a","description":"Line \"two\" é中 😀 / \\",
            "metadata":{"whole_double":3.0,"double":1.5,"exponent":1e3,"int":7,"long":9007199254740993,
            "null":null,"true":true,"list":[1,2.0,"three"]},
            "paywall_components":{"revision":4.0,"components_config":{"base":{}}}}]}
        """.trimIndent()
        fun offeringOf(json: JSONObject) = json.getJSONArray("offerings").getJSONObject(0)

        val serialized = OfferingsResponseReader.read(payload).serialize()

        val offering = offeringOf(JSONObject(serialized))
        val expected = offeringOf(JSONObject(payload))
        assertThat(serialized).doesNotContain("base")
        assertThat(offering.getString("description")).isEqualTo(expected.getString("description"))
        assertThat(offering.getJSONObject("metadata").toMap<Any>(deep = true))
            .isEqualTo(expected.getJSONObject("metadata").toMap<Any>(deep = true))
        assertThat(offering.getJSONObject("metadata").get("whole_double")).isEqualTo(3.0)
        assertThat(offering.getJSONObject("paywall_components").get("revision")).isEqualTo(4.0)
    }

    @Test
    fun `serializing does not throw for a number that is not finite`() {
        val payload = """
            {"offerings":[{"identifier":"a","metadata":{"list":[1e999]},
            "paywall_components":{"components_config":{"base":{}}}}]}
        """.trimIndent()

        val serialized = OfferingsResponseReader.read(payload).serialize()

        assertThat(serialized).doesNotContain("base")
        assertThat(OfferingsResponseReader.read(serialized).json.getJSONArray("offerings").length()).isEqualTo(1)
    }

    @Test
    fun `malformed responses throw a JSONException`() {
        listOf("", "   ", "not json", "[1, 2]", "{\"offerings\": [", "{\"offerings\": [}").forEach { payload ->
            assertThatThrownBy { OfferingsResponseReader.read(payload) }
                .`as`("payload <$payload>")
                .isInstanceOf(JSONException::class.java)
        }
    }
}
