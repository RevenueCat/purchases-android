@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.subscriberdimensions

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.remoteconfig.ConfigTopic
import com.revenuecat.purchases.common.remoteconfig.GenerationGuardedCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigCommitListener
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.readConsistent
import com.revenuecat.purchases.common.verboseLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The topic-specific front door for `subscriber_dimensions`: a single inline `default` item whose metadata is
 * the [SubscriberDimensions] object (no blob).
 *
 * Memory-first: the resolution is warmed into memory at configure (never syncing `/v1/config`) and re-warmed on
 * every config commit, so [cachedDimensions] can be read synchronously on the caller's thread. It is served only
 * while warm at the manager's *current* [RemoteConfigManager.configGeneration]; otherwise [cachedDimensions] is
 * `null` and [getDimensions] falls through to the config layer. The topic is optional: a committed config
 * without it warms [SubscriberDimensionsResolution.NotConfigured], which is served from memory like any value.
 */
internal class SubscriberDimensionsConfigProvider(
    private val manager: RemoteConfigManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RemoteConfigCommitListener {

    private val cache = GenerationGuardedCache<SubscriberDimensionsResolution>()

    /**
     * The warmed resolution at the current config generation, or `null` when not warm. Never suspends or reads
     * disk.
     */
    fun cachedDimensions(): SubscriberDimensionsResolution? = cache.cachedAtOrAbove(manager.configGeneration)

    /**
     * The committed resolution. A cold read happens under one config generation: [readConsistent] re-reads once
     * if a commit races the read, and gives up with [SubscriberDimensionsResolution.NotConfigured] if that read
     * is superseded too.
     */
    suspend fun getDimensions(): SubscriberDimensionsResolution {
        cachedDimensions()?.let { return it }
        return manager.readConsistent(what = { "the subscriber_dimensions topic" }) { _ ->
            manager.topic(RemoteConfigTopic.SubscriberDimensions).toResolution()
        } ?: SubscriberDimensionsResolution.NotConfigured
    }

    /**
     * Populates the in-memory cache from the just-committed config, tagged with [generation]. No-op while no
     * config is committed yet, so a cold-disk init warm never triggers a network config fetch. A warm superseded
     * by a newer commit during its read gives up: that commit re-warms on its own.
     */
    suspend fun warm(generation: Int) {
        if (cache.isWarmAtOrAbove(generation) || !manager.hasCommittedConfig()) return
        val resolution = manager.committedTopicOrNull(RemoteConfigTopic.SubscriberDimensions).toResolution()
        if (manager.configGeneration != generation || !cache.store(generation, resolution)) return
        verboseLog {
            when (resolution) {
                is SubscriberDimensionsResolution.Found ->
                    "Warmed subscriber dimensions cache: ${resolution.dimensions.values.size} dimension(s) " +
                        "as of ${resolution.dimensions.asOf}."
                SubscriberDimensionsResolution.NotConfigured ->
                    "Warmed subscriber dimensions cache: the topic is not configured."
            }
        }
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

    private fun ConfigTopic?.toResolution(): SubscriberDimensionsResolution =
        this?.get(ITEM_DEFAULT)
            ?.let { SubscriberDimensions.parse(it.metadata) }
            ?.let { SubscriberDimensionsResolution.Found(it) }
            ?: SubscriberDimensionsResolution.NotConfigured

    private companion object {
        const val ITEM_DEFAULT = "default"
    }
}
