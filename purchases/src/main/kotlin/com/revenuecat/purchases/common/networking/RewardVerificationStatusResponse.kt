package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.RewardVerificationStatus
import com.revenuecat.purchases.common.warnLog
import kotlinx.serialization.Serializable

@Serializable
internal data class RewardVerificationStatusResponse(
    val status: String,
) {
    @OptIn(InternalRevenueCatAPI::class)
    fun toRewardVerificationStatus(): RewardVerificationStatus {
        return when (status.lowercase()) {
            "pending" -> RewardVerificationStatus.PENDING
            "verified" -> RewardVerificationStatus.VERIFIED
            "failed" -> RewardVerificationStatus.FAILED
            else -> {
                warnLog { "Unknown reward verification status: $status" }
                RewardVerificationStatus.UNKNOWN
            }
        }
    }
}
