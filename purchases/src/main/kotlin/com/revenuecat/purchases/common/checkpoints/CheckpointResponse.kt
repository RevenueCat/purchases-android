package com.revenuecat.purchases.common.checkpoints

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.utils.Iso8601Utils
import com.revenuecat.purchases.utils.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.util.Date
import kotlinx.serialization.SerializationException as JsonSerializationException

internal data class CheckpointResponse(
    val identifier: String,
    val id: String?,
    val rules: List<CheckpointRule>,
) {
    companion object {
        internal fun parse(identifier: String, bytes: ByteArray): CheckpointResponse? {
            val fields = try {
                JsonTools.json.parseToJsonElement(bytes.decodeToString()) as? JsonObject
            } catch (e: JsonSerializationException) {
                errorLog(e) { "Failed to parse checkpoint '$identifier' from remote config." }
                null
            } ?: return null

            return CheckpointResponse(
                identifier = identifier,
                id = fields.stringOrNull(ID_KEY),
                rules = parseRules(identifier, fields[RULES_KEY]),
            )
        }

        @Suppress("ReturnCount")
        private fun parseRules(identifier: String, element: JsonElement?): List<CheckpointRule> {
            if (element == null || element is JsonNull) return emptyList()
            val entries = element as? JsonArray ?: run {
                warnLog { "Skipping malformed rules for checkpoint '$identifier': expected an array." }
                return emptyList()
            }

            // Checkpoint config is forward-evolving: isolate malformed rules so one bad entry does not discard
            // valid siblings. Each accepted rule still fails closed on targeting and optional safety constraints.
            return entries.mapNotNull { parseRule(identifier, it) }
        }

        @Suppress("ReturnCount")
        private fun parseRule(identifier: String, element: JsonElement): CheckpointRule? {
            val fields = element as? JsonObject
                ?: return skipRule(identifier, "expected each rule to be an object")
            val audienceId = fields.stringOrNull(AUDIENCE_KEY)?.takeIf { it.isNotEmpty() }
                ?: return skipRule(identifier, "missing '$AUDIENCE_KEY'")
            val workflowId = fields.stringOrNull(WORKFLOW_ID_KEY)?.takeIf { it.isNotEmpty() }
                ?: return skipRule(identifier, "missing '$WORKFLOW_ID_KEY'")

            val frequencyCapElement = fields[FREQUENCY_CAP_KEY]
            val frequencyCap = when (frequencyCapElement) {
                null, JsonNull -> null
                else -> parseFrequencyCap(frequencyCapElement)
                    ?: return skipRule(identifier, "malformed '$FREQUENCY_CAP_KEY'")
            }
            val scheduleElement = fields[SCHEDULE_KEY]
            val schedule = when (scheduleElement) {
                null, JsonNull -> null
                else -> parseSchedule(scheduleElement)
                    ?: return skipRule(identifier, "malformed '$SCHEDULE_KEY'")
            }

            return CheckpointRule(
                id = fields.stringOrNull(ID_KEY),
                audienceId = audienceId,
                workflowId = workflowId,
                frequencyCap = frequencyCap,
                schedule = schedule,
            )
        }

        @Suppress("ReturnCount")
        private fun parseFrequencyCap(element: JsonElement): CheckpointFrequencyCap? {
            val fields = element as? JsonObject ?: return null
            val type = fields.stringOrNull(TYPE_KEY)?.takeIf { it.isNotEmpty() } ?: return null

            val countElement = fields[COUNT_KEY]
            if (countElement.isMalformedOptionalInt()) {
                return null
            }
            val windowElement = fields[WINDOW_KEY]
            if (windowElement != null && windowElement !is JsonNull && fields.stringOrNull(WINDOW_KEY) == null) {
                return null
            }

            return CheckpointFrequencyCap(
                type = type,
                count = (countElement as? JsonPrimitive)?.intOrNull,
                window = fields.stringOrNull(WINDOW_KEY),
            )
        }

        @Suppress("ReturnCount")
        private fun parseSchedule(element: JsonElement): CheckpointRuleSchedule? {
            val fields = element as? JsonObject ?: return null
            val hasStart = fields[START_KEY]?.let { it !is JsonNull } == true
            val hasEnd = fields[END_KEY]?.let { it !is JsonNull } == true
            if (!hasStart && !hasEnd) return null

            val start = if (hasStart) parseDate(fields[START_KEY]) ?: return null else null
            val end = if (hasEnd) parseDate(fields[END_KEY]) ?: return null else null
            return CheckpointRuleSchedule(start = start, end = end)
        }

        private fun parseDate(element: JsonElement?): Date? {
            val value = (element as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
            return try {
                Iso8601Utils.parse(value)
            } catch (_: SerializationException) {
                null
            }
        }

        private fun JsonElement?.isMalformedOptionalInt(): Boolean {
            if (this == null || this is JsonNull) return false
            return this !is JsonPrimitive || isString || intOrNull == null
        }

        private fun JsonObject.stringOrNull(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

        private fun skipRule(identifier: String, reason: String): CheckpointRule? {
            warnLog { "Skipping malformed rule for checkpoint '$identifier': $reason." }
            return null
        }

        private const val ID_KEY = "id"
        private const val RULES_KEY = "rules"
        private const val AUDIENCE_KEY = "audience"
        private const val WORKFLOW_ID_KEY = "workflow_id"
        private const val FREQUENCY_CAP_KEY = "frequency_cap"
        private const val TYPE_KEY = "type"
        private const val COUNT_KEY = "count"
        private const val WINDOW_KEY = "window"
        private const val SCHEDULE_KEY = "schedule"
        private const val START_KEY = "start"
        private const val END_KEY = "end"
    }
}

internal data class CheckpointRule(
    val id: String?,
    val audienceId: String,
    val workflowId: String,
    val frequencyCap: CheckpointFrequencyCap?,
    val schedule: CheckpointRuleSchedule?,
)

internal data class CheckpointFrequencyCap(
    val type: String,
    val count: Int? = null,
    val window: String? = null,
)

internal data class CheckpointRuleSchedule(
    val start: Date? = null,
    val end: Date? = null,
)
