package com.revenuecat.purchases.rules

/**
 * Marker annotation for declarations in the rules engine module that are
 * implementation details of the RevenueCat SDK. They are technically
 * `public` so the rest of the SDK (`:purchases`, `:ui:revenuecatui`,
 * hybrid bridges, etc.) can call them across module boundaries, but
 * they are not part of the public RevenueCat API and offer no
 * compatibility guarantees.
 *
 * Mirrors `com.revenuecat.purchases.InternalRevenueCatAPI` from the
 * `:purchases` module. We deliberately do not depend on `:purchases`
 * from here, so this is a sibling annotation rather than the same one.
 *
 * Annotate every new public declaration in this module with this
 * annotation (or with `@OptIn(InternalRulesEngineAPI::class)` on the
 * containing scope). The Metalava configuration in this module hides
 * everything annotated with it from the public API surface.
 */
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
    AnnotationTarget.CONSTRUCTOR,
)
public annotation class InternalRulesEngineAPI
