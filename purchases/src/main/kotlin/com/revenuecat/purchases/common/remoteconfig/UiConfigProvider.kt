package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.UiConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * The topic-specific front door for `ui_config`: three independently-updated parts — `app`, `localizations`,
 * `variable_config` — that together deserialize as one [UiConfig], the same shape the legacy offerings response
 * sends pre-assembled in a single JSON object. Each part is inline item metadata (no blob), so it's read straight
 * off the topic's item index. A part with no body (topic absent or item absent) is simply omitted, so [UiConfig]'s
 * own field defaults fill the gap.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class UiConfigProvider(
    private val manager: RemoteConfigManager,
) {

    suspend fun getUiConfig(): UiConfig {
        val topic = manager.topic(RemoteConfigTopic.UiConfig)
        val parts = PART_KEYS.mapNotNull { key -> topic?.get(key)?.let { key to it.metadata } }.toMap()
        return JsonTools.json.decodeFromJsonElement(JsonObject(parts))
    }

    private companion object {
        private val PART_KEYS = listOf("app", "localizations", "variable_config")
    }
}
