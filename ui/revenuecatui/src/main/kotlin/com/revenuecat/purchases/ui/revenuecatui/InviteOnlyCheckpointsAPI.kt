package com.revenuecat.purchases.ui.revenuecatui

/**
 * This annotation marks the Checkpoints API, which is available by invitation only. Reach out to RevenueCat to get
 * access. While invite-only, this API may change without a deprecation cycle.
 *
 * Any usage of a declaration annotated with `@InviteOnlyCheckpointsAPI` must be accepted either by annotating that
 * usage with the [OptIn] annotation, e.g. `@OptIn(InviteOnlyCheckpointsAPI::class)`, or by using the compiler
 * argument `-opt-in=com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI`.
 */
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "The Checkpoints API is available by invitation only and may change without a deprecation cycle. " +
        "Reach out to RevenueCat to get access.",
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
)
public annotation class InviteOnlyCheckpointsAPI
