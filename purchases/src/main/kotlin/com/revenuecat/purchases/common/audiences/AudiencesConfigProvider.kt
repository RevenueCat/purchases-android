package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import kotlinx.serialization.json.JsonObject

internal class AudiencesConfigProvider(
    private val manager: RemoteConfigManager,
) {
    suspend fun getAudience(identifier: String): JsonObject? =
        manager.blobData(RemoteConfigTopic.Audiences, identifier)
}
