package com.revenuecat.purchases.ui.debugview

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    error("RevenueCatDebugView: Could not find activity context from current context.")
}
