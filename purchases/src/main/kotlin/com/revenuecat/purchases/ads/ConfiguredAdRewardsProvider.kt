package com.revenuecat.purchases.ads

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.readConsistent
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
        onReceived: (ConfiguredAdRewards?) -> Unit,
    ) {
        scope.launch {
            onReceived(rewards(adUnitId))
        }
    }

    fun close() {
        scope.cancel()
    }

    suspend fun rewards(adUnitId: String): ConfiguredAdRewards? {
        if (adUnitId.isEmpty()) {
            return unavailable("The ad unit ID must not be empty.")
        }
        return manager.readConsistent(what = { "configured rewards for ad unit '$adUnitId'" }) { _ ->
            resolve(adUnitId)
        }
    }

    private suspend fun resolve(adUnitId: String): ConfiguredAdRewards? {
        if (manager.isDisabled) {
            return unavailable("Remote configuration is disabled.")
        }
        val topic = manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.AdRewards)
        return when {
            topic == null -> if (manager.hasCommittedConfig()) {
                ConfiguredAdRewards(emptyList())
            } else {
                unavailable("No committed remote configuration is available for configured ad rewards.")
            }
            else -> {
                val bytes = manager.blobData(RemoteConfigTopic.AdRewards, ITEM_DEFAULT) { it }
                if (bytes == null) {
                    unavailable("The configured ad rewards blob could not be resolved.")
                } else {
                    decode(bytes.decodeToString(), adUnitId)
                }
            }
        }
    }

    internal companion object {
        private const val ITEM_DEFAULT = "default"

        private fun unavailable(message: String): ConfiguredAdRewards? {
            warnLog { "Configured ad rewards unavailable: $message" }
            return null
        }

        @Suppress("ReturnCount")
        fun decode(data: String, adUnitId: String): ConfiguredAdRewards? {
            val payload = try {
                Json.parseToJsonElement(data)
            } catch (e: SerializationException) {
                return unavailable("Configured ad rewards contain invalid JSON: ${e.message}")
            }
            if (payload !is JsonObject) {
                return unavailable("Configured ad rewards must be a JSON object.")
            }
            val entry = payload[adUnitId] ?: return ConfiguredAdRewards(emptyList())
            return decodeAdUnit(entry, adUnitId)
        }

        @Suppress("ReturnCount")
        private fun decodeAdUnit(value: JsonElement, adUnitId: String): ConfiguredAdRewards? {
            if (value !is JsonObject) {
                return unavailable("Configured rewards for ad unit '$adUnitId' must be an object.")
            }
            val moreRewards = when (val more = value["more_rewards"]) {
                null, JsonNull -> emptyList()
                is JsonArray -> more.mapNotNull(::decodeReward)
                else -> return unavailable("more_rewards for ad unit '$adUnitId' must be an array or null.")
            }
            val reward = decodeReward(value["reward"])
            return when {
                reward != null -> ConfiguredAdRewards(listOf(reward) + moreRewards)
                moreRewards.isEmpty() -> ConfiguredAdRewards(emptyList())
                else -> unavailable(
                    "Ad unit '$adUnitId' has additional rewards without a primary reward.",
                )
            }
        }

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
                warnLog {
                    "Invalid configured virtual currency reward: expected a non-empty code and positive integer amount."
                }
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
                warnLog {
                    "Invalid configured entitlement reward: expected a non-empty identifier and " +
                        "a positive, finite duration in whole seconds."
                }
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
