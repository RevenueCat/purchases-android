package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression guard for the RenderThread `SkSurface::getCanvas()` crash: a hardware WebView drawn while
 * fully off-screen (e.g. a non-visible carousel page) must be hosted inside a FrameLayout so its GL
 * functor is not composited into a surfaceless offscreen layer. See [WebView.hostedInFrameLayout].
 */
@RunWith(AndroidJUnit4::class)
internal class WebViewFrameLayoutHostTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `hosts the web view as the single child of a full-size frame layout`() {
        val webView = WebView(context)

        val container = webView.hostedInFrameLayout()

        assertThat(container.childCount).isEqualTo(1)
        assertThat(container.getChildAt(0)).isSameAs(webView)
        assertThat(container.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.MATCH_PARENT)
        assertThat(container.layoutParams.height).isEqualTo(ViewGroup.LayoutParams.MATCH_PARENT)
    }
}
