package com.revenuecat.purchases.ui.revenuecatui.helpers

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume

/**
 * Resumes the continuation with [value] only if it is still active.
 * This prevents [IllegalStateException] when a callback fires after the coroutine was cancelled.
 */
internal fun <T> CancellableContinuation<T>.safeResume(value: T) {
    if (isActive) resume(value)
}
