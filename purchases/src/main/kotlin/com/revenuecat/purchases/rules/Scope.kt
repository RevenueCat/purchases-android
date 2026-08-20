package com.revenuecat.purchases.rules

/**
 * Evaluation scope pairing the active data ([current]) with the predicate's
 * original [root]. Iteration operators rebind [current] to each item while
 * preserving [root] for custom operators that need top-level data inside
 * nested predicates.
 */
internal class Scope private constructor(
    /** Data the enclosing expression reads from. Iteration operators replace this with the current item. */
    val current: Value,
    /** Data the predicate started with, never replaced. */
    val root: Value,
) {

    constructor(root: Value) : this(current = root, root = root)

    fun scoped(current: Value): Scope = Scope(current = current, root = root)
}
