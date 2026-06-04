package com.revenuecat.purchases.common

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Resumes the continuation with [value] only if it is still active.
 *
 * Guards against [IllegalStateException] when a callback fires twice — common with
 * developer-supplied listeners such as [com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic]
 * or paywall listeners. Resume-after-cancellation is already a silent no-op in the coroutines
 * runtime, so [isActive] primarily protects against double-resume, not cancellation.
 */
internal fun <T> CancellableContinuation<T>.safeResume(value: T) {
    if (isActive) resume(value)
}

/**
 * Resumes the continuation with [exception] only if it is still active.
 *
 * Same double-resume protection as [safeResume]. See its KDoc for details.
 */
internal fun <T> CancellableContinuation<T>.safeResumeWithException(exception: Throwable) {
    if (isActive) resumeWithException(exception)
}
