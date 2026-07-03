package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.RewardVerificationPollStatus
import com.revenuecat.purchases.VerifiedReward
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.utils.Iso8601Utils
import com.revenuecat.purchases.utils.SerializationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
internal data class RewardVerificationResponse(
    val status: String,
    val reward: JsonElement? = null,
    @SerialName("more_rewards") val moreRewards: List<JsonElement>? = null,
    @SerialName("failure_reason") val failureReason: String? = null,
    val message: String? = null,
) {
    @OptIn(InternalRevenueCatAPI::class)
    fun toRewardVerificationPollStatus(): RewardVerificationPollStatus {
        return when (status.lowercase()) {
            "pending" -> RewardVerificationPollStatus.PENDING
            "verified" -> RewardVerificationPollStatus.Verified(
                reward = reward.toVerifiedReward(),
                moreRewards = moreRewards.orEmpty().map { it.toVerifiedReward() },
            )
            "failed" -> RewardVerificationPollStatus.Failed(failureReason = failureReason, message = message)
            else -> {
                warnLog { "Unknown reward verification status: $status" }
                RewardVerificationPollStatus.UNKNOWN
            }
        }
    }

    @OptIn(InternalRevenueCatAPI::class)
    private fun JsonElement?.toVerifiedReward(): VerifiedReward {
        return when {
            this == null || this is JsonNull -> null
            this is JsonObject -> this
            else -> {
                warnLog { "Unexpected reward verification reward payload shape: $this" }
                return VerifiedReward.UnsupportedReward
            }
        }?.let {
            val rewardType = it["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
            when (rewardType) {
                "virtual_currency" -> it.toVirtualCurrencyRewardOrUnsupported()
                "entitlement" -> it.toEntitlementRewardOrUnsupported()
                else -> {
                    warnLog { "Unsupported reward verification reward type: $rewardType" }
                    VerifiedReward.UnsupportedReward
                }
            }
        } ?: VerifiedReward.NoReward
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

    @OptIn(InternalRevenueCatAPI::class)
    private fun JsonObject.toEntitlementRewardOrUnsupported(): VerifiedReward {
        val identifier = this["identifier"]?.jsonPrimitive?.contentOrNull
        // `expires_at` is an ISO-8601 string (e.g. "2026-06-16T12:00:00Z"), not epoch seconds.
        val expiresAtRaw = this["expires_at"]?.jsonPrimitive?.contentOrNull

        if (identifier.isNullOrBlank() || expiresAtRaw.isNullOrBlank()) {
            warnLog { "Malformed entitlement reward payload: $this" }
            return VerifiedReward.UnsupportedReward
        }

        return try {
            VerifiedReward.Entitlement(identifier = identifier, expiresAt = Iso8601Utils.parse(expiresAtRaw))
        } catch (e: SerializationException) {
            warnLog { "Malformed entitlement reward expires_at '$expiresAtRaw': ${e.message}" }
            VerifiedReward.UnsupportedReward
        }
    }
}
