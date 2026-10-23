package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.Locale

/**
 * Screen readers get a spoken copy of paywall text where periods are words ("$6.99 monthly")
 * rather than abbreviations ("$6.99/mo"). Mirrors `VariableHandlerV2` on iOS.
 */
@Suppress("DEPRECATION")
class SpokenVariableProcessingTests {

    private val enUs = variableLocalizationKeysForEnUs()

    private val monthlyPackage = subscriptionPackage("monthly", "$6.99", 6_990_000, "P1M")
    private val threeMonthPackage = subscriptionPackage("three_month", "$4.99", 4_990_000, "P3M")
    private val annualPackage = subscriptionPackage("annual", "$53.88", 53_880_000, "P1Y")

    @Before
    fun setUp() {
        mockkObject(Logger)
        every { Logger.w(any()) } returns Unit
        every { Logger.e(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
    }

    @Test
    fun `price per period abbreviated is spoken as words`() {
        assertThat(process("{{ product.price_per_period_abbreviated }}", monthlyPackage, spoken = true))
            .isEqualTo("$6.99 monthly")
    }

    @Test
    fun `price per period is spoken as words`() {
        assertThat(process("{{ product.price_per_period }}", monthlyPackage, spoken = true))
            .isEqualTo("$6.99 monthly")
    }

    @Test
    fun `price per period abbreviated is spoken as words for multiple months`() {
        assertThat(process("{{ product.price_per_period_abbreviated }}", threeMonthPackage, spoken = true))
            .isEqualTo("$4.99 3 months")
    }

    @Test
    fun `period abbreviated is spoken as the full noun`() {
        assertThat(process("{{ product.period_abbreviated }}", monthlyPackage, spoken = true))
            .isEqualTo("month")
    }

    @Test
    fun `displayed text keeps its abbreviations`() {
        assertThat(process("{{ product.price_per_period_abbreviated }}", monthlyPackage, spoken = false))
            .isEqualTo("$6.99/mo")
        assertThat(process("{{ product.price_per_month }}/mo", annualPackage, spoken = false))
            .isEqualTo("$4.49/mo")
    }

    @Test
    fun `literal period abbreviation typed in the copy is spoken as a word`() {
        assertThat(process("{{ product.price_per_month }}/mo", annualPackage, spoken = true))
            .isEqualTo("$4.49 monthly")
    }

    @Test
    fun `link URLs are not mistaken for a period`() {
        val localizations = mapOf(
            VariableLocalizationKey.MONTH_SHORT to "mo",
            VariableLocalizationKey.MONTHLY to "monthly",
        )

        assertThat(VariableProcessorV2.expandPeriodAbbreviations("[Terms](https://rev.cat/mo)", localizations))
            .isEqualTo("[Terms](https://rev.cat/mo)")
        assertThat(VariableProcessorV2.expandPeriodAbbreviations("See https://rev.cat/mo for details", localizations))
            .isEqualTo("See https://rev.cat/mo for details")
        // A digit before the segment is what a price looks like, so URLs are skipped as URLs.
        assertThat(VariableProcessorV2.expandPeriodAbbreviations("[Docs](https://rev.cat/v2/mo)", localizations))
            .isEqualTo("[Docs](https://rev.cat/v2/mo)")
    }

    @Test
    fun `link URL with a day path is not expanded`() {
        val localizations = mapOf(
            VariableLocalizationKey.DAY_SHORT to "day",
            VariableLocalizationKey.DAILY to "daily",
        )

        assertThat(
            VariableProcessorV2.expandPeriodAbbreviations("See https://rev.cat/2024/day for details", localizations),
        ).isEqualTo("See https://rev.cat/2024/day for details")
        assertThat(VariableProcessorV2.expandPeriodAbbreviations("$1/day", localizations))
            .isEqualTo("$1 daily")
    }

    @Test
    fun `price still expands alongside a link`() {
        val localizations = mapOf(
            VariableLocalizationKey.MONTH_SHORT to "mo",
            VariableLocalizationKey.MONTHLY to "monthly",
        )

        assertThat(
            VariableProcessorV2.expandPeriodAbbreviations("$5.83/mo. See [terms](https://rev.cat/mo).", localizations),
        ).isEqualTo("$5.83 monthly. See [terms](https://rev.cat/mo).")
    }

    @Test
    fun `expansion is idempotent`() {
        val once = VariableProcessorV2.expandPeriodAbbreviations("$6.99/mo and $69.99/yr", enUs)
        val twice = VariableProcessorV2.expandPeriodAbbreviations(once, enUs)

        assertThat(once).isEqualTo("$6.99 monthly and $69.99 yearly")
        assertThat(twice).isEqualTo(once)
    }

    @Test
    fun `spelled out periods are expanded`() {
        assertThat(VariableProcessorV2.expandPeriodAbbreviations("$4.16/month", enUs)).isEqualTo("$4.16 monthly")
        assertThat(VariableProcessorV2.expandPeriodAbbreviations("$1.99/week", enUs)).isEqualTo("$1.99 weekly")
        assertThat(VariableProcessorV2.expandPeriodAbbreviations("$69.99/year", enUs)).isEqualTo("$69.99 yearly")
    }

    @Test
    fun `short and long forms agree across units`() {
        listOf("mo" to "monthly", "month" to "monthly", "wk" to "weekly", "week" to "weekly", "day" to "daily")
            .forEach { (written, spoken) ->
                assertThat(VariableProcessorV2.expandPeriodAbbreviations("$1/$written", enUs))
                    .isEqualTo("$1 $spoken")
            }
    }

    @Test
    fun `expansion uses the paywall localizations`() {
        val german = enUs + mapOf(
            VariableLocalizationKey.MONTH_SHORT to "Mon.",
            VariableLocalizationKey.MONTHLY to "monatlich",
        )

        assertThat(VariableProcessorV2.expandPeriodAbbreviations("9,99 €/Mon.", german))
            .isEqualTo("9,99 € monatlich")
    }

    @Test
    fun `offer periods are spoken as full words`() {
        val twoWeekTrial = mockk<SubscriptionOption> {
            every { freePhase } returns mockk<PricingPhase> {
                every { price } returns Price(amountMicros = 0, currencyCode = "USD", formatted = "$0.00")
                every { billingPeriod } returns Period(value = 2, unit = Period.Unit.WEEK, iso8601 = "P2W")
                every { billingCycleCount } returns 1
                every { recurrenceMode } returns RecurrenceMode.FINITE_RECURRING
            }
            every { introPhase } returns null
        }
        val template = "Free for {{ product.offer_period_abbreviated }}"

        assertThat(process(template, monthlyPackage, spoken = false, subscriptionOption = twoWeekTrial))
            .isEqualTo("Free for wk")
        assertThat(process(template, monthlyPackage, spoken = true, subscriptionOption = twoWeekTrial))
            .isEqualTo("Free for week")
    }

    private fun process(
        template: String,
        rcPackage: Package,
        spoken: Boolean,
        subscriptionOption: SubscriptionOption? = null,
    ): String =
        VariableProcessorV2.processVariables(
            template = template,
            localizedVariableKeys = enUs,
            variableConfig = UiConfig.VariableConfig(
                variableCompatibilityMap = emptyMap(),
                functionCompatibilityMap = emptyMap(),
            ),
            variableDataProvider = VariableDataProvider(MockResourceProvider()),
            packageContext = PackageContext(
                discountRelativeToMostExpensivePerMonth = null,
                showZeroDecimalPlacePrices = false,
            ),
            rcPackage = rcPackage,
            subscriptionOption = subscriptionOption,
            currencyLocale = Locale.US,
            dateLocale = Locale.US,
            date = Date(),
            spoken = spoken,
        )

    private fun subscriptionPackage(identifier: String, formatted: String, amountMicros: Long, period: String) =
        Package(
            packageType = PackageType.CUSTOM,
            identifier = identifier,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.$identifier",
                name = identifier,
                title = identifier,
                price = Price(amountMicros = amountMicros, currencyCode = "USD", formatted = formatted),
                description = identifier,
                period = Period.create(period),
            ),
        )
}
