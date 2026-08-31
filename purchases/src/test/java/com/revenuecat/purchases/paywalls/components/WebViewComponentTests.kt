package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.utils.filter
import kotlinx.serialization.SerializationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.intellij.lang.annotations.Language
import org.junit.Test
import kotlin.test.assertEquals

class WebViewComponentTests {

    private val fillFitSize = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit())

    @Language("json")
    private val fullJson = """
        {
          "id": "promo_web_view",
          "type": "web_view",
          "protocol_version": 1,
          "url": "https://assets.pawwalls.com/web_bundles/123/index.html",
          "size": {
            "width": { "type": "fill" },
            "height": { "type": "fit" }
          },
          "visible": true,
          "name": "Promo web component"
        }
        """

    @Test
    fun `deserializes full Khepri schema`() {
        val actual = JsonTools.json.decodeFromString<PaywallComponent>(fullJson)

        assertThat(actual).isInstanceOf(WebViewComponent::class.java)
        val webView = actual as WebViewComponent

        assertThat(webView.id).isEqualTo("promo_web_view")
        assertThat(webView.name).isEqualTo("Promo web component")
        assertThat(webView.visible).isTrue()
        assertThat(webView.protocolVersion).isEqualTo(1)
        assertThat(webView.url).isEqualTo("https://assets.pawwalls.com/web_bundles/123/index.html")
        assertThat(webView.size).isEqualTo(fillFitSize)
    }

    @Test
    fun `ignores schema fallback field`() {
        // web_view intentionally has no native fallback stack; a supported-version payload that
        // still includes `fallback` must decode as the web view (unknown keys are ignored).
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "id": "promo_web_view",
              "protocol_version": 1,
              "url": "https://paywalls.revenuecat.com/index.html",
              "size": { "width": { "type": "fill" }, "height": { "type": "fit" } },
              "fallback": {
                "type": "stack",
                "components": []
              }
            }
            """

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(json)

        assertEquals(
            WebViewComponent(
                url = "https://paywalls.revenuecat.com/index.html",
                id = "promo_web_view",
                protocolVersion = 1,
                size = fillFitSize,
            ),
            actual,
        )
    }

    @Test
    fun `unsupported protocol_version renders the fallback component`() {
        // A version this SDK cannot service is treated like an unrecognized component: the author's
        // fallback is rendered instead of the web view.
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "protocol_version": 2,
              "url": "https://paywalls.revenuecat.com/index.html",
              "fallback": {
                "type": "stack",
                "name": "web_view_fallback",
                "components": []
              }
            }
            """

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(json)

        assertThat(actual).isInstanceOf(StackComponent::class.java)
        // Prove it is the author's fallback that came back, not some other stack.
        assertThat((actual as StackComponent).name).isEqualTo("web_view_fallback")
    }

    @Test
    fun `unsupported protocol_version renders the fallback even when the body is not valid v1 schema`() {
        // Forward-compat: a future protocol version may ship an incompatible body (here, no `url`,
        // which v1 requires). The version gate must route to the fallback BEFORE attempting to decode
        // the body as today's WebViewComponent, so this must not throw.
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "protocol_version": 2,
              "entrypoint": "https://paywalls.revenuecat.com/v2-bundle/index.html",
              "fallback": {
                "type": "stack",
                "name": "web_view_fallback",
                "components": []
              }
            }
            """

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(json)

        assertThat(actual).isInstanceOf(StackComponent::class.java)
        assertThat((actual as StackComponent).name).isEqualTo("web_view_fallback")
    }

    @Test
    fun `unsupported protocol_version without a fallback throws`() {
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "protocol_version": 2,
              "id": "promo_web_view",
              "url": "https://paywalls.revenuecat.com/index.html"
            }
            """

        assertThatThrownBy { JsonTools.json.decodeFromString<PaywallComponent>(json) }
            .isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `missing protocol_version throws even with a fallback`() {
        assertMalformedVersionThrows(protocolVersionField = "")
    }

    @Test
    fun `quoted protocol_version throws instead of being coerced`() {
        // intOrNull parses digits regardless of quoting, so a quoted "1" must not slip past the gate.
        assertMalformedVersionThrows(""""protocol_version": "1",""")
    }

    @Test
    fun `non-numeric protocol_version throws even with a fallback`() {
        assertMalformedVersionThrows(""""protocol_version": true,""")
    }

    // A malformed version is a bad response rather than a newer protocol, so it fails the whole decode
    // (default paywall) instead of rendering the author's fallback.
    private fun assertMalformedVersionThrows(protocolVersionField: String) {
        val json = """
            {
              "type": "web_view",
              "id": "promo_web_view",
              $protocolVersionField
              "url": "https://paywalls.revenuecat.com/index.html",
              "fallback": {
                "type": "stack",
                "name": "web_view_fallback",
                "components": []
              }
            }
            """

        assertThatThrownBy { JsonTools.json.decodeFromString<PaywallComponent>(json) }
            .isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `supported protocol_version with another required field missing still throws`() {
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "protocol_version": 1,
              "url": "https://paywalls.revenuecat.com/index.html"
            }
            """

        assertThatThrownBy { JsonTools.json.decodeFromString<PaywallComponent>(json) }
            .isInstanceOf(SerializationException::class.java)
    }

    @Test
    fun `ignores capabilities declared by the schema`() {
        // Isolation from external sources is expected from the server-provided CSP, so any
        // schema-declared capabilities are decoded-and-ignored rather than failing to parse.
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "id": "promo_web_view",
              "protocol_version": 1,
              "url": "https://paywalls.revenuecat.com/index.html",
              "size": { "width": { "type": "fill" }, "height": { "type": "fit" } },
              "capabilities": {
                "network_access": { "allowed_domains": ["api.segment.io"] },
                "camera": true,
                "microphone": true,
                "clipboard_write": true,
                "clipboard_read": true,
                "geolocation": true
              }
            }
            """

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(json)

        assertEquals(
            WebViewComponent(
                url = "https://paywalls.revenuecat.com/index.html",
                id = "promo_web_view",
                protocolVersion = 1,
                size = fillFitSize,
            ),
            actual,
        )
    }

    @Test
    fun `deserializes template url correctly`() {
        @Language("json")
        val templateJson = """
            {
              "type": "web_view",
              "id": "promo_web_view",
              "protocol_version": 1,
              "url": "https://paywalls.revenuecat.com/{{ custom.animal }}.html",
              "size": { "width": { "type": "fill" }, "height": { "type": "fit" } }
            }
            """

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(templateJson)

        assertEquals(
            WebViewComponent(
                url = "https://paywalls.revenuecat.com/{{ custom.animal }}.html",
                id = "promo_web_view",
                protocolVersion = 1,
                size = fillFitSize,
            ),
            actual,
        )
    }

    @Test
    fun `decodes with defaults for missing name and visible`() {
        // Only name and visible are optional; id, url, protocol_version, and size are required.
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "id": "promo_web_view",
              "protocol_version": 1,
              "url": "https://paywalls.revenuecat.com/index.html",
              "size": { "width": { "type": "fill" }, "height": { "type": "fit" } }
            }
            """

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(json)

        assertEquals(
            WebViewComponent(
                url = "https://paywalls.revenuecat.com/index.html",
                id = "promo_web_view",
                protocolVersion = 1,
                size = fillFitSize,
            ),
            actual,
        )
        val webView = actual as WebViewComponent
        assertThat(webView.name).isNull()
        assertThat(webView.visible).isNull()
    }

    @Test
    fun `filter treats web_view as a leaf component`() {
        // web_view has no child components, so filtering a tree should return only the component itself.
        val webView = WebViewComponent(
            url = "https://paywalls.revenuecat.com/index.html",
            id = "promo_web_view",
            protocolVersion = 1,
            size = fillFitSize,
        )

        val matches = webView.filter { it is WebViewComponent }

        assertThat(matches).containsExactly(webView)
    }

    @Test
    fun `decodes with no overrides by default`() {
        val actual = JsonTools.json.decodeFromString<PaywallComponent>(fullJson) as WebViewComponent

        assertThat(actual.overrides).isEmpty()
    }

    @Test
    fun `decodes a rule-based visibility override, matching the dashboard-published shape`() {
        // Regression test: a dashboard rule that shows/hides a web_view previously failed SDK
        // decoding entirely with `extra_forbidden` on `...web_view.overrides` (PWENG-152).
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "id": "promo_web_view",
              "protocol_version": 1,
              "url": "https://paywalls.revenuecat.com/index.html",
              "size": { "width": { "type": "fill" }, "height": { "type": "fit" } },
              "overrides": [
                {
                  "conditions": [{ "type": "selected" }],
                  "properties": { "visible": false }
                },
                {
                  "conditions": [{ "type": "expanded" }],
                  "properties": { "visible": true }
                }
              ]
            }
            """

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(json) as WebViewComponent

        assertThat(actual.overrides).containsExactly(
            ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.Selected),
                properties = PartialWebViewComponent(visible = false),
            ),
            ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.Expanded),
                properties = PartialWebViewComponent(visible = true),
            ),
        )
    }
}
