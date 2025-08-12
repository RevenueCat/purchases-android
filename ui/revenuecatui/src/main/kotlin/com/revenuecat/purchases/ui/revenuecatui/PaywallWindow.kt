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
    private data class Pending(
        val offering: Offering?,
        val listener: PaywallListener?,
    )

    // key → the Offering + listener
    private val pending = mutableMapOf<String, Pending>()

    // key → the currently‐attached FrameLayout
    private val containers = mutableMapOf<String, FrameLayout>()

    // One store per Activity “session”
    private val storedVmStores = mutableMapOf<String, ViewModelStore>()

    private var isRegistered = false

    // call once in your library init:
    fun init(app: Application) {
        if (!isRegistered) {
            app.registerActivityLifecycleCallbacks(this)
            isRegistered = true
        }
    }

    // your public API:
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
        pending[key] = Pending(offering, listener)
        val vmStore = storedVmStores.getOrPut(key) { ViewModelStore() }
        attachWindow(key, activity, offering, listener, vmStore)
    }

    @Suppress("LongParameterList")
    private fun attachWindow(
        key: String,
        activity: Activity,
        offering: Offering?,
        listener: PaywallListener?,
        vmStore: ViewModelStore,
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

        val container = FrameLayout(activity)
        val paywallView = PaywallView(activity, offering, listener, null, null, vmStore) {
            windowManager.removeView(container)
        }
        container.addView(
            paywallView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )

        windowManager.addView(container, params)
        containers[key] = container
    }

    // LIFECYCLE CALLBACKS
    override fun onActivityStopped(activity: Activity) {
        println("TESTING onActivityStopped for key ${activity.key}, all: ${containers.keys}")
        // remove the view before Android logs WindowLeaked
        containers[activity.key]?.let {
            println("TESTING Removing window for key ${activity.key}, all: ${containers.keys}")
            activity.windowManager.removeViewImmediate(it)
        }
    }

    override fun onActivityCreated(activity: Activity, saved: Bundle?) {
        println("TESTING onActivityCreated for key ${activity.key}, all: ${containers.keys}")
    }

    override fun onActivityDestroyed(activity: Activity) {
        val key = activity.key
        if (!activity.isChangingConfigurations) {
            // real destroy: clear ViewModelStore
            storedVmStores.remove(key)?.clear()
            pending.remove(key)
            containers.remove(key)
        }
    }

    // unused:
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) {
        val key = activity.key
        println("TESTING onActivityResumed for key ${activity.key}, all: ${containers.keys}")

        addWindowWhenReady(activity) { token ->
            pending[key]?.let { (off, listener) ->
                println("TESTING adding window for key ${activity.key}, all: ${containers.keys}")
                val vmStore = storedVmStores[key]!!
                attachWindow(key, activity, off, listener, vmStore, token)
            }
        }
    }
    override fun onActivitySaveInstanceState(activity: Activity, out: Bundle) = Unit
    override fun onActivityStarted(activity: Activity) {
        println("TESTING onActivityStarted for key ${activity.key}, all: ${containers.keys}")
        // if we had a pending store + offering, re-attach
//        val key = activity.key
//        pending[key]?.let { (off, listener) ->
//            println("TESTING adding window for key ${activity.key}, all: ${containers.keys}")
//            val vmStore = storedVmStores[key]!!
//            attachWindow(key, activity, off, listener, vmStore)
//        }
    }

    private fun addWindowWhenReady(
        activity: Activity,
        actuallyAdd: (IBinder) -> Unit,
    ) {
        val decor = activity.window?.decorView ?: return

        fun tryAdd() {
            if (activity.isFinishing || activity.isDestroyed) return
            val token = decor.windowToken
            if (token != null) {
                actuallyAdd(token)
            } else {
                // Re-try next frame; token not ready yet.
                decor.post { tryAdd() }
            }
        }

        // If already attached, try now (token may still be null; tryAdd() will re-post).
        if (decor.isAttachedToWindow) {
            tryAdd()
            return
        }

        // Wait until the window is actually attached.
        decor.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                decor.removeOnAttachStateChangeListener(this)
                // One more posted pass to allow token to populate
                decor.post { tryAdd() }
            }
            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }

    private val Activity.key: String
        get() = "${this::class.java.name}+$taskId"
}
