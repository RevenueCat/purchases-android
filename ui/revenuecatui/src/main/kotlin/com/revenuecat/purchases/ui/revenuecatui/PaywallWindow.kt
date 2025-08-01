package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.graphics.PixelFormat
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.window.layout.WindowMetricsCalculator
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.views.PaywallView

/**
 * Launches the RevenueCat Paywall inside a new [android.view.Window].
 * The window will match the size of the current one and will be removed
 * when the paywall is dismissed.
 *
 * @param activity host activity used to create the window.
 * @param offering optional [Offering] to display.
 * @param listener optional [PaywallListener] to receive callbacks.
 */
@JvmOverloads
fun launchPaywallWindow(
    activity: Activity,
    offering: Offering? = null,
    listener: PaywallListener? = null,
) {
    val windowManager = activity.windowManager
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
    val params = WindowManager.LayoutParams(
        metrics.bounds.width(),
        metrics.bounds.height(),
        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        token = activity.window.decorView.applicationWindowToken
    }

    val container = FrameLayout(activity)
    val paywallView = PaywallView(activity, offering, listener) {
        windowManager.removeView(container)
    }
    container.addView(
        paywallView,
        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
    )

    windowManager.addView(container, params)
}
