package com.revenuecat.purchases

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Bundle

internal class AppLifecycleHandler(private val lifecycleDelegate: LifecycleDelegate) :
    Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

    private var appInForeground = false

    override fun onActivityPaused(p0: Activity?) {}

    override fun onActivityResumed(p0: Activity?) {
        if (!appInForeground) {
            appInForeground = true
            lifecycleDelegate.onAppForegrounded()
        }
    }

    override fun onActivityStarted(p0: Activity?) {
    }

    override fun onActivityDestroyed(p0: Activity?) {
    }

    override fun onActivitySaveInstanceState(p0: Activity?, p1: Bundle?) {
    }

    override fun onActivityStopped(p0: Activity?) {
    }

    override fun onActivityCreated(p0: Activity?, p1: Bundle?) {
    }

    override fun onLowMemory() {}

    override fun onConfigurationChanged(p0: Configuration?) {}

    override fun onTrimMemory(level: Int) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            appInForeground = false
            lifecycleDelegate.onAppBackgrounded()
        }
    }
}

internal interface LifecycleDelegate {
    fun onAppBackgrounded()
    fun onAppForegrounded()
}