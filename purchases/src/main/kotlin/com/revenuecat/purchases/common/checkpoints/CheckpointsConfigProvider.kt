package com.revenuecat.purchases.common.checkpoints

import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic

internal class CheckpointsConfigProvider(
    private val manager: RemoteConfigManager,
) {
    suspend fun getCheckpoint(identifier: String): CheckpointResponse? =
        manager.blobData<CheckpointPayload>(RemoteConfigTopic.Checkpoints, identifier)
            ?.toCheckpointResponse(identifier)
}
