package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.RewardVerificationResult
import com.revenuecat.purchases.VerifiedReward
import com.revenuecat.purchases.common.JsonProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
@OptIn(InternalRevenueCatAPI::class)
class RewardVerificationResponseTest {

    @Test
    fun `maps pending status`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """{"status":"pending"}""",
        )

        assertThat(response.toRewardVerificationResult()).isEqualTo(RewardVerificationResult.PENDING)
    }

    @Test
    fun `maps verified status`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """{"status":"verified"}""",
        )

        assertThat(response.toRewardVerificationResult())
            .isEqualTo(RewardVerificationResult.Verified(VerifiedReward.NoReward))
    }

    @Test
    fun `maps failed status`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """{"status":"failed"}""",
        )

        assertThat(response.toRewardVerificationResult()).isEqualTo(RewardVerificationResult.FAILED)
    }

    @Test
    fun `maps unknown status`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """{"status":"something_new"}""",
        )

        assertThat(response.toRewardVerificationResult()).isEqualTo(RewardVerificationResult.UNKNOWN)
    }

    @Test
    fun `maps verified virtual currency reward`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """
            {
              "status":"verified",
              "reward": {
                "type":"virtual_currency",
                "code":"coins",
                "amount":10
              }
            }
            """.trimIndent(),
        )

        assertThat(response.toRewardVerificationResult()).isEqualTo(
            RewardVerificationResult.Verified(
                VerifiedReward.VirtualCurrency(code = "coins", amount = 10),
            ),
        )
    }

    @Test
    fun `maps verified unknown reward type as unsupported reward`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """
            {
              "status":"verified",
              "reward": {
                "type":"other_reward",
                "code":"coins",
                "amount":10
              }
            }
            """.trimIndent(),
        )

        assertThat(response.toRewardVerificationResult()).isEqualTo(
            RewardVerificationResult.Verified(VerifiedReward.UnsupportedReward),
        )
    }

    @Test
    fun `maps malformed virtual currency reward as unsupported reward`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """
            {
              "status":"verified",
              "reward": {
                "type":"virtual_currency",
                "code":"",
                "amount":0
              }
            }
            """.trimIndent(),
        )

        assertThat(response.toRewardVerificationResult()).isEqualTo(
            RewardVerificationResult.Verified(VerifiedReward.UnsupportedReward),
        )
    }

    @Test
    fun `maps verified null reward as no reward`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """{"status":"verified","reward":null}""",
        )

        assertThat(response.toRewardVerificationResult()).isEqualTo(
            RewardVerificationResult.Verified(VerifiedReward.NoReward),
        )
    }

    @Test
    fun `maps verified non object reward as unsupported reward`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """{"status":"verified","reward":"bad"}""",
        )

        assertThat(response.toRewardVerificationResult()).isEqualTo(
            RewardVerificationResult.Verified(VerifiedReward.UnsupportedReward),
        )
    }
}
