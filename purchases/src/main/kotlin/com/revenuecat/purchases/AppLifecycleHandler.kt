package com.revenuecat.purchases

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

internal class AppLifecycleHandler(
    private val lifecycleDelegate: LifecycleDelegate,
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        lifecycleDelegate.onAppForegrounded()
    }

    override fun onStop(owner: LifecycleOwner) {
        lifecycleDelegate.onAppBackgrounded()
    }
}

internal interface LifecycleDelegate {
    fun onAppBackgrounded()
    fun onAppForegrounded()
}
