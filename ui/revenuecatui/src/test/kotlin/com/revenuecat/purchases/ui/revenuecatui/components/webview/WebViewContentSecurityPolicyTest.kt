package com.revenuecat.purchases.ui.revenuecatui.components.webview

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WebViewContentSecurityPolicyTest {

    @Test
    fun `policy allows same-origin and inlined images and fonts`() {
        // Self-contained bundles routinely inline assets as data: URIs; remote hosts stay blocked.
        assertThat(WEB_VIEW_CONTENT_SECURITY_POLICY).contains("img-src 'self' data:")
        assertThat(WEB_VIEW_CONTENT_SECURITY_POLICY).contains("font-src 'self' data:")
    }

    @Test
    fun `policy allows only same-origin scripts`() {
        assertThat(WEB_VIEW_CONTENT_SECURITY_POLICY).contains("script-src 'self'")
    }

    @Test
    fun `policy restricts XHR and other connections to the same origin`() {
        // Same-origin fetch/XHR is allowed so the bundle can load its own packaged JSON (e.g. Lottie),
        // but cross-origin connections are not.
        assertThat(WEB_VIEW_CONTENT_SECURITY_POLICY).contains("connect-src 'self'")
    }

    @Test
    fun `policy anchors everything else to same-origin via default-src`() {
        assertThat(WEB_VIEW_CONTENT_SECURITY_POLICY).contains("default-src 'self'")
    }

    @Test
    fun `policy whitelists no external origins`() {
        // Isolation invariant: only 'self'/data:/inline keywords — never a wildcard or a remote host.
        assertThat(WEB_VIEW_CONTENT_SECURITY_POLICY).doesNotContain("*")
        assertThat(WEB_VIEW_CONTENT_SECURITY_POLICY).doesNotContain("http")
    }

    @Test
    fun `meta script installs the policy as an http-equiv meta element in head`() {
        val script = contentSecurityPolicyMetaScript("default-src 'self'")

        assertThat(script).contains("http-equiv")
        assertThat(script).contains("Content-Security-Policy")
        assertThat(script).contains("default-src 'self'")
        assertThat(script).contains("insertBefore")
        assertThat(script).contains("document.head")
    }

    @Test
    fun `meta script does not set installed flag when head is missing`() {
        val script = contentSecurityPolicyMetaScript()

        assertThat(script).contains("if (!head) { return; }")
        assertThat(script).doesNotContain("document.documentElement")
    }

    @Test
    fun `meta script is idempotent via a window flag set only after head insertion`() {
        val script = contentSecurityPolicyMetaScript()

        assertThat(script).contains("__revenueCatCspInstalled")
        assertThat(script).contains("if (!head) { return; }")
        assertThat(script.indexOf("window.__revenueCatCspInstalled = true"))
            .isGreaterThan(script.indexOf("if (!head)"))
    }

    @Test
    fun `meta script escapes double quotes and backslashes in the policy`() {
        val script = contentSecurityPolicyMetaScript("default-src \"x\\y\"")

        // The double quotes and backslash are escaped so they cannot break out of the JS string literal.
        assertThat(script).contains("""default-src \"x\\y\"""")
    }
}
