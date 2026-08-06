package com.revenuecat.purchases.common.checkpoints

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.utils.SerializationException
import com.revenuecat.purchases.utils.serializers.ISO8601DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.Date
import kotlinx.serialization.SerializationException as JsonSerializationException

internal data class CheckpointResponse(
    val identifier: String,
    val id: String?,
    val rules: List<CheckpointRule>,
) {
    companion object {
        internal fun parse(identifier: String, bytes: ByteArray): CheckpointResponse? {
            val payload = try {
                JsonTools.json.decodeFromString<CheckpointPayload>(bytes.decodeToString())
            } catch (e: JsonSerializationException) {
                errorLog(e) { "Failed to parse checkpoint '$identifier' from remote config." }
                return null
            }

            return CheckpointResponse(
                identifier = identifier,
                id = payload.id,
                rules = parseRules(identifier, payload.rules),
            )
        }

        private fun parseRules(identifier: String, element: JsonElement?): List<CheckpointRule> {
            return when (element) {
                null, JsonNull -> emptyList()
                is JsonArray -> {
                    // Checkpoint config is forward-evolving: decode rules independently so one malformed rule
                    // does not discard valid siblings. Each accepted rule still fails closed on targeting and
                    // safety constraints.
                    element.mapNotNull { parseRule(identifier, it) }
                }
                else -> {
                    warnLog { "Skipping malformed rules for checkpoint '$identifier': expected an array." }
                    emptyList()
                }
            }
        }

        private fun parseRule(identifier: String, element: JsonElement): CheckpointRule? {
            val rule = decodeRule(identifier, element) ?: return null
            val reason = when {
                rule.audienceId.isEmpty() -> "missing 'audience'"
                rule.workflowId.isEmpty() -> "missing 'workflow_id'"
                rule.frequencyCap?.type?.isEmpty() == true -> "malformed 'frequency_cap'"
                rule.schedule?.let { it.start == null && it.end == null } == true -> "malformed 'schedule'"
                else -> null
            }
            return if (reason == null) rule else skipRule(identifier, reason)
        }

        private fun decodeRule(identifier: String, element: JsonElement): CheckpointRule? =
            try {
                JsonTools.json.decodeFromJsonElement<CheckpointRule>(element)
            } catch (_: JsonSerializationException) {
                skipRule(identifier, "invalid structure")
            } catch (_: SerializationException) {
                skipRule(identifier, "invalid date")
            }

        private fun skipRule(identifier: String, reason: String): CheckpointRule? {
            warnLog { "Skipping malformed rule for checkpoint '$identifier': $reason." }
            return null
        }
    }
}

@Serializable
private data class CheckpointPayload(
    val id: String? = null,
    val rules: JsonElement? = null,
)

@Serializable
internal data class CheckpointRule(
    val id: String? = null,
    @SerialName("audience")
    val audienceId: String,
    @SerialName("workflow_id")
    val workflowId: String,
    @SerialName("frequency_cap")
    val frequencyCap: CheckpointFrequencyCap? = null,
    val schedule: CheckpointRuleSchedule? = null,
)

@Serializable
internal data class CheckpointFrequencyCap(
    val type: String,
    val count: Int? = null,
    val window: String? = null,
)

@Serializable
internal data class CheckpointRuleSchedule(
    @Serializable(with = ISO8601DateSerializer::class)
    val start: Date? = null,
    @Serializable(with = ISO8601DateSerializer::class)
    val end: Date? = null,
)
