package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.RewardVerificationPollStatus
import com.revenuecat.purchases.VerifiedReward
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.utils.Iso8601Utils
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

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(RewardVerificationPollStatus.PENDING)
    }

    @Test
    fun `maps verified status`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """{"status":"verified"}""",
        )

        assertThat(response.toRewardVerificationPollStatus())
            .isEqualTo(RewardVerificationPollStatus.Verified(VerifiedReward.NoReward))
    }

    @Test
    fun `maps failed status without reason or message`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """{"status":"failed"}""",
        )

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(RewardVerificationPollStatus.Failed())
    }

    @Test
    fun `maps failed status with failure reason and message`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """
            {
              "status":"failed",
              "failure_reason":"ssv_not_enabled",
              "message":"AdMob server-side reward verification is not enabled for this app."
            }
            """.trimIndent(),
        )

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(
            RewardVerificationPollStatus.Failed(
                failureReason = "ssv_not_enabled",
                message = "AdMob server-side reward verification is not enabled for this app.",
            ),
        )
    }

    @Test
    fun `maps unknown status`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """{"status":"something_new"}""",
        )

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(RewardVerificationPollStatus.UNKNOWN)
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

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(
            RewardVerificationPollStatus.Verified(
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

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(
            RewardVerificationPollStatus.Verified(VerifiedReward.UnsupportedReward),
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

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(
            RewardVerificationPollStatus.Verified(VerifiedReward.UnsupportedReward),
        )
    }

    @Test
    fun `maps verified entitlement reward`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """
            {
              "status":"verified",
              "reward": {
                "type":"entitlement",
                "identifier":"pro",
                "expires_at":"2026-06-16T12:00:00Z"
              }
            }
            """.trimIndent(),
        )

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(
            RewardVerificationPollStatus.Verified(
                VerifiedReward.Entitlement(
                    identifier = "pro",
                    expiresAt = Iso8601Utils.parse("2026-06-16T12:00:00Z"),
                ),
            ),
        )
    }

    @Test
    fun `maps entitlement reward missing identifier as unsupported reward`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """
            {
              "status":"verified",
              "reward": {
                "type":"entitlement",
                "expires_at":"2026-06-16T12:00:00Z"
              }
            }
            """.trimIndent(),
        )

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(
            RewardVerificationPollStatus.Verified(VerifiedReward.UnsupportedReward),
        )
    }

    @Test
    fun `maps entitlement reward with malformed expires_at as unsupported reward`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """
            {
              "status":"verified",
              "reward": {
                "type":"entitlement",
                "identifier":"pro",
                "expires_at":"not-a-date"
              }
            }
            """.trimIndent(),
        )

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(
            RewardVerificationPollStatus.Verified(VerifiedReward.UnsupportedReward),
        )
    }

    @Test
    fun `maps verified reward with more rewards`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """
            {
              "status":"verified",
              "reward": {
                "type":"virtual_currency",
                "code":"coins",
                "amount":10
              },
              "more_rewards": [
                {
                  "type":"entitlement",
                  "identifier":"pro",
                  "expires_at":"2026-06-16T12:00:00Z"
                },
                {
                  "type":"other_reward"
                }
              ]
            }
            """.trimIndent(),
        )

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(
            RewardVerificationPollStatus.Verified(
                reward = VerifiedReward.VirtualCurrency(code = "coins", amount = 10),
                moreRewards = listOf(
                    VerifiedReward.Entitlement(
                        identifier = "pro",
                        expiresAt = Iso8601Utils.parse("2026-06-16T12:00:00Z"),
                    ),
                    VerifiedReward.UnsupportedReward,
                ),
            ),
        )
    }

    @Test
    fun `maps verified null reward as no reward`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """{"status":"verified","reward":null}""",
        )

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(
            RewardVerificationPollStatus.Verified(VerifiedReward.NoReward),
        )
    }

    @Test
    fun `maps verified non object reward as unsupported reward`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationResponse>(
            """{"status":"verified","reward":"bad"}""",
        )

        assertThat(response.toRewardVerificationPollStatus()).isEqualTo(
            RewardVerificationPollStatus.Verified(VerifiedReward.UnsupportedReward),
        )
    }
}
