@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.HeaderComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.WebViewComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class PaywallComponentAssetsTest {

    @Test
    fun `collects a web_view's url`() {
        val config = configWith(webView(url = "https://example.com/index.html"))

        val assets = config.collectAssets()

        assertThat(assets.webViewUrls).containsExactly("https://example.com/index.html")
    }

    @Test
    fun `finds web_views nested anywhere in the tree`() {
        val config = PaywallComponentsConfig(
            stack = StackComponent(
                components = listOf(StackComponent(components = listOf(webView(url = "https://a.example.com/x.html")))),
            ),
            background = transparentBackground,
            header = HeaderComponent(StackComponent(components = listOf(webView(url = "https://b.example.com/x.html")))),
            stickyFooter = StickyFooterComponent(
                StackComponent(components = listOf(webView(url = "https://c.example.com/x.html"))),
            ),
        )

        val assets = config.collectAssets()

        assertThat(assets.webViewUrls).containsExactlyInAnyOrder(
            "https://a.example.com/x.html",
            "https://b.example.com/x.html",
            "https://c.example.com/x.html",
        )
    }

    @Test
    fun `collapses two components sharing a url into one`() {
        val config = configWith(
            webView(url = "https://example.com/index.html", id = "first"),
            webView(url = "https://example.com/index.html", id = "second"),
        )

        assertThat(config.collectAssets().webViewUrls).containsExactly("https://example.com/index.html")
    }

    @Test
    fun `reports no web_views when the tree has none`() {
        val config = configWith()

        assertThat(config.collectAssets().webViewUrls).isEmpty()
    }

    @Test
    fun `collects images and web_views in the same pass`() {
        val config = PaywallComponentsConfig(
            stack = StackComponent(
                components = listOf(webView(id = "component-1")),
                background = Background.Image(
                    value = ThemeImageUrls(light = imageUrls("https://example.com/stack.webp")),
                ),
            ),
            background = transparentBackground,
        )

        val assets = config.collectAssets()

        assertThat(assets.webViewUrls).containsExactly("https://example.com/index.html")
        assertThat(assets.imageUris.map { it.toString() }).containsExactly("https://example.com/stack.webp")
    }

    private val transparentBackground =
        Background.Color(ColorScheme(light = ColorInfo.Alias(ColorAlias(""))))

    private fun configWith(vararg components: PaywallComponent) = PaywallComponentsConfig(
        stack = StackComponent(components = components.toList()),
        background = transparentBackground,
    )

    private fun webView(
        url: String = "https://example.com/index.html",
        id: String = "component-1",
    ) = WebViewComponent(
        url = url,
        id = id,
        protocolVersion = WebViewComponent.SUPPORTED_PROTOCOL_VERSION,
        size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fill),
    )

    private fun imageUrls(webpLowRes: String) = ImageUrls(
        original = URL("https://example.com/original.png"),
        webp = URL("https://example.com/original.webp"),
        webpLowRes = URL(webpLowRes),
        width = 200u,
        height = 200u,
    )
}
