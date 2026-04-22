package com.revenuecat.purchases.common

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Resumes the continuation with [value] only if it is still active.
 * This prevents [IllegalStateException] when a callback fires after the coroutine was cancelled.
 */
internal fun <T> CancellableContinuation<T>.safeResume(value: T) {
    if (isActive) resume(value)
}

/**
 * Resumes the continuation with [exception] only if it is still active.
 * This prevents [IllegalStateException] when a callback fires after the coroutine was cancelled.
 */
internal fun <T> CancellableContinuation<T>.safeResumeWithException(exception: Throwable) {
    if (isActive) resumeWithException(exception)
}
