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
}
