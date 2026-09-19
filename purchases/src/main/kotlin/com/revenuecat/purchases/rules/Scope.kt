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
    /**
     * Names bound by enclosing `rc.let` calls. Unlike [current], these survive
     * iteration, which is what lets an inner predicate still see a value
     * captured outside the loop.
     */
    val bindings: Map<String, Value>,
) {

    constructor(root: Value) : this(current = root, root = root, bindings = emptyMap())

    fun scoped(current: Value): Scope = Scope(current = current, root = root, bindings = bindings)

    /**
     * Adds names visible from here down. An inner `rc.let` reusing a name
     * shadows the outer one.
     */
    fun binding(names: Map<String, Value>): Scope =
        Scope(current = current, root = root, bindings = bindings + names)
}
