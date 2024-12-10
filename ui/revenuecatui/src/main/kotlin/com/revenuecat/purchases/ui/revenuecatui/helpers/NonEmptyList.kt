@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.helpers

/**
 * A List that is guaranteed to have at least 1 element. Inspired by Arrow. Use [nonEmptyListOf] or
 * [toNonEmptyListOrNull] to construct.
 */
internal class NonEmptyList<out A> private constructor(
    private val all: List<A>,
) : List<A> by all {

    constructor(head: A, tail: List<A>) : this(listOf(head) + tail)

    @JvmSynthetic
    fun toList(): List<A> = all

    @get:JvmSynthetic
    val head: A
        get() = all.first()

    @JvmSynthetic
    override fun isEmpty(): Boolean = false

    override fun equals(other: Any?): Boolean = when (other) {
        is NonEmptyList<*> -> this.all == other.all
        else -> this.all == other
    }

    override fun hashCode(): Int = all.hashCode()

    override fun toString(): String =
        "NonEmptyList(${all.joinToString()})"
}

@JvmSynthetic
internal fun <A> nonEmptyListOf(head: A, vararg t: A): NonEmptyList<A> =
    NonEmptyList(head, t.toList())

@JvmSynthetic
internal fun <A> Iterable<A>.toNonEmptyListOrNull(): NonEmptyList<A>? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    return NonEmptyList(iterator.next(), Iterable { iterator }.toList())
}
