package com.revenuecat.purchases.common.telemetry

import org.json.JSONArray
import org.json.JSONObject

sealed class TelemetryEvent(val telemetryType: String) {
    companion object {
        private const val VERSION_KEY = "version"
        private const val TYPE_KEY = "type"

        private const val VERSION = 1
    }

    data class Metric(
        val name: MetricEventName,
        val tags: List<String>,
        val value: Int
    ) : TelemetryEvent("metric") {
        companion object {
            private const val NAME_KEY = "name"
            private const val TAGS_KEY = "tags"
            private const val VALUE_KEY = "value"
        }

        override fun toString(): String {
            return toJSONObject().toString()
        }

        private fun toJSONObject() = JSONObject().apply {
            put(VERSION_KEY, VERSION)
            put(TYPE_KEY, telemetryType)
            put(NAME_KEY, name.name.lowercase())
            put(TAGS_KEY, JSONArray(tags))
            put(VALUE_KEY, value)
        }
    }
    data class Exception(
        val exceptionClass: String,
        val message: String,
        val location: String,
        val timestamp: String
    ) : TelemetryEvent("exception") {
        companion object {
            private const val EXCEPTION_CLASS_KEY = "exc_class"
            private const val MESSAGE_KEY = "message"
            private const val LOCATION_KEY = "location"
            private const val TIMESTAMP_KEY = "timestamp"
        }

        override fun toString(): String {
            return toJSONObject().toString()
        }

        private fun toJSONObject() = JSONObject().apply {
            put(VERSION_KEY, VERSION)
            put(TYPE_KEY, telemetryType)
            put(EXCEPTION_CLASS_KEY, exceptionClass)
            put(MESSAGE_KEY, message)
            put(LOCATION_KEY, location)
            put(TIMESTAMP_KEY, timestamp)
        }
    }
}
