package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.models.GoogleReplacementMode
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
public class ProductChangeConfigTest(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val args: Args,
) {

    class Args(
        @Language("json")
        public val json: String,
        public val expected: ProductChangeConfig,
    )

    public companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        public fun parameters(): Collection<*> = listOf(
            arrayOf(
                "default values when empty",
                Args(
                    json = """{}""",
                    expected = ProductChangeConfig(
                        upgradeReplacementMode = GoogleReplacementMode.CHARGE_PRORATED_PRICE,
                        downgradeReplacementMode = GoogleReplacementMode.DEFERRED,
                    ),
                ),
            ),
            arrayOf(
                "charge_prorated_price and deferred",
                Args(
                    json = """
                        {
                          "upgrade_replacement_mode": "charge_prorated_price",
                          "downgrade_replacement_mode": "deferred"
                        }
                    """.trimIndent(),
                    expected = ProductChangeConfig(
                        upgradeReplacementMode = GoogleReplacementMode.CHARGE_PRORATED_PRICE,
                        downgradeReplacementMode = GoogleReplacementMode.DEFERRED,
                    ),
                ),
            ),
            arrayOf(
                "charge_full_price and with_time_proration",
                Args(
                    json = """
                        {
                          "upgrade_replacement_mode": "charge_full_price",
                          "downgrade_replacement_mode": "with_time_proration"
                        }
                    """.trimIndent(),
                    expected = ProductChangeConfig(
                        upgradeReplacementMode = GoogleReplacementMode.CHARGE_FULL_PRICE,
                        downgradeReplacementMode = GoogleReplacementMode.WITH_TIME_PRORATION,
                    ),
                ),
            ),
            arrayOf(
                "without_proration and without_proration",
                Args(
                    json = """
                        {
                          "upgrade_replacement_mode": "without_proration",
                          "downgrade_replacement_mode": "without_proration"
                        }
                    """.trimIndent(),
                    expected = ProductChangeConfig(
                        upgradeReplacementMode = GoogleReplacementMode.WITHOUT_PRORATION,
                        downgradeReplacementMode = GoogleReplacementMode.WITHOUT_PRORATION,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `Should properly deserialize ProductChangeConfig`() {
        val actual = JsonTools.json.decodeFromString<ProductChangeConfig>(args.json)

        assert(actual == args.expected)
    }
}
