package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.net.Uri
import android.os.Looper
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWebView

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingShadowWebView::class])
internal class WebViewJavaScriptBridgeTest {

    private val componentId = "promo_web_view"
    private val expectedUrl = "https://assets.example.com/promo/index.html"
    private val expectedOrigin = "https://assets.example.com"

    private lateinit var webView: WebView
    private lateinit var shadowWebView: ShadowWebView

    // Captures every onContentResize callback — the observable for "an app frame was processed".
    private val resizes = mutableListOf<Pair<Int?, Int?>>()

    @Before
    fun setUp() {
        webView = WebView(ApplicationProvider.getApplicationContext())
        shadowWebView = shadowOf(webView)
        resizes.clear()
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
        componentId: String = this.componentId,
        navigateTo: String? = expectedUrl,
        sizeToContentWidth: Boolean = false,
        sizeToContentHeight: Boolean = false,
        onDocumentReset: () -> Unit = {},
        onSecureMessagingUnsupported: () -> Unit = {},
        contextSnapshotProvider: () -> JsonObject = { testContextSnapshot() },
    ): WebViewJavaScriptBridge {
        val bridge = WebViewJavaScriptBridge(
            webView = webView,
            componentId = componentId,
            expectedUrl = expectedUrl,
            sizeToContentWidth = sizeToContentWidth,
            sizeToContentHeight = sizeToContentHeight,
            onContentResize = { widthCssPx, heightCssPx -> resizes.add(widthCssPx to heightCssPx) },
            onDocumentReset = onDocumentReset,
            onSecureMessagingUnsupported = onSecureMessagingUnsupported,
            contextSnapshotProvider = contextSnapshotProvider,
        )
        bridge.attach()
        navigateTo?.let { webView.loadUrl(it) }
        return bridge
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Every outbound script in delivery order; [ShadowWebView.getLastEvaluatedJavascript] keeps only the last. */
    private fun scripts(): List<String> = Shadow.extract<RecordingShadowWebView>(webView).evaluatedScripts

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

    /** Serializes an inbound frame, with the channel field every one of them carries. */
    private fun frame(build: JsonObjectBuilder.() -> Unit): String = Json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("channel", "rc-web-components")
            build()
        },
    )

    private fun connectJson(protocolVersion: Int = 1) = frame {
        put("protocol_version", protocolVersion)
        put("kind", "connect")
        put("component_id", "")
    }

    private fun WebViewJavaScriptBridge.connect(
        sourceOrigin: Uri = Uri.parse(expectedOrigin),
        isMainFrame: Boolean = true,
    ) {
        postFromWeb(connectJson(), sourceOrigin = sourceOrigin, isMainFrame = isMainFrame)
        idleMainLooper()
    }

    private fun appMessage(
        type: String,
        payload: JsonObject? = null,
        kind: String = "message",
        id: String? = null,
        componentId: String = this.componentId,
    ): String = frame {
        put("protocol_version", 1)
        put("kind", kind)
        put("component_id", componentId)
        put("type", type)
        payload?.let { put("payload", it) }
        id?.let { put("id", it) }
    }

    private fun sizePayload(width: Int? = null, height: Int? = null): JsonObject = buildJsonObject {
        width?.let { put("width", it) }
        height?.let { put("height", it) }
    }

    /** Sends a resize and pumps the looper; onContentResize lands in [resizes]. */
    private fun WebViewJavaScriptBridge.resize(heightCssPx: Int, componentId: String = this@WebViewJavaScriptBridgeTest.componentId) {
        postFromWeb(appMessage(type = WebViewMessageType.RESIZE, payload = sizePayload(height = heightCssPx), componentId = componentId))
        idleMainLooper()
    }

    // --- Handshake ---

    @Test
    fun `completes connect handshake with init`() {
        val bridge = bridge()
        bridge.connect()

        val script = scripts().first()
        assertThat(script).contains("window.__rcWebComponentsReceive(")
        assertThat(script).contains("\"kind\":\"init\"")
        assertThat(script).contains("\"component_id\":\"promo_web_view\"")
    }

    @Test
    fun `completes connect handshake before the web view URL is available`() {
        val bridge = bridge(navigateTo = null)
        assertThat(webView.url).isNull()

        bridge.connect()

        val script = scripts().first()
        assertThat(script).contains("\"kind\":\"init\"")
        assertThat(script).contains("\"component_id\":\"promo_web_view\"")
    }

    @Test
    fun `handshake sends init then fit, with no separate context frame`() {
        val bridge = bridge(sizeToContentHeight = true)
        bridge.connect()

        assertThat(scripts()).hasSize(2)
        assertThat(scripts()[0]).contains("\"kind\":\"init\"")
        assertThat(scripts()[1]).contains("\"type\":\"fit\"")
        assertThat(scripts().none { it.contains("\"type\":\"context\"") }).isTrue()
    }

    @Test
    fun `init carries the first snapshot for fixed-size components`() {
        val bridge = bridge()
        bridge.connect()

        assertThat(scripts()).hasSize(1)
        assertThat(scripts().single()).contains("\"kind\":\"init\"")
    }

    @Test
    fun `init payload carries the shaped empty snapshot`() {
        val bridge = bridge()
        bridge.connect()

        val script = scripts().single()
        assertThat(script).contains("\"kind\":\"init\"")
        assertThat(script).contains("\"payload\":{\"context\":{")
        assertThat(script).contains("\"custom\":{}")
        assertThat(script).contains("\"offering\":null")
        assertThat(script).contains("\"packages\":[]")
        assertThat(script).contains("\"package\":null")
        assertThat(script).contains("\"selected_package\":null")
        assertThat(script).contains("\"inputs\":{}")
        assertThat(script).contains("\"is_preview\":false")
        assertThat(script).contains("\"locale\":\"en-US\"")
        assertThat(script).doesNotContain("\"workflow\"")
    }

    @Test
    fun `rejects unsupported protocol version`() {
        val bridge = bridge()
        bridge.postFromWeb(connectJson(protocolVersion = 2))
        idleMainLooper()

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"kind\":\"reject\"")
        assertThat(script).contains("Unsupported protocol_version 2")
    }

    @Test
    fun `escapes line and paragraph separators in outbound payloads`() {
        // U+2028/U+2029 are valid in JSON strings but terminate JS statements; every outbound frame
        // (here the init's component_id) must escape them.
        val bridge = bridge(componentId = "promo\u2028\u2029view")
        bridge.connect()

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).doesNotContain("\u2028")
        assertThat(script).doesNotContain("\u2029")
        assertThat(script).contains("\\u2028")
        assertThat(script).contains("\\u2029")
    }

    // --- App-frame gating (resize is the only serviced app frame) ---

    @Test
    fun `ignores app frames before handshake`() {
        bridge(sizeToContentHeight = true).resize(300)

        assertThat(resizes).isEmpty()
        assertThat(shadowWebView.lastEvaluatedJavascript).isNull()
    }

    @Test
    fun `does not process app frames after release`() {
        val bridge = bridge(sizeToContentHeight = true)
        bridge.connect()
        bridge.release()

        bridge.resize(300)

        assertThat(resizes).isEmpty()
    }

    @Test
    fun `does not execute queued outbound evaluateJavascript after release`() {
        val bridge = bridge()
        val background = Thread { bridge.postFromWeb(connectJson()) }
        background.start()
        background.join()
        bridge.release()
        idleMainLooper()

        // The queued connect's init was scheduled on the (now-cancelled) document scope.
        assertThat(shadowWebView.lastEvaluatedJavascript).isNull()
    }

    @Test
    fun `re-validates source origin at main-thread delivery`() {
        val bridge = bridge()
        val background = Thread {
            bridge.postFromWeb(connectJson(), sourceOrigin = Uri.parse("https://evil.example.org"))
        }
        background.start()
        background.join()
        idleMainLooper()

        assertThat(shadowWebView.lastEvaluatedJavascript).isNull()
    }

    @Test
    fun `origin and main-frame gating`() {
        // Channel is opened from the trusted origin, then the inbound resize is gated by origin/frame.
        data class Case(
            val name: String,
            val navigateTo: String = expectedUrl,
            val sourceOrigin: String = expectedOrigin,
            val isMainFrame: Boolean = true,
            val expectProcessed: Boolean,
        )
        listOf(
            Case("wrong origin", sourceOrigin = "https://evil.example.org", expectProcessed = false),
            Case("subframe", isMainFrame = false, expectProcessed = false),
            Case(
                "case-insensitive host",
                navigateTo = "https://Assets.Example.COM/promo/index.html",
                sourceOrigin = "https://Assets.Example.COM",
                expectProcessed = true,
            ),
        ).forEach { case ->
            resizes.clear()
            val bridge = bridge(navigateTo = case.navigateTo, sizeToContentHeight = true)
            bridge.connect()
            bridge.postFromWeb(
                appMessage(type = WebViewMessageType.RESIZE, payload = sizePayload(height = 300)),
                sourceOrigin = Uri.parse(case.sourceOrigin),
                isMainFrame = case.isMainFrame,
            )
            idleMainLooper()
            if (case.expectProcessed) {
                assertThat(resizes).describedAs(case.name).containsExactly(null to 300)
            } else {
                assertThat(resizes).describedAs(case.name).isEmpty()
            }
        }
    }

    @Test
    fun `default https port is treated as same-origin`() {
        val bridge = bridge(navigateTo = "https://assets.example.com:443/promo/index.html")
        bridge.connect()

        assertThat(scripts().first()).contains("\"kind\":\"init\"")
    }

    @Test
    fun `allows messages from the same origin on a different path`() {
        val bridge = bridge(navigateTo = "https://assets.example.com/promo/step-two.html")
        bridge.connect()

        assertThat(scripts().first()).contains("\"kind\":\"init\"")
    }

    @Test
    fun `does not deliver outbound messages after navigation to an unexpected origin`() {
        val bridge = bridge(navigateTo = "https://evil.example.org/phish.html")
        // Inbound source is spoofed as the expected origin, but the top-level URL left the origin;
        // the outbound defense-in-depth check drops every outbound frame (even init), so nothing runs.
        bridge.connect()

        assertThat(shadowWebView.lastEvaluatedJavascript).isNull()
    }

    @Test
    fun `rejects invalid inbound connect frames`() {
        val cases = mapOf(
            "wrong channel" to
                """{"channel":"other","protocol_version":1,"kind":"connect","component_id":""}""",
            "unknown kind" to
                """{"channel":"rc-web-components","protocol_version":1,"kind":"ping","component_id":""}""",
            "malformed json" to "not even json",
        )
        cases.forEach { (name, frame) ->
            val bridge = bridge()
            bridge.postFromWeb(frame)
            idleMainLooper()
            assertThat(shadowWebView.lastEvaluatedJavascript).describedAs(name).isNull()
        }
    }

    @Test
    fun `drops resize whose component id does not match`() {
        val bridge = bridge(sizeToContentHeight = true)
        bridge.connect()
        bridge.resize(300, componentId = "other_web_view")

        assertThat(resizes).isEmpty()
    }

    // --- attach / release ---

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
    fun `attach reports unsupported secure messaging when listener installation throws`() {
        every {
            WebViewCompat.addWebMessageListener(any(), any(), any(), any())
        } throws IllegalArgumentException("invalid origin rule")
        var unsupportedCount = 0

        val bridge = bridge(onSecureMessagingUnsupported = { unsupportedCount += 1 })
        assertThat(unsupportedCount).isEqualTo(1)

        // Installation did not latch as successful: a later attach retries (and fails again here).
        bridge.attach()
        assertThat(unsupportedCount).isEqualTo(2)
        verify(exactly = 2) { WebViewCompat.addWebMessageListener(any(), any(), any(), any()) }
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
    fun `attach is idempotent`() {
        val bridge = bridge() // calls attach() once

        bridge.attach()
        bridge.attach()

        verify(exactly = 1) { WebViewCompat.addWebMessageListener(any(), any(), any(), any()) }
    }

    @Test
    fun `reopens the channel when a handshake init could not be delivered`() {
        // First connect while the top-level URL is off-origin: the outbound defense-in-depth check
        // drops init, so the channel must NOT latch open.
        val bridge = bridge(navigateTo = "https://evil.example.org/phish.html")
        bridge.connect()
        assertThat(shadowWebView.lastEvaluatedJavascript).isNull()

        // URL returns to the expected origin (without a navigation reset); a retried connect must
        // now complete the handshake rather than being ignored as a duplicate.
        webView.loadUrl(expectedUrl)
        bridge.connect()

        assertThat(scripts().first()).contains("\"kind\":\"init\"")
    }

    @Test
    fun `outbound script guards the receive function with a typeof check`() {
        val bridge = bridge()
        bridge.connect()

        assertThat(shadowWebView.lastEvaluatedJavascript)
            .contains("typeof window.__rcWebComponentsReceive === 'function'")
    }

    // --- Sizing (fit / resize) ---

    @Test
    fun `sends fit message after init when height is fit`() {
        val bridge = bridge(sizeToContentHeight = true)
        bridge.connect()

        val script = scripts().single { it.contains("\"type\":\"fit\"") }
        assertThat(script).contains("\"height\":true")
        assertThat(script).doesNotContain("\"width\":true")
    }

    @Test
    fun `sends fit message before the web view URL is available`() {
        // The fit frame is part of the handshake, so it must be delivered in the pre-navigation
        // window like init — even before the top-level URL is populated.
        val bridge = bridge(navigateTo = null, sizeToContentHeight = true)
        assertThat(webView.url).isNull()

        bridge.connect()

        val script = scripts().single { it.contains("\"type\":\"fit\"") }
        assertThat(script).contains("\"height\":true")
    }

    @Test
    fun `ignores resize dimensions that are not json numbers`() {
        listOf(
            "string height" to buildJsonObject { put("height", "300") },
            "boolean height" to buildJsonObject { put("height", true) },
        ).forEach { (name, payload) ->
            resizes.clear()
            val bridge = bridge(sizeToContentHeight = true)
            bridge.connect()
            bridge.postFromWeb(appMessage(type = WebViewMessageType.RESIZE, payload = payload))
            idleMainLooper()
            assertThat(resizes).describedAs(name).isEmpty()
        }
    }

    @Test
    fun `resize matrix covers fit axes, clamp, and threshold`() {
        data class Case(
            val name: String,
            val fitW: Boolean,
            val fitH: Boolean,
            val payloads: List<JsonObject>,
            val expected: List<Pair<Int?, Int?>>,
        )
        listOf(
            Case("both fit axes", true, true, listOf(sizePayload(320, 480)), listOf(320 to 480)),
            Case("non-fit width ignored", false, true, listOf(sizePayload(400, 500)), listOf(null to 500)),
            // Invalid width (negative) + oversized height → height clamped, width dropped.
            Case("clamp/invalid", true, true, listOf(sizePayload(-1, 99999)), listOf(null to 10_000)),
            Case(
                "threshold",
                true,
                true,
                listOf(
                    sizePayload(200, 300),
                    sizePayload(200, 300), // below 1px threshold → no callback
                    sizePayload(200, 301), // only changed axis delivered
                ),
                listOf(200 to 300, null to 301),
            ),
        ).forEach { case ->
            resizes.clear()
            val bridge = bridge(sizeToContentWidth = case.fitW, sizeToContentHeight = case.fitH)
            bridge.connect()
            case.payloads.forEach { bridge.postFromWeb(appMessage(type = WebViewMessageType.RESIZE, payload = it)) }
            idleMainLooper()
            assertThat(resizes).describedAs(case.name).containsExactlyElementsOf(case.expected)
        }
    }

    @Test
    fun `handles resize sent as a transport request`() {
        val bridge = bridge(sizeToContentHeight = true)
        bridge.connect()
        bridge.postFromWeb(
            appMessage(
                type = WebViewMessageType.RESIZE,
                payload = sizePayload(height = 250),
                kind = "request",
                id = "resize-1",
            ),
        )
        idleMainLooper()

        assertThat(resizes).containsExactly(null to 250)
    }

    @Test
    fun `drops a request frame without an id`() {
        val bridge = bridge(sizeToContentHeight = true)
        bridge.connect()
        bridge.postFromWeb(
            appMessage(
                type = WebViewMessageType.RESIZE,
                payload = sizePayload(height = 250),
                kind = "request",
                // no id → must be dropped for response-correlation safety
            ),
        )
        idleMainLooper()

        assertThat(resizes).isEmpty()
    }

    // --- Snapshot delivery ---

    @Test
    fun `context frame carries resolved custom variables`() {
        val bridge = bridge(
            contextSnapshotProvider = {
                testContextSnapshot(mapOf("org" to CustomVariableValue.String("RevenueCat")))
            },
        )
        bridge.connect()

        assertThat(scripts().last()).contains("\"custom\":{\"org\":\"RevenueCat\"}")
    }

    @Test
    fun `builds the snapshot once per handshake`() {
        var builds = 0
        val bridge = bridge(
            contextSnapshotProvider = {
                builds += 1
                testContextSnapshot()
            },
        )
        bridge.connect()

        assertThat(builds).isEqualTo(1)
    }

    // --- Document lifecycle ---

    @Test
    fun `initial document connect produces one init`() {
        val bridge = bridge()
        bridge.onMainFrameNavigationStarted()
        bridge.connect()

        assertThat(scripts().count { it.contains("\"kind\":\"init\"") }).isEqualTo(1)
    }

    @Test
    fun `duplicate connect in the same document is ignored`() {
        val bridge = bridge()
        bridge.onMainFrameNavigationStarted()
        bridge.connect()
        val afterFirst = scripts().size
        bridge.connect()

        assertThat(scripts()).hasSize(afterFirst)
    }

    @Test
    fun `after navigation starts app frames are rejected until reconnect`() {
        val bridge = bridge(sizeToContentHeight = true)
        bridge.onMainFrameNavigationStarted()
        bridge.connect()
        bridge.onMainFrameNavigationStarted()

        bridge.resize(300)

        assertThat(resizes).isEmpty()
    }

    @Test
    fun `new document reconnects and services app frames again`() {
        val bridge = bridge(sizeToContentHeight = true)
        bridge.onMainFrameNavigationStarted()
        bridge.connect()

        bridge.onMainFrameNavigationStarted()
        webView.loadUrl("https://assets.example.com/promo/step-two.html")
        bridge.resize(300)
        assertThat(resizes).describedAs("channel closed after navigation").isEmpty()

        bridge.connect()
        bridge.resize(300)
        assertThat(resizes).describedAs("channel reopened after reconnect").containsExactly(null to 300)
    }

    @Test
    fun `rc fit is resent after reconnect when height is fit`() {
        val bridge = bridge(sizeToContentHeight = true)
        bridge.onMainFrameNavigationStarted()
        bridge.connect()
        assertThat(scripts().count { it.contains("\"type\":\"fit\"") }).isEqualTo(1)

        bridge.onMainFrameNavigationStarted()
        webView.loadUrl("https://assets.example.com/promo/step-two.html")
        bridge.connect()

        val fitScripts = scripts().filter { it.contains("\"type\":\"fit\"") }
        assertThat(fitScripts).hasSize(2)
        assertThat(fitScripts.last()).contains("\"height\":true")
    }

    @Test
    fun `each document's init carries a fresh snapshot`() {
        val bridge = bridge()
        bridge.onMainFrameNavigationStarted()
        bridge.connect()

        bridge.onMainFrameNavigationStarted()
        webView.loadUrl("https://assets.example.com/promo/step-two.html")
        bridge.connect()

        val initScripts = scripts().filter { it.contains("\"kind\":\"init\"") }
        assertThat(initScripts).hasSize(2)
        val timestamps = initScripts.map { script ->
            Regex("\"updated_at\":(\\d+)").find(script)!!.groupValues[1].toLong()
        }
        assertThat(timestamps[0]).isGreaterThan(0)
        assertThat(timestamps[1]).isGreaterThanOrEqualTo(timestamps[0])
    }

    @Test
    fun `resize threshold state is reset between documents`() {
        val bridge = bridge(sizeToContentHeight = true)
        bridge.onMainFrameNavigationStarted()
        bridge.connect()
        bridge.resize(300)
        assertThat(resizes).containsExactly(null to 300)

        bridge.onMainFrameNavigationStarted()
        webView.loadUrl("https://assets.example.com/promo/step-two.html")
        bridge.connect()
        // Same height as before the document reset — must apply again because threshold state cleared.
        bridge.resize(300)

        assertThat(resizes).containsExactly(null to 300, null to 300)
    }

    @Test
    fun `document reset clears compose content dimensions`() {
        var resets = 0
        val bridge = bridge(onDocumentReset = { resets += 1 })
        bridge.onMainFrameNavigationStarted()
        bridge.onMainFrameNavigationStarted()

        assertThat(resets).isEqualTo(2)
    }

    @Test
    fun `inbound message queued before document reset is dropped`() {
        val bridge = bridge()
        val background = Thread { bridge.postFromWeb(connectJson()) }
        background.start()
        background.join()
        bridge.onMainFrameNavigationStarted()
        idleMainLooper()

        // Stale connect was cancelled with the old document scope — channel stays closed.
        assertThat(shadowWebView.lastEvaluatedJavascript).isNull()
    }

    @Test
    fun `outbound message queued before document reset is dropped`() {
        val bridge = bridge()
        bridge.onMainFrameNavigationStarted()
        val background = Thread { bridge.postFromWeb(connectJson()) }
        background.start()
        background.join()
        bridge.onMainFrameNavigationStarted()
        idleMainLooper()

        assertThat(shadowWebView.lastEvaluatedJavascript).isNull()
    }

    @Test
    fun `reload follows the same reconnect behavior`() {
        val bridge = bridge(sizeToContentHeight = true)
        bridge.onMainFrameNavigationStarted()
        bridge.connect()
        bridge.onMainFrameNavigationStarted() // reload
        bridge.resize(300)
        assertThat(resizes).describedAs("channel closed after reload").isEmpty()

        bridge.connect()
        bridge.resize(300)
        assertThat(resizes).describedAs("channel reopened after reload").containsExactly(null to 300)
    }
}

/**
 * Records every evaluated script; [ShadowWebView] only keeps the last one. A shadow rather than a
 * [WebView] subclass because Paparazzi's layoutlib jar shadows `android.jar` on the unit-test
 * compile classpath with a `WebView` stub that lacks `evaluateJavascript`.
 */
@Implements(WebView::class)
internal class RecordingShadowWebView : ShadowWebView() {
    val evaluatedScripts = mutableListOf<String>()

    @Implementation
    override fun evaluateJavascript(script: String, callback: ValueCallback<String>?) {
        evaluatedScripts += script
        super.evaluateJavascript(script, callback)
    }
}
