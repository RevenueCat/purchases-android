package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Returns the activity from a given context. Most times, the context itself will be
 * an activity, but in the case it's not, it will iterate through the context wrappers until it
 * finds one that is an activity.
 */
@JvmSynthetic internal fun Context.getActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

