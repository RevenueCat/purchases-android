package com.revenuecat.purchases

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

@SuppressWarnings("EmptyFunctionBlock")
internal class AppLifecycleHandler(
    private val lifecycleDelegate: LifecycleDelegate,
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        lifecycleDelegate.onAppForegrounded()
    }

    override fun onStop(owner: LifecycleOwner) {
        lifecycleDelegate.onAppBackgrounded()
    }

    // Some functions are implemented and left empty to prevent issues with Java default interfaces
    // region default implementations
    override fun onCreate(owner: LifecycleOwner) {
    }

    override fun onResume(owner: LifecycleOwner) {
    }

    override fun onPause(owner: LifecycleOwner) {
    }

    override fun onDestroy(owner: LifecycleOwner) {
    }
    // endregion
}

internal interface LifecycleDelegate {
    public fun onAppBackgrounded()
    public fun onAppForegrounded()
}
