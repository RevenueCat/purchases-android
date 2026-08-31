package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.JsonTools
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class TabControlComponentTests {

    @Test
    fun `Should deserialize a tab_control_button`() {
        // language=json
        val serialized = """
        {
          "type": "tab_control_button",
          "tab_index": 0,
          "tab_id": "zero",
          "stack": {
            "type": "stack",
            "components": []
          }
        }
        """.trimIndent()

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(serialized)

        assertThat(actual).isInstanceOf(TabControlButtonComponent::class.java)
    }

    @Test
    fun `Should deserialize a tab_control_toggle`() {
        // language=json
        val serialized = """
        {
          "type": "tab_control_toggle",
          "default_value": true,
          "thumb_color_on": { "light": { "type": "alias", "value": "primary" } },
          "thumb_color_off": { "light": { "type": "alias", "value": "primary" } },
          "track_color_on": { "light": { "type": "alias", "value": "primary" } },
          "track_color_off": { "light": { "type": "alias", "value": "primary" } }
        }
        """.trimIndent()

        val actual = JsonTools.json.decodeFromString<PaywallComponent>(serialized)

        assertThat(actual).isInstanceOf(TabControlToggleComponent::class.java)
    }
}
