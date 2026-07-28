package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.gestures.Orientation
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.ComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GrowsToWebViewContentHeightTests {

    @Test
    fun `fill-height web view grows to content height`() {
        assertThat(webView(height = Fill).growsToWebViewContentHeight).isTrue()
    }

    @Test
    fun `fit-height web view does not grow to content height`() {
        assertThat(webView(height = Fit()).growsToWebViewContentHeight).isFalse()
    }

    @Test
    fun `fill-height stack containing a fill-height web view grows to content height`() {
        val stack = fillHeightStack(webView(height = Fill))

        assertThat(stack.growsToWebViewContentHeight).isTrue()
    }

    @Test
    fun `chain of fill-height stacks ending in a fill-height web view grows to content height`() {
        val stack = fillHeightStack(fillHeightStack(fillHeightStack(webView(height = Fill))))

        assertThat(stack.growsToWebViewContentHeight).isTrue()
    }

    @Test
    fun `fit-height stack breaks the chain`() {
        val stack = previewStackComponentStyle(
            children = listOf(webView(height = Fill)),
            size = Size(width = Fill, height = Fit()),
        )

        assertThat(stack.growsToWebViewContentHeight).isFalse()
    }

    @Test
    fun `vertically scrolling stack breaks the chain`() {
        val stack = previewStackComponentStyle(
            children = listOf(webView(height = Fill)),
            size = Size(width = Fill, height = Fill),
            scrollOrientation = Orientation.Vertical,
        )

        assertThat(stack.growsToWebViewContentHeight).isFalse()
    }

    @Test
    fun `fill-height stack containing only a fit-height web view does not grow to content height`() {
        val stack = fillHeightStack(webView(height = Fit()))

        assertThat(stack.growsToWebViewContentHeight).isFalse()
    }

    private fun fillHeightStack(child: ComponentStyle) = previewStackComponentStyle(
        children = listOf(child),
        size = Size(width = Fill, height = Fill),
    )

    private fun webView(height: SizeConstraint) =
        WebViewComponentStyle(
            url = "https://paywalls.revenuecat.com/index.html",
            visible = true,
            size = Size(width = Fill, height = height),
            componentId = "web_view",
            overrides = emptyList(),
            rcPackage = null,
            tabIndex = null,
        )
}
