package com.revenuecat.purchases.common.checkpoints

import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.GenerationGuardedCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigCommitListener
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.readConsistent
import com.revenuecat.purchases.common.verboseLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * The topic-specific front door for `checkpoint_rules`: one item per checkpoint identifier, whose blob is the
 * ordered rule list that maps that checkpoint to a workflow.
 *
 * Memory-first: every committed checkpoint's rules are warmed into memory at configure (from disk, never
 * syncing) and re-warmed on every config commit, so a checkpoint hit resolves without disk IO. A cached value is
 * served only while it is warm at the manager's *current* [RemoteConfigManager.configGeneration]: the resolver
 * verifies the generation a match came from, and reads audiences under the same one, so an older snapshot must
 * not be served during a re-warm. Anything not in memory at the current generation — the re-warm window, an
 * identifier the warm could not read, an identity change — falls through to the config layer, which self-primes
 * a `/v1/config` sync on a cold read.
 */
internal class CheckpointsConfigProvider(
    private val manager: RemoteConfigManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RemoteConfigCommitListener {

    private val cache = GenerationGuardedCache<Map<String, CheckpointResponse>>()

    /**
     * Reads [identifier]'s rules, reporting *why* nothing came back so an unconfigured checkpoint can be told
     * apart from one whose rules could not be read. See [CheckpointRulesResolution].
     *
     * A cold read suspends across disk IO and possibly a self-primed `/v1/config` sync, so the committed state
     * can change under it; [readConsistent] re-reads once against the new state. Since a miss has no in-memory
     * value to fall back on, a read superseded twice is [CheckpointRulesResolution.Unavailable].
     */
    suspend fun resolveCheckpoint(identifier: String): CheckpointRulesResolution {
        cache.cached?.let { cached ->
            val generation = manager.configGeneration
            if (cache.isWarmAtOrAbove(generation)) {
                cached[identifier]?.let { return CheckpointRulesResolution.Found(it, generation) }
            }
        }
        return manager.readConsistent(what = { "checkpoint '$identifier'" }) { generation ->
            readCheckpoint(identifier, generation)
        } ?: CheckpointRulesResolution.Unavailable
    }

    fun isCurrent(resolution: CheckpointRulesResolution.Found): Boolean =
        manager.configGeneration == resolution.configGeneration

    /**
     * Best-effort populate of the in-memory cache from already-committed config, tagged with [generation]. No-op
     * (no `/v1/config` sync) when the topic isn't committed yet, so a cold-disk init warm never triggers a
     * network config fetch. Only items whose blob resolves and parses are cached; the rest fall through to the
     * config layer on read, which classifies them.
     */
    suspend fun warm(generation: Int) {
        if (cache.isWarmAtOrAbove(generation)) return
        val topic = manager.committedTopicOrNull(RemoteConfigTopic.CheckpointRules) ?: return
        val checkpoints = coroutineScope {
            topic.keys
                .map { identifier -> async { identifier to readCheckpointBlob(identifier) } }
                .awaitAll()
        }.mapNotNull { (identifier, checkpoint) -> checkpoint?.let { identifier to it } }.toMap()
        verboseLog { "Warmed checkpoint rules cache: ${checkpoints.size} of ${topic.size} checkpoint(s)." }
        cache.store(generation, checkpoints)
    }

    /** Fire-and-forget [warm] on this provider's own scope; used for the cold-start init warm. */
    fun warmAsync(generation: Int) {
        scope.launch { warm(generation) }
    }

    override fun onConfigCommitted(generation: Int) {
        scope.launch { warm(generation) }
    }

    override fun onConfigInvalidated(generation: Int) {
        cache.invalidate(generation)
    }

    fun close() {
        scope.cancel()
    }

    private suspend fun readCheckpoint(identifier: String, generation: Int): CheckpointRulesResolution =
        readCheckpointBlob(identifier)
            ?.let { CheckpointRulesResolution.Found(it, generation) }
            ?: classifyUnresolved(identifier)

    private suspend fun readCheckpointBlob(identifier: String): CheckpointResponse? =
        manager.blobData<CheckpointResponse>(RemoteConfigTopic.CheckpointRules, identifier)

    /**
     * Only ever called **after** a read attempt: a cold read waits for (or triggers) a `/v1/config` sync, so the
     * committed state inspected here is post-sync. Classifying first would see the cold cache and report
     * [CheckpointRulesResolution.NotConfigured] for a checkpoint that does exist.
     */
    private suspend fun classifyUnresolved(identifier: String): CheckpointRulesResolution {
        // First: committedTopicOrNull also returns null when the endpoint is disabled, which would otherwise be
        // indistinguishable from an absent topic. Checkpoints are never resolved with remote config off
        // (customEntitlementComputation), so a call here is a wiring bug worth surfacing.
        if (manager.isDisabled) {
            errorLog {
                "Checkpoint '$identifier' is unavailable: remote config is disabled for this SDK configuration."
            }
            return CheckpointRulesResolution.Unavailable
        }
        // A project with no checkpoints still gets checkpoint_rules committed as an empty item index, so only an
        // item-level miss on a committed topic means "not configured": an absent topic means nothing is committed
        // for it at all (a failed or not-yet-run sync), which is a read failure and not the server's answer.
        val topic = manager.committedTopicOrNull(RemoteConfigTopic.CheckpointRules)
        return when {
            topic == null -> {
                verboseLog { "The checkpoint_rules topic is not committed; checkpoint '$identifier' is unknown." }
                CheckpointRulesResolution.Unavailable
            }
            topic.containsKey(identifier) -> {
                verboseLog { "Checkpoint '$identifier' is published, but its rules could not be read." }
                CheckpointRulesResolution.Unavailable
            }
            else -> {
                verboseLog {
                    "The checkpoint_rules topic carries no item for '$identifier'; it is not configured."
                }
                CheckpointRulesResolution.NotConfigured
            }
        }
    }
}
