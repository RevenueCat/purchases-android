package com.revenuecat.purchases.common.diagnostics

import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.utils.Event
import com.revenuecat.purchases.utils.Iso8601Utils
import org.json.JSONObject
import java.util.Date
import java.util.UUID

internal data class DiagnosticsEntry(
    val id: UUID = UUID.randomUUID(),
    val name: DiagnosticsEntryName,
    val properties: Map<String, Any>,
    val appSessionID: UUID,
    val dateProvider: DateProvider = DefaultDateProvider(),
    val dateTime: Date = dateProvider.now,
) : Event {
    private companion object {
        const val ID_KEY = "id"
        const val VERSION_KEY = "version"
        const val NAME_KEY = "name"
        const val PROPERTIES_KEY = "properties"
        const val APP_SESSION_ID_KEY = "app_session_id"
        const val TIMESTAMP_KEY = "timestamp"

        const val VERSION = 1
    }

    override fun toString(): String {
        return toJSONObject().toString()
    }

    private fun toJSONObject() = JSONObject().apply {
        put(ID_KEY, id)
        put(VERSION_KEY, VERSION)
        put(NAME_KEY, name.name.lowercase())
        put(PROPERTIES_KEY, JSONObject(properties))
        put(APP_SESSION_ID_KEY, appSessionID)
        put(TIMESTAMP_KEY, Iso8601Utils.format(dateTime))
    }
}
