package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.UiConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * The topic-specific front door for `ui_config` (WFL-374): three independently-updated blob refs — `app`,
 * `localizations`, `variable_config` — that together deserialize as one [UiConfig], the same shape the legacy
 * offerings response sends pre-assembled in a single JSON object. A part with no body (topic absent, item
 * absent, or its blob unavailable) is simply omitted, so [UiConfig]'s own field defaults fill the gap.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class UiConfigProvider(
    private val manager: RemoteConfigManager,
) {

    suspend fun getUiConfig(): UiConfig {
        val parts = PART_KEYS.mapNotNull { key ->
            manager.body(TOPIC_NAME, key)?.let { bytes ->
                key to JsonTools.json.parseToJsonElement(bytes.decodeToString())
            }
        }.toMap()
        return JsonTools.json.decodeFromJsonElement(JsonObject(parts))
    }

    private companion object {
        private const val TOPIC_NAME = "ui_config"
        private val PART_KEYS = listOf("app", "localizations", "variable_config")
    }
}
