package com.revenuecat.purchases.common.uiconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * The topic-specific front door for `ui_config`: four independently-updated parts — `app`, `localizations`,
 * `variable_config`, `custom_variables` — that together deserialize as one [UiConfig], the same shape the legacy
 * offerings response sends pre-assembled in a single JSON object. Each part is its own blob-ref item under the
 * topic (not inline metadata), resolved through [RemoteConfigManager.blobData]. A part with no resolvable blob
 * (topic absent, item absent, or blob unresolvable) is simply omitted, so [UiConfig]'s own field defaults fill
 * the gap.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class UiConfigProvider(
    private val manager: RemoteConfigManager,
) {

    suspend fun getUiConfig(): UiConfig {
        val parts = PART_KEYS.mapNotNull { key ->
            manager.blobData<JsonObject>(RemoteConfigTopic.UiConfig, key)?.let { key to it }
        }.toMap()
        return JsonTools.json.decodeFromJsonElement(JsonObject(parts))
    }

    private companion object {
        private val PART_KEYS = listOf("app", "localizations", "variable_config", "custom_variables")
    }
}
