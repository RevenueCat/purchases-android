@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.helpers

/**
 * A Set that is guaranteed to have at least 1 element. Inspired by Arrow. Use [nonEmptySetOf] or
 * [toNonEmptySetOrNull] to construct.
 */
internal class NonEmptySet<out A> private constructor(
    @get:JvmSynthetic
    val head: A,
    private val all: Set<A>,
) : Set<A> by all {

    constructor(head: A, rest: Iterable<A>) : this(head, all = rest.toSet() + head)

    @JvmSynthetic
    fun toSet(): Set<A> = all

    @JvmSynthetic
    override fun isEmpty(): Boolean = false

    override fun equals(other: Any?): Boolean = when (other) {
        is NonEmptySet<*> -> this.all == other.all
        else -> this.all == other
    }

    override fun hashCode(): Int = all.hashCode()

    override fun toString(): String =
        "NonEmptySet(${all.joinToString()})"
}

@JvmSynthetic
internal fun <A> nonEmptySetOf(head: A, vararg t: A): NonEmptySet<A> =
    NonEmptySet(head, t.asIterable())

@JvmSynthetic
internal fun <A> Iterable<A>.toNonEmptySetOrNull(): NonEmptySet<A>? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    return NonEmptySet(iterator.next(), Iterable { iterator })
}
