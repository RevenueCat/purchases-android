@file:Suppress("TooManyFunctions")
@file:JvmSynthetic

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

@get:JvmSynthetic
internal val Result<*, *>.isSuccess: Boolean
    get() = this is Result.Success

@get:JvmSynthetic
internal val Result<*, *>.isError: Boolean
    get() = this is Result.Error

/**
 * Side effect to run when this Result represents an Error.
 */
@JvmSynthetic
internal inline fun <A, B> Result<A, B>.onError(block: (value: B) -> Unit): Result<A, B> = apply {
    when (this) {
        is Result.Success -> { }
        is Result.Error -> block(value)
    }
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

@JvmSynthetic
internal fun <A, B> Result<A, B>.getOrThrow(): A =
    when (this) {
        is Result.Success -> value
        is Result.Error -> if (value is Throwable) throw value else error("Result was unsuccessful: $value")
    }

@JvmSynthetic
internal fun <A, B> Result<A, B>.getOrNull(): A? =
    when (this) {
        is Result.Success -> value
        is Result.Error -> null
    }

@JvmSynthetic
internal fun <A, B> Result<A, B>.errorOrNull(): B? =
    when (this) {
        is Result.Success -> null
        is Result.Error -> value
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
    first: Result<A, NonEmptyList<F>>,
    second: Result<B, NonEmptyList<F>>,
    transform: (A, B) -> E,
): Result<E, NonEmptyList<F>> = zipOrAccumulate(
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
    first: Result<A, NonEmptyList<F>>,
    second: Result<B, NonEmptyList<F>>,
    third: Result<C, NonEmptyList<F>>,
    transform: (A, B, C) -> E,
): Result<E, NonEmptyList<F>> = zipOrAccumulate(
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
    first: Result<A, NonEmptyList<F>>,
    second: Result<B, NonEmptyList<F>>,
    third: Result<C, NonEmptyList<F>>,
    fourth: Result<D, NonEmptyList<F>>,
    transform: (A, B, C, D) -> E,
): Result<E, NonEmptyList<F>> {
    // This one can be extended to support as many parameters as we need.
    val results = listOf(first, second, third, fourth)
    val errors = results.collectErrors()

    return errors.toNonEmptyListOrNull()
        ?.let { Result.Error(it) }
        // We know they're all successful here.
        ?: Result.Success(
            transform(
                (first as Result.Success<A>).value,
                (second as Result.Success<B>).value,
                (third as Result.Success<C>).value,
                (fourth as Result.Success<D>).value,
            ),
        )
}

/**
 * Maps the values from these Results using [transform], or accumulates the errors if at least one is a [Result.Error].
 */
@JvmSynthetic
internal inline fun <A, B, E> Iterable<Result<A, NonEmptyList<E>>>.mapOrAccumulate(
    transform: (A) -> B,
): Result<List<B>, NonEmptyList<E>> {
    val successes = mutableListOf<B>()
    val errors = mutableListOf<E>()

    for (result in this) {
        when (result) {
            is Result.Success -> if (errors.isEmpty()) successes.add(transform(result.value))
            is Result.Error -> errors.addAll(result.value)
        }
    }

    return errors.toNonEmptyListOrNull()
        ?.let { Result.Error(it) }
        ?: Result.Success(successes)
}

/**
 * Maps the Result values in this map using [transform], or accumulates the errors if at least one is a [Result.Error].
 */
@JvmSynthetic
internal inline fun <K, A, B, E> NonEmptyMap<K, Result<A, NonEmptyList<E>>>.mapValuesOrAccumulate(
    transform: (A) -> B,
): Result<NonEmptyMap<K, B>, NonEmptyList<E>> {
    val successes = mutableMapOf<K, B>()
    val errors = mutableListOf<E>()

    val mappedEntry = entry.value
        .map(transform)
        .map { entry.key to it }
        .onError { errors.addAll(it) }

    for ((key, result) in this) {
        when (result) {
            is Result.Success -> if (errors.isEmpty() && key != entry.key) successes[key] = transform(result.value)
            is Result.Error -> errors.addAll(result.value)
        }
    }

    return errors.toNonEmptyListOrNull()
        ?.let { Result.Error(it) }
        ?: Result.Success(nonEmptyMapOf(entry = mappedEntry.getOrThrow(), map = successes))
}

private fun <T, F> List<Result<T, NonEmptyList<F>>>.collectErrors(): List<F> =
    mapNotNull { result -> (result as? Result.Error)?.value }.flatten()
