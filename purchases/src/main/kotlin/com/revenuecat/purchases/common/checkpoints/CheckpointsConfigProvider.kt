package com.revenuecat.purchases.common.checkpoints

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromJsonElement

internal class CheckpointsConfigProvider(
    private val manager: RemoteConfigManager,
) {
    suspend fun getCheckpoint(identifier: String): CheckpointResponse? {
        val item = manager.topic(RemoteConfigTopic.Checkpoints)?.get(identifier) ?: return null
        return if (item.blobRef != null) {
            manager.blobData(RemoteConfigTopic.Checkpoints, identifier)
        } else {
            try {
                JsonTools.json.decodeFromJsonElement<CheckpointResponse>(item.metadata)
            } catch (e: SerializationException) {
                errorLog(e) { "Failed to parse inline checkpoint '$identifier' as JSON." }
                null
            }
        }
    }
}
