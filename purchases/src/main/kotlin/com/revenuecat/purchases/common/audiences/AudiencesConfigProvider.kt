package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromJsonElement

internal class AudiencesConfigProvider(
    private val manager: RemoteConfigManager,
) {
    suspend fun getAudience(identifier: String): Audience? {
        val metadata = manager.topic(RemoteConfigTopic.Audiences)
            ?.get(identifier)
            ?.metadata
            ?: return null
        return try {
            JsonTools.json.decodeFromJsonElement<Audience>(metadata)
        } catch (e: SerializationException) {
            errorLog(e) { "Failed to parse remote config metadata for audience '$identifier' as JSON." }
            null
        }
    }
}
