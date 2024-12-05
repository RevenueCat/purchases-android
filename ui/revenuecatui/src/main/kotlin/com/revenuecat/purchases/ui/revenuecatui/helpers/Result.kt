package com.revenuecat.purchases.ui.revenuecatui.helpers

/**
 * This is an exact duplicate of `com.revenuecat.purchases.utils.Result`, which is internal to purchases. We don't want
 * to make that one public (marked with `@InternalRevenueCatAPI`), as that will still show up in autocomplete if devs
 * are trying to use the built-in `kotlin.Result` type.
 *
 * This version, in revenuecatui, has some additional extensions. If we ever want to consolidate the two `Result`
 * classes, or use the extensions in purchases, we can consider these alternatives:
 * * Rename `Result` to something like `RcResult`, make it public, and mark it with
 *   `@InternalRevenueCatAPI`. Refactor the extensions to use `RcResult`.
 * * Create a separate Gradle module, move `Result` there together with the extensions, and make it an `implementation`
 *   of both purchases and revenuecatui.
 */
internal sealed class Result<out A, out B> {
    class Success<A>(val value: A) : Result<A, Nothing>()
    class Error<B>(val value: B) : Result<Nothing, B>()
}

/**
 * Maps this Result's success value.
 */
@JvmSynthetic
internal inline fun <A, B, R> Result<A, B>.map(transform: (value: A) -> R): Result<R, B> =
    when (this) {
        is Result.Success -> Result.Success(transform(value))
        is Result.Error -> this
    }

/**
 * Maps this Result's error value.
 */
@JvmSynthetic
internal inline fun <A, B, R> Result<A, B>.mapError(transform: (value: B) -> R): Result<A, R> =
    when (this) {
        is Result.Success -> this
        is Result.Error -> Result.Error(transform(value))
    }

@JvmSynthetic
internal inline fun <A : R, B, R> Result<A, B>.getOrElse(onFailure: (error: B) -> R): R =
    when (this) {
        is Result.Success -> value
        is Result.Error -> onFailure(value)
    }

/**
 * Returns Result.Success(null) if this Result is null.
 */
@JvmSynthetic
internal fun <A, B> Result<A, B>?.orSuccessfullyNull(): Result<A?, B> =
    this ?: Result.Success(null)

// Most of the extensions below are inspired by Arrow.

/**
 * Combines the values from the provided Results using [transform], or accumulates the errors if at least
 * one is a [Result.Error].
 */
@JvmSynthetic
internal inline fun <A, B, E, F> zipOrAccumulate(
    first: Result<A, List<F>>,
    second: Result<B, List<F>>,
    transform: (A, B) -> E,
): Result<E, List<F>> = zipOrAccumulate(
    first = first,
    second = second,
    third = Result.Success(Unit),
    transform = { a, b, _ -> transform(a, b) },
)

/**
 * Combines the values from the provided Results using [transform], or accumulates the errors if at least
 * one is a [Result.Error].
 */
@JvmSynthetic
internal inline fun <A, B, C, E, F> zipOrAccumulate(
    first: Result<A, List<F>>,
    second: Result<B, List<F>>,
    third: Result<C, List<F>>,
    transform: (A, B, C) -> E,
): Result<E, List<F>> = zipOrAccumulate(
    first = first,
    second = second,
    third = third,
    fourth = Result.Success(Unit),
    transform = { a, b, c, _ -> transform(a, b, c) },
)

/**
 * Combines the values from the provided Results using [transform], or accumulates the errors if at least
 * one is a [Result.Error].
 */
@JvmSynthetic
internal inline fun <A, B, C, D, E, F> zipOrAccumulate(
    first: Result<A, List<F>>,
    second: Result<B, List<F>>,
    third: Result<C, List<F>>,
    fourth: Result<D, List<F>>,
    transform: (A, B, C, D) -> E,
): Result<E, List<F>> {
    // This one can be extended to support as many parameters as we need.
    val results = listOf(first, second, third, fourth)
    val errors = results.collectErrors()

    return if (errors.isEmpty()) {
        // We know they're all successful here.
        Result.Success(
            transform(
                (first as Result.Success<A>).value,
                (second as Result.Success<B>).value,
                (third as Result.Success<C>).value,
                (fourth as Result.Success<D>).value,
            ),
        )
    } else {
        Result.Error(errors)
    }
}

/**
 * Maps the values from these Results using [transform], or accumulates the errors if at least one is a [Result.Error].
 */
@JvmSynthetic
internal inline fun <A, B, E> Iterable<Result<A, List<E>>>.mapOrAccumulate(
    transform: (A) -> B,
): Result<List<B>, List<E>> {
    val successes = mutableListOf<B>()
    val errors = mutableListOf<E>()

    for (result in this) {
        when (result) {
            is Result.Success -> if (errors.isEmpty()) successes.add(transform(result.value))
            is Result.Error -> errors.addAll(result.value)
        }
    }

    return if (errors.isEmpty()) {
        Result.Success(successes)
    } else {
        Result.Error(errors)
    }
}

private fun <T, F> List<Result<T, List<F>>>.collectErrors(): List<F> =
    mapNotNull { result -> (result as? Result.Error)?.value }.flatten()
