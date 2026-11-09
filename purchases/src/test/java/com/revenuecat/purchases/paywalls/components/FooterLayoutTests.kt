package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.JsonTools
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class FooterLayoutTests {
    @Test
    fun `decodes every layout and defaults unknown layouts to normal`() {
        val layouts = mapOf(
            "normal" to StickyFooterComponent.Layout.NORMAL,
            "hidden" to StickyFooterComponent.Layout.HIDDEN,
            "split_leading" to StickyFooterComponent.Layout.SPLIT_LEADING,
            "split_trailing" to StickyFooterComponent.Layout.SPLIT_TRAILING,
            "future" to StickyFooterComponent.Layout.NORMAL,
        )
        layouts.forEach { (value, expected) ->
            val footer = JsonTools.json.decodeFromString<StickyFooterComponent>(
                """{"layout":"$value","stack":{"type":"stack","components":[]}}""",
            )
            assertThat(footer.layout).isEqualTo(expected)
        }
    }

    @Test
    fun `decodes viewport overrides without changing the base`() {
        val footer = JsonTools.json.decodeFromString<StickyFooterComponent>(
            """{
                "stack":{"type":"stack","components":[]},
                "overrides":[{
                    "conditions":[{"type":"window_width_condition","operator":">=","value":700}],
                    "properties":{"layout":"split_trailing"}
                }]
            }""".trimIndent(),
        )
        assertThat(footer.layout).isNull()
        assertThat(footer.overrides.single().properties.layout).isEqualTo(StickyFooterComponent.Layout.SPLIT_TRAILING)
    }
}
