package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Find the closest Activity in a given Context. Throws an IllegalStateException if this Context is not an Activity
 * context.
 */
internal fun Context.findActivityOrThrow(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    error("This Context is not an Activity context.")
}
