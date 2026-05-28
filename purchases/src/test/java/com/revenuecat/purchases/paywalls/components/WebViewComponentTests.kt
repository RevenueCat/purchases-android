package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import org.intellij.lang.annotations.Language
import org.junit.Test
import kotlin.test.assertEquals

class WebViewComponentTests {

    @Language("json")
    private val json = """
        {
          "type": "web_view",
          "url": "https://paywalls.revenuecat.com/index.html"
        }
        """

    @Test
    fun `deserializes correctly`() {
        val expected = WebViewComponent(
            url = "https://paywalls.revenuecat.com/index.html",
        )

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(json)

        assertEquals(expected, actual)
    }

    @Test
    fun `deserializes template url correctly`() {
        @Language("json")
        val templateJson = """
            {
              "type": "web_view",
              "url": "https://paywalls.revenuecat.com/{{ custom.animal }}.html"
            }
            """

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(templateJson)

        assertEquals(
            WebViewComponent(url = "https://paywalls.revenuecat.com/{{ custom.animal }}.html"),
            actual,
        )
    }

    @Test
    fun `deserializes optional presentation properties`() {
        @Language("json")
        val json = """
            {
              "type": "web_view",
              "url": "https://paywalls.revenuecat.com/index.html",
              "visible": false,
              "size": {
                "width": {
                  "type": "fill"
                },
                "height": {
                  "type": "fit"
                }
              }
            }
            """

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(json)

        assertEquals(
            WebViewComponent(
                url = "https://paywalls.revenuecat.com/index.html",
                visible = false,
                size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit),
            ),
            actual,
        )
    }
}
