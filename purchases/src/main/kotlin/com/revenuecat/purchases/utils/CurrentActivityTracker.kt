package com.revenuecat.purchases.utils

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Tracks the currently started [Activity], fed from the SDK's activity lifecycle callbacks, so components
 * that need to present UI (e.g. checkpoints) can obtain it without tracking lifecycle themselves.
 */
internal class CurrentActivityTracker {

    private var currentActivityRef: WeakReference<Activity>? = null

    val currentActivity: Activity?
        @Synchronized get() = currentActivityRef?.get()

    @Synchronized
    fun onActivityStarted(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    @Synchronized
    fun onActivityStopped(activity: Activity) {
        if (currentActivityRef?.get() == activity) {
            currentActivityRef = null
        }
    }
}
