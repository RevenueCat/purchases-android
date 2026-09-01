package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaywallWebViewMediaPolicyTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun warmingRefusesMediaAutoplay() {
        var requiresGesture: Boolean? = null
        onMain {
            val webView = createWarmingWebView(
                context = context,
                resolvedUrl = TEST_BUNDLE_URL,
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
                    resolvedUrl = TEST_BUNDLE_URL,
                    componentId = "component-1",
                    sizeToContentWidth = false,
                    sizeToContentHeight = false,
                ),
                onLoadFailed = {},
                contextSnapshotProvider = { deviceContextSnapshot() },
            )
            built = configured != null
            configured?.let {
                requiresGesture = it.webView.settings.mediaPlaybackRequiresUserGesture
                it.webView.releasePaywallWebView(it.bridge)
            }
        }

        assumeTrue(built)
        assertThat(requiresGesture).isFalse()
    }
}
