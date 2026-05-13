package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.RewardVerificationResult
import com.revenuecat.purchases.VerifiedReward
import com.revenuecat.purchases.common.warnLog
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
internal data class RewardVerificationStatusResponse(
    val status: String,
    val reward: JsonElement? = null,
) {
    @OptIn(InternalRevenueCatAPI::class)
    fun toRewardVerificationResult(): RewardVerificationResult {
        return when (status.lowercase()) {
            "pending" -> RewardVerificationResult.PENDING
            "verified" -> RewardVerificationResult.Verified(reward = reward.toVerifiedReward())
            "failed" -> RewardVerificationResult.FAILED
            else -> {
                warnLog { "Unknown reward verification status: $status" }
                RewardVerificationResult.UNKNOWN
            }
        }
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun JsonElement?.toVerifiedReward(): VerifiedReward {
        if (this == null || this is JsonNull) {
            return VerifiedReward.NoReward
        }

        val rewardObject = this as? JsonObject
        if (rewardObject == null) {
            warnLog { "Unexpected reward verification reward payload shape: $this" }
            return VerifiedReward.UnsupportedReward
        }

        val rewardType = rewardObject["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
        return when (rewardType) {
            "virtual_currency" -> rewardObject.toVirtualCurrencyRewardOrUnsupported()
            else -> {
                warnLog { "Unsupported reward verification reward type: $rewardType" }
                VerifiedReward.UnsupportedReward
            }
        }
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun JsonObject.toVirtualCurrencyRewardOrUnsupported(): VerifiedReward {
        val code = this["code"]?.jsonPrimitive?.contentOrNull
        val amount = this["amount"]?.jsonPrimitive?.intOrNull

        return if (!code.isNullOrBlank() && amount != null && amount > 0) {
            VerifiedReward.VirtualCurrency(code = code, amount = amount)
        } else {
            warnLog { "Malformed virtual_currency reward payload: $this" }
            VerifiedReward.UnsupportedReward
        }
    }
}
