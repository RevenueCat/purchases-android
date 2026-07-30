@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

private const val GESTURE_PROBE_OBJECT_NAME = "rcPaywallGestureProbe"
private const val VERDICT_OWN = "own"

// On each touchstart, reports "own" if the touched element or an inner ancestor scrolls or declares
// `touch-action: none` — the per-element signals native canScroll can't see. Passive.
private val GESTURE_PROBE_SCRIPT =
    """
    (function () {
      var name = '$GESTURE_PROBE_OBJECT_NAME';
      var ELEMENT_NODE = Node.ELEMENT_NODE;
      function isRootScroller(n) {
        return n === document.scrollingElement || n === document.documentElement || n === document.body;
      }
      function consumesGesture(el) {
        var node = el && el.nodeType === ELEMENT_NODE ? el : (el ? el.parentElement : null);
        for (var n = node; n && n.nodeType === ELEMENT_NODE; n = n.parentElement) {
          var s = getComputedStyle(n);
          // Only `none` means the browser won't pan this element at all (JS owns every axis). `pan-x`/
          // `pan-y` still let native scroll handle an axis, already covered by canScroll*.
          if (s.touchAction === 'none') return true;
          // The root scroller is the WebView's own scroll, already tracked by canScroll* with correct
          // edge handoff; only inner scrollers are invisible to it.
          if (isRootScroller(n)) continue;
          if ((s.overflowY === 'auto' || s.overflowY === 'scroll') && n.scrollHeight > n.clientHeight) return true;
          if ((s.overflowX === 'auto' || s.overflowX === 'scroll') && n.scrollWidth > n.clientWidth) return true;
        }
        return false;
      }
      document.addEventListener('touchstart', function (event) {
        // Only the first finger of a sequence: the recognizer tracks one gesture, so a second finger's
        // verdict must not overwrite it.
        if (event.touches.length > 1) return;
        try {
          window[name].postMessage(consumesGesture(event.target) ? '$VERDICT_OWN' : 'release');
        } catch (e) {}
      }, { passive: true, capture: true });
    })();
    """.trimIndent()

/**
 * Best-effort per-gesture probe reporting via [onVerdict] whether the touched content consumes a drag.
 * No-op on old WebViews lacking the features (native scrollability arbitrates alone). Content panning
 * purely via a non-passive `preventDefault` without a `touch-action` isn't detected.
 */
@Suppress("TooGenericExceptionCaught", "ReturnCount")
internal fun WebView.installGestureOwnershipProbe(
    expectedOrigin: String?,
    onVerdict: (wantsGesture: Boolean) -> Unit,
) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
    val origin = expectedOrigin ?: return
    val allowedOrigins = setOf(origin)
    try {
        WebViewCompat.addWebMessageListener(
            this,
            GESTURE_PROBE_OBJECT_NAME,
            allowedOrigins,
        ) { _, message, _, isMainFrame, _ ->
            if (isMainFrame) onVerdict(message.data == VERDICT_OWN)
        }
        WebViewCompat.addDocumentStartJavaScript(this, GESTURE_PROBE_SCRIPT, allowedOrigins)
    } catch (error: RuntimeException) {
        Logger.w(
            "Paywalls V2 web_view could not install the gesture-ownership probe; drag arbitration " +
                "will use native scrollability only. $error",
        )
    }
}

/** Symmetric teardown for [installGestureOwnershipProbe] so a late verdict can't fire after release. */
@Suppress("TooGenericExceptionCaught")
internal fun WebView.removeGestureOwnershipProbe() {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
    try {
        WebViewCompat.removeWebMessageListener(this, GESTURE_PROBE_OBJECT_NAME)
    } catch (error: RuntimeException) {
        Logger.d("Paywalls V2 web_view gesture probe listener already removed. $error")
    }
}
