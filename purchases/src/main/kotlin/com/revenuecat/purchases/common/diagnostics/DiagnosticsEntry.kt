package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.utils.Event
import com.revenuecat.purchases.utils.Iso8601Utils
import org.json.JSONObject
import java.util.Date

internal data class DiagnosticsEntry(
    val name: DiagnosticsEntryName,
    val properties: Map<String, Any>,
    val dateProvider: DateProvider = DefaultDateProvider(),
    val dateTime: Date = dateProvider.now,
) : Event {
    private companion object {
        const val VERSION_KEY = "version"
        const val NAME_KEY = "name"
        const val PROPERTIES_KEY = "properties"
        const val TIMESTAMP_KEY = "timestamp"

        const val VERSION = 1
    }

    override fun toString(): String {
        return toJSONObject().toString()
    }

    private fun toJSONObject() = JSONObject().apply {
        put(VERSION_KEY, VERSION)
        put(NAME_KEY, name.name.lowercase())
        put(PROPERTIES_KEY, JSONObject(properties))
        put(TIMESTAMP_KEY, Iso8601Utils.format(dateTime))
    }
}
