package com.revenuecat.purchases.common.uiconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
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
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class UiConfigProviderTest {

    private val manager = mockk<RemoteConfigManager>()
    private val provider = UiConfigProvider(manager)

    // This is a plain JUnit test (no Robolectric), so the default log handler's android.util.Log calls aren't
    // mocked. Swap in a no-op handler so the malformed-blob path can log without blowing up.
    private val originalLogHandler = currentLogHandler

    @Before
    fun setUp() {
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

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

    @Test
    fun `getUiConfig defaults localizations to empty when its blob is malformed`() = runTest {
        // A localizations blob that isn't the expected locale->map shape must default to empty, not throw out
        // of the provider — matching how the reified blobData overload swallows malformed data for other parts.
        stubBlobRaw("localizations", """["not", "an", "object"]""")
        coEvery { manager.blobData<Any>(RemoteConfigTopic.UiConfig, "app", any()) } returns null
        coEvery { manager.blobData<Any>(RemoteConfigTopic.UiConfig, "variable_config", any()) } returns null
        coEvery { manager.blobData<Any>(RemoteConfigTopic.UiConfig, "custom_variables", any()) } returns null

        val uiConfig = provider.getUiConfig()

        assertThat(uiConfig.localizations).isEmpty()
    }

    // Each part is decoded by blobData's transform overload. Stubbing that overload to actually run the
    // transform against the part's real bytes exercises each field's real serializer, exactly as production does.
    private fun stubBlob(key: String, content: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) {
        stubBlobRaw(key, buildJsonObject(content).toString())
    }

    private fun stubBlobRaw(key: String, json: String) {
        val bytes = json.encodeToByteArray()
        coEvery {
            manager.blobData(RemoteConfigTopic.UiConfig, key, any<(ByteArray) -> Any?>())
        } answers {
            thirdArg<(ByteArray) -> Any?>().invoke(bytes)
        }
    }
}
