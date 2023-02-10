package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

sealed class DiagnosticsEvent(val diagnosticType: String) {
    companion object {
        private const val VERSION_KEY = "version"
        private const val TYPE_KEY = "type"

        private const val VERSION = 1
    }

    data class Log(
        val name: DiagnosticsLogEventName,
        val properties: Map<String, Any>,
        val dateProvider: DateProvider = DefaultDateProvider(),
        val dateTime: Date = dateProvider.now
    ) : DiagnosticsEvent("log") {
        private companion object {
            const val NAME_KEY = "name"
            const val PROPERTIES_KEY = "properties"
            const val TIMESTAMP_KEY = "timestamp"
        }

        override fun toString(): String {
            return toJSONObject().toString()
        }

        private fun toJSONObject() = JSONObject().apply {
            put(VERSION_KEY, VERSION)
            put(TYPE_KEY, diagnosticType)
            put(NAME_KEY, name.name.lowercase())
            put(PROPERTIES_KEY, JSONObject(properties))
            put(TIMESTAMP_KEY, dateTime.time)
        }
    }

    data class Metric(
        val name: String,
        val tags: List<String>,
        val value: Int
    ) : DiagnosticsEvent("metric") {
        private companion object {
            const val NAME_KEY = "name"
            const val TAGS_KEY = "tags"
            const val VALUE_KEY = "value"
        }

        override fun toString(): String {
            return toJSONObject().toString()
        }

        private fun toJSONObject() = JSONObject().apply {
            put(VERSION_KEY, VERSION)
            put(TYPE_KEY, diagnosticType)
            put(NAME_KEY, name.lowercase())
            put(TAGS_KEY, JSONArray(tags))
            put(VALUE_KEY, value)
        }
    }

    data class Exception(
        val exceptionClass: String,
        val message: String,
        val location: String,
        val dateProvider: DateProvider = DefaultDateProvider(),
        val dateTime: Date = dateProvider.now
    ) : DiagnosticsEvent("exception") {
        private companion object {
            const val EXCEPTION_CLASS_KEY = "exc_class"
            const val MESSAGE_KEY = "message"
            const val LOCATION_KEY = "location"
            const val TIMESTAMP_KEY = "timestamp"
        }

        override fun toString(): String {
            return toJSONObject().toString()
        }

        private fun toJSONObject() = JSONObject().apply {
            put(VERSION_KEY, VERSION)
            put(TYPE_KEY, diagnosticType)
            put(EXCEPTION_CLASS_KEY, exceptionClass)
            put(MESSAGE_KEY, message)
            put(LOCATION_KEY, location)
            put(TIMESTAMP_KEY, dateTime.time)
        }
    }
}
