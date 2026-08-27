package com.revenuecat.purchases.common.checkpoints

import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
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
     * change under it: an identity change wipes the cache (the rules just read may belong to the previous user)
     * and an ordinary commit can publish fresher rules. Either advances [RemoteConfigManager.configGeneration],
     * and since there is no in-memory cache to fall back on the answer is to read once more against the new
     * state rather than to discard. Only once, so a burst of commits can't spin here.
     */
    suspend fun resolveCheckpoint(identifier: String): CheckpointRulesResolution {
        attemptRead(identifier)?.let { return it }
        verboseLog { "Remote config changed while resolving checkpoint '$identifier'; reading it again." }
        return attemptRead(identifier) ?: CheckpointRulesResolution.Unavailable
    }

    /**
     * One read, or `null` if the committed state moved under it and the answer can no longer be trusted.
     * [resolveCheckpoint] retries such a read exactly once, so it never reads more than twice.
     */
    private suspend fun attemptRead(identifier: String): CheckpointRulesResolution? {
        val generation = manager.configGeneration
        val resolution = readCheckpoint(identifier, generation)
        return resolution.takeIf { manager.configGeneration == generation }
    }

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
        // indistinguishable from an absent topic.
        if (manager.isDisabled) {
            verboseLog { "Remote config is disabled (4xx); checkpoint '$identifier' cannot be resolved." }
            return CheckpointRulesResolution.Disabled
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
