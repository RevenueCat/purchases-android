package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic

internal class AudiencesConfigProvider(
    private val manager: RemoteConfigManager,
) {
    suspend fun getAudience(identifier: String): Audience? =
        manager.blobData(RemoteConfigTopic.Audiences, identifier)
}
