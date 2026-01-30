package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.models.GoogleReplacementMode
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ProductChangeConfigTest(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val args: Args,
) {

    class Args(
        @Language("json")
        val json: String,
        val expected: ProductChangeConfig,
    )

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "default values when empty",
                Args(
                    json = """{}""",
                    expected = ProductChangeConfig(
                        upgradeReplacementMode = SerializableReplacementMode.CHARGE_PRORATED_PRICE,
                        downgradeReplacementMode = SerializableReplacementMode.DEFERRED,
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
                        upgradeReplacementMode = SerializableReplacementMode.CHARGE_PRORATED_PRICE,
                        downgradeReplacementMode = SerializableReplacementMode.DEFERRED,
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
                        upgradeReplacementMode = SerializableReplacementMode.CHARGE_FULL_PRICE,
                        downgradeReplacementMode = SerializableReplacementMode.WITH_TIME_PRORATION,
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
                        upgradeReplacementMode = SerializableReplacementMode.WITHOUT_PRORATION,
                        downgradeReplacementMode = SerializableReplacementMode.WITHOUT_PRORATION,
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

@RunWith(Parameterized::class)
class SerializableReplacementModeTest(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val args: Args,
) {

    class Args(
        val mode: SerializableReplacementMode,
        val expectedGoogleMode: GoogleReplacementMode,
    )

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "WITHOUT_PRORATION",
                Args(
                    mode = SerializableReplacementMode.WITHOUT_PRORATION,
                    expectedGoogleMode = GoogleReplacementMode.WITHOUT_PRORATION,
                ),
            ),
            arrayOf(
                "WITH_TIME_PRORATION",
                Args(
                    mode = SerializableReplacementMode.WITH_TIME_PRORATION,
                    expectedGoogleMode = GoogleReplacementMode.WITH_TIME_PRORATION,
                ),
            ),
            arrayOf(
                "CHARGE_FULL_PRICE",
                Args(
                    mode = SerializableReplacementMode.CHARGE_FULL_PRICE,
                    expectedGoogleMode = GoogleReplacementMode.CHARGE_FULL_PRICE,
                ),
            ),
            arrayOf(
                "CHARGE_PRORATED_PRICE",
                Args(
                    mode = SerializableReplacementMode.CHARGE_PRORATED_PRICE,
                    expectedGoogleMode = GoogleReplacementMode.CHARGE_PRORATED_PRICE,
                ),
            ),
            arrayOf(
                "DEFERRED",
                Args(
                    mode = SerializableReplacementMode.DEFERRED,
                    expectedGoogleMode = GoogleReplacementMode.DEFERRED,
                ),
            ),
        )
    }

    @Test
    fun `Should properly convert to GoogleReplacementMode`() {
        val actual = args.mode.toGoogleReplacementMode()

        assert(actual == args.expectedGoogleMode)
    }
}
