package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.os.Looper
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewController
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewMessage
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewMessageHandler
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewValue
import org.assertj.core.api.Assertions.assertThat
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

    private lateinit var webView: WebView
    private lateinit var shadowWebView: ShadowWebView
    private val received = mutableListOf<PaywallWebViewMessage>()

    @Before
    fun setUp() {
        webView = WebView(ApplicationProvider.getApplicationContext())
        shadowWebView = shadowOf(webView)
        received.clear()
    }

    private fun bridge(
        handler: PaywallWebViewMessageHandler? = PaywallWebViewMessageHandler { message, _ -> received.add(message) },
        locale: String = "en-US",
        navigateTo: String? = expectedUrl,
        sizeToContentWidth: Boolean = false,
        sizeToContentHeight: Boolean = false,
        onContentResize: (widthCssPx: Int?, heightCssPx: Int?) -> Unit = { _, _ -> },
    ): WebViewJavaScriptBridge {
        val bridge = WebViewJavaScriptBridge(
            webView = webView,
            componentId = componentId,
            expectedUrl = expectedUrl,
            locale = locale,
            messageHandler = handler,
            sizeToContentWidth = sizeToContentWidth,
            sizeToContentHeight = sizeToContentHeight,
            onContentResize = onContentResize,
        )
        bridge.attach()
        navigateTo?.let { webView.loadUrl(it) }
        return bridge
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun connect(bridge: WebViewJavaScriptBridge) {
        bridge.postMessage(
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
        bridge.postMessage(
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
    fun `delivers valid message after handshake`() {
        val bridge = bridge()
        connect(bridge)
        bridge.postMessage(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()

        assertThat(received).hasSize(1)
        assertThat(received.single().componentId).isEqualTo("promo_web_view")
        assertThat(received.single().type).isEqualTo("rc:step-loaded")
    }

    @Test
    fun `ignores app messages before handshake`() {
        bridge().postMessage(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()

        assertThat(received).isEmpty()
    }

    @Test
    fun `rejects messages for a different component id`() {
        val bridge = bridge()
        connect(bridge)
        bridge.postMessage(
            appMessage(
                type = WebViewMessageType.STEP_LOADED,
            ).replace(componentId, "other_web_view"),
        )
        idleMainLooper()

        assertThat(received).isEmpty()
    }

    @Test
    fun `request-variables auto-sends rc variables with locale only`() {
        val bridge = bridge()
        connect(bridge)
        bridge.postMessage(appMessage(WebViewMessageType.REQUEST_VARIABLES))
        idleMainLooper()

        assertThat(received.single().type).isEqualTo("rc:request-variables")
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
        bridge.postMessage(
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
    fun `app can reply with extra variables through the controller`() {
        val handler = PaywallWebViewMessageHandler { message, controller ->
            received.add(message)
            if (message.type == WebViewMessageType.REQUEST_VARIABLES) {
                controller.postVariables(
                    componentId = message.componentId,
                    variables = mapOf(
                        "custom" to PaywallWebViewValue.Object(
                            mapOf("app_segment" to PaywallWebViewValue.String("high_intent")),
                        ),
                    ),
                )
            }
        }
        val bridge = bridge(handler = handler)
        connect(bridge)
        bridge.postMessage(appMessage(WebViewMessageType.REQUEST_VARIABLES))
        idleMainLooper()

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"rc:variables\"")
        assertThat(script).contains("\"app_segment\":\"high_intent\"")
    }

    @Test
    fun `controller postVariables drops reserved keys`() {
        val controllerHolder = arrayOfNulls<PaywallWebViewController>(1)
        val handler = PaywallWebViewMessageHandler { _, controller -> controllerHolder[0] = controller }
        val bridge = bridge(handler = handler)
        connect(bridge)
        bridge.postMessage(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()

        controllerHolder[0]!!.postVariables(
            componentId = componentId,
            variables = mapOf(
                "locale" to PaywallWebViewValue.String("zz-ZZ"),
                "custom" to PaywallWebViewValue.Object(mapOf("k" to PaywallWebViewValue.String("v"))),
            ),
        )
        idleMainLooper()

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).doesNotContain("zz-ZZ")
        assertThat(script).contains("\"k\":\"v\"")
    }

    @Test
    fun `does not deliver messages after release`() {
        val bridge = bridge()
        connect(bridge)
        bridge.release()

        bridge.postMessage(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()

        assertThat(received).isEmpty()
    }

    @Test
    fun `does not execute queued outbound evaluateJavascript after release`() {
        val bridge = bridge()
        connect(bridge)
        val background = Thread {
            bridge.postMessage(
                componentId = componentId,
                type = "rc:queued",
                variables = mapOf("foo" to PaywallWebViewValue.String("bar")),
            )
        }
        background.start()
        background.join()
        bridge.release()
        idleMainLooper()

        assertThat(shadowWebView.lastEvaluatedJavascript).doesNotContain("rc:queued")
    }

    @Test
    fun `re-validates origin at main-thread delivery after navigation race`() {
        val bridge = bridge()
        connect(bridge)
        webView.loadUrl("https://evil.example.org/phish.html")
        val background = Thread {
            bridge.postMessage(appMessage(WebViewMessageType.STEP_LOADED))
        }
        background.start()
        background.join()
        idleMainLooper()

        assertThat(received).isEmpty()
    }

    @Test
    fun `treats host comparison as case insensitive`() {
        val bridge = bridge(navigateTo = "https://Assets.Example.COM/promo/index.html")
        connect(bridge)
        bridge.postMessage(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()

        assertThat(received).hasSize(1)
    }

    @Test
    fun `rejects messages after navigation to an unexpected origin`() {
        val bridge = bridge(navigateTo = "https://evil.example.org/phish.html")
        connect(bridge)
        bridge.postMessage(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()

        assertThat(received).isEmpty()
    }

    @Test
    fun `allows messages from the same origin on a different path`() {
        val bridge = bridge(navigateTo = "https://assets.example.com/promo/step-two.html")
        connect(bridge)
        bridge.postMessage(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()

        assertThat(received).hasSize(1)
    }

    @Test
    fun `does not deliver outbound messages after navigation to an unexpected origin`() {
        val bridge = bridge(navigateTo = "https://evil.example.org/phish.html")
        connect(bridge)
        bridge.postMessage(
            componentId = componentId,
            type = "rc:custom",
            variables = mapOf("foo" to PaywallWebViewValue.String("bar")),
        )
        idleMainLooper()

        assertThat(shadowWebView.lastEvaluatedJavascript).isNull()
    }

    @Test
    fun `treats the default https port as the same origin`() {
        val bridge = bridge(navigateTo = "https://assets.example.com:443/promo/index.html")
        connect(bridge)
        bridge.postMessage(
            componentId = componentId,
            type = "rc:custom",
            variables = mapOf("foo" to PaywallWebViewValue.String("bar")),
        )
        idleMainLooper()

        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"type\":\"rc:custom\"")
    }

    @Test
    fun `delivers rc step-complete responses to the handler without sending an outbound message`() {
        val bridge = bridge()
        connect(bridge)
        val scriptAfterConnect = shadowWebView.lastEvaluatedJavascript
        bridge.postMessage(
            appMessage(
                type = WebViewMessageType.STEP_COMPLETE,
                payload = """{"responses":{"selected_plan":"annual","accepted_terms":true}}""",
            ),
        )
        idleMainLooper()

        val message = received.single()
        assertThat(message.type).isEqualTo("rc:step-complete")
        assertThat(message.responses?.get("selected_plan")).isEqualTo(PaywallWebViewValue.String("annual"))
        assertThat(message.responses?.get("accepted_terms")).isEqualTo(PaywallWebViewValue.Boolean(true))
        assertThat(shadowWebView.lastEvaluatedJavascript).isEqualTo(scriptAfterConnect)
    }

    @Test
    fun `delivers rc error to the handler`() {
        val bridge = bridge()
        connect(bridge)
        bridge.postMessage(
            appMessage(
                type = WebViewMessageType.ERROR,
                payload = """{"error":"Something went wrong"}""",
            ),
        )
        idleMainLooper()

        val message = received.single()
        assertThat(message.type).isEqualTo("rc:error")
        assertThat(message.error).isEqualTo("Something went wrong")
    }

    @Test
    fun `does not deliver malformed messages to the handler`() {
        val bridge = bridge()
        connect(bridge)
        bridge.postMessage("""not even json""")
        idleMainLooper()

        assertThat(received).isEmpty()
    }

    @Test
    fun `auto-sends rc variables even when no handler is set`() {
        val bridge = bridge(handler = null)
        connect(bridge)
        bridge.postMessage(appMessage(WebViewMessageType.REQUEST_VARIABLES))
        idleMainLooper()

        assertThat(received).isEmpty()
        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"rc:variables\"")
        assertThat(script).contains("\"locale\":\"en-US\"")
    }

    @Test
    fun `update refreshes the variables sent on a subsequent request`() {
        val bridge = bridge(locale = "en-US")
        connect(bridge)
        bridge.update(
            locale = "fr-FR",
            messageHandler = null,
        )

        bridge.postMessage(appMessage(WebViewMessageType.REQUEST_VARIABLES))
        idleMainLooper()

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"locale\":\"fr-FR\"")
        assertThat(script).doesNotContain("en-US")
    }

    @Test
    fun `generic postMessage sends transport envelope with payload`() {
        val bridge = bridge()
        connect(bridge)
        bridge.postMessage(
            componentId = componentId,
            type = "rc:custom",
            variables = mapOf("foo" to PaywallWebViewValue.String("bar")),
        )
        idleMainLooper()

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("window.__rcWebComponentsReceive(")
        assertThat(script).contains("\"kind\":\"message\"")
        assertThat(script).contains("\"type\":\"rc:custom\"")
        assertThat(script).contains("\"component_id\":\"promo_web_view\"")
        assertThat(script).contains("\"payload\":{\"foo\":\"bar\"}")
    }

    @Test
    fun `escapes line and paragraph separators in outbound payloads`() {
        val raw = "a\u2028b\u2029c"
        val bridge = bridge()
        connect(bridge)
        bridge.postVariables(
            componentId = componentId,
            variables = mapOf(
                "custom" to PaywallWebViewValue.Object(mapOf("note" to PaywallWebViewValue.String(raw))),
            ),
        )
        idleMainLooper()

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).doesNotContain("\u2028")
        assertThat(script).doesNotContain("\u2029")
        assertThat(script).contains("\\u2028")
        assertThat(script).contains("\\u2029")
    }

    @Test
    fun `attach registers native interface under rcWebComponents`() {
        bridge()

        assertThat(shadowWebView.getJavascriptInterface("rcWebComponents")).isNotNull
    }

    @Test
    fun `release removes the native interface`() {
        val bridge = bridge()
        assertThat(shadowWebView.getJavascriptInterface("rcWebComponents")).isNotNull

        bridge.release()

        assertThat(shadowWebView.getJavascriptInterface("rcWebComponents")).isNull()
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
    fun `reports content resize from inbound resize message`() {
        var reportedWidth: Int? = null
        var reportedHeight: Int? = null
        val bridge = bridge(
            handler = null,
            sizeToContentWidth = true,
            sizeToContentHeight = true,
            onContentResize = { widthCssPx, heightCssPx ->
                reportedWidth = widthCssPx
                reportedHeight = heightCssPx
            },
        )
        connect(bridge)
        bridge.postMessage(
            appMessage(
                type = WebViewMessageType.RESIZE,
                payload = """{"width":320,"height":480}""",
            ),
        )
        idleMainLooper()

        assertThat(reportedWidth).isEqualTo(320)
        assertThat(reportedHeight).isEqualTo(480)
        assertThat(received).isEmpty()
    }

    @Test
    fun `resize ignores non-fit axes`() {
        val resizes = mutableListOf<Pair<Int?, Int?>>()
        val bridge = bridge(
            handler = null,
            sizeToContentWidth = false,
            sizeToContentHeight = true,
            onContentResize = { widthCssPx, heightCssPx -> resizes.add(widthCssPx to heightCssPx) },
        )
        connect(bridge)
        bridge.postMessage(
            appMessage(type = WebViewMessageType.RESIZE, payload = """{"width":400,"height":500}"""),
        )
        idleMainLooper()

        assertThat(resizes).containsExactly(null to 500)
    }

    @Test
    fun `resize clamps oversized values and drops invalid values per axis`() {
        val resizes = mutableListOf<Pair<Int?, Int?>>()
        val bridge = bridge(
            handler = null,
            sizeToContentWidth = true,
            sizeToContentHeight = true,
            onContentResize = { widthCssPx, heightCssPx -> resizes.add(widthCssPx to heightCssPx) },
        )
        connect(bridge)
        // Invalid width (negative) with a valid, oversized height: height still applies, clamped.
        bridge.postMessage(
            appMessage(type = WebViewMessageType.RESIZE, payload = """{"width":-1,"height":99999}"""),
        )
        idleMainLooper()

        assertThat(resizes).containsExactly(null to 10_000)
    }

    @Test
    fun `resize applies a per-axis change threshold`() {
        val resizes = mutableListOf<Pair<Int?, Int?>>()
        val bridge = bridge(
            handler = null,
            sizeToContentWidth = true,
            sizeToContentHeight = true,
            onContentResize = { widthCssPx, heightCssPx -> resizes.add(widthCssPx to heightCssPx) },
        )
        connect(bridge)
        bridge.postMessage(
            appMessage(type = WebViewMessageType.RESIZE, payload = """{"width":200,"height":300}"""),
        )
        // Same values re-reported: below the 1px threshold on both axes, no callback at all.
        bridge.postMessage(
            appMessage(type = WebViewMessageType.RESIZE, payload = """{"width":200,"height":300}"""),
        )
        // One axis changes: only that axis is delivered.
        bridge.postMessage(
            appMessage(type = WebViewMessageType.RESIZE, payload = """{"width":200,"height":301}"""),
        )
        idleMainLooper()

        assertThat(resizes).containsExactly(200 to 300, null to 301)
    }

    @Test
    fun `handles resize sent as a transport request without forwarding to the app handler`() {
        var reportedHeight: Int? = null
        val bridge = bridge(
            sizeToContentHeight = true,
            onContentResize = { _, heightCssPx -> reportedHeight = heightCssPx },
        )
        connect(bridge)
        bridge.postMessage(
            appMessage(
                type = WebViewMessageType.RESIZE,
                payload = """{"height":250}""",
                kind = WebViewEnvelope.KIND_REQUEST,
                id = "resize-1",
            ),
        )
        idleMainLooper()

        assertThat(reportedHeight).isEqualTo(250)
        assertThat(received).isEmpty()
    }

    @Test
    fun `drops any transport request without an id`() {
        val bridge = bridge()
        connect(bridge)

        bridge.postMessage(
            appMessage(type = WebViewMessageType.STEP_LOADED, kind = WebViewEnvelope.KIND_REQUEST),
        )
        bridge.postMessage(
            appMessage(type = WebViewMessageType.REQUEST_VARIABLES, kind = WebViewEnvelope.KIND_REQUEST),
        )
        idleMainLooper()

        assertThat(received).isEmpty()
        // No auto-reply was sent for the id-less variables request.
        assertThat(shadowWebView.lastEvaluatedJavascript).doesNotContain("rc:variables")
    }

    @Test
    fun `rejects hostile deeply nested frames without crashing`() {
        val bridge = bridge()
        connect(bridge)

        val depth = 30_000
        val nested = "[".repeat(depth) + "]".repeat(depth)
        bridge.postMessage(
            appMessage(type = WebViewMessageType.STEP_COMPLETE, payload = """{"responses":{"deep":$nested}}"""),
        )
        idleMainLooper()

        assertThat(received).isEmpty()
    }
}
