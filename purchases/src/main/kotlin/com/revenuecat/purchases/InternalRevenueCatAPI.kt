package com.revenuecat.purchases

@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal RevenueCat API that may change frequently and without warning. " +
        "No compatibility guarantees are provided. It is strongly discouraged to use this API.",
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
annotation class InternalRevenueCatAPI
