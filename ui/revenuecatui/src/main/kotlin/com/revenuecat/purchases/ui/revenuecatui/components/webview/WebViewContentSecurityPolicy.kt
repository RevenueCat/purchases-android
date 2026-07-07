@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

/**
 * Fixed Content-Security-Policy used to isolate Paywalls V2 `web_view` content from external sources.
 * The bundle must be fully self-contained: it may use its own packaged and inlined resources, but
 * cannot reach any external origin.
 *
 * - `img-src 'self' data:` / `font-src 'self' data:`: same-origin and inlined (`data:`) images/fonts
 *   are allowed (self-contained bundles routinely inline assets); remote images/fonts are blocked.
 * - `script-src 'self'`: remote scripts are blocked, same-origin scripts are allowed. `'unsafe-inline'`
 *   / `'unsafe-eval'` keep inline/eval'd first-party scripts working — the goal here is network
 *   isolation, not locking down the (trusted) bundle's own inline code. The native bridge runs via
 *   `evaluateJavascript`, which is privileged and unaffected by CSP.
 * - `connect-src 'self'`: XHR/fetch/WebSocket are allowed to the bundle's own origin only (so it can
 *   load its packaged JSON — e.g. Lottie animation data); cross-origin requests are blocked.
 * - `default-src 'self'`: anchors every other resource type to same-origin (no remote).
 *
 * These functions are pure so the policy and its injection wrapper can be unit tested without an
 * Android `WebView`.
 */
internal const val WEB_VIEW_CONTENT_SECURITY_POLICY: String =
    "default-src 'self'; " +
        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
        "style-src 'self' 'unsafe-inline'; " +
        "img-src 'self' data:; " +
        "font-src 'self' data:; " +
        "connect-src 'self'"

/**
 * Builds the document-start script that installs [policy] as a `<meta http-equiv>` Content-Security-
 * Policy. Injected from `WebViewClient.onPageStarted` (before the page's own scripts run) and retried
 * from `onPageFinished` when `<head>` was not yet available. The meta is inserted as the first child
 * of `<head>` so it precedes resource-loading markup. The install is idempotent via a window flag that
 * is set only after a successful `<head>` insertion (CSP metas outside `<head>` are ignored by
 * browsers).
 */
internal fun contentSecurityPolicyMetaScript(policy: String = WEB_VIEW_CONTENT_SECURITY_POLICY): String =
    """
    (function() {
        if (window.__revenueCatCspInstalled) { return; }
        var head = document.head || document.getElementsByTagName('head')[0];
        if (!head) { return; }
        window.__revenueCatCspInstalled = true;
        var meta = document.createElement('meta');
        meta.setAttribute('http-equiv', 'Content-Security-Policy');
        meta.setAttribute('content', "${policy.escapeForMetaContent()}");
        if (head.firstChild) { head.insertBefore(meta, head.firstChild); } else { head.appendChild(meta); }
    })();
    """.trimIndent()

/**
 * The policy is embedded inside a double-quoted JS string literal. Escape backslashes and double
 * quotes so a policy value can never break out of the literal. The canonical policy contains neither,
 * but this keeps the wrapper safe for any caller-supplied [policy].
 */
private fun String.escapeForMetaContent(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")
