package com.revenuecat.purchases.common.checkpoints

import com.revenuecat.purchases.common.warnLog
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.SerializationException as JsonSerializationException

@Serializable
internal data class CheckpointResponse(
    @Serializable(with = CheckpointRulesSerializer::class)
    val rules: List<CheckpointRule> = emptyList(),
)

@Serializable
internal data class CheckpointRule(
    val id: String? = null,
    @SerialName("audience_id")
    val audienceId: String,
    @SerialName("workflow_id")
    val workflowId: String,
)

private val checkpointRuleListSerializer = ListSerializer(CheckpointRule.serializer())

internal object CheckpointRulesSerializer : KSerializer<List<CheckpointRule>> {
    override val descriptor: SerialDescriptor = checkpointRuleListSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<CheckpointRule> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw JsonSerializationException("Checkpoint rules can only be deserialized from JSON.")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            JsonNull -> emptyList()
            is JsonArray -> {
                // Decode each rule independently so malformed entries do not prevent valid sibling rules
                // from being parsed.
                element.mapNotNull { entry -> decodeRule(jsonDecoder, entry) }
            }
            else -> {
                warnLog { "Skipping malformed checkpoint rules: expected an array." }
                emptyList()
            }
        }
    }

    override fun serialize(encoder: Encoder, value: List<CheckpointRule>) {
        checkpointRuleListSerializer.serialize(encoder, value)
    }

    private fun decodeRule(decoder: JsonDecoder, element: JsonElement): CheckpointRule? =
        try {
            validateRule(decoder.json.decodeFromJsonElement<CheckpointRule>(element))
        } catch (_: JsonSerializationException) {
            skipRule("invalid structure")
        }

    private fun validateRule(rule: CheckpointRule): CheckpointRule? {
        val reason = when {
            rule.audienceId.isEmpty() -> "missing 'audience_id'"
            rule.workflowId.isEmpty() -> "missing 'workflow_id'"
            else -> null
        }
        return if (reason == null) rule else skipRule(reason)
    }

    private fun skipRule(reason: String): CheckpointRule? {
        warnLog { "Skipping malformed checkpoint rule: $reason." }
        return null
    }
}
