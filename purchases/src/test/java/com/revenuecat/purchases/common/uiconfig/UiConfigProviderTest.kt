package com.revenuecat.purchases.common.uiconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class UiConfigProviderTest {

    private val manager = mockk<RemoteConfigManager>()
    private val provider = UiConfigProvider(manager)

    @Test
    fun `getUiConfig assembles all four blob-ref parts into one UiConfig`() = runTest {
        // Production serves every ui_config part (app, localizations, variable_config, custom_variables) as
        // its own blob-ref item under the topic — never as inline item metadata. An earlier revision read
        // item.metadata directly, which silently produced an all-defaults UiConfig against real backend data.
        stubBlob("app") {
            put("colors", buildJsonObject {})
            put("fonts", buildJsonObject {})
        }
        stubBlob("localizations") {
            put("en_US", buildJsonObject { put("day", "Day") })
        }
        stubBlob("variable_config") {
            put("variable_compatibility_map", buildJsonObject { put("old_var", "new_var") })
            put("function_compatibility_map", buildJsonObject {})
        }
        stubBlob("custom_variables") {
            put(
                "user_name",
                buildJsonObject {
                    put("type", "string")
                    put("default_value", "Friend")
                },
            )
        }

        val uiConfig = provider.getUiConfig()

        assertThat(uiConfig.localizations)
            .isEqualTo(mapOf(LocaleId("en_US") to mapOf(VariableLocalizationKey.DAY to "Day")))
        assertThat(uiConfig.variableConfig.variableCompatibilityMap).isEqualTo(mapOf("old_var" to "new_var"))
        assertThat(uiConfig.customVariables).containsKey("user_name")
        assertThat(uiConfig.customVariables["user_name"]?.type).isEqualTo("string")
        assertThat(uiConfig.customVariables["user_name"]?.defaultValue).isEqualTo("Friend")
    }

    @Test
    fun `getUiConfig defaults every field when no part's blob resolves`() = runTest {
        coEvery {
            manager.blobData<Any>(RemoteConfigTopic.UiConfig, any(), any())
        } returns null

        val uiConfig = provider.getUiConfig()

        assertThat(uiConfig).isEqualTo(com.revenuecat.purchases.UiConfig())
    }

    @Test
    fun `getUiConfig defaults custom_variables to empty when only its blob is unresolved`() = runTest {
        stubBlob("app") {}
        coEvery {
            manager.blobData<Any>(RemoteConfigTopic.UiConfig, "custom_variables", any())
        } returns null
        coEvery {
            manager.blobData<Any>(RemoteConfigTopic.UiConfig, "localizations", any())
        } returns null
        coEvery {
            manager.blobData<Any>(RemoteConfigTopic.UiConfig, "variable_config", any())
        } returns null

        val uiConfig = provider.getUiConfig()

        assertThat(uiConfig.customVariables).isEmpty()
    }

    private fun stubBlob(key: String, content: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) {
        coEvery {
            manager.blobData<Any>(RemoteConfigTopic.UiConfig, key, any())
        } returns buildJsonObject(content)
    }
}
