package com.revenuecat.purchases.ui.revenuecatui.components.webview

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebViewUrlResolverTest {

    @Test
    fun `resolves static https url unchanged`() {
        val result = WebViewUrlResolver.resolve("https://paywalls.revenuecat.com/index.html")

        assertThat(result).isEqualTo("https://paywalls.revenuecat.com/index.html")
    }

    @Test
    fun `resolves static https url with characters accepted by WebView`() {
        val result = WebViewUrlResolver.resolve("https://paywalls.revenuecat.com/index.html?filters=a|b")

        assertThat(result).isEqualTo("https://paywalls.revenuecat.com/index.html?filters=a|b")
    }

    @Test
    fun `returns null for url containing template placeholders`() {
        val result = WebViewUrlResolver.resolve("https://paywalls.revenuecat.com/{{ custom.animal }}.html")

        assertThat(result).isNull()
    }

    @Test
    fun `returns null for malformed url`() {
        val result = WebViewUrlResolver.resolve("not a url")

        assertThat(result).isNull()
    }

    @Test
    fun `returns null for malformed https url`() {
        val result = WebViewUrlResolver.resolve("https:///missing-host")

        assertThat(result).isNull()
    }

    @Test
    fun `resolves https url with uppercase scheme`() {
        val result = WebViewUrlResolver.resolve("HTTPS://paywalls.revenuecat.com/index.html")

        assertThat(result).isEqualTo("HTTPS://paywalls.revenuecat.com/index.html")
    }

    @Test
    fun `returns null for non https url`() {
        val result = WebViewUrlResolver.resolve("http://paywalls.revenuecat.com/index.html")

        assertThat(result).isNull()
    }

    @Test
    fun `returns null for file url`() {
        val result = WebViewUrlResolver.resolve("file:///android_asset/index.html")

        assertThat(result).isNull()
    }

    @Test
    fun `returns null for custom scheme url`() {
        val result = WebViewUrlResolver.resolve("myapp://paywalls.revenuecat.com/index.html")

        assertThat(result).isNull()
    }
}
