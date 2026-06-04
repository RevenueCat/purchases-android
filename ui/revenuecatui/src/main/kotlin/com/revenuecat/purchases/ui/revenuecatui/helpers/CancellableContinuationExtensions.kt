package com.revenuecat.purchases.ui.revenuecatui.helpers

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume

/**
 * Resumes the continuation with [value] only if it is still active.
 *
 * Guards against [IllegalStateException] when a callback fires twice — common with
 * developer-supplied listeners (e.g. [com.revenuecat.purchases.ui.revenuecatui.PurchaseLogic],
 * paywall listeners). Resume-after-cancellation is already a silent no-op in the coroutines
 * runtime, so [isActive] primarily protects against double-resume, not cancellation.
 */
internal fun <T> CancellableContinuation<T>.safeResume(value: T) {
    if (isActive) resume(value)
}
