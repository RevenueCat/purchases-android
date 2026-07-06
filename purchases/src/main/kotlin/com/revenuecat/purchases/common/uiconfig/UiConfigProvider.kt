package com.revenuecat.purchases.common.uiconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.UiConfig.AppConfig
import com.revenuecat.purchases.UiConfig.CustomVariableDefinition
import com.revenuecat.purchases.UiConfig.VariableConfig
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.paywalls.components.common.LocalizedVariableLocalizationKeyMapSerializer
import kotlinx.serialization.SerializationException

/**
 * The topic-specific front door for `ui_config`: four independently-updated parts — `app`, `localizations`,
 * `variable_config`, `custom_variables` — that together make up one [UiConfig], the same shape the legacy
 * offerings response sends pre-assembled in a single JSON object. Each part is its own blob-ref item under the
 * topic (not inline metadata), resolved and decoded into its target type through [RemoteConfigManager.blobData].
 * A part with no resolvable blob (topic absent, item absent, or blob unresolvable) falls back to [UiConfig]'s
 * own field default.
 */
@OptIn(InternalRevenueCatAPI::class)
internal class UiConfigProvider(
    private val manager: RemoteConfigManager,
) {

    suspend fun getUiConfig(): UiConfig {
        val topic = RemoteConfigTopic.UiConfig
        return UiConfig(
            app = manager.blobData<AppConfig>(topic, "app") ?: AppConfig(),
            // `localizations` needs its property-level serializer, which skips unknown VariableLocalizationKeys.
            // Swallow a malformed blob to null (like the reified blobData overload does) so it defaults to empty
            // instead of throwing out of the provider.
            localizations = manager.blobData(topic, "localizations") { bytes ->
                try {
                    JsonTools.json.decodeFromString(
                        LocalizedVariableLocalizationKeyMapSerializer,
                        bytes.decodeToString(),
                    )
                } catch (e: SerializationException) {
                    errorLog(e) { "Failed to parse remote config blob for item 'localizations' as JSON." }
                    null
                }
            } ?: emptyMap(),
            variableConfig = manager.blobData<VariableConfig>(topic, "variable_config") ?: VariableConfig(),
            customVariables = manager.blobData<Map<String, CustomVariableDefinition>>(topic, "custom_variables")
                ?: emptyMap(),
        )
    }
}
