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
    fun `collects a web_view's url and component id`() {
        val config = configWith(webView(url = "https://example.com/index.html", id = "component-1"))

        val assets = config.collectAssets()

        assertThat(assets.webViews).containsExactly(
            WebViewAsset(
                url = "https://example.com/index.html",
                componentId = "component-1",
                sizeToContentWidth = false,
                sizeToContentHeight = false,
            ),
        )
    }

    @Test
    fun `derives each fit axis independently`() {
        val config = configWith(
            webView(
                id = "fit-height-only",
                size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit()),
            ),
        )

        val webView = config.collectAssets().webViews.single()

        assertThat(webView.sizeToContentWidth).isFalse()
        assertThat(webView.sizeToContentHeight).isTrue()
    }

    @Test
    fun `treats a fixed axis as not sizing to content`() {
        val config = configWith(
            webView(size = Size(width = SizeConstraint.Fixed(320u), height = SizeConstraint.Fixed(240u))),
        )

        val webView = config.collectAssets().webViews.single()

        assertThat(webView.sizeToContentWidth).isFalse()
        assertThat(webView.sizeToContentHeight).isFalse()
    }

    @Test
    fun `finds web_views nested anywhere in the tree`() {
        val config = PaywallComponentsConfig(
            stack = StackComponent(
                components = listOf(StackComponent(components = listOf(webView(id = "nested")))),
            ),
            background = transparentBackground,
            header = HeaderComponent(StackComponent(components = listOf(webView(id = "in-header")))),
            stickyFooter = StickyFooterComponent(
                StackComponent(components = listOf(webView(id = "in-footer"))),
            ),
        )

        val assets = config.collectAssets()

        assertThat(assets.webViews.map { it.componentId })
            .containsExactlyInAnyOrder("nested", "in-header", "in-footer")
    }

    @Test
    fun `keeps two components sharing a url apart by component id`() {
        val config = configWith(
            webView(url = "https://example.com/index.html", id = "first"),
            webView(url = "https://example.com/index.html", id = "second"),
        )

        assertThat(config.collectAssets().webViews).hasSize(2)
    }

    @Test
    fun `reports no web_views when the tree has none`() {
        val config = configWith()

        assertThat(config.collectAssets().webViews).isEmpty()
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

        assertThat(assets.webViews.map { it.componentId }).containsExactly("component-1")
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
        size: Size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fill),
    ) = WebViewComponent(
        url = url,
        id = id,
        protocolVersion = WebViewComponent.SUPPORTED_PROTOCOL_VERSION,
        size = size,
    )

    private fun imageUrls(webpLowRes: String) = ImageUrls(
        original = URL("https://example.com/original.png"),
        webp = URL("https://example.com/original.webp"),
        webpLowRes = URL(webpLowRes),
        width = 200u,
        height = 200u,
    )
}
