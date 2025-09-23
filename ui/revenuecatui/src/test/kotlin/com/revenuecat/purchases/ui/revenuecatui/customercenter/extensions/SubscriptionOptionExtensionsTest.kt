package com.revenuecat.purchases.ui.revenuecatui.customercenter.extensions

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Localization.CommonLocalizedString
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.utils.previewPricingPhase
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

            val trialPhase = previewPricingPhase(
                billingPeriod = Period.create("P2M"),
                priceCurrencyCodeValue = "USD",
                price = 0.0,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
            )
            val trial3DPhase = previewPricingPhase(
                billingPeriod = Period.create("P3D"),
                priceCurrencyCodeValue = "USD",
                price = 0.0,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
            )
            val discountPhase = previewPricingPhase(
                billingPeriod = Period.create("P1M"),
                priceCurrencyCodeValue = "USD",
                price = 0.99,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 2,
            )
            val discount6MPhase = previewPricingPhase(
                billingPeriod = Period.create("P6M"),
                priceCurrencyCodeValue = "USD",
                price = 0.99,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 2,
            )
            val singlePaymentPhase = previewPricingPhase(
                billingPeriod = Period.create("P1M"),
                priceCurrencyCodeValue = "USD",
                price = 1.99,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
            )
            val single6MPaymentPhase = previewPricingPhase(
                billingPeriod = Period.create("P6M"),
                priceCurrencyCodeValue = "USD",
                price = 1.99,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
            )
            val single6YPaymentPhase = previewPricingPhase(
                billingPeriod = Period.create("P6Y"),
                priceCurrencyCodeValue = "USD",
                price = 1.99,
                recurrenceMode = ProductDetails.RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
            )
            val fullPricePhase = previewPricingPhase(
                billingPeriod = Period.create("P1M"),
                priceCurrencyCodeValue = "USD",
                price = 3.99,
                recurrenceMode = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = null,
            )
            val full6MPricePhase = previewPricingPhase(
                billingPeriod = Period.create("P6M"),
                priceCurrencyCodeValue = "USD",
                price = 3.99,
                recurrenceMode = ProductDetails.RecurrenceMode.INFINITE_RECURRING,
                billingCycleCount = null,
            )

            // Setup localization mock responses
            every {
                mockLocalization.commonLocalizedString(CommonLocalizedString.FREE_TRIAL_THEN_PRICE)
            } returns CommonLocalizedString.FREE_TRIAL_THEN_PRICE.defaultValue

            every {
                mockLocalization.commonLocalizedString(CommonLocalizedString.DISCOUNTED_RECURRING_THEN_PRICE)
            } returns CommonLocalizedString.DISCOUNTED_RECURRING_THEN_PRICE.defaultValue

            every {
                mockLocalization.commonLocalizedString(CommonLocalizedString.FREE_TRIAL_SINGLE_PAYMENT_THEN_PRICE)
            } returns CommonLocalizedString.FREE_TRIAL_SINGLE_PAYMENT_THEN_PRICE.defaultValue

            every {
                mockLocalization.commonLocalizedString(CommonLocalizedString.FREE_TRIAL_DISCOUNTED_THEN_PRICE)
            } returns CommonLocalizedString.FREE_TRIAL_DISCOUNTED_THEN_PRICE.defaultValue

            every {
                mockLocalization.commonLocalizedString(CommonLocalizedString.SINGLE_PAYMENT_THEN_PRICE)
            } returns CommonLocalizedString.SINGLE_PAYMENT_THEN_PRICE.defaultValue

            every {
                mockLocalization.commonLocalizedString(
                    CommonLocalizedString.DISCOUNTED_RECURRING_PRICE_PER_PERIOD_THEN_PRICE,
                )
            } returns CommonLocalizedString.DISCOUNTED_RECURRING_PRICE_PER_PERIOD_THEN_PRICE.defaultValue

            every {
                mockLocalization.commonLocalizedString(
                    CommonLocalizedString.FREE_TRIAL_DISCOUNTED_PRICE_PER_PERIOD_THEN_PRICE,
                )
            } returns CommonLocalizedString.FREE_TRIAL_DISCOUNTED_PRICE_PER_PERIOD_THEN_PRICE.defaultValue

            return listOf(
                arrayOf(
                    "Two phases - free trial then full price",
                    TestCase(
                        pricingPhases = listOf(trialPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "First 2 months free, then $3.99/mth",
                    ),
                ),
                arrayOf(
                    "Two phases - free 3 days trial then full price",
                    TestCase(
                        pricingPhases = listOf(trial3DPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "First 3 days free, then $3.99/mth",
                    ),
                ),
                arrayOf(
                    "Two phases - discounted then full price",
                    TestCase(
                        pricingPhases = listOf(discountPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "$0.99/mth for 2 periods, then $3.99/mth",
                    ),
                ),
                arrayOf(
                    "Two phases - discounted then full 6 month price",
                    TestCase(
                        pricingPhases = listOf(discount6MPhase, full6MPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "$0.99/6 mths for 2 periods, then $3.99/6 mths",
                    ),
                ),
                arrayOf(
                    "Two phases - single payment then full price",
                    TestCase(
                        pricingPhases = listOf(singlePaymentPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "1 month for $1.99, then $3.99/mth",
                    ),
                ),
                arrayOf(
                    "Two phases - single 6 months payment then full price",
                    TestCase(
                        pricingPhases = listOf(single6MPaymentPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "6 months for $1.99, then $3.99/mth",
                    ),
                ),
                arrayOf(
                    "Two phases - single 6 years payment then full price",
                    TestCase(
                        pricingPhases = listOf(single6YPaymentPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "6 years for $1.99, then $3.99/mth",
                    ),
                ),
                arrayOf(
                    "Three phases - free trial, single payment, then full price",
                    TestCase(
                        pricingPhases = listOf(trialPhase, singlePaymentPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 2 months for free, then 1 month for $1.99, and $3.99/mth thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free 3 days trial, single payment, then full price",
                    TestCase(
                        pricingPhases = listOf(trial3DPhase, singlePaymentPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 3 days for free, then 1 month for $1.99, and $3.99/mth thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free trial, single 6M payment, then full price",
                    TestCase(
                        pricingPhases = listOf(trialPhase, single6MPaymentPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 2 months for free, then 6 months for $1.99, and $3.99/mth " +
                            "thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free 3 days trial, single 6M payment, then full price",
                    TestCase(
                        pricingPhases = listOf(trial3DPhase, single6MPaymentPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 3 days for free, then 6 months for $1.99, and $3.99/mth thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free trial, single payment, then full price",
                    TestCase(
                        pricingPhases = listOf(trialPhase, single6YPaymentPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 2 months for free, then 6 years for $1.99, and $3.99/mth thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free 3 days trial, single 6Y payment, then full price",
                    TestCase(
                        pricingPhases = listOf(trial3DPhase, single6YPaymentPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 3 days for free, then 6 years for $1.99, and $3.99/mth thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free trial, single payment, then full 6M price",
                    TestCase(
                        pricingPhases = listOf(trialPhase, singlePaymentPhase, full6MPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 2 months for free, then 1 month for $1.99, and " +
                            "$3.99/6 mths thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free 3 days trial, single payment, then full 6M price",
                    TestCase(
                        pricingPhases = listOf(trial3DPhase, singlePaymentPhase, full6MPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 3 days for free, then 1 month for $1.99, and " +
                            "$3.99/6 mths thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free trial, single 6M payment, then full 6M price",
                    TestCase(
                        pricingPhases = listOf(trialPhase, single6MPaymentPhase, full6MPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 2 months for free, then 6 months for $1.99, and " +
                            "$3.99/6 mths thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free 3 days trial, single 6M payment, then full 6M price",
                    TestCase(
                        pricingPhases = listOf(trial3DPhase, single6MPaymentPhase, full6MPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 3 days for free, then 6 months for $1.99, and " +
                            "$3.99/6 mths thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free trial, single payment, then full 6M price",
                    TestCase(
                        pricingPhases = listOf(trialPhase, single6YPaymentPhase, full6MPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 2 months for free, then 6 years for $1.99, and " +
                            "$3.99/6 mths thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free 3 days trial, single 6Y payment, then full 6M price",
                    TestCase(
                        pricingPhases = listOf(trial3DPhase, single6YPaymentPhase, full6MPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 3 days for free, then 6 years for $1.99, and " +
                            "$3.99/6 mths thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free trial, discounted recurring, then full price",
                    TestCase(
                        pricingPhases = listOf(trialPhase, discountPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 2 months for free, then $0.99/mth " +
                            "for 2 periods, and $3.99/mth thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free 3 days trial, discounted recurring, then full price",
                    TestCase(
                        pricingPhases = listOf(trial3DPhase, discountPhase, fullPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 3 days for free, then $0.99/mth " +
                            "for 2 periods, and $3.99/mth thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free trial, discounted recurring, then full price",
                    TestCase(
                        pricingPhases = listOf(trialPhase, discount6MPhase, full6MPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 2 months for free, then $0.99/6 mths " +
                            "for 2 periods, and $3.99/6 mths thereafter",
                    ),
                ),
                arrayOf(
                    "Three phases - free 3 days trial, discounted recurring, then full price",
                    TestCase(
                        pricingPhases = listOf(trial3DPhase, discount6MPhase, full6MPricePhase),
                        localization = mockLocalization,
                        locale = Locale.US,
                        expectedDescription = "Try 3 days for free, then $0.99/6 mths " +
                            "for 2 periods, and $3.99/6 mths thereafter",
                    ),
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
            testCase.locale,
        )

        assertThat(result).isEqualTo(testCase.expectedDescription)
    }
}