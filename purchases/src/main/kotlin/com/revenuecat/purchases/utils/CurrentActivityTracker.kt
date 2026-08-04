package com.revenuecat.purchases.utils

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Tracks the started [Activity]s, fed from the SDK's activity lifecycle callbacks, so components that need to
 * present UI (e.g. checkpoints) can obtain the most recently started one without tracking lifecycle themselves.
 * Keeping every started activity (not just the last) preserves the current activity when a dialog-themed or
 * translucent activity on top of it stops: the one below never restarted, but it is still started and visible.
 */
internal class CurrentActivityTracker {

    private val startedActivities = mutableListOf<WeakReference<Activity>>()

    val currentActivity: Activity?
        @Synchronized get() = startedActivities.lastOrNull { it.get() != null }?.get()

    @Synchronized
    fun onActivityStarted(activity: Activity) {
        startedActivities.removeAll { it.get() == null || it.get() == activity }
        startedActivities.add(WeakReference(activity))
    }

    @Synchronized
    fun onActivityStopped(activity: Activity) {
        startedActivities.removeAll { it.get() == null || it.get() == activity }
    }
}
