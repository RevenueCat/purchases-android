package com.revenuecat.purchases.common.networking

import com.revenuecat.purchases.AdMobRewardVerificationStatus
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.JsonProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
@OptIn(InternalRevenueCatAPI::class)
class AdMobRewardVerificationStatusResponseTest {

    @Test
    fun `maps pending status`() {
        val response = JsonProvider.defaultJson.decodeFromString<AdMobRewardVerificationStatusResponse>(
            """{"status":"pending"}""",
        )

        assertThat(response.toAdMobRewardVerificationStatus()).isEqualTo(AdMobRewardVerificationStatus.PENDING)
    }

    @Test
    fun `maps verified status`() {
        val response = JsonProvider.defaultJson.decodeFromString<AdMobRewardVerificationStatusResponse>(
            """{"status":"verified"}""",
        )

        assertThat(response.toAdMobRewardVerificationStatus()).isEqualTo(AdMobRewardVerificationStatus.VERIFIED)
    }

    @Test
    fun `maps failed status`() {
        val response = JsonProvider.defaultJson.decodeFromString<AdMobRewardVerificationStatusResponse>(
            """{"status":"failed"}""",
        )

        assertThat(response.toAdMobRewardVerificationStatus()).isEqualTo(AdMobRewardVerificationStatus.FAILED)
    }

    @Test
    fun `maps unknown status`() {
        val response = JsonProvider.defaultJson.decodeFromString<AdMobRewardVerificationStatusResponse>(
            """{"status":"something_new"}""",
        )

        assertThat(response.toAdMobRewardVerificationStatus()).isEqualTo(AdMobRewardVerificationStatus.UNKNOWN)
    }
}
