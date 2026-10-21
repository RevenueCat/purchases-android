package com.revenuecat.purchases.ads

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.readConsistent
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.common.warnLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class ConfiguredAdRewardsProvider(
    private val manager: RemoteConfigManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    fun getRewards(
        adUnitId: String,
        onSuccess: (ConfiguredAdRewards) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        scope.launch {
            val rewards = try {
                rewards(adUnitId)
            } catch (e: PurchasesException) {
                onError(e.error)
                return@launch
            }
            onSuccess(rewards)
        }
    }

    fun close() {
        scope.cancel()
    }

    suspend fun rewards(adUnitId: String): ConfiguredAdRewards {
        if (adUnitId.isEmpty()) {
            throw PurchasesException(
                PurchasesError(PurchasesErrorCode.ConfigurationError, "The ad unit ID must not be empty."),
            )
        }
        val resolution = manager.readConsistent(what = { "configured rewards for ad unit '$adUnitId'" }) { _ ->
            resolve(adUnitId)
        }
        return resolution?.getOrThrow()
            ?: throw unavailable("Remote configuration changed while resolving configured ad rewards.")
    }

    // Keep failures inside the generation check too, so a superseded failure can be retried.
    private suspend fun resolve(adUnitId: String): Result<ConfiguredAdRewards> {
        if (manager.isDisabled) {
            return Result.failure(
                PurchasesException(
                    PurchasesError(PurchasesErrorCode.ConfigurationError, "Remote configuration is disabled."),
                ),
            )
        }
        val topic = manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.AdRewards)
        return when {
            topic == null -> if (manager.hasCommittedConfig()) {
                Result.success(ConfiguredAdRewards(null, emptyList()))
            } else {
                Result.failure(unavailable("No committed remote configuration is available for configured ad rewards."))
            }
            topic[ITEM_DEFAULT]?.blobRef == null ->
                Result.failure(invalidConfiguration("The ad_rewards topic has no default blob reference."))
            else -> manager.blobData(RemoteConfigTopic.AdRewards, ITEM_DEFAULT) { bytes ->
                try {
                    Result.success(decode(bytes.decodeToString(), adUnitId))
                } catch (e: PurchasesException) {
                    Result.failure(e)
                }
            } ?: Result.failure(unavailable("The configured ad rewards blob could not be resolved."))
        }
    }

    private fun unavailable(message: String): PurchasesException =
        PurchasesException(PurchasesError(PurchasesErrorCode.UnknownError, message))

    internal companion object {
        private const val ITEM_DEFAULT = "default"

        fun decode(data: String, adUnitId: String): ConfiguredAdRewards {
            val payload = try {
                Json.parseToJsonElement(data)
            } catch (e: SerializationException) {
                throw PurchasesException(e.toPurchasesError()).apply { initCause(e) }
            }
            if (payload !is JsonObject) {
                throw invalidConfiguration("Configured ad rewards must be a JSON object.")
            }
            val entry = payload[adUnitId] ?: return ConfiguredAdRewards(null, emptyList())
            return decodeAdUnit(entry, adUnitId)
        }

        private fun decodeAdUnit(value: JsonElement, adUnitId: String): ConfiguredAdRewards {
            if (value !is JsonObject) {
                throw invalidConfiguration("Configured rewards for ad unit '$adUnitId' must be an object.")
            }
            val moreRewards = decodeMoreRewards(value["more_rewards"], adUnitId)
            val reward = decodeReward(value["reward"])
            return when {
                reward != null -> ConfiguredAdRewards(reward, moreRewards)
                moreRewards.isEmpty() -> ConfiguredAdRewards(null, emptyList())
                else -> throw invalidConfiguration(
                    "Ad unit '$adUnitId' has additional rewards without a primary reward.",
                )
            }
        }

        private fun decodeMoreRewards(
            value: JsonElement?,
            adUnitId: String,
        ): List<AdRewardConfiguration> = when (value) {
            null, JsonNull -> emptyList()
            is JsonArray -> value.mapNotNull(::decodeReward)
            else -> throw invalidConfiguration("more_rewards for ad unit '$adUnitId' must be an array or null.")
        }

        private fun invalidConfiguration(message: String): PurchasesException =
            PurchasesException(PurchasesError(PurchasesErrorCode.UnknownError, message))

        private fun decodeReward(value: JsonElement?): AdRewardConfiguration? {
            if (value == null || value == JsonNull) return null
            val payload = value as? JsonObject
            return when (val type = payload?.string("type")) {
                "no_reward" -> null
                "virtual_currency" -> decodeCurrency(payload)
                "entitlement" -> decodeEntitlement(payload)
                else -> {
                    warnLog { "Unsupported configured ad reward type: $type" }
                    AdRewardConfiguration.UnsupportedReward
                }
            }
        }

        private fun decodeCurrency(payload: JsonObject): AdRewardConfiguration {
            val code = payload.string("code")
            val amount = payload.number("amount")?.intOrNull
            return if (code.isNullOrEmpty() || amount == null || amount <= 0) {
                AdRewardConfiguration.UnsupportedReward
            } else {
                AdRewardConfiguration.VirtualCurrency(code, amount)
            }
        }

        private fun decodeEntitlement(payload: JsonObject): AdRewardConfiguration {
            val identifier = payload.string("identifier")
            val duration = payload.number("duration_in_seconds")?.longOrNull
                ?.takeIf { it > 0 }?.seconds?.takeIf { it.isFinite() }
            return if (identifier.isNullOrEmpty() || duration == null) {
                AdRewardConfiguration.UnsupportedReward
            } else {
                AdRewardConfiguration.Entitlement(identifier, duration)
            }
        }

        private fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

        private fun JsonObject.number(key: String): JsonPrimitive? =
            (this[key] as? JsonPrimitive)?.takeUnless { it.isString }
    }
}
