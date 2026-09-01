package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class WebViewContextSnapshotTest {

    @Test
    fun `contains every section with its empty shape and no workflow key`() {
        val snapshot = webViewContextSnapshot(
            customVariables = emptyMap(),
            locale = "en-US",
            darkMode = true,
        )

        assertThat(snapshot.keys).containsExactly(
            "custom",
            "offering",
            "packages",
            "package",
            "selected_package",
            "inputs",
            "device_meta",
        )
        assertThat(snapshot.getValue("custom").jsonObject).isEmpty()
        assertThat(snapshot.getValue("offering")).isEqualTo(JsonNull)
        assertThat(snapshot.getValue("packages").jsonArray).isEmpty()
        assertThat(snapshot.getValue("package")).isEqualTo(JsonNull)
        assertThat(snapshot.getValue("selected_package")).isEqualTo(JsonNull)
        assertThat(snapshot.getValue("inputs").jsonObject).isEmpty()
    }

    @Test
    fun `custom carries every variable with its type intact`() {
        val custom = snapshotWith(
            "org" to CustomVariableValue.String("RevenueCat"),
            "is_premium" to CustomVariableValue.Boolean(true),
            "rating" to CustomVariableValue.Number(4.5),
        )

        assertThat(Json.encodeToString(JsonObject.serializer(), custom))
            .isEqualTo("""{"org":"RevenueCat","is_premium":true,"rating":4.5}""")
    }

    @Test
    fun `whole numbers keep no decimal part`() {
        // The contract's example is `"streak_days": 12`.
        val custom = snapshotWith("streak_days" to CustomVariableValue.Number(12))

        assertThat(Json.encodeToString(JsonObject.serializer(), custom)).isEqualTo("""{"streak_days":12}""")
    }

    @Test
    fun `non-finite numbers stay encodable`() {
        val custom = snapshotWith("broken" to CustomVariableValue.Number(Double.NaN))

        assertThat(Json.encodeToString(JsonObject.serializer(), custom)).isEqualTo("""{"broken":"NaN"}""")
    }

    private fun snapshotWith(vararg variables: Pair<String, CustomVariableValue>) =
        webViewContextSnapshot(
            customVariables = variables.toMap(),
            locale = "en-US",
            darkMode = false,
        ).getValue("custom").jsonObject

    @Test
    fun `device_meta carries host details`() {
        val before = System.currentTimeMillis()

        val deviceMeta = webViewContextSnapshot(
            customVariables = emptyMap(),
            locale = "en-US",
            darkMode = true,
        )
            .getValue("device_meta").jsonObject

        assertThat(deviceMeta.getValue("is_preview").jsonPrimitive.boolean).isFalse()
        assertThat(deviceMeta.getValue("locale").jsonPrimitive.content).isEqualTo("en-US")
        assertThat(deviceMeta.getValue("dark_mode").jsonPrimitive.boolean).isTrue()
        assertThat(deviceMeta.getValue("updated_at").jsonPrimitive.long)
            .isBetween(before, System.currentTimeMillis())
    }

    @Test
    fun `derives the custom variables from the paywall state`() {
        val state = FakePaywallState(
            components = emptyList(),
            customVariables = mapOf("org" to CustomVariableValue.String("RevenueCat")),
        )

        val custom = webViewContextSnapshot(state, darkMode = false).getValue("custom").jsonObject

        assertThat(custom.getValue("org").jsonPrimitive.content).isEqualTo("RevenueCat")
    }

    @Test
    fun `derives the locale from the paywall state as a BCP-47 tag`() {
        // The state carries the locale as an underscored `LocaleId`; the wire needs a tag.
        val deviceMeta = webViewContextSnapshot(FakePaywallState(components = emptyList()), darkMode = false)
            .getValue("device_meta").jsonObject

        assertThat(deviceMeta.getValue("locale").jsonPrimitive.content).isEqualTo("en-US")
    }
}
