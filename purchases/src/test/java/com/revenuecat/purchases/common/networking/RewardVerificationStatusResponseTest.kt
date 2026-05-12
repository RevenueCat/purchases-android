package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.RewardVerificationStatus
import com.revenuecat.purchases.common.JsonProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
@OptIn(InternalRevenueCatAPI::class)
class RewardVerificationStatusResponseTest {

    @Test
    fun `maps pending status`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationStatusResponse>(
            """{"status":"pending"}""",
        )

        assertThat(response.toRewardVerificationStatus()).isEqualTo(RewardVerificationStatus.PENDING)
    }

    @Test
    fun `maps verified status`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationStatusResponse>(
            """{"status":"verified"}""",
        )

        assertThat(response.toRewardVerificationStatus()).isEqualTo(RewardVerificationStatus.VERIFIED)
    }

    @Test
    fun `maps failed status`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationStatusResponse>(
            """{"status":"failed"}""",
        )

        assertThat(response.toRewardVerificationStatus()).isEqualTo(RewardVerificationStatus.FAILED)
    }

    @Test
    fun `maps unknown status`() {
        val response = JsonProvider.defaultJson.decodeFromString<RewardVerificationStatusResponse>(
            """{"status":"something_new"}""",
        )

        assertThat(response.toRewardVerificationStatus()).isEqualTo(RewardVerificationStatus.UNKNOWN)
    }
}
