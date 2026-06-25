package com.revenuecat.purchases.common.remoteconfig

import kotlin.random.Random

/**
 * A source the SDK can fetch a resource from, carrying the metadata used to choose between
 * alternatives.
 */
internal interface WeightedSource {

    /** Higher values are preferred. A tier is exhausted before a lower one is considered. */
    val priority: Int

    /** Relative likelihood of being chosen among sources tied at the same [priority]. */
    val weight: Int
}

/**
 * Picks which [WeightedSource] to use and exposes [current] so callers can advance through the
 * fallback order when a source is unusable.
 *
 * The order is computed up front: priority tiers from highest to lowest, each tier arranged into a
 * weight-biased random order. Negative weights are treated as 0; when a group's weights sum to 0,
 * the next source is drawn uniformly at random.
 *
 * Not thread-safe. Callers sharing an instance must serialize access.
 */
internal class WeightedSourceSelector<T : WeightedSource>(
    sources: List<T>,
    random: Random = Random.Default,
) {

    private val orderedSources: List<T> = computeOrder(sources, random)
    private var iterator: Iterator<T> = orderedSources.iterator()

    /** The source currently in use, or null if there are no sources left to try. */
    var current: T? = nextOrNull()
        private set

    /** Moves to the next source in the fallback order. Returns null if none remain. */
    fun advance(): T? {
        current = nextOrNull()
        return current
    }

    /** Rewinds to the first source in the fallback order. */
    fun reset() {
        iterator = orderedSources.iterator()
        current = nextOrNull()
    }

    private fun nextOrNull(): T? = if (iterator.hasNext()) iterator.next() else null

    private companion object {

        fun <T : WeightedSource> computeOrder(sources: List<T>, random: Random): List<T> =
            sources
                .groupBy { it.priority }
                .entries
                .sortedByDescending { it.key }
                .flatMap { weightedShuffle(it.value, random) }

        fun <T : WeightedSource> weightedShuffle(tier: List<T>, random: Random): List<T> {
            if (tier.size <= 1) return tier

            val remaining = tier.toMutableList()
            val ordered = ArrayList<T>(remaining.size)
            while (remaining.size > 1) {
                ordered.add(remaining.removeAt(weightedPickIndex(remaining, random)))
            }
            ordered.add(remaining.first())
            return ordered
        }

        fun <T : WeightedSource> weightedPickIndex(sources: List<T>, random: Random): Int {
            val weights = sources.map { maxOf(0, it.weight) }
            val totalWeight = weights.fold(0, ::sumOrIntMax)

            if (totalWeight <= 0) return random.nextInt(sources.size)

            var target = random.nextInt(totalWeight)
            var selected = weights.lastIndex
            for (index in weights.indices) {
                target -= weights[index]
                if (target < 0) {
                    selected = index
                    break
                }
            }
            return selected
        }

        /** Adds [lhs] and [rhs], clamping to Int.MAX_VALUE instead of overflowing. */
        fun sumOrIntMax(lhs: Int, rhs: Int): Int =
            (lhs.toLong() + rhs.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
