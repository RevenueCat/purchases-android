package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

sealed class DiagnosticsEntry(val diagnosticType: String) {
    companion object {
        private const val VERSION_KEY = "version"
        private const val TYPE_KEY = "type"

        private const val VERSION = 1
    }

    data class Event(
        val name: DiagnosticsEventName,
        val properties: Map<String, Any>,
        val dateProvider: DateProvider = DefaultDateProvider(),
        val dateTime: Date = dateProvider.now,
    ) : DiagnosticsEntry("event") {
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

    data class Counter(
        val name: DiagnosticsCounterName,
        val tags: Map<String, String>,
        val value: Int,
    ) : DiagnosticsEntry("counter") {
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
            put(NAME_KEY, name.name.lowercase())
            put(TAGS_KEY, JSONObject(tags))
            put(VALUE_KEY, value)
        }
    }

    data class Histogram(
        val name: String,
        val tags: Map<String, String>,
        val values: List<Double>,
    ) : DiagnosticsEntry("histogram") {
        private companion object {
            const val NAME_KEY = "name"
            const val TAGS_KEY = "tags"
            const val VALUES_KEY = "values"
        }

        override fun toString(): String {
            return toJSONObject().toString()
        }

        private fun toJSONObject() = JSONObject().apply {
            put(VERSION_KEY, VERSION)
            put(TYPE_KEY, diagnosticType)
            put(NAME_KEY, name.lowercase())
            put(TAGS_KEY, JSONObject(tags))
            put(VALUES_KEY, JSONArray(values))
        }
    }
}
