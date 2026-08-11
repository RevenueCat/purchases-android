package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.common.remoteconfig.GenerationGuardedCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigCommitListener
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

internal class AudiencesConfigProvider(
    private val manager: RemoteConfigManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RemoteConfigCommitListener {
    private val cache = GenerationGuardedCache<Map<String, JsonObject>>()

    suspend fun getAudience(identifier: String): JsonObject? {
        cache.cached?.get(identifier)?.let { return it }
        val generation = manager.configGeneration
        val resolved = manager.blobData<JsonObject>(RemoteConfigTopic.Audiences, identifier)
        return if (cache.isCurrent(generation)) resolved else cache.cached?.get(identifier)
    }

    suspend fun warm(generation: Int) {
        if (cache.isWarmAtOrAbove(generation)) return
        val topic = manager.committedTopicOrNull(RemoteConfigTopic.Audiences)
        val audiences = coroutineScope {
            topic.orEmpty()
                .filterValues { it.prefetch }
                .keys
                .map { identifier ->
                    async {
                        identifier to manager.blobData<JsonObject>(RemoteConfigTopic.Audiences, identifier)
                    }
                }
                .awaitAll()
                .mapNotNull { (identifier, audience) -> audience?.let { identifier to it } }
                .toMap()
        }
        cache.store(generation, audiences)
    }

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
}
