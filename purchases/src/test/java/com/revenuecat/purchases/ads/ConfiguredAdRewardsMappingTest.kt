package com.revenuecat.purchases.ads

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.common.currentLogHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class ConfiguredAdRewardsMappingTest {
    private val originalLogHandler = currentLogHandler

    @Before
    fun setUp() {
        currentLogHandler = NoOpLogHandler
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    private val currency = """{"type":"virtual_currency","code":"coins","amount":10}"""
    private val entitlement = """{"type":"entitlement","identifier":"pro","duration_in_seconds":1800}"""

    @Test
    fun `maps primary and ordered additional rewards ignoring unknown fields`() {
        val result = requireNotNull(decode("""{"reward":$currency,"more_rewards":[$entitlement,$currency],"extra":true}"""))
        assertThat(result.reward).isEqualTo(AdRewardConfiguration.VirtualCurrency("coins", 10))
        assertThat(result.moreRewards).containsExactly(
            AdRewardConfiguration.Entitlement("pro", 1800.seconds),
            AdRewardConfiguration.VirtualCurrency("coins", 10),
        )
    }

    @Test
    fun `lookup is exact and case sensitive and bad neighbor does not hide valid entry`() {
        val data = """{"AdUnit":{"reward":$currency},"bad":null,"bad2":{"more_rewards":false}}"""
        assertThat(ConfiguredAdRewardsProvider.decode(data, "AdUnit")?.reward)
            .isEqualTo(AdRewardConfiguration.VirtualCurrency("coins", 10))
        listOf("adunit", " AdUnit", "AdUnit ", "missing").forEach {
            assertThat(ConfiguredAdRewardsProvider.decode(data, it))
                .isEqualTo(ConfiguredAdRewards(emptyList()))
        }
        assertMalformed { ConfiguredAdRewardsProvider.decode(data, "bad") }
        assertMalformed { ConfiguredAdRewardsProvider.decode(data, "bad2") }
    }

    @Test
    fun `missing null and no_reward primary with no actual additional rewards mean no reward`() {
        listOf("{}", """{"reward":null}""", """{"reward":{"type":"no_reward"}}""").forEach { entry ->
            assertThat(decode(entry)).isEqualTo(ConfiguredAdRewards(emptyList()))
        }
        listOf("null", "[]", "[null,{\"type\":\"no_reward\"}]").forEach { more ->
            assertThat(decode("""{"more_rewards":$more}""")).isEqualTo(ConfiguredAdRewards(emptyList()))
        }
    }

    @Test
    fun `actual additional rewards without a primary are malformed`() {
        listOf("", "\"reward\":null,", "\"reward\":{\"type\":\"no_reward\"},").forEach { primary ->
            listOf(currency, entitlement, "{\"type\":\"future\"}").forEach { extra ->
                assertMalformed { decode("""{$primary"more_rewards":[$extra]}""") }
            }
        }
    }

    @Test
    fun `unknown and malformed reward shapes are explicit unsupported rewards`() {
        listOf(
            """{"type":"future","payload":{"value":1}}""", "{}", "[]", "1", "\"reward\"",
            """{"type":"virtual_currency","code":"coins","amount":0}""",
            """{"type":"virtual_currency","code":"","amount":1}""",
            """{"type":"virtual_currency","code":"coins","amount":-1}""",
            """{"type":"virtual_currency","code":"coins","amount":1.5}""",
            """{"type":"virtual_currency","code":"coins","amount":"10"}""",
            """{"type":"virtual_currency","code":"coins","amount":2147483648}""",
            """{"type":"entitlement","identifier":"pro","duration":1800}""",
            """{"type":"entitlement","identifier":"pro","duration_in_seconds":0}""",
            """{"type":"entitlement","identifier":"pro","duration_in_seconds":-1}""",
            """{"type":"entitlement","identifier":"","duration_in_seconds":1}""",
            """{"type":"entitlement","identifier":"pro","duration_in_seconds":"1"}""",
            """{"type":"entitlement","identifier":"pro","duration_in_seconds":1.5}""",
            """{"type":"entitlement","identifier":"pro","duration_in_seconds":9223372036854775807}""",
        ).forEach { reward ->
            assertThat(decode("""{"reward":$reward}""")?.reward)
                .describedAs(reward).isEqualTo(AdRewardConfiguration.UnsupportedReward)
        }
    }

    @Test
    fun `additional placeholders are omitted and unsupported rewards retain their position`() {
        val result = requireNotNull(decode("""{"reward":$currency,"more_rewards":[null,{"type":"no_reward"},{"type":"future"},$entitlement]}"""))
        assertThat(result.moreRewards).containsExactly(
            AdRewardConfiguration.UnsupportedReward,
            AdRewardConfiguration.Entitlement("pro", 1800.seconds),
        )
    }

    @Test
    fun `malformed blob entry or additional collection is unavailable`() {
        listOf("garbage", "null", "[]", "1", "\"config\"").forEach {
            assertMalformed { ConfiguredAdRewardsProvider.decode(it, "unit") }
        }
        listOf("null", "1", "[]", """{"reward":$currency,"more_rewards":{}}""").forEach {
            assertMalformed { decode(it) }
        }
        assertThat(ConfiguredAdRewardsProvider.decode("{}", "unit"))
            .isEqualTo(ConfiguredAdRewards(emptyList()))
    }

    private fun assertMalformed(block: () -> ConfiguredAdRewards?) {
        assertThat(block()).isNull()
    }

    private fun decode(entry: String): ConfiguredAdRewards? =
        ConfiguredAdRewardsProvider.decode("""{"unit":$entry}""", "unit")
}
