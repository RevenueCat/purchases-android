package com.revenuecat.purchases

/**
 * This annotation marks the experimental preview of the RevenueCat Purchases API.
 * This API is in a preview state and has a very high chance of being changed in the future.
 *
 * Any usage of a declaration annotated with `@ExperimentalPreviewRevenueCatPurchasesAPI` must be
 * accepted either by annotating that usage with the [OptIn] annotation,
 * e.g. `@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)`, or by using the compiler argument
 * `-Xopt-in=kotlin.time.ExperimentalPreviewRevenueCatPurchasesAPI`.
 */
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
)
annotation class ExperimentalPreviewRevenueCatPurchasesAPI
