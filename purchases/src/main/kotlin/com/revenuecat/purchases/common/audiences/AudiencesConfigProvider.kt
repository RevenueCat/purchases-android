@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Memory-first: the audience dictionary is warmed into memory at configure (never syncing `/v1/config`) and
 * re-warmed on every config commit. The warm honours the `default` item's `prefetch` flag, so it only joins a
 * blob the manager already prefetches at commit and never downloads one the server left lazy. The dictionary is
 * served only while warm at the manager's *current* [RemoteConfigManager.configGeneration], so rules and
 * audiences read in one checkpoint resolution always come from the same committed config; otherwise the read
 * falls through to the config layer.
 */
internal class AudiencesConfigProvider(
    private val manager: RemoteConfigManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RemoteConfigCommitListener {

    private val cache = GenerationGuardedCache<Map<String, Audience>>()

    /**
     * The audience dictionary from the topic's static `default` blob, or `null` when the topic, that item, or its
     * blob is unavailable. A cold read happens under one config generation: [readConsistent] re-reads once if a
     * commit races the read, and gives up with `null` if that read is superseded too.
     */
    suspend fun getAudiences(): Map<String, Audience>? {
        if (cache.isWarm()) {
            cache.cachedAtOrAbove(manager.configGeneration)?.let { return it }
        }
        return manager.readConsistent(what = { "the audiences topic" }) { _ -> readAudiences() }
    }

    /**
     * Best-effort populate of the in-memory cache from already-committed config, tagged with [generation]. No-op
     * (no `/v1/config` sync) when the topic isn't committed yet, so a cold-disk init warm never triggers a
     * network config fetch, and when the `default` item is not flagged `prefetch`.
     */
    suspend fun warm(generation: Int) {
        if (cache.isWarmAtOrAbove(generation)) return
        val audiences = manager.committedTopicOrNull(RemoteConfigTopic.Audiences)
            ?.get(ITEM_DEFAULT)
            ?.takeIf { it.prefetch }
            ?.let { readAudiences() }
            ?: return
        verboseLog { "Warmed audiences cache: ${audiences.size} audience(s)." }
        cache.store(generation, audiences)
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

    private suspend fun readAudiences(): Map<String, Audience>? =
        manager.blobData(RemoteConfigTopic.Audiences, ITEM_DEFAULT, ::parseAudiences)

    /**
     * An audience that does not parse is dropped rather than failing the whole blob, so one audience on a shape
     * this SDK version does not understand cannot make every other audience unreadable. A rule that references
     * the dropped audience still fails to resolve, which is the honest answer for that rule alone.
     */
    @Suppress("ReturnCount")
    private fun parseAudiences(bytes: ByteArray): Map<String, Audience>? {
        val entries = try {
            JsonTools.json.parseToJsonElement(bytes.decodeToString()).jsonObject
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to parse the audiences blob as JSON." }
            return null
        } catch (e: IllegalArgumentException) {
            errorLog(e) { "The audiences blob is not a JSON object." }
            return null
        }
        return entries.mapNotNull { (identifier, element) ->
            try {
                identifier to JsonTools.json.decodeFromJsonElement<Audience>(element)
            } catch (e: SerializationException) {
                errorLog(e) { "Ignoring audience '$identifier' in the audiences blob: it could not be parsed." }
                null
            }
        }.toMap()
    }

    private companion object {
        const val ITEM_DEFAULT = "default"
    }
}
