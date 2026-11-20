package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.ui.revenuecatui.utils.URLOpeningMethod
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric is required because the request parser reads schemes and hosts via android.net.Uri.
@RunWith(RobolectricTestRunner::class)
class WebViewOpenUrlRequestTest {

    private fun payload(url: String?, method: String? = null): JsonObject = buildJsonObject {
        url?.let { put("url", it) }
        method?.let { put("method", it) }
    }

    @Test
    fun `defaults to the external browser`() {
        assertThat(WebViewOpenUrlRequest.parse(payload("https://example.org/terms")))
            .isEqualTo(WebViewOpenUrlRequest("https://example.org/terms", URLOpeningMethod.EXTERNAL_BROWSER))
    }

    @Test
    fun `accepts https for the browser methods`() {
        assertThat(WebViewOpenUrlRequest.parse(payload("https://example.org", "external_browser"))?.method)
            .isEqualTo(URLOpeningMethod.EXTERNAL_BROWSER)
        assertThat(WebViewOpenUrlRequest.parse(payload("https://example.org", "in_app_browser"))?.method)
            .isEqualTo(URLOpeningMethod.IN_APP_BROWSER)
    }

    @Test
    fun `refuses non-https urls for the browser methods`() {
        listOf("external_browser", "in_app_browser").forEach { method ->
            listOf("http://example.org", "myapp://settings", "mailto:a@example.org").forEach { url ->
                assertThat(WebViewOpenUrlRequest.parse(payload(url, method))).describedAs("$method $url").isNull()
            }
        }
    }

    @Test
    fun `accepts app schemes and https for deep links`() {
        listOf("myapp://settings?tab=1", "https://example.org/app", "mailto:a@example.org").forEach { url ->
            assertThat(WebViewOpenUrlRequest.parse(payload(url, "deep_link")))
                .describedAs(url)
                .isEqualTo(WebViewOpenUrlRequest(url, URLOpeningMethod.DEEP_LINK))
        }
    }

    @Test
    fun `refuses blocked deep link schemes`() {
        listOf(
            "http://example.org",
            "javascript:alert(1)",
            "JavaScript:alert(1)",
            "vbscript:msgbox(1)",
            "data:text/html,hi",
            "file:///etc/passwd",
            "blob:https://example.org/uuid",
            "about:blank",
            "content://com.example/data",
            "intent://scan#Intent;scheme=zxing;end",
        ).forEach { url ->
            assertThat(WebViewOpenUrlRequest.parse(payload(url, "deep_link"))).describedAs(url).isNull()
        }
    }

    @Test
    fun `refuses malformed payloads`() {
        val tooLong = "https://example.org/" + "a".repeat(WebViewOpenUrlRequest.MAX_URL_LENGTH)
        listOf(
            null,
            buildJsonObject { },
            payload(""),
            buildJsonObject { put("url", 1) },
            payload("not a url"),
            payload("jav\tascript:alert(1)", "deep_link"),
            payload(tooLong),
            payload("https://user:pass@example.org"),
            payload("https://example.org", "popup"),
            payload("https://example.org", "unknown"),
            buildJsonObject {
                put("url", "https://example.org")
                put("method", 1)
            },
        ).forEach { payload ->
            assertThat(WebViewOpenUrlRequest.parse(payload)).describedAs("$payload").isNull()
        }
    }
}
