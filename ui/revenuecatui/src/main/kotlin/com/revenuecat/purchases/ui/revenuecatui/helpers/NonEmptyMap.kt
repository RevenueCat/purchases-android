@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.helpers

/**
 * A Map that is guaranteed to have at least 1 entry. Use [nonEmptyMapOf] or [toNonEmptyMapOrNull] to construct.
 */
internal class NonEmptyMap<K, out V> private constructor(
    @get:JvmSynthetic
    val entry: Map.Entry<K, V>,
    private val all: Map<K, V>,
) : Map<K, V> by all {

    constructor(entry: Pair<K, V>, tail: Map<K, V>) : this(entry = mapOf(entry).entries.first(), all = tail + entry)

    override val keys: NonEmptySet<K> = NonEmptySet(entry.key, all.keys)

    @JvmSynthetic
    fun toMap(): Map<K, V> = all

    @JvmSynthetic
    override fun isEmpty(): Boolean = false

    override fun equals(other: Any?): Boolean = when (other) {
        is NonEmptyMap<*, *> -> this.entry == other.entry && this.all == other.all
        else -> this.all == other
    }

    override fun hashCode(): Int = all.hashCode()

    override fun toString(): String =
        "NonEmptyMap(${all.entries.joinToString()})"

    @JvmSynthetic
    inline fun <R> mapValues(transform: (Map.Entry<K, V>) -> R): NonEmptyMap<K, R> =
        // Map all values.
        all.mapValues(transform)
            // Make sure the entry still has the same key.
            .let { nonEmptyMapOf(entry = entry.key to it.getValue(entry.key), map = it) }
}

@JvmSynthetic
internal fun <K, V> nonEmptyMapOf(entry: Pair<K, V>, vararg t: Pair<K, V>): NonEmptyMap<K, V> =
    NonEmptyMap(entry, t.toMap())

@JvmSynthetic
internal fun <K, V> nonEmptyMapOf(entry: Pair<K, V>, map: Map<K, V>): NonEmptyMap<K, V> =
    NonEmptyMap(entry, map)

@JvmSynthetic
internal fun <K, V> Map<K, V>.toNonEmptyMapOrNull(): NonEmptyMap<K, V>? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null

    val first = iterator.next()
    val remaining = LinkedHashMap<K, V>(size - 1)
    iterator.forEachRemaining { (key, value) -> remaining[key] = value }

    return NonEmptyMap(first.toPair(), remaining)
}
