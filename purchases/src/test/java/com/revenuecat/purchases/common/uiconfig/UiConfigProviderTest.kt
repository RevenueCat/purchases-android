package com.revenuecat.purchases.common.uiconfig

import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.FontStyle
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class UiConfigProviderTest {

    private val manager = mockk<RemoteConfigManager>()
    private val provider = UiConfigProvider(manager)

    // This is a plain JUnit test (no Robolectric), so the default log handler's android.util.Log calls aren't
    // mocked. Swap in a no-op handler so the undecodable-merge path can log without blowing up.
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
    fun `getUiConfig decodes the merged four-part object into one UiConfig`() = runTest {
        // Production serves every ui_config part (app, localizations, variable_config, custom_variables) as
        // its own blob-ref item under the topic — never as inline item metadata. An earlier revision read
        // item.metadata directly, which silently produced an all-defaults UiConfig against real backend data.
        val requestedKeys = stubMergedRead(
            buildJsonObject {
                putJsonObject("app") {
                    put("colors", buildJsonObject {})
                    put("fonts", buildJsonObject {})
                }
                putJsonObject("localizations") {
                    putJsonObject("en_US") { put("day", "Day") }
                }
                putJsonObject("variable_config") {
                    putJsonObject("variable_compatibility_map") { put("old_var", "new_var") }
                    put("function_compatibility_map", buildJsonObject {})
                }
                putJsonObject("custom_variables") {
                    putJsonObject("user_name") {
                        put("type", "string")
                        put("default_value", "Friend")
                    }
                }
            },
        )

        val uiConfig = provider.getUiConfig()

        assertThat(requestedKeys.captured)
            .containsExactly("app", "localizations", "variable_config", "custom_variables")
        assertThat(uiConfig.localizations)
            .isEqualTo(mapOf(LocaleId("en_US") to mapOf(VariableLocalizationKey.DAY to "Day")))
        assertThat(uiConfig.variableConfig.variableCompatibilityMap).isEqualTo(mapOf("old_var" to "new_var"))
        assertThat(uiConfig.customVariables).containsKey("user_name")
        assertThat(uiConfig.customVariables["user_name"]?.type).isEqualTo("string")
        assertThat(uiConfig.customVariables["user_name"]?.defaultValue).isEqualTo("Friend")
    }

    @Test
    fun `getUiConfig decodes a font whose discriminator uses the real backend key`() = runTest {
        // Regression test: the backend sends FontInfo's sealed-type discriminator under "type" (as in this
        // real payload shape). The merge decode must use a Json instance that resolves that key correctly
        // (JsonTools.json), not JsonProvider.defaultJson, whose classDiscriminator is overridden to
        // "discriminator" for BackendEvent and would silently drop the font instead of decoding it.
        stubMergedRead(
            buildJsonObject {
                putJsonObject("app") {
                    put("colors", buildJsonObject {})
                    putJsonObject("fonts") {
                        putJsonObject("primary") {
                            putJsonObject("android") {
                                put("type", "name")
                                put("value", "CinzelDecorative-Bold")
                                put("url", "https://fonts.pawwalls.com/1195295_cb0a3b55_5b882c667007133983d5.ttf")
                                put("hash", "a388d4f6e855b334da95b975bb30bf4d")
                                put("family", "Cinzel Decorative")
                                put("weight", 700)
                                put("style", "normal")
                            }
                        }
                    }
                }
                put("localizations", buildJsonObject {})
                put("variable_config", buildJsonObject {})
                put("custom_variables", buildJsonObject {})
            },
        )

        val uiConfig = provider.getUiConfig()

        assertThat(uiConfig.app.fonts).containsKey(FontAlias("primary"))
        assertThat(uiConfig.app.fonts[FontAlias("primary")]?.android).isEqualTo(
            UiConfig.AppConfig.FontsConfig.FontInfo.Name(
                value = "CinzelDecorative-Bold",
                url = "https://fonts.pawwalls.com/1195295_cb0a3b55_5b882c667007133983d5.ttf",
                hash = "a388d4f6e855b334da95b975bb30bf4d",
                family = "Cinzel Decorative",
                weight = 700,
                style = FontStyle.NORMAL,
            ),
        )
    }

    @Test
    fun `getUiConfig returns an all-defaults UiConfig when the merged read returns null`() = runTest {
        // mergeItemsBlobData is all-or-nothing: any unresolvable part nulls the whole merge, so the provider
        // falls back to a fully-default UiConfig rather than a partially-populated one.
        coEvery {
            manager.mergeItemsBlobData(RemoteConfigTopic.UiConfig, any(), any<(JsonObject) -> UiConfig?>())
        } returns null

        val uiConfig = provider.getUiConfig()

        assertThat(uiConfig).isEqualTo(UiConfig())
    }

    @Test
    fun `getUiConfig returns an all-defaults UiConfig when the merged object doesn't decode`() = runTest {
        // A localizations part that isn't the expected locale->map shape makes the whole merged object
        // undecodable; the reified mergeItemsBlobData swallows that to null instead of throwing out of the
        // provider, so the caller gets a default UiConfig.
        stubMergedRead(
            buildJsonObject {
                put("app", buildJsonObject {})
                putJsonArray("localizations") { add(JsonPrimitive("not an object")) }
                put("variable_config", buildJsonObject {})
                put("custom_variables", buildJsonObject {})
            },
        )

        val uiConfig = provider.getUiConfig()

        assertThat(uiConfig).isEqualTo(UiConfig())
    }

    // The provider calls the reified mergeItemsBlobData overload, which compiles down to the transform
    // overload. Stubbing that overload to run the provided transform against [merged] exercises the real
    // UiConfig decode — including each field's real serializer — exactly as production does.
    private fun stubMergedRead(merged: JsonObject): CapturingSlot<Collection<String>> {
        val keys = slot<Collection<String>>()
        coEvery {
            manager.mergeItemsBlobData(RemoteConfigTopic.UiConfig, capture(keys), any<(JsonObject) -> UiConfig?>())
        } answers {
            thirdArg<(JsonObject) -> UiConfig?>().invoke(merged)
        }
        return keys
    }
}
