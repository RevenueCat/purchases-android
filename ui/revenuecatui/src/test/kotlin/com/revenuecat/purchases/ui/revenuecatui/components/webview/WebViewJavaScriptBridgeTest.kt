package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.os.Looper
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
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
import java.net.URL

@RunWith(RobolectricTestRunner::class)
internal class WebViewJavaScriptBridgeTest {

    private val componentId = "promo_web_view"
    private val expectedUrl = URL("https://assets.example.com/promo/index.html")

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
        customVariables: Map<String, CustomVariableValue> = emptyMap(),
        colorScheme: WebViewColorScheme = WebViewColorScheme.DARK,
        navigateTo: URL = expectedUrl,
    ): WebViewJavaScriptBridge {
        val bridge = WebViewJavaScriptBridge(
            webView = webView,
            componentId = componentId,
            expectedUrl = expectedUrl,
            locale = "en-US",
            colorScheme = colorScheme,
            customVariables = customVariables,
            messageHandler = handler,
        )
        bridge.attach()
        // Robolectric's ShadowWebView reports the last loaded URL via getUrl().
        webView.loadUrl(navigateTo.toString())
        return bridge
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `delivers valid message using the canonical component id`() {
        bridge().postMessage("""{"type":"rc:step-loaded","component_id":"promo_web_view"}""")
        idleMainLooper()

        assertThat(received).hasSize(1)
        assertThat(received.single().componentId).isEqualTo("promo_web_view")
        assertThat(received.single().type).isEqualTo("rc:step-loaded")
    }

    @Test
    fun `rejects messages for a different component id`() {
        bridge().postMessage("""{"type":"rc:step-loaded","component_id":"other_web_view"}""")
        idleMainLooper()

        assertThat(received).isEmpty()
    }

    @Test
    fun `request-variables auto-sends rc variables with locale and color scheme`() {
        bridge(
            handler = PaywallWebViewMessageHandler { message, _ -> received.add(message) },
            customVariables = mapOf("plan" to CustomVariableValue.String("annual")),
        ).postMessage("""{"type":"rc:request-variables","component_id":"promo_web_view"}""")
        idleMainLooper()

        // The app handler is still notified...
        assertThat(received.single().type).isEqualTo("rc:request-variables")
        // ...and the SDK sent rc:variables back into the web view via the receive hook.
        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("window.__revenueCatReceiveMessage(")
        assertThat(script).contains("\"rc:variables\"")
        assertThat(script).contains("\"component_id\":\"promo_web_view\"")
        assertThat(script).contains("\"locale\":\"en-US\"")
        assertThat(script).contains("\"color_scheme\":\"dark\"")
        assertThat(script).contains("\"plan\":\"annual\"")
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
        bridge(handler = handler)
            .postMessage("""{"type":"rc:request-variables","component_id":"promo_web_view"}""")
        idleMainLooper()

        // The controller's reply is the most recent script delivered to the web view.
        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"rc:variables\"")
        assertThat(script).contains("\"app_segment\":\"high_intent\"")
    }

    @Test
    fun `controller postVariables drops reserved keys`() {
        val controllerHolder = arrayOfNulls<PaywallWebViewController>(1)
        val handler = PaywallWebViewMessageHandler { _, controller -> controllerHolder[0] = controller }
        bridge(handler = handler)
            .postMessage("""{"type":"rc:step-loaded","component_id":"promo_web_view"}""")
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
        bridge.release()

        bridge.postMessage("""{"type":"rc:step-loaded","component_id":"promo_web_view"}""")
        idleMainLooper()

        assertThat(received).isEmpty()
    }

    @Test
    fun `rejects messages after navigation to an unexpected origin`() {
        bridge(navigateTo = URL("https://evil.example.org/phish.html"))
            .postMessage("""{"type":"rc:step-loaded","component_id":"promo_web_view"}""")
        idleMainLooper()

        assertThat(received).isEmpty()
    }

    @Test
    fun `allows messages from the same origin on a different path`() {
        bridge(navigateTo = URL("https://assets.example.com/promo/step-two.html"))
            .postMessage("""{"type":"rc:step-loaded","component_id":"promo_web_view"}""")
        idleMainLooper()

        assertThat(received).hasSize(1)
    }

    @Test
    fun `delivers rc step-complete responses to the handler without sending an outbound message`() {
        bridge().postMessage(
            """
            {
              "type":"rc:step-complete",
              "component_id":"promo_web_view",
              "responses":{"selected_plan":"annual","accepted_terms":true}
            }
            """.trimIndent(),
        )
        idleMainLooper()

        val message = received.single()
        assertThat(message.type).isEqualTo("rc:step-complete")
        assertThat(message.responses?.get("selected_plan")).isEqualTo(PaywallWebViewValue.String("annual"))
        assertThat(message.responses?.get("accepted_terms")).isEqualTo(PaywallWebViewValue.Boolean(true))
        // rc:step-complete must not auto-dismiss or send anything back; the app decides.
        assertThat(shadowWebView.lastEvaluatedJavascript).isNull()
    }

    @Test
    fun `delivers rc error to the handler`() {
        bridge().postMessage(
            """{"type":"rc:error","component_id":"promo_web_view","error":"Something went wrong"}""",
        )
        idleMainLooper()

        val message = received.single()
        assertThat(message.type).isEqualTo("rc:error")
        assertThat(message.error).isEqualTo("Something went wrong")
    }

    @Test
    fun `does not deliver malformed messages to the handler`() {
        bridge().postMessage("""not even json""")
        idleMainLooper()

        assertThat(received).isEmpty()
    }

    @Test
    fun `auto-sends rc variables even when no handler is set`() {
        bridge(handler = null)
            .postMessage("""{"type":"rc:request-variables","component_id":"promo_web_view"}""")
        idleMainLooper()

        assertThat(received).isEmpty()
        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"rc:variables\"")
        assertThat(script).contains("\"locale\":\"en-US\"")
    }

    @Test
    fun `update refreshes the variables sent on a subsequent request`() {
        val bridge = bridge(
            customVariables = mapOf("plan" to CustomVariableValue.String("annual")),
            colorScheme = WebViewColorScheme.DARK,
        )
        bridge.update(
            locale = "fr-FR",
            colorScheme = WebViewColorScheme.LIGHT,
            customVariables = mapOf("plan" to CustomVariableValue.String("monthly")),
            messageHandler = null,
        )

        bridge.postMessage("""{"type":"rc:request-variables","component_id":"promo_web_view"}""")
        idleMainLooper()

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("\"locale\":\"fr-FR\"")
        assertThat(script).contains("\"color_scheme\":\"light\"")
        assertThat(script).contains("\"plan\":\"monthly\"")
        assertThat(script).doesNotContain("annual")
    }

    @Test
    fun `generic postMessage builds a flat envelope and protects identity fields`() {
        bridge().postMessage(
            componentId = componentId,
            type = "rc:custom",
            message = mapOf(
                "foo" to PaywallWebViewValue.String("bar"),
                // Attempting to override the envelope identity fields must be ignored.
                "type" to PaywallWebViewValue.String("evil"),
                "component_id" to PaywallWebViewValue.String("other"),
            ),
        )
        idleMainLooper()

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("window.__revenueCatReceiveMessage(")
        assertThat(script).contains("\"type\":\"rc:custom\"")
        assertThat(script).contains("\"component_id\":\"promo_web_view\"")
        assertThat(script).contains("\"foo\":\"bar\"")
        assertThat(script).doesNotContain("evil")
        assertThat(script).doesNotContain("\"other\"")
    }

    @Test
    fun `escapes line and paragraph separators in outbound payloads`() {
        // U+2028/U+2029 are valid in JSON strings but terminate JS statements; they must be escaped.
        val raw = "a\u2028b\u2029c"
        bridge().postVariables(
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
    fun `attach registers a single native interface under the private name`() {
        bridge()

        assertThat(shadowWebView.getJavascriptInterface("__RevenueCatNativeBridge")).isNotNull
        // The public object name is created by the injected shim, not exposed as a native interface.
        assertThat(shadowWebView.getJavascriptInterface("RevenueCatWebView")).isNull()
    }

    @Test
    fun `injectBridgeScript exposes the public surface without overwriting an existing bridge`() {
        bridge().injectBridgeScript()

        val script = shadowWebView.lastEvaluatedJavascript
        assertThat(script).contains("window.RevenueCatWebView")
        assertThat(script).contains("window.__RevenueCatNativeBridge")
        // Guard so an existing bridge is not clobbered.
        assertThat(script).contains("if (window.RevenueCatWebView)")
    }

    @Test
    fun `release removes the native interface`() {
        val bridge = bridge()
        assertThat(shadowWebView.getJavascriptInterface("__RevenueCatNativeBridge")).isNotNull

        bridge.release()

        assertThat(shadowWebView.getJavascriptInterface("__RevenueCatNativeBridge")).isNull()
    }
}
