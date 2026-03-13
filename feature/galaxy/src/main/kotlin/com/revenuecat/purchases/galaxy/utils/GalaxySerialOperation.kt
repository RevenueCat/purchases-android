package com.revenuecat.purchases.galaxy.utils

/**
 * Indicates that the annotated function must be called serially relative to other functions
 * also marked with this annotation, because the underlying operation relies on
 * calls to the Galaxy Store's [com.samsung.android.sdk.iap.lib.helper.IapHelper] calls,
 * which requires that certain function calls not be executed in parallel.
 *
 * ### Usage Requirement
 *
 * Functions marked with `@GalaxySerialOperation` **must** be executed one at a time,
 * in strict sequence, without concurrent execution. This is usually managed by ensuring the
 * execution path for these functions goes through a mechanism like
 * the [SerialRequestExecutor]
 * or a single-threaded dispatcher (e.g., a single-threaded Coroutine scope).
 *
 * Failing to execute these functions serially can lead to the `IapHelper` throwing
 * an [com.samsung.android.sdk.iap.lib.util.InProgressHandler.IapInProgressException]
 * and not executing the function call until the in-flight request has completed.
 *
 * Apply `@OptIn(GalaxySerialOperation::class)` to the calling function. This should only be done if the calling
 * scope *guarantees* serial execution for the call.
 *
 * @see [SerialRequestExecutor] The underlying mechanism typically used to enforce
 * this constraint.
 */
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Target(AnnotationTarget.FUNCTION)
internal annotation class GalaxySerialOperation
