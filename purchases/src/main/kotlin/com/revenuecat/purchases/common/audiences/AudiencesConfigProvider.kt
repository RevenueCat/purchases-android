package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.common.remoteconfig.GenerationGuardedCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import kotlinx.serialization.json.JsonObject

internal class AudiencesConfigProvider(
    private val manager: RemoteConfigManager,
) {
    private val cache = GenerationGuardedCache<Map<String, JsonObject>>()

    suspend fun getAudience(identifier: String): JsonObject? {
        cache.cached?.get(identifier)?.let { return it }
        val generation = manager.configGeneration
        val resolved = manager.blobData<JsonObject>(RemoteConfigTopic.Audiences, identifier)
        return if (cache.isCurrent(generation)) resolved else cache.cached?.get(identifier)
    }
}
