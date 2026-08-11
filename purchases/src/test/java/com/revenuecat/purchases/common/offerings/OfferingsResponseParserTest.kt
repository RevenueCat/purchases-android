package com.revenuecat.purchases.common.offerings

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfferingsResponseParserTest {

    @Test
    fun `elides paywall_components out of the returned json`() {
        val payload = offeringsPayload(offeringWithComponents("offering_1"))

        val result = OfferingsResponseParser.parse(payload)

        val offeringJson = result.json.getJSONArray("offerings").getJSONObject(0)
        assertThat(offeringJson.isNull("paywall_components")).isTrue
        assertThat(result.paywallComponents).hasSize(1)
        assertThat(result.paywallComponents[0]).isNotNull
    }

    @Test
    fun `extracted component text reparses to exactly the original subtree`() {
        val payload = offeringsPayload(offeringWithComponents("offering_1"))

        val result = OfferingsResponseParser.parse(payload)

        val originalComponents = JSONObject(payload)
            .getJSONArray("offerings")
            .getJSONObject(0)
            .getJSONObject("paywall_components")
        val extracted = JSONObject(result.paywallComponents[0]!!.text())
        assertThat(extracted.toString()).isEqualTo(originalComponents.toString())
    }

    @Test
    fun `captures the top-level keys of the components object`() {
        val payload = offeringsPayload(offeringWithComponents("offering_1"))

        val result = OfferingsResponseParser.parse(payload)

        assertThat(result.paywallComponents[0]!!.topLevelKeys).containsExactlyInAnyOrder(
            "template_name",
            "asset_base_url",
            "components_config",
            "components_localizations",
            "default_locale",
        )
    }

    @Test
    fun `braces inside string values do not confuse span detection`() {
        val componentsConfig = """{"text":"Billed at {{ product.price_per_month }}/mo. {{ product.relative_discount }} OFF"}"""
        val payload = offeringsPayload(offeringWithComponents("offering_1", componentsConfig = componentsConfig))

        val result = OfferingsResponseParser.parse(payload)

        assertThat(result.json.getJSONArray("offerings").getJSONObject(0).isNull("paywall_components")).isTrue
        val extracted = JSONObject(result.paywallComponents[0]!!.text())
        assertThat(extracted.getJSONObject("components_config").getString("text"))
            .isEqualTo("Billed at {{ product.price_per_month }}/mo. {{ product.relative_discount }} OFF")
    }

    @Test
    fun `escaped quotes inside string values do not confuse span detection`() {
        val componentsConfig = """{"text":"Save \"big\" today"}"""
        val payload = offeringsPayload(offeringWithComponents("offering_1", componentsConfig = componentsConfig))

        val result = OfferingsResponseParser.parse(payload)

        assertThat(result.json.getJSONArray("offerings").getJSONObject(0).isNull("paywall_components")).isTrue
        val extracted = JSONObject(result.paywallComponents[0]!!.text())
        assertThat(extracted.getJSONObject("components_config").getString("text")).isEqualTo("Save \"big\" today")
    }

    @Test
    fun `nested objects and arrays inside paywall_components are spanned correctly`() {
        val componentsConfig = """
            {
                "type": "stack",
                "children": [
                    {"type": "text", "value": "Hello"},
                    {"type": "stack", "children": [{"type": "text", "value": "Nested"}]}
                ]
            }
        """.trimIndent()
        val payload = offeringsPayload(offeringWithComponents("offering_1", componentsConfig = componentsConfig))

        val result = OfferingsResponseParser.parse(payload)

        val originalComponents = JSONObject(payload)
            .getJSONArray("offerings")
            .getJSONObject(0)
            .getJSONObject("paywall_components")
        val extracted = JSONObject(result.paywallComponents[0]!!.text())
        assertThat(extracted.toString()).isEqualTo(originalComponents.toString())
        assertThat(
            extracted.getJSONObject("components_config").getJSONArray("children").length(),
        ).isEqualTo(2)
    }

    @Test
    fun `offerings without paywall_components keep the list index-aligned`() {
        val payload = offeringsPayload(
            offeringWithComponents("offering_1"),
            offeringWithoutComponents("offering_2"),
            offeringWithComponents("offering_3"),
        )

        val result = OfferingsResponseParser.parse(payload)

        assertThat(result.paywallComponents).hasSize(3)
        assertThat(result.paywallComponents[0]).isNotNull
        assertThat(result.paywallComponents[1]).isNull()
        assertThat(result.paywallComponents[2]).isNotNull
    }

    @Test
    fun `paywall_components as the last key of the last offering is elided correctly`() {
        // language=JSON
        val payload = """
            {
                "offerings": [
                    {
                        "identifier": "offering_1",
                        "description": "d",
                        "packages": [],
                        "paywall_components": ${sampleComponentsJson()}
                    }
                ],
                "current_offering_id": "offering_1"
            }
        """.trimIndent()

        val result = OfferingsResponseParser.parse(payload)

        val offeringJson = result.json.getJSONArray("offerings").getJSONObject(0)
        assertThat(offeringJson.isNull("paywall_components")).isTrue
        assertThat(result.paywallComponents).hasSize(1)
        assertThat(result.paywallComponents[0]).isNotNull
    }

    @Test
    fun `the elided tree preserves every non-components key`() {
        // language=JSON
        val payload = """
            {
                "offerings": [
                    {
                        "identifier": "offering_1",
                        "description": "The base offering",
                        "metadata": {"title": "Premium"},
                        "packages": [{"identifier": "monthly", "platform_product_identifier": "prod_1"}],
                        "web_checkout_url": "https://example.com/checkout",
                        "paywall_components": ${sampleComponentsJson()}
                    }
                ],
                "current_offering_id": "offering_1",
                "ui_config": {"app_icon": "icon.png"},
                "targeting": {"revision": 1, "rule_id": "abc123"},
                "placements": {"fallback_offering_id": "offering_1", "offering_ids_by_placement": {"a": "offering_1"}}
            }
        """.trimIndent()

        val result = OfferingsResponseParser.parse(payload)

        assertThat(result.json.getString("current_offering_id")).isEqualTo("offering_1")
        assertThat(result.json.getJSONObject("ui_config").getString("app_icon")).isEqualTo("icon.png")
        assertThat(result.json.getJSONObject("targeting").getInt("revision")).isEqualTo(1)
        assertThat(result.json.getJSONObject("targeting").getString("rule_id")).isEqualTo("abc123")
        assertThat(result.json.getJSONObject("placements").getString("fallback_offering_id"))
            .isEqualTo("offering_1")
        val offeringJson = result.json.getJSONArray("offerings").getJSONObject(0)
        assertThat(offeringJson.getString("identifier")).isEqualTo("offering_1")
        assertThat(offeringJson.getString("description")).isEqualTo("The base offering")
        assertThat(offeringJson.getJSONObject("metadata").getString("title")).isEqualTo("Premium")
        assertThat(offeringJson.getJSONArray("packages").length()).isEqualTo(1)
        assertThat(
            offeringJson.getJSONArray("packages").getJSONObject(0).getString("platform_product_identifier"),
        ).isEqualTo("prod_1")
        assertThat(offeringJson.getString("web_checkout_url")).isEqualTo("https://example.com/checkout")
        assertThat(offeringJson.isNull("paywall_components")).isTrue
    }

    @Test
    fun `a response shape without an offerings array falls back to a plain full parse`() {
        val payload = """{"foo": "bar"}"""

        val result = OfferingsResponseParser.parse(payload)

        assertThat(result.json.getString("foo")).isEqualTo("bar")
        assertThat(result.paywallComponents).isEmpty()
    }

    @Test
    fun `lenient single-quoted JSON defeats the strict scanner but still parses via the fallback`() {
        val payload = "{'offerings': [{'identifier':'id','description':'d','packages':[]}]," +
            "'current_offering_id': 'id'}"

        val result = OfferingsResponseParser.parse(payload)

        assertThat(result.json.getJSONArray("offerings").length()).isEqualTo(1)
        assertThat(result.paywallComponents).isEmpty()
    }

    @Test
    fun `a genuinely invalid payload throws JSONException like a plain parse would`() {
        val payload = "this is not json"

        assertThatThrownBy { OfferingsResponseParser.parse(payload) }.isInstanceOf(JSONException::class.java)
    }

    @Test
    fun `a duplicate offerings key falls back rather than desynchronizing the component list`() {
        val payload = "{\"offerings\": [${offeringWithComponents("offering_1")}], " +
            "\"offerings\": [${offeringWithoutComponents("offering_2")}], \"current_offering_id\": null}"

        val result = OfferingsResponseParser.parse(payload)

        assertThat(result.paywallComponents).isEmpty()
        assertThat(result.json.getJSONArray("offerings").getJSONObject(0).getString("identifier"))
            .isEqualTo("offering_2")
    }

    @Test
    fun `a duplicate paywall_components key falls back rather than eliding two spans for one entry`() {
        val payload = offeringsPayload(
            """
            {
                "identifier": "offering_1",
                "description": "d",
                "packages": [],
                "paywall_components": ${sampleComponentsJson()},
                "paywall_components": ${sampleComponentsJson()}
            }
            """.trimIndent(),
        )

        val result = OfferingsResponseParser.parse(payload)

        assertThat(result.paywallComponents).isEmpty()
        assertThat(
            result.json.getJSONArray("offerings").getJSONObject(0).getJSONObject("paywall_components")
                .getString("default_locale"),
        ).isEqualTo("en_US")
    }

    @Test
    fun `an escaped key falls back so it is not misjudged against the raw comparison`() {
        // Unescapes to `paywall_components`, which only the full parse resolves.
        val escapedKey = "paywall_com\\u0070onents"
        val payload = "{\"offerings\": [{\"identifier\": \"offering_1\", \"description\": \"d\", " +
            "\"packages\": [], \"$escapedKey\": ${sampleComponentsJson()}}], \"current_offering_id\": null}"

        val result = OfferingsResponseParser.parse(payload)

        assertThat(result.paywallComponents).isEmpty()
        assertThat(
            result.json.getJSONArray("offerings").getJSONObject(0).getJSONObject("paywall_components")
                .getString("template_name"),
        ).isEqualTo("template_1")
    }

    @Test
    fun `a malformed escape inside components is carried through rather than failing the whole response`() {
        // org.json rejects a truncated \u, so eliding the span moves that failure from the response parse to
        // the components decode, leaving the other offerings usable.
        val payload = offeringsPayload(
            offeringWithComponents("offering_1", componentsConfig = """{"text":"\u12"}"""),
            offeringWithComponents("offering_2"),
        )
        assertThatThrownBy { JSONObject(payload) }.isInstanceOf(JSONException::class.java)

        val result = OfferingsResponseParser.parse(payload)

        assertThat(result.json.getJSONArray("offerings").length()).isEqualTo(2)
        assertThat(result.paywallComponents.filterNotNull()).hasSize(2)
        assertThat(result.paywallComponents[0]!!.text()).contains("""\u12""")
        assertThat(result.paywallComponents[1]!!.text()).contains("template_1")
    }

    private fun sampleComponentsJson(): String = """
        {
            "template_name": "template_1",
            "asset_base_url": "https://example.com",
            "components_config": {"type": "stack"},
            "components_localizations": {"en_US": {}},
            "default_locale": "en_US"
        }
    """.trimIndent()

    private fun offeringWithComponents(identifier: String, componentsConfig: String = """{"type": "stack"}"""): String =
        """
        {
            "identifier": "$identifier",
            "description": "d",
            "packages": [],
            "paywall_components": {
                "template_name": "template_1",
                "asset_base_url": "https://example.com",
                "components_config": $componentsConfig,
                "components_localizations": {"en_US": {}},
                "default_locale": "en_US"
            }
        }
        """.trimIndent()

    private fun offeringWithoutComponents(identifier: String): String =
        """{"identifier": "$identifier", "description": "d", "packages": []}"""

    private fun offeringsPayload(vararg offerings: String): String =
        """{"offerings": [${offerings.joinToString(",")}], "current_offering_id": null}"""
}
