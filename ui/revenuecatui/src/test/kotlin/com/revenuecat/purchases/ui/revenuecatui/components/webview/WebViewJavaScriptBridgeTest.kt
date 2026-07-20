package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.net.Uri
import android.os.Looper
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowWebView

@RunWith(RobolectricTestRunner::class)
internal class WebViewJavaScriptBridgeTest {

    private val componentId = "promo_web_view"
    private val expectedUrl = "https://assets.example.com/promo/index.html"
    private val expectedOrigin = "https://assets.example.com"

    private lateinit var webView: WebView
    private lateinit var shadowWebView: ShadowWebView

    @Before
    fun setUp() {
        webView = WebView(ApplicationProvider.getApplicationContext())
        shadowWebView = shadowOf(webView)
    }

    @Before
    fun mockWebkitStatics() {
        mockkStatic(WebViewFeature::class, WebViewCompat::class)
        every { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) } returns true
        every { WebViewCompat.addWebMessageListener(any(), any(), any(), any()) } just Runs
        every { WebViewCompat.removeWebMessageListener(any(), any()) } just Runs
    }

    @After
    fun unmockStatics() {
        unmockkStatic(WebViewFeature::class, WebViewCompat::class)
    }

    private fun bridge(
        locale: String = "en-US",
        navigateTo: String? = expectedUrl,
        sizeToContentWidth: Boolean = false,
        sizeToContentHeight: Boolean = false,
        onContentResize: (widthCssPx: Int?, heightCssPx: Int?) -> Unit = { _, _ -> },
        onDocumentReset: () -> Unit = {},
        onSecureMessagingUnsupported: () -> Unit = {},
    ): WebViewJavaScriptBridge {
        val bridge = WebViewJavaScriptBridge(
            webView = webView,
            componentId = componentId,
            expectedUrl = expectedUrl,
            locale = locale,
            sizeToContentWidth = sizeToContentWidth,
            sizeToContentHeight = sizeToContentHeight,
            onContentResize = onContentResize,
            onDocumentReset = onDocumentReset,
            onSecureMessagingUnsupported = onSecureMessagingUnsupported,
        )
        bridge.attach()
        navigateTo?.let { webView.loadUrl(it) }
        return bridge
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * Exercises the genuine production inbound path:
     * `webMessageListener.onPostMessage` → per-document scope enqueue → protocol handling.
     */
    private fun WebViewJavaScriptBridge.postFromWeb(
        json: String,
        sourceOrigin: Uri = Uri.parse(expectedOrigin),
        isMainFrame: Boolean = true,
    ) = webMessageListener.onPostMessage(
        webView,
        WebMessageCompat(json),
        sourceOrigin,
        isMainFrame,
        mockk(),
    )

    private fun connect(bridge: WebViewJavaScriptBridge) {
        bridge.postFromWeb(
            """
            {"channel":"rc-web-components","protocol_version":1,"kind":"connect","component_id":""}
            """.trimIndent(),
        )
        idleMainLooper()
    }

    private fun appMessage(
        type: String,
        payload: String? = null,
        kind: String = WebViewEnvelope.KIND_MESSAGE,
        id: String? = null,
    ): String {
        val payloadField = payload?.let { ""","payload":$it""" } ?: ""
        val idField = id?.let { ""","id":"$it"""" } ?: ""
        return """
            {"channel":"rc-web-components","protocol_version":1,"kind":"$kind","component_id":"$componentId","type":"$type"$payloadField$idField}
            """.trimIndent()
    }

    private fun requestVariables(bridge: WebViewJavaScriptBridge) {
        bridge.postFromWeb(appMessage(WebViewMessageType.REQUEST_VARIABLES))
        idleMainLooper()
    }

    @Test
    fun `completes connect handshake with init`() {
        val bridge = bridge()
        connect(bridge)

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("window.__rcWebComponentsReceive(")
        assertThat(script).contains("\"kind\":\"init\"")
        assertThat(script).contains("\"component_id\":\"promo_web_view\"")
    }

    @Test
    fun `completes connect handshake before the web view URL is available`() {
        val bridge = bridge(navigateTo = null)
        assertThat(webView.url).isNull()

        connect(bridge)

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"kind\":\"init\"")
        assertThat(script).contains("\"component_id\":\"promo_web_view\"")
    }

    @Test
    fun `rejects unsupported protocol version`() {
        val bridge = bridge()
        bridge.postFromWeb(
            """
            {"channel":"rc-web-components","protocol_version":2,"kind":"connect","component_id":""}
            """.trimIndent(),
        )
        idleMainLooper()

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"kind\":\"reject\"")
        assertThat(script).contains("Unsupported protocol_version 2")
    }

    @Test
    fun `ignores app frames before handshake`() {
        requestVariables(bridge())

        // Nothing is sent: no init, no variables reply.
        assertThat(shadowWebView.lastEvaluatedJavascript).isNull()
    }

    @Test
    fun `request-variables auto-sends rc variables with locale only`() {
        val bridge = bridge()
        connect(bridge)
        requestVariables(bridge)

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("window.__rcWebComponentsReceive(")
        assertThat(script).contains("\"kind\":\"message\"")
        assertThat(script).contains("\"rc:variables\"")
        assertThat(script).contains("\"component_id\":\"promo_web_view\"")
        assertThat(script).contains("\"locale\":\"en-US\"")
        assertThat(script).doesNotContain("\"custom\"")
    }

    @Test
    fun `request-variables transport request also sends response`() {
        val bridge = bridge()
        connect(bridge)
        bridge.postFromWeb(
            appMessage(
                type = WebViewMessageType.REQUEST_VARIABLES,
                kind = WebViewEnvelope.KIND_REQUEST,
                id = "req-1",
            ),
        )
        idleMainLooper()

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"kind\":\"response\"")
        assertThat(script).contains("\"id\":\"req-1\"")
        assertThat(script).contains("\"locale\":\"en-US\"")
        assertThat(script).doesNotContain("\"rc:variables\"")
    }

    @Test
    fun `does not reply to app frames after release`() {
        val bridge = bridge()
        connect(bridge)
        bridge.release()

        requestVariables(bridge)

        assertThat(shadowWebView.lastEvaluatedJavascript).doesNotContain("\"rc:variables\"")
    }

    @Test
    fun `does not execute queued outbound evaluateJavascript after release`() {
        val bridge = bridge()
        connect(bridge)
        val background = Thread {
            bridge.postFromWeb(appMessage(WebViewMessageType.REQUEST_VARIABLES))
        }
        background.start()
        background.join()
        bridge.release()
        idleMainLooper()

        assertThat(shadowWebView.lastEvaluatedJavascript).doesNotContain("\"rc:variables\"")
    }

    @Test
    fun `re-validates source origin at main-thread delivery`() {
        val bridge = bridge()
        connect(bridge)
        val background = Thread {
            bridge.postFromWeb(
                json = appMessage(WebViewMessageType.REQUEST_VARIABLES),
                sourceOrigin = Uri.parse("https://evil.example.org"),
                isMainFrame = true,
            )
        }
        background.start()
        background.join()
        idleMainLooper()

        assertThat(shadowWebView.lastEvaluatedJavascript).doesNotContain("\"rc:variables\"")
    }

    @Test
    fun `origin and main-frame gating`() {
        data class Case(
            val name: String,
            val navigateTo: String = expectedUrl,
            val sourceOrigin: String = expectedOrigin,
            val isMainFrame: Boolean = true,
            val expectReply: Boolean,
        )
        listOf(
            Case("wrong origin", sourceOrigin = "https://evil.example.org", expectReply = false),
            Case("subframe", isMainFrame = false, expectReply = false),
            Case(
                "case-insensitive host",
                navigateTo = "https://Assets.Example.COM/promo/index.html",
                sourceOrigin = "https://Assets.Example.COM",
                expectReply = true,
            ),
        ).forEach { case ->
            val bridge = bridge(navigateTo = case.navigateTo)
            connect(bridge)
            bridge.postFromWeb(
                json = appMessage(WebViewMessageType.REQUEST_VARIABLES),
                sourceOrigin = Uri.parse(case.sourceOrigin),
                isMainFrame = case.isMainFrame,
            )
            idleMainLooper()
            val script = shadowWebView.lastEvaluatedJavascript
            if (case.expectReply) {
                assertThat(script).describedAs(case.name).contains("\"rc:variables\"")
            } else {
                assertThat(script).describedAs(case.name).doesNotContain("\"rc:variables\"")
            }
        }
    }

    @Test
    fun `default https port is treated as same-origin`() {
        val bridge = bridge(navigateTo = "https://assets.example.com:443/promo/index.html")
        connect(bridge)
        requestVariables(bridge)

        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"rc:variables\"")
    }

    @Test
    fun `allows messages from the same origin on a different path`() {
        val bridge = bridge(navigateTo = "https://assets.example.com/promo/step-two.html")
        connect(bridge)
        requestVariables(bridge)

        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"rc:variables\"")
    }

    @Test
    fun `does not deliver outbound messages after navigation to an unexpected origin`() {
        val bridge = bridge(navigateTo = "https://evil.example.org/phish.html")
        connect(bridge)
        // Inbound source is spoofed as the expected origin, but the top-level URL left the origin;
        // the outbound defense-in-depth check drops every outbound frame (even the init handshake),
        // so nothing is ever evaluated.
        requestVariables(bridge)

        assertThat(shadowWebView.lastEvaluatedJavascript).isNull()
    }

    @Test
    fun `rejects invalid inbound protocol frames`() {
        // Each frame is an rc:request-variables variant that must NOT produce a variables reply.
        val cases = listOf(
            "wrong channel" to
                """{"channel":"other","protocol_version":1,"kind":"message","component_id":"$componentId","type":"rc:request-variables"}""",
            "unknown kind" to
                """{"channel":"rc-web-components","protocol_version":1,"kind":"ping","component_id":"$componentId","type":"rc:request-variables"}""",
            "malformed json" to """not even json""",
            "wrong component id" to
                appMessage(type = WebViewMessageType.REQUEST_VARIABLES).replace(componentId, "other_web_view"),
            "request missing id" to
                appMessage(type = WebViewMessageType.REQUEST_VARIABLES, kind = WebViewEnvelope.KIND_REQUEST),
        )

        cases.forEach { (name, frame) ->
            val bridge = bridge()
            connect(bridge)
            bridge.postFromWeb(frame)
            idleMainLooper()
            assertThat(shadowWebView.lastEvaluatedJavascript)
                .describedAs(name)
                .doesNotContain("\"rc:variables\"")
                .doesNotContain("\"kind\":\"response\"")
        }
    }

    @Test
    fun `update refreshes the variables sent on a subsequent request`() {
        val bridge = bridge(locale = "en-US")
        connect(bridge)
        bridge.update(locale = "fr-FR")

        requestVariables(bridge)

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"locale\":\"fr-FR\"")
        assertThat(script).doesNotContain("en-US")
    }

    @Test
    fun `escapes line and paragraph separators in outbound payloads`() {
        // U+2028/U+2029 are valid in JSON strings but terminate JS statements; the outbound
        // variables reply (the only remaining outbound content path) must escape them.
        val bridge = bridge(locale = "en\u2028\u2029US")
        connect(bridge)
        requestVariables(bridge)

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).doesNotContain("\u2028")
        assertThat(script).doesNotContain("\u2029")
        assertThat(script).contains("\\u2028")
        assertThat(script).contains("\\u2029")
    }

    @Test
    fun `attach installs the web message listener when supported`() {
        bridge()

        verify(exactly = 1) {
            WebViewCompat.addWebMessageListener(
                webView,
                WebViewEnvelope.NATIVE_OBJECT_NAME,
                setOf(expectedOrigin),
                any(),
            )
        }
    }

    @Test
    fun `attach reports unsupported secure messaging when the feature is missing`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) } returns false
        var unsupported = false
        bridge(onSecureMessagingUnsupported = { unsupported = true })

        assertThat(unsupported).isTrue()
        verify(exactly = 0) { WebViewCompat.addWebMessageListener(any(), any(), any(), any()) }
    }

    @Test
    fun `release removes the web message listener`() {
        val bridge = bridge()
        verify(exactly = 1) {
            WebViewCompat.addWebMessageListener(
                webView,
                WebViewEnvelope.NATIVE_OBJECT_NAME,
                setOf(expectedOrigin),
                any(),
            )
        }

        bridge.release()

        verify(exactly = 1) {
            WebViewCompat.removeWebMessageListener(webView, WebViewEnvelope.NATIVE_OBJECT_NAME)
        }
    }

    @Test
    fun `sends fit message after init when height is fit`() {
        val bridge = bridge(sizeToContentHeight = true)
        connect(bridge)

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"type\":\"fit\"")
        assertThat(script).contains("\"height\":true")
        assertThat(script).doesNotContain("\"width\":true")
    }

    @Test
    fun `resize matrix covers fit axes, clamp, and threshold`() {
        data class Case(
            val name: String,
            val fitW: Boolean,
            val fitH: Boolean,
            val payloads: List<String>,
            val expected: List<Pair<Int?, Int?>>,
        )
        listOf(
            Case("both fit axes", true, true, listOf("""{"width":320,"height":480}"""), listOf(320 to 480)),
            Case("non-fit width ignored", false, true, listOf("""{"width":400,"height":500}"""), listOf(null to 500)),
            // Invalid width (negative) + oversized height → height clamped, width dropped.
            Case("clamp/invalid", true, true, listOf("""{"width":-1,"height":99999}"""), listOf(null to 10_000)),
            Case(
                "threshold",
                true,
                true,
                listOf(
                    """{"width":200,"height":300}""",
                    """{"width":200,"height":300}""", // below 1px threshold → no callback
                    """{"width":200,"height":301}""", // only changed axis delivered
                ),
                listOf(200 to 300, null to 301),
            ),
        ).forEach { case ->
            val resizes = mutableListOf<Pair<Int?, Int?>>()
            val bridge = bridge(
                sizeToContentWidth = case.fitW,
                sizeToContentHeight = case.fitH,
                onContentResize = { w, h -> resizes.add(w to h) },
            )
            connect(bridge)
            case.payloads.forEach { bridge.postFromWeb(appMessage(type = WebViewMessageType.RESIZE, payload = it)) }
            idleMainLooper()
            assertThat(resizes).describedAs(case.name).containsExactlyElementsOf(case.expected)
        }
    }

    @Test
    fun `handles resize sent as a transport request`() {
        var reportedHeight: Int? = null
        val bridge = bridge(
            sizeToContentHeight = true,
            onContentResize = { _, heightCssPx -> reportedHeight = heightCssPx },
        )
        connect(bridge)
        bridge.postFromWeb(
            appMessage(
                type = WebViewMessageType.RESIZE,
                payload = """{"height":250}""",
                kind = WebViewEnvelope.KIND_REQUEST,
                id = "resize-1",
            ),
        )
        idleMainLooper()

        assertThat(reportedHeight).isEqualTo(250)
    }

    @Test
    fun `initial document connect produces one init`() {
        val bridge = bridge()
        bridge.onMainFrameNavigationStarted(expectedUrl)
        connect(bridge)

        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"kind\":\"init\"")
    }

    @Test
    fun `duplicate connect in the same document is ignored`() {
        val bridge = bridge()
        bridge.onMainFrameNavigationStarted(expectedUrl)
        connect(bridge)
        val afterFirst = shadowWebView.lastEvaluatedJavascript
        connect(bridge)

        assertThat(shadowWebView.lastEvaluatedJavascript).isEqualTo(afterFirst)
    }

    @Test
    fun `after navigation starts app frames are rejected until reconnect`() {
        val bridge = bridge()
        bridge.onMainFrameNavigationStarted(expectedUrl)
        connect(bridge)
        bridge.onMainFrameNavigationStarted("https://assets.example.com/promo/step-two.html")

        requestVariables(bridge)

        assertThat(shadowWebView.lastEvaluatedJavascript).doesNotContain("\"rc:variables\"")
    }

    @Test
    fun `new document connect produces a second init`() {
        val bridge = bridge()
        bridge.onMainFrameNavigationStarted(expectedUrl)
        connect(bridge)
        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"kind\":\"init\"")

        bridge.onMainFrameNavigationStarted("https://assets.example.com/promo/step-two.html")
        webView.loadUrl("https://assets.example.com/promo/step-two.html")
        requestVariables(bridge)
        // Channel closed after navigation: no reply until reconnect.
        assertThat(shadowWebView.lastEvaluatedJavascript).doesNotContain("\"rc:variables\"")

        connect(bridge)
        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"kind\":\"init\"")
        requestVariables(bridge)
        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"rc:variables\"")
    }

    @Test
    fun `rc fit is resent after reconnect when height is fit`() {
        val bridge = bridge(sizeToContentHeight = true)
        bridge.onMainFrameNavigationStarted(expectedUrl)
        connect(bridge)
        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"type\":\"fit\"")

        bridge.onMainFrameNavigationStarted("https://assets.example.com/promo/step-two.html")
        webView.loadUrl("https://assets.example.com/promo/step-two.html")
        connect(bridge)

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"type\":\"fit\"")
        assertThat(script).contains("\"height\":true")
    }

    @Test
    fun `resize threshold state is reset between documents`() {
        val resizes = mutableListOf<Pair<Int?, Int?>>()
        val bridge = bridge(
            sizeToContentHeight = true,
            onContentResize = { widthCssPx, heightCssPx -> resizes.add(widthCssPx to heightCssPx) },
        )
        bridge.onMainFrameNavigationStarted(expectedUrl)
        connect(bridge)
        bridge.postFromWeb(
            appMessage(type = WebViewMessageType.RESIZE, payload = """{"height":300}"""),
        )
        idleMainLooper()
        assertThat(resizes).containsExactly(null to 300)

        bridge.onMainFrameNavigationStarted("https://assets.example.com/promo/step-two.html")
        webView.loadUrl("https://assets.example.com/promo/step-two.html")
        connect(bridge)
        // Same height as before the document reset — must apply again because threshold state cleared.
        bridge.postFromWeb(
            appMessage(type = WebViewMessageType.RESIZE, payload = """{"height":300}"""),
        )
        idleMainLooper()

        assertThat(resizes).containsExactly(null to 300, null to 300)
    }

    @Test
    fun `document reset clears compose content dimensions`() {
        var resets = 0
        val bridge = bridge(onDocumentReset = { resets += 1 })
        bridge.onMainFrameNavigationStarted(expectedUrl)
        bridge.onMainFrameNavigationStarted("https://assets.example.com/promo/reload.html")

        assertThat(resets).isEqualTo(2)
    }

    @Test
    fun `inbound message queued before document reset is dropped`() {
        val bridge = bridge()
        val background = Thread {
            bridge.postFromWeb(
                """
                {"channel":"rc-web-components","protocol_version":1,"kind":"connect","component_id":""}
                """.trimIndent(),
            )
        }
        background.start()
        background.join()
        bridge.onMainFrameNavigationStarted(null)
        idleMainLooper()

        // Stale connect was cancelled with the old document scope — channel stays closed.
        assertThat(shadowWebView.lastEvaluatedJavascript).isNull()
    }

    @Test
    fun `outbound message queued before document reset is dropped`() {
        val bridge = bridge()
        bridge.onMainFrameNavigationStarted(expectedUrl)
        connect(bridge)
        val background = Thread {
            bridge.postFromWeb(appMessage(WebViewMessageType.REQUEST_VARIABLES))
        }
        background.start()
        background.join()
        bridge.onMainFrameNavigationStarted(null)
        idleMainLooper()

        assertThat(shadowWebView.lastEvaluatedJavascript).doesNotContain("\"rc:variables\"")
    }

    @Test
    fun `reload follows the same reconnect behavior`() {
        val bridge = bridge()
        bridge.onMainFrameNavigationStarted(expectedUrl)
        connect(bridge)
        bridge.onMainFrameNavigationStarted(expectedUrl) // reload
        requestVariables(bridge)
        assertThat(shadowWebView.lastEvaluatedJavascript).doesNotContain("\"rc:variables\"")

        connect(bridge)
        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"kind\":\"init\"")
        requestVariables(bridge)
        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"rc:variables\"")
    }
}
