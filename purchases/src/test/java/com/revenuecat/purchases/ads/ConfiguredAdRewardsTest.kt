package com.revenuecat.purchases.ads

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class ConfiguredAdRewardsTest {
    @Test
    fun `resolved empty configuration has no primary or additional rewards`() {
        val rewards = ConfiguredAdRewards(emptyList())
        assertThat(rewards.reward).isNull()
        assertThat(rewards.moreRewards).isEmpty()
    }

    @Test
    fun `rewards preserve primary and additional order and value equality`() {
        val primary = AdRewardConfiguration.VirtualCurrency("coins", 10)
        val extra = AdRewardConfiguration.Entitlement("premium", 60.seconds)
        val rewards = ConfiguredAdRewards(listOf(primary, extra, AdRewardConfiguration.UnsupportedReward))
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
    }

}
