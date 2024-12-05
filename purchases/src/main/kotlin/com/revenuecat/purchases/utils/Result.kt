package com.revenuecat.purchases.utils

/**
 * This is an exact duplicate of `com.revenuecat.purchases.ui.revenuecatui.helpers.Result`, which is internal to
 * revenuecatui. We don't want to make this one public (marked with `@InternalRevenueCatAPI`), as that will still show
 * up in autocomplete if devs are trying to use the built-in `kotlin.Result` type.
 *
 * That version, in revenuecatui, has some additional extensions. If we ever want to consolidate the two `Result`
 * classes, or use those extensions in purchases, we can consider these alternatives:
 * * Rename `com.revenuecat.purchases.utils.Result` to something like `RcResult`, make it public, and mark it with
 *   `@InternalRevenueCatAPI`. Refactor the extensions to use `RcResult`.
 * * Create a separate Gradle module, move `Result` there together with the extensions, and make it an `implementation`
 *   of both purchases and revenuecatui.
 */
internal sealed class Result<out A, out B> {
    class Success<A>(val value: A) : Result<A, Nothing>()
    class Error<B>(val value: B) : Result<Nothing, B>()
}
