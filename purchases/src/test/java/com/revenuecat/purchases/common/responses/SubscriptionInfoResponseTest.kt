package com.revenuecat.purchases.common.responses

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Locale

class SubscriptionInfoResponseTest {

    @Test
    fun `toPrice converts the amount to micros without losing a micro`() {
        val expectedMicrosByAmount = mapOf(
            0.99 to 990_000L,
            2.05 to 2_050_000L,
            8.2 to 8_200_000L,
            64.99 to 64_990_000L,
            1149.99 to 1_149_990_000L,
        )

        expectedMicrosByAmount.forEach { (amount, expectedMicros) ->
            val price = SubscriptionInfoResponse.PriceResponse(amount = amount, currencyCode = "USD")
                .toPrice(Locale.US)

            assertThat(price.amountMicros).describedAs("micros for %s", amount).isEqualTo(expectedMicros)
        }
    }
}
