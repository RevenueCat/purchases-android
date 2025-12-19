package com.revenuecat.purchases

/**
 * This annotation marks the experimental RevenueCat API.
 * This API is in an experimental state and may change in future versions.
 *
 * Any usage of a declaration annotated with `@ExperimentalRevenueCatAPI` must be
 * accepted either by annotating that usage with the [OptIn] annotation,
 * e.g. `@OptIn(ExperimentalRevenueCatAPI::class)`, or by using the compiler argument
 * `-Xopt-in=com.revenuecat.purchases.ExperimentalRevenueCatAPI`.
 */
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an experimental RevenueCat API that may change in future versions. " +
        "Use with caution.",
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.CONSTRUCTOR,
)
annotation class ExperimentalRevenueCatAPI
