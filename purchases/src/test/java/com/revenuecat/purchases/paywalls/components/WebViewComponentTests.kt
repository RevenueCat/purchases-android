package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.JsonTools
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.net.URL

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
            url = URL("https://paywalls.revenuecat.com/index.html"),
        )

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(json)

        assert(actual == expected)
    }
}
