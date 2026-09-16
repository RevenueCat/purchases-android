package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
internal class PaywallPreviewRendererTest {

    @Test
    fun `load decodes sample JSON into an offering`() {
        val fixture = PaywallPreviewRenderer.load(json = PaywallPreviewRenderer.SAMPLE_JSON)

        assertThat(fixture.offering.identifier).isEqualTo("json-preview")
        assertThat(fixture.offering.serverDescription).isEqualTo("JSON paywall preview")
        assertThat(fixture.offering.availablePackages).hasSize(3)
        assertThat(fixture.offering.availablePackages.map { it.identifier }).containsExactly(
            "\$rc_weekly",
            "\$rc_monthly",
            "\$rc_annual",
        )
        val data = fixture.paywallComponents.data.getOrThrow()
        assertThat(data.templateName).isEqualTo("components")
        assertThat(data.defaultLocaleIdentifier).isEqualTo(LocaleId("en_US"))
        assertThat(data.revision).isEqualTo(1)
        assertThat(
            data.componentsLocalizations[LocaleId("en_US")]?.get(LocalizationKey("title")),
        ).isEqualTo(LocalizationData.Text("JSON paywall preview"))
    }

    @Test
    fun `load uses custom offering identifier and packages`() {
        val fixture = PaywallPreviewRenderer.load(
            json = PaywallPreviewRenderer.SAMPLE_JSON,
            offeringIdentifier = "custom-offering",
            serverDescription = "Custom",
            packages = emptyList(),
        )

        assertThat(fixture.offering.identifier).isEqualTo("custom-offering")
        assertThat(fixture.offering.serverDescription).isEqualTo("Custom")
        assertThat(fixture.offering.availablePackages).isEmpty()
    }

    @Test
    fun `load ignores unknown dashboard keys`() {
        val jsonWithExtraKeys = """
            {
              "template_name": "components",
              "asset_base_url": "https://assets.pawwalls.com",
              "revision": 1,
              "generated_by": "dashboard",
              "published_revision": 9,
              "components_config": {
                "base": {
                  "background": {
                    "type": "color",
                    "value": { "light": { "type": "hex", "value": "#ffffffff" } }
                  },
                  "stack": {
                    "type": "stack",
                    "components": [],
                    "size": { "width": { "type": "fill" }, "height": { "type": "fit" } },
                    "dimension": {
                      "type": "vertical",
                      "alignment": "center",
                      "distribution": "center"
                    },
                    "padding": { "leading": 0, "trailing": 0, "top": 0, "bottom": 0 },
                    "margin": { "leading": 0, "trailing": 0, "top": 0, "bottom": 0 }
                  }
                }
              },
              "components_localizations": { "en_US": {} },
              "default_locale": "en_US"
            }
        """.trimIndent()

        val fixture = PaywallPreviewRenderer.load(json = jsonWithExtraKeys, packages = emptyList())

        assertThat(fixture.paywallComponents.data.getOrThrow().templateName).isEqualTo("components")
    }

    @Test
    fun `load throws on invalid JSON`() {
        assertThatThrownBy {
            PaywallPreviewRenderer.load(json = "{ not-json")
        }.isInstanceOf(Exception::class.java)
    }
}
