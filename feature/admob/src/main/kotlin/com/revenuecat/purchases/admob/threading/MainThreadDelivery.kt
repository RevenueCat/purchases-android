package com.revenuecat.purchases.admob.threading

import android.os.Handler
import android.os.Looper

internal fun runOnMainIfPresent(
    mainHandler: Handler,
    block: (() -> Unit)?,
) {
    if (block == null) return
    if (Thread.currentThread() != Looper.getMainLooper().thread) {
        mainHandler.post { block() }
    } else {
        block()
    }
}
