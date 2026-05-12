package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.AdMobRewardVerificationStatus
import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdMobRewardVerificationStatusResponse(
    @SerialName("status")
    val status: String,
) {
    @OptIn(InternalRevenueCatAPI::class)
    fun toAdMobRewardVerificationStatus(): AdMobRewardVerificationStatus {
        return when (status.lowercase()) {
            "pending" -> AdMobRewardVerificationStatus.PENDING
            "verified" -> AdMobRewardVerificationStatus.VERIFIED
            "failed" -> AdMobRewardVerificationStatus.FAILED
            else -> AdMobRewardVerificationStatus.UNKNOWN
        }
    }
}
