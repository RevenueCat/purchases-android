package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.readConsistent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromJsonElement

internal class AudiencesConfigProvider(
    private val manager: RemoteConfigManager,
) {
    /**
     * Reads [identifier]'s audience from the `audiences` topic's inline item metadata, or `null` when the item
     * is unknown or malformed. Guarded by [readConsistent]: the read may suspend across a self-primed
     * `/v1/config` sync, and an audience read against a generation that moved underneath may belong to the
     * previous user.
     */
    suspend fun getAudience(identifier: String): Audience? =
        manager.readConsistent(what = { "audience '$identifier'" }) { _ ->
            val metadata = manager.topic(RemoteConfigTopic.Audiences)
                ?.get(identifier)
                ?.metadata
                ?: return@readConsistent null
            try {
                JsonTools.json.decodeFromJsonElement<Audience>(metadata)
            } catch (e: SerializationException) {
                errorLog(e) { "Failed to parse remote config metadata for audience '$identifier' as JSON." }
                null
            }
        }
}
