package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.utils.filter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.intellij.lang.annotations.Language
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Decodes [WebViewComponent] directly (not via [PaywallComponent]) because the polymorphic
 * `"web_view" ->` serializer registration lands in A8. A8 restores polymorphic decode tests.
 */
class WebViewComponentTests {

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
        val webView = JsonTools.json.decodeFromString<WebViewComponent>(fullJson)

        assertThat(webView.id).isEqualTo("promo_web_view")
        assertThat(webView.name).isEqualTo("Promo web component")
        assertThat(webView.visible).isTrue()
        assertThat(webView.protocolVersion).isEqualTo(1)
        assertThat(webView.url).isEqualTo("https://assets.pawwalls.com/web_bundles/123/index.html")
        assertThat(webView.size).isEqualTo(Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit()))
    }

    @Test
    fun `ignores schema fallback field`() {
        // web_view intentionally has no native fallback stack; older/dashboard payloads that
        // still include `fallback` must decode without error (unknown keys are ignored).
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "id": "promo_web_view",
              "protocol_version": 1,
              "url": "https://paywalls.revenuecat.com/index.html",
              "fallback": {
                "type": "stack",
                "components": []
              }
            }
            """

        val actual = JsonTools.json.decodeFromString<WebViewComponent>(json)

        assertEquals(
            WebViewComponent(url = "https://paywalls.revenuecat.com/index.html", id = "promo_web_view", protocolVersion = 1),
            actual,
        )
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

        val actual = JsonTools.json.decodeFromString<WebViewComponent>(json)

        assertEquals(
            WebViewComponent(url = "https://paywalls.revenuecat.com/index.html", id = "promo_web_view", protocolVersion = 1),
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
              "url": "https://paywalls.revenuecat.com/{{ custom.animal }}.html"
            }
            """

        val actual = JsonTools.json.decodeFromString<WebViewComponent>(templateJson)

        assertEquals(
            WebViewComponent(
                url = "https://paywalls.revenuecat.com/{{ custom.animal }}.html",
                id = "promo_web_view",
                protocolVersion = 1,
            ),
            actual,
        )
    }

    @Test
    fun `deserializes required shape with defaults for missing optional fields`() {
        // Only name, visible, and size are optional; id, url, and protocol_version are required.
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "id": "promo_web_view",
              "protocol_version": 1,
              "url": "https://paywalls.revenuecat.com/index.html"
            }
            """

        val actual = JsonTools.json.decodeFromString<WebViewComponent>(json)

        assertEquals(
            WebViewComponent(url = "https://paywalls.revenuecat.com/index.html", id = "promo_web_view", protocolVersion = 1),
            actual,
        )
        assertThat(actual.name).isNull()
        assertThat(actual.visible).isNull()
        assertThat(actual.size).isEqualTo(Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit()))
    }

    @Test
    fun `fails to decode when required id is missing`() {
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "protocol_version": 1,
              "url": "https://paywalls.revenuecat.com/index.html"
            }
            """

        assertThatThrownBy { JsonTools.json.decodeFromString<WebViewComponent>(json) }
    }

    @Test
    fun `fails to decode when required protocol_version is missing`() {
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "id": "promo_web_view",
              "url": "https://paywalls.revenuecat.com/index.html"
            }
            """

        assertThatThrownBy { JsonTools.json.decodeFromString<WebViewComponent>(json) }
    }

    @Test
    fun `filter treats web_view as a leaf component`() {
        // web_view has no child components, so filtering a tree should return only the component itself.
        val webView = WebViewComponent(
            url = "https://paywalls.revenuecat.com/index.html",
            id = "promo_web_view",
            protocolVersion = 1,
        )

        val matches = webView.filter { it is WebViewComponent }

        assertThat(matches).containsExactly(webView)
    }
}
