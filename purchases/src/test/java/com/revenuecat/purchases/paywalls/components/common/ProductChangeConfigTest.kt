package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.StoreReplacementMode
import com.revenuecat.purchases.utils.serializers.EmptyObjectToNullSerializer
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
                "default values when only upgrade mode provided",
                Args(
                    json = """{"upgrade_replacement_mode": "charge_prorated_price"}""",
                    expected = ProductChangeConfig(
                        upgradeReplacementMode = StoreReplacementMode.CHARGE_PRORATED_PRICE,
                        downgradeReplacementMode = StoreReplacementMode.DEFERRED,
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
                        upgradeReplacementMode = StoreReplacementMode.CHARGE_PRORATED_PRICE,
                        downgradeReplacementMode = StoreReplacementMode.DEFERRED,
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
                        upgradeReplacementMode = StoreReplacementMode.CHARGE_FULL_PRICE,
                        downgradeReplacementMode = StoreReplacementMode.WITH_TIME_PRORATION,
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
                        upgradeReplacementMode = StoreReplacementMode.WITHOUT_PRORATION,
                        downgradeReplacementMode = StoreReplacementMode.WITHOUT_PRORATION,
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

    @Test
    fun `Empty object deserializes to null via ProductChangeConfigSerializer`() {
        val json = """{"play_store_product_change_mode": {}}"""
        val wrapper = JsonTools.json.decodeFromString<Wrapper>(json)
        assert(wrapper.productChangeConfig == null)
    }

    @Test
    fun `Non-empty object deserializes via ProductChangeConfigSerializer`() {
        val json = """{"play_store_product_change_mode": {"upgrade_replacement_mode": "charge_full_price"}}"""
        val wrapper = JsonTools.json.decodeFromString<Wrapper>(json)
        assert(wrapper.productChangeConfig != null)
        assert(wrapper.productChangeConfig!!.upgradeReplacementMode == StoreReplacementMode.CHARGE_FULL_PRICE)
        assert(wrapper.productChangeConfig!!.downgradeReplacementMode == StoreReplacementMode.DEFERRED)
    }

    @Test
    fun `Missing field deserializes to null via ProductChangeConfigSerializer`() {
        val json = """{}"""
        val wrapper = JsonTools.json.decodeFromString<Wrapper>(json)
        assert(wrapper.productChangeConfig == null)
    }

    @Test
    fun `Malformed non-object play_store_product_change_mode deserializes to null`() {
        val json = """{"play_store_product_change_mode":"unexpected"}"""
        val wrapper = JsonTools.json.decodeFromString<Wrapper>(json)
        assert(wrapper.productChangeConfig == null)
    }

    private object TestSerializer : EmptyObjectToNullSerializer<ProductChangeConfig>(
        ProductChangeConfig.serializer(),
    )

    @kotlinx.serialization.Serializable
    private data class Wrapper(
        @kotlinx.serialization.Serializable(with = TestSerializer::class)
        @kotlinx.serialization.SerialName("play_store_product_change_mode")
        val productChangeConfig: ProductChangeConfig? = null,
    )
}
