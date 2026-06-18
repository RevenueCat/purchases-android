package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test
import kotlin.test.assertEquals

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
          "name": "Promo web component",
          "fallback": {
            "id": "promo_web_view_fallback",
            "type": "stack",
            "components": [
              { "type": "stack", "components": [] }
            ],
            "size": {
              "width": { "type": "fill" },
              "height": { "type": "fit" }
            },
            "dimension": {
              "type": "vertical",
              "alignment": "center",
              "distribution": "start"
            },
            "padding": { "top": 0, "bottom": 0, "leading": 0, "trailing": 0 },
            "margin": { "top": 0, "bottom": 0, "leading": 0, "trailing": 0 }
          },
          "capabilities": {
            "network_access": {
              "allowed_domains": ["api.segment.io"]
            },
            "camera": false,
            "microphone": false,
            "clipboard_write": false,
            "clipboard_read": false,
            "geolocation": false
          }
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
        assertThat(webView.size).isEqualTo(Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit))

        // The fallback is decoded as a normal Stack, and its children are not stripped.
        val fallback = webView.fallback
        assertThat(fallback).isNotNull
        assertThat(fallback!!.components).hasSize(1)
        assertThat(fallback.components.first()).isInstanceOf(StackComponent::class.java)

        // Capabilities are decoded and preserved, but not used functionally.
        val capabilities = webView.capabilities
        assertThat(capabilities).isNotNull
        assertThat(capabilities!!.networkAccess?.allowedDomains).containsExactly("api.segment.io")
        assertThat(capabilities.camera).isFalse()
        assertThat(capabilities.microphone).isFalse()
        assertThat(capabilities.clipboardWrite).isFalse()
        assertThat(capabilities.clipboardRead).isFalse()
        assertThat(capabilities.geolocation).isFalse()
    }

    @Test
    fun `deserializes template url correctly`() {
        @Language("json")
        val templateJson = """
            {
              "type": "web_view",
              "protocol_version": 1,
              "url": "https://paywalls.revenuecat.com/{{ custom.animal }}.html"
            }
            """

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(templateJson)

        assertEquals(
            WebViewComponent(
                url = "https://paywalls.revenuecat.com/{{ custom.animal }}.html",
                protocolVersion = 1,
            ),
            actual,
        )
    }

    @Test
    fun `deserializes minimal shape with defaults for missing optional fields`() {
        // Older/partial configs may omit protocol_version, size, fallback and capabilities. These
        // should still decode without crashing, using defaults.
        @Language("json")
        val minimalJson = """
            {
              "type": "web_view",
              "url": "https://paywalls.revenuecat.com/index.html"
            }
            """

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(minimalJson)

        assertEquals(
            WebViewComponent(url = "https://paywalls.revenuecat.com/index.html"),
            actual,
        )
        assertThat((actual as WebViewComponent).fallback).isNull()
        assertThat(actual.capabilities).isNull()
        assertThat(actual.protocolVersion).isNull()
    }
}
