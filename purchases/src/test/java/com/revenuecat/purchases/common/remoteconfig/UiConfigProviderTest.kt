package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class UiConfigProviderTest {

    private val manager = mockk<RemoteConfigManager>()
    private val provider = UiConfigProvider(manager)

    @Test
    fun `getUiConfig assembles all four topic parts into one UiConfig`() = runTest {
        val topic = ConfigTopic(
            mapOf(
                "app" to RemoteConfiguration.ConfigItem(
                    metadata = buildJsonObject {
                        put("colors", buildJsonObject {})
                        put("fonts", buildJsonObject {})
                    },
                ),
                "localizations" to RemoteConfiguration.ConfigItem(
                    metadata = buildJsonObject {
                        put("en_US", buildJsonObject { put("day", "Day") })
                    },
                ),
                "variable_config" to RemoteConfiguration.ConfigItem(
                    metadata = buildJsonObject {
                        put("variable_compatibility_map", buildJsonObject { put("old_var", "new_var") })
                        put("function_compatibility_map", buildJsonObject {})
                    },
                ),
                // The part this test exists to guard: dropped from PART_KEYS in an earlier revision, which
                // silently discarded custom_variables from every ui_config read.
                "custom_variables" to RemoteConfiguration.ConfigItem(
                    metadata = buildJsonObject {
                        put(
                            "user_name",
                            buildJsonObject {
                                put("type", "string")
                                put("default_value", "Friend")
                            },
                        )
                    },
                ),
            ),
        )
        coEvery { manager.topic(RemoteConfigTopic.UiConfig) } returns topic

        val uiConfig = provider.getUiConfig()

        assertThat(uiConfig.localizations)
            .isEqualTo(mapOf(LocaleId("en_US") to mapOf(VariableLocalizationKey.DAY to "Day")))
        assertThat(uiConfig.variableConfig.variableCompatibilityMap).isEqualTo(mapOf("old_var" to "new_var"))
        assertThat(uiConfig.customVariables).containsKey("user_name")
        assertThat(uiConfig.customVariables["user_name"]?.type).isEqualTo("string")
        assertThat(uiConfig.customVariables["user_name"]?.defaultValue).isEqualTo("Friend")
    }

    @Test
    fun `getUiConfig defaults every field when the ui_config topic is absent`() = runTest {
        coEvery { manager.topic(RemoteConfigTopic.UiConfig) } returns null

        val uiConfig = provider.getUiConfig()

        assertThat(uiConfig).isEqualTo(com.revenuecat.purchases.UiConfig())
    }

    @Test
    fun `getUiConfig defaults custom_variables to empty when its topic item is absent`() = runTest {
        val topic = ConfigTopic(
            mapOf(
                "app" to RemoteConfiguration.ConfigItem(metadata = JsonObject(emptyMap())),
            ),
        )
        coEvery { manager.topic(RemoteConfigTopic.UiConfig) } returns topic

        val uiConfig = provider.getUiConfig()

        assertThat(uiConfig.customVariables).isEmpty()
    }
}
