package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented because WebView's instance API does not resolve on this module's unit-test classpath. An
 * autoplaying `<video>`/`<audio>` really does play from a view that was never attached to a window.
 */
@RunWith(AndroidJUnit4::class)
class PaywallWebViewMediaPolicyTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun onMain(block: () -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    @Test
    fun warmingRefusesMediaAutoplay() {
        var requiresGesture: Boolean? = null
        onMain {
            val webView = createWarmingWebView(
                context = context,
                resolvedUrl = URL,
                onLoadFailed = {},
                onLoadFinished = {},
            )
            requiresGesture = webView.settings.mediaPlaybackRequiresUserGesture
            webView.destroyPaywallWebView()
        }

        assertThat(requiresGesture).isTrue()
    }

    @Test
    fun displayAllowsMediaAutoplay() {
        var requiresGesture: Boolean? = null
        var built = false
        onMain {
            val configured = createPaywallWebView(
                context = context,
                identity = WebViewIdentity(
                    resolvedUrl = URL,
                    componentId = "component-1",
                    sizeToContentWidth = false,
                    sizeToContentHeight = false,
                ),
                onLoadFailed = {},
            )
            built = configured != null
            configured?.let {
                requiresGesture = it.webView.settings.mediaPlaybackRequiresUserGesture
                it.webView.releasePaywallWebView(it.bridge)
            }
        }

        // Null means this System WebView cannot install secure messaging, which is not what this asserts.
        assumeTrue(built)
        assertThat(requiresGesture).isFalse()
    }

    private companion object {
        const val URL = "https://assets.example.com/promo/index.html"
    }
}
