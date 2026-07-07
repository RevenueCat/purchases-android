package com.revenuecat.purchases.common.uiconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic

/**
 * The topic-specific front door for `ui_config`: four independently-updated parts — `app`, `localizations`,
 * `variable_config`, `custom_variables` — that together make up one [UiConfig], the same shape the legacy
 * offerings response sends pre-assembled in a single JSON object. Each part is its own blob-ref item under the
 * topic (not inline metadata). The parts are resolved concurrently and merged into a single keyed object via
 * [RemoteConfigManager.mergeItemsBlobData], whose item-key-to-blob shape matches [UiConfig]'s wire format
 * exactly, so the merged object decodes straight into [UiConfig] — including the property-level localizations
 * serializer that skips unknown variable localization keys.
 *
 * The merge is all-or-nothing: if any part is missing, unresolvable, or the merged object doesn't decode, the
 * whole config falls back to a default [UiConfig] rather than a partially-populated one.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class UiConfigProvider(
    private val manager: RemoteConfigManager,
) {

    suspend fun getUiConfig(): UiConfig =
        manager.mergeItemsBlobData<UiConfig>(
            RemoteConfigTopic.UiConfig,
            listOf("app", "localizations", "variable_config", "custom_variables"),
        ) ?: UiConfig()
}
