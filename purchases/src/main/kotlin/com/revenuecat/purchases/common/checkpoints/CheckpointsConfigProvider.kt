package com.revenuecat.purchases.common.checkpoints

import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.readConsistent
import com.revenuecat.purchases.common.verboseLog

/**
 * The topic-specific front door for `checkpoint_rules`: one item per checkpoint identifier, whose blob is the
 * ordered rule list that maps that checkpoint to a workflow. Rule sets are small and read once per checkpoint
 * registration, so there is no in-memory cache here — [RemoteConfigManager] already serves committed blobs from
 * its own caches, and a cold read self-primes a `/v1/config` sync.
 */
internal class CheckpointsConfigProvider(
    private val manager: RemoteConfigManager,
) {
    /**
     * Reads [identifier]'s rules, reporting *why* nothing came back so an unconfigured checkpoint can be told
     * apart from one whose rules could not be read. See [CheckpointRulesResolution].
     *
     * The read suspends across disk IO and possibly a self-primed `/v1/config` sync, so the committed state can
     * change under it; [readConsistent] re-reads once against the new state. Since there is no in-memory cache
     * to fall back on, a read superseded twice is [CheckpointRulesResolution.Unavailable].
     */
    suspend fun resolveCheckpoint(identifier: String): CheckpointRulesResolution =
        manager.readConsistent(what = { "checkpoint '$identifier'" }) { generation ->
            readCheckpoint(identifier, generation)
        } ?: CheckpointRulesResolution.Unavailable

    fun isCurrent(resolution: CheckpointRulesResolution.Found): Boolean =
        manager.configGeneration == resolution.configGeneration

    private suspend fun readCheckpoint(identifier: String, generation: Int): CheckpointRulesResolution =
        manager.blobData<CheckpointResponse>(RemoteConfigTopic.CheckpointRules, identifier)
            ?.let { CheckpointRulesResolution.Found(it, generation) }
            ?: classifyUnresolved(identifier)

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
