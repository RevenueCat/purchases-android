package com.revenuecat.purchases.ui.revenuecatui.components.webview

import kotlinx.serialization.json.JsonNull
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
        val snapshot = webViewContextSnapshot(locale = "en_US", darkMode = true)

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
    fun `device_meta carries host details`() {
        val before = System.currentTimeMillis()

        val deviceMeta = webViewContextSnapshot(locale = "en_US", darkMode = true)
            .getValue("device_meta").jsonObject

        assertThat(deviceMeta.getValue("is_preview").jsonPrimitive.boolean).isFalse()
        assertThat(deviceMeta.getValue("locale").jsonPrimitive.content).isEqualTo("en_US")
        assertThat(deviceMeta.getValue("dark_mode").jsonPrimitive.boolean).isTrue()
        assertThat(deviceMeta.getValue("updated_at").jsonPrimitive.long)
            .isBetween(before, System.currentTimeMillis())
    }
}
