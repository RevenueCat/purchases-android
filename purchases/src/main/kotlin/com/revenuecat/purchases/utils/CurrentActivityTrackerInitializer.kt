package com.revenuecat.purchases.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.startup.Initializer

/**
 * Feeds [CurrentActivityTracker] from process start, before any Activity exists and before Purchases is
 * configured. Registering lifecycle callbacks only when Purchases is configured misses an Activity that already
 * started, as happens when a hybrid SDK configures it from JS.
 */
internal class CurrentActivityTrackerInitializer : Initializer<CurrentActivityTracker> {

    override fun create(context: Context): CurrentActivityTracker {
        val tracker = CurrentActivityTracker
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(
            object : CustomActivityLifecycleHandler {
                override fun onActivityStarted(activity: Activity) = tracker.onActivityStarted(activity)

                override fun onActivityStopped(activity: Activity) = tracker.onActivityStopped(activity)
            },
        )
        return tracker
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
