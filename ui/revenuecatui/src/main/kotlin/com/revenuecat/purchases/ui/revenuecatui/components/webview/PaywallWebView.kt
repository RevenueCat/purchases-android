@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.WebView
import kotlin.math.abs

/**
 * Whether the web_view should claim the drag (else the paywall scroll keeps it). An "own" verdict — an
 * inner scroller or `touch-action` map that native can't see — wins; otherwise native root
 * scrollability decides. [canScrollVertically]/[canScrollHorizontally] mirror View: `direction > 0`
 * = down/end.
 *
 * The native-scrollability decision fires at half of [touchSlop], not the full slop: Compose's
 * scrollable consumes the drag on the MOVE that crosses the full slop, and interop views only receive
 * MOVEs in the Final pointer pass — so a claim decided at the full slop always loses (that MOVE
 * arrives as ACTION_CANCEL and the view is cut off for the rest of the gesture). Half slop is early
 * enough to win on a pre-slop MOVE while still reading a reliable drag direction.
 */
@Suppress("ReturnCount", "LongParameterList")
internal fun shouldWebViewOwnGesture(
    totalDx: Float,
    totalDy: Float,
    touchSlop: Int,
    webContentWantsGesture: Boolean?,
    canScrollHorizontally: (direction: Int) -> Boolean,
    canScrollVertically: (direction: Int) -> Boolean,
): Boolean {
    if (webContentWantsGesture == true) return true
    val claimThreshold = (touchSlop / 2).coerceAtLeast(1)
    if (abs(totalDx) < claimThreshold && abs(totalDy) < claimThreshold) return false
    return if (abs(totalDy) >= abs(totalDx)) {
        canScrollVertically(if (totalDy < 0) 1 else -1)
    } else {
        canScrollHorizontally(if (totalDx < 0) 1 else -1)
    }
}

/**
 * The tracked finger's total drag delta from [downX]/[downY], or `null` once that finger is no longer
 * in [event] (lifted, or another pointer's index shifted it out). Reading [MotionEvent.getX]/[getY]
 * unconditionally (pointer index 0) would silently jump to a different finger's position once the
 * original one lifts mid-gesture.
 */
internal fun trackedPointerDelta(
    event: MotionEvent,
    trackedPointerId: Int,
    downX: Float,
    downY: Float,
): Pair<Float, Float>? {
    val pointerIndex = event.findPointerIndex(trackedPointerId)
    if (pointerIndex == -1) return null
    return (event.getX(pointerIndex) - downX) to (event.getY(pointerIndex) - downY)
}

/**
 * A [WebView] that claims a drag from the paywall scroll it's embedded in, via
 * [android.view.ViewParent.requestDisallowInterceptTouchEvent]. Compose won't hand a gesture back once
 * decided, so we never pre-claim and claim exactly once, when [shouldWebViewOwnGesture] says so.
 */
@SuppressLint("ViewConstructor")
internal class PaywallWebView(context: Context) : WebView(context) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    // Bound once to avoid allocating a reference on every ACTION_MOVE.
    private val canScrollHorizontallyReference: (Int) -> Boolean = ::canScrollHorizontally
    private val canScrollVerticallyReference: (Int) -> Boolean = ::canScrollVertically

    private var downX = 0f
    private var downY = 0f
    private var trackedPointerId = MotionEvent.INVALID_POINTER_ID
    private var lastTotalDx = 0f
    private var lastTotalDy = 0f
    private var webContentWantsGesture: Boolean? = null
    private var claimedForWebView = false

    // A verdict can arrive (asynchronously) after the finger is up; only act on it during a gesture.
    private var gestureActive = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                trackedPointerId = event.getPointerId(0)
                lastTotalDx = 0f
                lastTotalDy = 0f
                webContentWantsGesture = null
                claimedForWebView = false
                gestureActive = true
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = trackedPointerDelta(event, trackedPointerId, downX, downY)
                    ?: return super.onTouchEvent(event)
                lastTotalDx = delta.first
                lastTotalDy = delta.second
                claimForWebViewIfNeeded()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> gestureActive = false
        }
        return super.onTouchEvent(event)
    }

    /**
     * Records the content's verdict and claims immediately if it owns the gesture. The
     * [WebMessageListener][androidx.webkit.WebViewCompat] callback may run off the UI thread, so hop to
     * it (inline when already there, to keep the claim within touch-slop).
     */
    @JvmSynthetic
    fun onContentGestureVerdict(wantsGesture: Boolean) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post { onContentGestureVerdict(wantsGesture) }
            return
        }
        if (!gestureActive) return
        webContentWantsGesture = wantsGesture
        claimForWebViewIfNeeded()
    }

    private fun claimForWebViewIfNeeded() {
        if (claimedForWebView) return
        val shouldOwn = shouldWebViewOwnGesture(
            totalDx = lastTotalDx,
            totalDy = lastTotalDy,
            touchSlop = touchSlop,
            webContentWantsGesture = webContentWantsGesture,
            canScrollHorizontally = canScrollHorizontallyReference,
            canScrollVertically = canScrollVerticallyReference,
        )
        if (shouldOwn) {
            claimedForWebView = true
            parent?.requestDisallowInterceptTouchEvent(true)
        }
    }
}
