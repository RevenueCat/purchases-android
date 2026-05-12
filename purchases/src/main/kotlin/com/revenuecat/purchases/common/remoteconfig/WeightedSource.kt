package com.revenuecat.purchases.common.remoteconfig

import kotlin.random.Random

internal interface WeightedSource {
    val priority: Int
    val weight: Int
}

/**
 * Helper function to select a source from a list of [WeightedSource]s
 * - Highest priority
 * - Tied priority uses weights to choose randomly.
 * - If weights add up to 0 or any negative weight, uniform random selection.
 */
internal fun <T : WeightedSource> List<T>.selectWeighted(random: Random = Random.Default): T? {
    val highestPriority = maxOfOrNull { it.priority } ?: return null
    val candidates = filter { it.priority == highestPriority }
    val totalWeight = candidates.sumOf { it.weight }
    val anyNegativeWeights = candidates.any { it.weight < 0 }
    return if (totalWeight <= 0 || anyNegativeWeights) {
        candidates[random.nextInt(candidates.size)]
    } else {
        val r = random.nextInt(totalWeight)
        var cumulative = 0
        candidates.first { candidate ->
            cumulative += candidate.weight
            r < cumulative
        }
    }
}

internal fun <T : WeightedSource> List<T>.selectWeightedExcluding(
    excludedIds: Set<String>,
    idOf: (T) -> String,
    random: Random = Random.Default,
): T? = filter { idOf(it) !in excludedIds }.selectWeighted(random)
