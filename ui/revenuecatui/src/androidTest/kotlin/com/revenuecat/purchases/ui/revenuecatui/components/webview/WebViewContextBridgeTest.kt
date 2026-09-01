package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.webkit.WebViewFeature
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Drives the real `rc-web-components` handshake against Chromium: a page on the expected origin
 * connects and reads the first snapshot off `init`, the way the content SDK does.
 *
 * Robolectric mocks [androidx.webkit.WebViewCompat], so the message listener and its origin gating
 * are only exercised for real here.
 */
@RunWith(AndroidJUnit4::class)
class WebViewContextBridgeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun seedsTheFirstSnapshotOnTheHandshake() {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))

        var configured: ConfiguredPaywallWebView? = null
        onMain {
            configured = createPaywallWebView(
                context = context,
                identity = WebViewIdentity(
                    resolvedUrl = TEST_BUNDLE_URL,
                    componentId = COMPONENT_ID,
                    sizeToContentWidth = false,
                    sizeToContentHeight = true,
                ),
                onLoadFailed = {},
                contextSnapshotProvider = {
                    deviceContextSnapshot(mapOf("org" to CustomVariableValue.String("RevenueCat")))
                },
            )
        }
        assumeTrue(configured != null)
        val webView = configured!!

        try {
            onMain { webView.webView.loadDataWithBaseURL(TEST_BUNDLE_URL, PAGE, "text/html", "utf-8", null) }

            val frames = awaitExchange(webView)

            assertThat(frames).describedAs("frames the page received").isNotNull()
            assertThat(frames).contains(
                """"kind":"init"""",
                """"type":"fit"""",
                """"payload":{"context":{""",
                """"is_preview":false""",
                """"locale":"en-US"""",
                """"custom":{"org":"RevenueCat"}""",
            )
            assertThat(frames).doesNotContain("workflow")
        } finally {
            onMain { webView.webView.releasePaywallWebView(webView.bridge) }
        }
    }

    private fun awaitExchange(configured: ConfiguredPaywallWebView): String? {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        while (SystemClock.uptimeMillis() < deadline) {
            var recorded: String? = null
            val latch = CountDownLatch(1)
            onMain {
                configured.webView.evaluateJavascript(RECORDED_FRAMES) { value ->
                    // evaluateJavascript JSON-encodes its result, so the page's own JSON arrives
                    // double-encoded; unwrap once to get plain JSON text back.
                    if (value != null && value != "null") recorded = Json.decodeFromString<String>(value)
                    latch.countDown()
                }
            }
            latch.await(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
            recorded?.let { return it }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return null
    }

    private companion object {
        const val COMPONENT_ID = "promo_web_view"
        const val TIMEOUT_MS = 10_000L
        const val POLL_INTERVAL_MS = 200L

        const val RECORDED_FRAMES = "window.__rcDone ? JSON.stringify(window.__rcSeen) : null"

        /** Stands in for a bundle: the frames `rc-sdk.js` itself exchanges during a handshake. */
        val PAGE = """
            <!doctype html><html><body><script>
            window.__rcSeen = [];
            window.__rcDone = false;
            window.__rcWebComponentsReceive = function (frame) {
              window.__rcSeen.push(frame);
              // `contextFromInitPayload` in the content SDK reads the snapshot from here.
              if (frame.kind === 'init' && frame.payload && frame.payload.context) {
                window.__rcDone = true;
              }
            };
            rcWebComponents.postMessage(JSON.stringify({
              channel: 'rc-web-components', protocol_version: 1, kind: 'connect', component_id: ''
            }));
            </script></body></html>
        """.trimIndent()
    }
}
