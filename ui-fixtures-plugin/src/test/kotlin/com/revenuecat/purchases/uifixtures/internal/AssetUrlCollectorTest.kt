package com.revenuecat.purchases.uifixtures.internal

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class AssetUrlCollectorTest {

    @Test
    fun `collects icon url when base_url and formats are on the same object`() {
        val json = JSONObject(
            """{"c":{"base_url":"https://icons.pawwalls.com/icons","formats":{"webp":"check.webp"},"icon_name":"check"}}""",
        )

        assertContains(AssetUrlCollector.collect(json), "https://icons.pawwalls.com/icons/check.webp")
    }

    @Test
    fun `collects icon url for an override that inherits base_url from an ancestor`() {
        // The override carries only `formats`/`icon_name`; its base_url must be inherited from the
        // enclosing icon component. This is the case that previously slipped through.
        val json = JSONObject(
            """
            {
              "component": {
                "base_url": "https://icons.pawwalls.com/icons",
                "formats": { "webp": "parent.webp" },
                "icon_name": "parent",
                "override": { "formats": { "webp": "filled-circle-check.webp" }, "icon_name": "filled-circle-check" }
              }
            }
            """.trimIndent(),
        )

        assertContains(
            AssetUrlCollector.collect(json),
            "https://icons.pawwalls.com/icons/filled-circle-check.webp",
        )
    }

    @Test
    fun `collects plain image url strings`() {
        val json = JSONObject("""{"bg":{"webp":"https://assets.pawwalls.com/bg.webp"}}""")

        assertContains(AssetUrlCollector.collect(json), "https://assets.pawwalls.com/bg.webp")
    }

    @Test
    fun `ignores non-image strings`() {
        val json = JSONObject("""{"a":"hello","b":"https://example.com/page"}""")

        assertTrue(AssetUrlCollector.collect(json).isEmpty())
    }
}
