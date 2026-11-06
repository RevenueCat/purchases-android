@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.common.localrules

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensions
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensionsReceiptStore
import com.revenuecat.purchases.common.subscriberdimensions.SubscriberDimensionsResolution
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class AudienceStatusCompatibilityTest {
    @Test
    fun `subscription status decisions match Khepri`() = runTest {
        val profiles = fixture("profiles.json").getJSONObject("profiles")
        val cases = fixture("status.json").getJSONArray("cases")
        assertThat(cases.length()).isGreaterThan(0)
        val assertions = SoftAssertions()
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val profile = profiles.getJSONObject(case.getString("profile"))
            val payload = profile.getJSONObject("subscriber_dimensions")
            val dimensions = requireNotNull(
                SubscriberDimensions.parse(Json.parseToJsonElement(payload.toString()).jsonObject),
            )
            val user = profile.getString("app_user_id")
            val cache = mockk<DeviceCache> {
                every { getCachedSubscriberDimensionsJson(user) } returns null
            }
            val provider = SubscriberDimensionsProvider(
                configDimensions = { SubscriberDimensionsResolution.Found(dimensions) },
                receiptStore = SubscriberDimensionsReceiptStore(cache),
                currentAppUserId = { user },
            )
            val rule = object : LocalRule {
                override val predicate = case.getString("predicate")
            }
            val result = LocalRulesEvaluator(listOf(provider), currentAppUserId = { user })
                .match(listOf(rule))
            val expected = case.getBoolean("expected")
            val actual = result.getOrThrow() != null
            println("${case.getString("id")}: expected=$expected actual=$actual")
            assertions.assertThat(actual).describedAs(case.getString("id")).isEqualTo(expected)
        }
        assertions.assertAll()
    }

    private fun fixture(name: String): JSONObject = JSONObject(
        requireNotNull(javaClass.getResourceAsStream("/audience_status_compatibility/$name"))
            .bufferedReader().use { it.readText() },
    )
}
