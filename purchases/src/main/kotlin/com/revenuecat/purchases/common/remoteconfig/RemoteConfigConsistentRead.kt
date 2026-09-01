package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.common.verboseLog

/**
 * Runs [block] against a snapshot of [RemoteConfigManager.configGeneration] and trusts its result only if the
 * generation is unchanged when it returns. A read that suspends across disk IO or a self-primed `/v1/config`
 * sync can have the committed state change under it: an identity change wipes the cache (what was just read may
 * belong to the previous user) and an ordinary commit can publish fresher data. Either advances the generation,
 * so a superseded read is re-run exactly once against the new state; only once, so a burst of commits can't
 * spin here.
 *
 * A **consistent** attempt returning `null` is the answer (the item isn't there), not staleness, and does not
 * retry. `null` is returned when both attempts were superseded — and also for that consistent-`null` answer; a
 * caller that must tell those apart should return a non-null result object from [block] (see the topic
 * providers). [block] receives the snapshot generation, so a caller can tag what it resolves with the
 * generation it belongs to.
 *
 * [what] labels the read in the retry log.
 */
internal suspend fun <T : Any> RemoteConfigManager.readConsistent(
    what: () -> String,
    block: suspend (generation: Int) -> T?,
): T? {
    var generation = configGeneration
    val first = block(generation)
    if (configGeneration == generation) return first
    verboseLog { "Remote config changed while reading ${what()}; reading it again." }
    generation = configGeneration
    return block(generation).takeIf { configGeneration == generation }
}
