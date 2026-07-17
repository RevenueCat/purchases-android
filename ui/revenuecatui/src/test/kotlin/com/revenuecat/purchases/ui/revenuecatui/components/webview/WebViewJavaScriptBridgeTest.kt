package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.net.Uri
import android.os.Looper
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewController
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewMessage
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewMessageHandler
import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewValue
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
    private val received = mutableListOf<PaywallWebViewMessage>()

    @Before
    fun setUp() {
        webView = WebView(ApplicationProvider.getApplicationContext())
        shadowWebView = shadowOf(webView)
        received.clear()
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
        handler: PaywallWebViewMessageHandler? = PaywallWebViewMessageHandler { message, _ -> received.add(message) },
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
            messageHandler = handler,
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
    fun `delivers valid message after handshake`() {
        val bridge = bridge()
        connect(bridge)
        bridge.postFromWeb(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()

        assertThat(received).hasSize(1)
        assertThat(received.single().componentId).isEqualTo("promo_web_view")
        assertThat(received.single().type).isEqualTo("rc:step-loaded")
    }

    @Test
    fun `ignores app messages before handshake`() {
        bridge().postFromWeb(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()

        assertThat(received).isEmpty()
    }


    @Test
    fun `request-variables auto-sends rc variables with locale only`() {
        val bridge = bridge()
        connect(bridge)
        bridge.postFromWeb(appMessage(WebViewMessageType.REQUEST_VARIABLES))
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
        bridge.postFromWeb(appMessage(WebViewMessageType.REQUEST_VARIABLES))
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
        bridge.postFromWeb(appMessage(WebViewMessageType.STEP_LOADED))
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

        bridge.postFromWeb(appMessage(WebViewMessageType.STEP_LOADED))
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
    fun `re-validates source origin at main-thread delivery`() {
        val bridge = bridge()
        connect(bridge)
        val background = Thread {
            bridge.postFromWeb(
                json = appMessage(WebViewMessageType.STEP_LOADED),
                sourceOrigin = Uri.parse("https://evil.example.org"),
                isMainFrame = true,
            )
        }
        background.start()
        background.join()
        idleMainLooper()

        assertThat(received).isEmpty()
    }

    @Test
    fun `origin and main-frame gating`() {
        data class Case(
            val name: String,
            val navigateTo: String = expectedUrl,
            val sourceOrigin: String = expectedOrigin,
            val isMainFrame: Boolean = true,
            val expectedReceived: Int,
        )
        listOf(
            Case("wrong origin", sourceOrigin = "https://evil.example.org", expectedReceived = 0),
            Case("subframe", isMainFrame = false, expectedReceived = 0),
            Case(
                "case-insensitive host",
                navigateTo = "https://Assets.Example.COM/promo/index.html",
                sourceOrigin = "https://Assets.Example.COM",
                expectedReceived = 1,
            ),
        ).forEach { case ->
            received.clear()
            val bridge = bridge(navigateTo = case.navigateTo)
            connect(bridge)
            bridge.postFromWeb(
                json = appMessage(WebViewMessageType.STEP_LOADED),
                sourceOrigin = Uri.parse(case.sourceOrigin),
                isMainFrame = case.isMainFrame,
            )
            idleMainLooper()
            assertThat(received).describedAs(case.name).hasSize(case.expectedReceived)
        }

        // Default https port: outbound from :443 top-level URL is same-origin.
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
    fun `allows messages from the same origin on a different path`() {
        val bridge = bridge(navigateTo = "https://assets.example.com/promo/step-two.html")
        connect(bridge)
        bridge.postFromWeb(appMessage(WebViewMessageType.STEP_LOADED))
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
    fun `delivers rc step-complete responses to the handler without sending an outbound message`() {
        val bridge = bridge()
        connect(bridge)
        val scriptAfterConnect = shadowWebView.lastEvaluatedJavascript
        bridge.postFromWeb(
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
        bridge.postFromWeb(
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
    fun `rejects invalid inbound protocol frames`() {
        val nested = "[".repeat(30_000) + "]".repeat(30_000)
        // name to frames; missing-request-id also asserts no variables auto-reply.
        val cases = listOf(
            "wrong channel" to listOf(
                """{"channel":"other","protocol_version":1,"kind":"message","component_id":"$componentId","type":"rc:step-loaded"}""",
            ),
            "unknown kind" to listOf(
                """{"channel":"rc-web-components","protocol_version":1,"kind":"ping","component_id":"$componentId","type":"rc:step-loaded"}""",
            ),
            "malformed json" to listOf("""not even json"""),
            "wrong component id" to listOf(
                appMessage(type = WebViewMessageType.STEP_LOADED).replace(componentId, "other_web_view"),
            ),
            "missing request id" to listOf(
                appMessage(type = WebViewMessageType.STEP_LOADED, kind = WebViewEnvelope.KIND_REQUEST),
                appMessage(type = WebViewMessageType.REQUEST_VARIABLES, kind = WebViewEnvelope.KIND_REQUEST),
            ),
            "hostile nested payload" to listOf(
                appMessage(
                    type = WebViewMessageType.STEP_COMPLETE,
                    payload = """{"responses":{"deep":$nested}}""",
                ),
            ),
        )

        cases.forEach { (name, frames) ->
            received.clear()
            val bridge = bridge()
            connect(bridge)
            frames.forEach { bridge.postFromWeb(it) }
            idleMainLooper()
            assertThat(received).describedAs(name).isEmpty()
            if (name == "missing request id") {
                assertThat(shadowWebView.lastEvaluatedJavascript)
                    .describedAs(name)
                    .doesNotContain("rc:variables")
            }
        }
    }

    @Test
    fun `auto-sends rc variables even when no handler is set`() {
        val bridge = bridge(handler = null)
        connect(bridge)
        bridge.postFromWeb(appMessage(WebViewMessageType.REQUEST_VARIABLES))
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

        bridge.postFromWeb(appMessage(WebViewMessageType.REQUEST_VARIABLES))
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
            received.clear()
            val resizes = mutableListOf<Pair<Int?, Int?>>()
            val bridge = bridge(
                handler = null,
                sizeToContentWidth = case.fitW,
                sizeToContentHeight = case.fitH,
                onContentResize = { w, h -> resizes.add(w to h) },
            )
            connect(bridge)
            case.payloads.forEach { bridge.postFromWeb(appMessage(type = WebViewMessageType.RESIZE, payload = it)) }
            idleMainLooper()
            assertThat(resizes).describedAs(case.name).containsExactlyElementsOf(case.expected)
            assertThat(received).describedAs(case.name).isEmpty()
        }
    }

    @Test
    fun `handles resize sent as a transport request without forwarding to the app handler`() {
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
        assertThat(received).isEmpty()
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

        bridge.postFromWeb(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()

        assertThat(received).isEmpty()
    }

    @Test
    fun `new document connect produces a second init`() {
        val bridge = bridge()
        bridge.onMainFrameNavigationStarted(expectedUrl)
        connect(bridge)
        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"kind\":\"init\"")

        bridge.onMainFrameNavigationStarted("https://assets.example.com/promo/step-two.html")
        webView.loadUrl("https://assets.example.com/promo/step-two.html")
        bridge.postFromWeb(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()
        assertThat(received).isEmpty()

        connect(bridge)
        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"kind\":\"init\"")
        bridge.postFromWeb(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()
        assertThat(received).hasSize(1)
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
            handler = null,
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
            bridge.postMessage(
                componentId = componentId,
                type = "rc:x",
                variables = mapOf("foo" to PaywallWebViewValue.String("bar")),
            )
        }
        background.start()
        background.join()
        bridge.onMainFrameNavigationStarted(null)
        idleMainLooper()

        assertThat(shadowWebView.lastEvaluatedJavascript).doesNotContain("rc:x")
    }

    @Test
    fun `reload follows the same reconnect behavior`() {
        val bridge = bridge()
        bridge.onMainFrameNavigationStarted(expectedUrl)
        connect(bridge)
        bridge.onMainFrameNavigationStarted(expectedUrl) // reload
        bridge.postFromWeb(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()
        assertThat(received).isEmpty()

        connect(bridge)
        bridge.postFromWeb(appMessage(WebViewMessageType.STEP_LOADED))
        idleMainLooper()

        assertThat(received).hasSize(1)
        assertThat(shadowWebView.lastEvaluatedJavascript).contains("\"kind\":\"init\"")
    }
}
