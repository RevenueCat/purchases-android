package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.AdMobRewardVerificationStatus
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.warnLog
import kotlinx.serialization.Serializable

@Serializable
internal data class AdMobRewardVerificationStatusResponse(
    val status: String,
) {
    @OptIn(InternalRevenueCatAPI::class)
    fun toAdMobRewardVerificationStatus(): AdMobRewardVerificationStatus {
        return when (status.lowercase()) {
            "pending" -> AdMobRewardVerificationStatus.PENDING
            "verified" -> AdMobRewardVerificationStatus.VERIFIED
            "failed" -> AdMobRewardVerificationStatus.FAILED
            else -> {
                warnLog { "Unknown AdMob reward verification status: $status" }
                AdMobRewardVerificationStatus.UNKNOWN
            }
        }
    }
}
