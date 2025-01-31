@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter.extensions

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.utils.stubPricingPhase
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

@RunWith(Parameterized::class)
class SubscriptionOptionExtensionsTest(
    private val testName: String,
    private val testCase: TestCase,
) {
    data class TestCase(
        val pricingPhases: List<PricingPhase>,
        val localization: CustomerCenterConfigData.Localization,
        val locale: Locale,
        val expectedDescription: String,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val mockLocalization = mockk<CustomerCenterConfigData.Localization>()

            // Setup common test data
            val trialPhase = stubPricingPhase(
                billingPeriod = Period.create("P1M"),
                priceCurrencyCodeValue = "USD",
                price = 0.0,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 2,
            )
            val discountPhase = stubPricingPhase(
                billingPeriod = Period.create("P1M"),
                priceCurrencyCodeValue = "USD",
                price = 0.99,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 2,
            )
            val singlePaymentPhase = stubPricingPhase(
                billingPeriod = Period.create("P1M"),
                priceCurrencyCodeValue = "USD",
                price = 1.99,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
            )
            val fullPricePhase = stubPricingPhase(
                billingPeriod = Period.create("P1M"),
                priceCurrencyCodeValue = "USD",
                price = 3.99,
                recurrenceMode = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = null,
            )

            // Setup localization mock responses
            every {
                mockLocalization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.FREE_TRIAL_THEN_PRICE
                )
            } returns CustomerCenterConfigData.Localization.CommonLocalizedString.FREE_TRIAL_THEN_PRICE.defaultValue

            every {
                mockLocalization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.DISCOUNTED_RECURRING_THEN_PRICE
                )
            } returns CustomerCenterConfigData.Localization.CommonLocalizedString.DISCOUNTED_RECURRING_THEN_PRICE.defaultValue

            every {
                mockLocalization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.FREE_TRIAL_SINGLE_PAYMENT_THEN_PRICE
                )
            } returns CustomerCenterConfigData.Localization.CommonLocalizedString.FREE_TRIAL_SINGLE_PAYMENT_THEN_PRICE.defaultValue

            every {
                mockLocalization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.FREE_TRIAL_DISCOUNTED_THEN_PRICE
                )
            } returns CustomerCenterConfigData.Localization.CommonLocalizedString.FREE_TRIAL_DISCOUNTED_THEN_PRICE.defaultValue

            every {
                mockLocalization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.SINGLE_PAYMENT_THEN_PRICE
                )
            } returns CustomerCenterConfigData.Localization.CommonLocalizedString.SINGLE_PAYMENT_THEN_PRICE.defaultValue

            return listOf(
                arrayOf(
                    "Single phase - full price",
                    TestCase(
                        pricingPhases = listOf(fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "$3.99"
                    )
                ),
                arrayOf(
                    "Two phases - free trial then full price",
                    TestCase(
                        pricingPhases = listOf(trialPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "First 2 months free, then $3.99/mth"
                    )
                ),
                arrayOf(
                    "Two phases - discounted then full price",
                    TestCase(
                        pricingPhases = listOf(discountPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "$0.99 during 2 months, then $3.99/mth"
                    )
                ),
                arrayOf(
                    "Two phases - single payment then full price",
                    TestCase(
                        pricingPhases = listOf(singlePaymentPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "1 month for $1.99, then $3.99/mth"
                    )
                ),
                arrayOf(
                    "Three phases - free trial, single payment, then full price",
                    TestCase(
                        pricingPhases = listOf(trialPhase, singlePaymentPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 2 months for free, then 1 month for $1.99, and $3.99/mth thereafter"
                    )
                ),
                arrayOf(
                    "Three phases - free trial, discounted recurring, then full price",
                    TestCase(
                        pricingPhases = listOf(trialPhase, discountPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 2 months for free, then $0.99 during 2 months, and $3.99/mth thereafter"
                    )
                ),
                arrayOf(
                    "Three phases - first phase not free trial falls back to two phase",
                    TestCase(
                        pricingPhases = listOf(discountPhase, singlePaymentPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "$0.99 during 2 months, then $3.99/mth"
                    )
                ),
                arrayOf(
                    "Three phases - second phase unexpected payment mode falls back to base price",
                    TestCase(
                        pricingPhases = listOf(
                            trialPhase,
                            trialPhase,
                            fullPricePhase
                        ),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "$3.99/mth"
                    )
                ),
            )
        }
    }

    @Test
    fun `getLocalizedDescription returns correct description`() {
        val subscriptionOption = mockk<SubscriptionOption>()
        every { subscriptionOption.pricingPhases } returns testCase.pricingPhases

        val result = subscriptionOption.getLocalizedDescription(
            testCase.localization,
            testCase.locale
        )

        assertThat(result).isEqualTo(testCase.expectedDescription)
    }
}