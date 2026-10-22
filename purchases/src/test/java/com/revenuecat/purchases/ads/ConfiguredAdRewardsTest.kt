package com.revenuecat.purchases.ads

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ConfiguredAdRewardsTest {
    @Test
    fun `resolved empty configuration has no primary or additional rewards`() {
        val rewards = ConfiguredAdRewards(emptyList())
        assertThat(rewards.allRewards).isEmpty()
        assertThat(rewards.reward).isNull()
        assertThat(rewards.moreRewards).isEmpty()
    }

    @Test
    fun `rewards preserve primary and additional order and value equality`() {
        val primary = AdRewardConfiguration.VirtualCurrency("coins", 10)
        val extra = AdRewardConfiguration.Entitlement("premium", 60.seconds)
        val rewards = ConfiguredAdRewards(listOf(primary, extra, AdRewardConfiguration.UnsupportedReward))
        assertThat(rewards.allRewards).containsExactly(primary, extra, AdRewardConfiguration.UnsupportedReward)
        assertThat(rewards.reward).isEqualTo(AdRewardConfiguration.VirtualCurrency("coins", 10))
        assertThat(rewards.moreRewards).containsExactly(extra, AdRewardConfiguration.UnsupportedReward)
        assertThat(rewards).isEqualTo(
            ConfiguredAdRewards(
                listOf(
                    AdRewardConfiguration.VirtualCurrency("coins", 10),
                    AdRewardConfiguration.Entitlement("premium", 60.seconds),
                    AdRewardConfiguration.UnsupportedReward,
                ),
            ),
        )
        assertThat(extra.duration).isEqualTo(60.seconds)
        assertThat(extra.durationInSeconds).isEqualTo(60L)
    }

    @Test
    fun `single reward is primary with no additional rewards`() {
        val rewards = ConfiguredAdRewards(listOf(AdRewardConfiguration.UnsupportedReward))
        assertThat(rewards.reward).isEqualTo(AdRewardConfiguration.UnsupportedReward)
        assertThat(rewards.moreRewards).isEmpty()
        assertThat(rewards.allRewards).containsExactly(AdRewardConfiguration.UnsupportedReward)
    }

    @Test
    fun `models reject invalid currency and entitlement values`() {
        assertThatIllegalArgumentException().isThrownBy { AdRewardConfiguration.VirtualCurrency("", 10) }
        listOf(0, -1).forEach { amount ->
            assertThatIllegalArgumentException().isThrownBy { AdRewardConfiguration.VirtualCurrency("coins", amount) }
        }
        assertThatIllegalArgumentException().isThrownBy { AdRewardConfiguration.Entitlement("", 1.seconds) }
        listOf(0.seconds, (-1).seconds, Duration.INFINITE, 500.milliseconds, 1500.milliseconds).forEach { duration ->
            assertThatIllegalArgumentException().isThrownBy { AdRewardConfiguration.Entitlement("pro", duration) }
        }
    }
}
