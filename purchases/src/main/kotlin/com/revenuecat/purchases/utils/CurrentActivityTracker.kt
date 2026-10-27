package com.revenuecat.purchases.utils

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Tracks the started [Activity]s, fed from the SDK's activity lifecycle callbacks, so components that need to
 * present UI (e.g. checkpoints) can obtain the most recently started usable (not finishing/destroyed) one
 * without tracking lifecycle themselves.
 * It is fed from process start by [CurrentActivityTrackerInitializer], so it also knows the activities that started
 * before the SDK was configured, as happens when a hybrid SDK configures it from JS. It is a singleton so that every
 * reader sees the activities that initializer feeds it.
 * Keeping every started activity (not just the last) preserves the current activity when a dialog-themed or
 * translucent activity on top of it stops: the one below never restarted, but it is still started and visible.
 */
internal object CurrentActivityTracker {

    private val startedActivities = mutableListOf<WeakReference<Activity>>()

    val currentActivity: Activity?
        @Synchronized get() = startedActivities.asReversed().firstNotNullOfOrNull { ref ->
            ref.get()?.takeIf { !it.isFinishing && !it.isDestroyed }
        }

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
