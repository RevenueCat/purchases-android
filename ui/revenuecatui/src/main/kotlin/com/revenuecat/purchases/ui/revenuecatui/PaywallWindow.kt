package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import android.app.Application
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.lifecycle.ViewModelStore
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
    PaywallWindowManager.launch(activity, offering, listener)
}

@Suppress("TooManyFunctions")
private object PaywallWindowManager : Application.ActivityLifecycleCallbacks {
    private data class PaywallLaunchArgs(
        val offering: Offering?,
        val listener: PaywallListener?,
    )

    private val paywallLaunchArgs = mutableMapOf<String, PaywallLaunchArgs>()
    private val viewsAddedToTheWindow = mutableMapOf<String, FrameLayout>()
    private val viewModelStores = mutableMapOf<String, ViewModelStore>()
    private var isRegistered = false

    fun launch(
        activity: Activity,
        offering: Offering?,
        listener: PaywallListener?,
    ) {
        if (!isRegistered) {
            activity.application.registerActivityLifecycleCallbacks(this)
            isRegistered = true
        }
        val key = activity.key
        paywallLaunchArgs[key] = PaywallLaunchArgs(offering, listener)
        val viewModelStore = viewModelStores.getOrPut(key) { ViewModelStore() }
        attachWindow(key, activity, offering, listener, viewModelStore)
    }

    @Suppress("LongParameterList")
    private fun attachWindow(
        key: String,
        activity: Activity,
        offering: Offering?,
        listener: PaywallListener?,
        viewModelStore: ViewModelStore,
        token: IBinder = activity.window.decorView.windowToken,
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
            this.token = token
        }

        // TODO Do we need a container? Can we not add the PaywallView to the windowManager directly?
        val container = FrameLayout(activity)
        val paywallView = PaywallView(activity, offering, listener, null, null, viewModelStore) {
            windowManager.removeView(container)
        }
        container.addView(
            paywallView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )

        windowManager.addView(container, params)
        viewsAddedToTheWindow[key] = container
    }

    override fun onActivityResumed(activity: Activity) {
        waitForWindowToken(activity) { token ->
            val key = activity.key
            paywallLaunchArgs[key]?.let { (off, listener) ->
                val viewModelStore = viewModelStores.getValue(key)
                attachWindow(key, activity, off, listener, viewModelStore, token)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        viewsAddedToTheWindow[activity.key]?.let {
            activity.windowManager.removeViewImmediate(it)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (!activity.isChangingConfigurations) {
            val key = activity.key
            viewModelStores.remove(key)?.clear()
            paywallLaunchArgs.remove(key)
            viewsAddedToTheWindow.remove(key)
        }
    }

    private fun waitForWindowToken(
        activity: Activity,
        onTokenAvailable: (IBinder) -> Unit,
    ) {
        val decor = activity.window?.decorView ?: return

        fun checkWindowTokenAvailable() {
            if (activity.isFinishing || activity.isDestroyed) return
            val token = decor.windowToken
            if (token != null) {
                onTokenAvailable(token)
            } else {
                decor.post { checkWindowTokenAvailable() }
            }
        }

        if (decor.isAttachedToWindow) {
            checkWindowTokenAvailable()
            return
        }

        decor.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    decor.removeOnAttachStateChangeListener(this)
                    decor.post { checkWindowTokenAvailable() }
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            },
        )
    }

    private val Activity.key: String
        get() = "${this::class.java.name}+$taskId"

    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityCreated(activity: Activity, saved: Bundle?) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, out: Bundle) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
}
