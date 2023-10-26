package com.revenuecat.purchases.ui.revenuecatui

/**
 * This annotation marks the experimental preview of the RevenueCat UI Purchases API.
 * This API is in a preview state and has a very high chance of being changed in the future.
 *
 * Any usage of a declaration annotated with `@ExperimentalPreviewRevenueCatUIPurchasesAPI` must be
 * accepted either by annotating that usage with the [OptIn] annotation,
 * e.g. `@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)`, or by using the compiler argument
 * `-Xopt-in=kotlin.time.ExperimentalPreviewRevenueCatUIPurchasesAPI`.
 */
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
)
annotation class ExperimentalPreviewRevenueCatUIPurchasesAPI
