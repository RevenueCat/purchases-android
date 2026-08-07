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
     */
    suspend fun resolveCheckpoint(identifier: String): CheckpointRulesResolution {
        manager.blobData<CheckpointResponse>(RemoteConfigTopic.CheckpointRules, identifier)
            ?.let { return CheckpointRulesResolution.Found(it) }
        return classifyUnresolved(identifier)
    }

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
        val topic = manager.committedTopicOrNull(RemoteConfigTopic.CheckpointRules)
        return if (topic?.containsKey(identifier) == true) {
            verboseLog { "Checkpoint '$identifier' is published, but its rules could not be read." }
            CheckpointRulesResolution.Unavailable
        } else {
            verboseLog {
                val state = if (topic == null) "is absent" else "carries no item for it"
                "The checkpoint_rules topic $state; checkpoint '$identifier' is not configured."
            }
            CheckpointRulesResolution.NotConfigured
        }
    }
}
