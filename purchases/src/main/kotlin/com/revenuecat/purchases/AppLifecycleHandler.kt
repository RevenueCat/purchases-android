package com.revenuecat.purchases

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

@SuppressWarnings("EmptyFunctionBlock")
internal class AppLifecycleHandler(
    private val lifecycleDelegate: LifecycleDelegate,
) : DefaultLifecycleObserver {
    // Some functions are implemented and left empty to prevent issues with Java default interfaces

    override fun onCreate(owner: LifecycleOwner) {
    }

    override fun onStart(owner: LifecycleOwner) {
        lifecycleDelegate.onAppForegrounded()
    }

    override fun onResume(owner: LifecycleOwner) {
    }

    override fun onPause(owner: LifecycleOwner) {
    }

    override fun onStop(owner: LifecycleOwner) {
        lifecycleDelegate.onAppBackgrounded()
    }

    override fun onDestroy(owner: LifecycleOwner) {
    }
}

internal interface LifecycleDelegate {
    fun onAppBackgrounded()
    fun onAppForegrounded()
}
