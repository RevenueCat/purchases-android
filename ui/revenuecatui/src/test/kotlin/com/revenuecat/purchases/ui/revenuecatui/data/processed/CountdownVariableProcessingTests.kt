package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.ui.revenuecatui.components.countdown.CountdownTime
import com.revenuecat.purchases.ui.revenuecatui.components.variableLocalizationKeysForEnUs
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date
import java.util.Locale

/**
 * Tests that verify countdown variables are correctly replaced in text templates.
 *
 * These tests verify the entire pipeline:
 * Template: "{{ count_hours_without_zero }}" → Actual text: "0"
 */
class CountdownVariableProcessingTests {

    private companion object {
        private const val OFFERING_ID = "offering_identifier"

        private val productYearlyUsd = TestStoreProduct(
            id = "com.revenuecat.annual_product",
            name = "Annual",
            title = "Annual (App name)",
            description = "Annual",
            price = Price(
                amountMicros = 2_000_000,
                currencyCode = "USD",
                formatted = "$ 2.00",
            ),
            period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
            freeTrialPricingPhase = null,
            introPricePricingPhase = null,
        )

        private val packageYearlyUsd = Package(
            identifier = "package_yearly",
            packageType = PackageType.ANNUAL,
            product = productYearlyUsd,
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = OFFERING_ID),
        )
    }

    @Test
    fun `template with 2 days replaces hours with 0 not 48`() {
        val countdownTime = CountdownTime(days = 2, hours = 0, minutes = 0, seconds = 0)
        val template = "{{ count_hours_without_zero }} hours remaining"

        val result = processTemplate(template, countdownTime)

        assertThat(result).isEqualTo("0 hours remaining")
    }

    @Test
    fun `template with 48 hours from interval replaces correctly`() {
        val fortyEightHours = 48 * 3_600_000L
        val countdownTime = CountdownTime.fromInterval(fortyEightHours)
        val template = "Days: {{ count_days_without_zero }}, Hours: {{ count_hours_without_zero }}"

        val result = processTemplate(template, countdownTime)

        assertThat(result).isEqualTo("Days: 2, Hours: 0")
    }

    @Test
    fun `template with multiple countdown variables replaces all correctly`() {
        val countdownTime = CountdownTime(days = 2, hours = 5, minutes = 30, seconds = 15)
        val template = "{{ count_days_without_zero }}d {{ count_hours_without_zero }}h {{ count_minutes_without_zero }}m {{ count_seconds_without_zero }}s"

        val result = processTemplate(template, countdownTime)

        assertThat(result).isEqualTo("2d 5h 30m 15s")
    }

    @Test
    fun `template with zero padding replaces correctly`() {
        val countdownTime = CountdownTime(days = 1, hours = 2, minutes = 3, seconds = 4)
        val template = "{{ count_days_with_zero }}:{{ count_hours_with_zero }}:{{ count_minutes_with_zero }}:{{ count_seconds_with_zero }}"

        val result = processTemplate(template, countdownTime)

        assertThat(result).isEqualTo("01:02:03:04")
    }

    @Test
    fun `template without zero padding replaces correctly`() {
        val countdownTime = CountdownTime(days = 1, hours = 2, minutes = 3, seconds = 4)
        val template = "{{ count_days_without_zero }}:{{ count_hours_without_zero }}:{{ count_minutes_without_zero }}:{{ count_seconds_without_zero }}"

        val result = processTemplate(template, countdownTime)

        assertThat(result).isEqualTo("1:2:3:4")
    }

    @Test
    fun `template with ended countdown replaces with zeros`() {
        val countdownTime = CountdownTime.ZERO
        val template = "Time: {{ count_hours_without_zero }}h {{ count_minutes_without_zero }}m {{ count_seconds_without_zero }}s"

        val result = processTemplate(template, countdownTime)

        assertThat(result).isEqualTo("Time: 0h 0m 0s")
    }

    @Test
    fun `template with 51 hours shows component values not total hours`() {
        val fiftyOneHours = 51 * 3_600_000L
        val countdownTime = CountdownTime.fromInterval(fiftyOneHours)
        val template = "{{ count_days_without_zero }} days and {{ count_hours_without_zero }} hours"

        val result = processTemplate(template, countdownTime)

        assertThat(result).isEqualTo("2 days and 3 hours")
    }

    @Test
    fun `template with only hours variable shows component hours`() {
        val twoDaysThreeHours = 2 * 86_400_000L + 3 * 3_600_000L
        val countdownTime = CountdownTime.fromInterval(twoDaysThreeHours)
        val template = "Only showing: {{ count_hours_without_zero }}"

        val result = processTemplate(template, countdownTime)

        assertThat(result).isEqualTo("Only showing: 3")
    }

    @Test
    fun `template with mixed text and countdown variables replaces correctly`() {
        val countdownTime = CountdownTime(days = 5, hours = 12, minutes = 34, seconds = 56)
        val template = "Sale ends in {{ count_days_without_zero }} days, {{ count_hours_without_zero }}:{{ count_minutes_with_zero }}:{{ count_seconds_with_zero }}!"

        val result = processTemplate(template, countdownTime)

        assertThat(result).isEqualTo("Sale ends in 5 days, 12:34:56!")
    }

    @Test
    fun `template with no countdown variables returns unchanged`() {
        val countdownTime = CountdownTime(days = 2, hours = 5, minutes = 0, seconds = 0)
        val template = "This has no countdown variables"

        val result = processTemplate(template, countdownTime)

        assertThat(result).isEqualTo("This has no countdown variables")
    }

    @Test
    fun `countFrom DAYS shows component values for all units`() {
        val countdownTime = CountdownTime(days = 2, hours = 5, minutes = 30, seconds = 15)
        val template = "{{ count_days_without_zero }}:{{ count_hours_without_zero }}:{{ count_minutes_without_zero }}:{{ count_seconds_without_zero }}"

        val result = processTemplate(template, countdownTime, CountdownComponent.CountFrom.DAYS)

        assertThat(result).isEqualTo("2:5:30:15")
    }

    @Test
    fun `countFrom HOURS shows total hours with days hidden`() {
        val countdownTime = CountdownTime(days = 2, hours = 5, minutes = 30, seconds = 15)
        val template = "{{ count_days_without_zero }}:{{ count_hours_without_zero }}:{{ count_minutes_without_zero }}:{{ count_seconds_without_zero }}"

        val result = processTemplate(template, countdownTime, CountdownComponent.CountFrom.HOURS)

        assertThat(result).isEqualTo("0:53:30:15")
    }

    @Test
    fun `countFrom HOURS with 48 hours shows 48 not 24`() {
        val countdownTime = CountdownTime(days = 2, hours = 0, minutes = 0, seconds = 0)
        val template = "{{ count_hours_without_zero }} hours remaining"

        val result = processTemplate(template, countdownTime, CountdownComponent.CountFrom.HOURS)

        assertThat(result).isEqualTo("48 hours remaining")
    }

    @Test
    fun `countFrom MINUTES shows total minutes with days and hours hidden`() {
        val countdownTime = CountdownTime(days = 2, hours = 5, minutes = 30, seconds = 15)
        val template = "{{ count_days_without_zero }}:{{ count_hours_without_zero }}:{{ count_minutes_without_zero }}:{{ count_seconds_without_zero }}"

        val result = processTemplate(template, countdownTime, CountdownComponent.CountFrom.MINUTES)

        assertThat(result).isEqualTo("0:0:3210:15")
    }

    @Test
    fun `countFrom MINUTES with 6 days shows total minutes`() {
        val countdownTime = CountdownTime(days = 6, hours = 22, minutes = 59, seconds = 56)
        val template = "{{ count_hours_without_zero }}:{{ count_minutes_without_zero }}:{{ count_seconds_without_zero }}"

        val result = processTemplate(template, countdownTime, CountdownComponent.CountFrom.MINUTES)

        assertThat(result).isEqualTo("0:10019:56")
    }

    @Test
    fun `countFrom HOURS with zero padding shows total hours correctly`() {
        val countdownTime = CountdownTime(days = 7, hours = 3, minutes = 15, seconds = 45)
        val template = "{{ count_days_with_zero }}:{{ count_hours_with_zero }}:{{ count_minutes_with_zero }}:{{ count_seconds_with_zero }}"

        val result = processTemplate(template, countdownTime, CountdownComponent.CountFrom.HOURS)

        assertThat(result).isEqualTo("00:171:15:45")
    }

    @Test
    fun `countFrom MINUTES with zero padding shows total minutes correctly`() {
        val countdownTime = CountdownTime(days = 1, hours = 2, minutes = 3, seconds = 4)
        val template = "{{ count_days_with_zero }}:{{ count_hours_with_zero }}:{{ count_minutes_with_zero }}:{{ count_seconds_with_zero }}"

        val result = processTemplate(template, countdownTime, CountdownComponent.CountFrom.MINUTES)

        assertThat(result).isEqualTo("00:00:1563:04")
    }

    @Test
    fun `countdown variables work without a package`() {
        val countdownTime = CountdownTime(days = 2, hours = 5, minutes = 30, seconds = 15)
        val template = "{{ count_days_without_zero }}d {{ count_hours_without_zero }}h {{ count_minutes_without_zero }}m {{ count_seconds_without_zero }}s"

        val result = processTemplateWithoutPackage(template, countdownTime)

        assertThat(result).isEqualTo("2d 5h 30m 15s")
    }

    @Test
    fun `countdown variables with zero padding work without a package`() {
        val countdownTime = CountdownTime(days = 1, hours = 2, minutes = 3, seconds = 4)
        val template = "{{ count_days_with_zero }}:{{ count_hours_with_zero }}:{{ count_minutes_with_zero }}:{{ count_seconds_with_zero }}"

        val result = processTemplateWithoutPackage(template, countdownTime)

        assertThat(result).isEqualTo("01:02:03:04")
    }

    @Test
    fun `countdown variables with countFrom HOURS work without a package`() {
        val countdownTime = CountdownTime(days = 2, hours = 0, minutes = 30, seconds = 15)
        val template = "{{ count_hours_without_zero }}:{{ count_minutes_with_zero }}:{{ count_seconds_with_zero }}"

        val result = processTemplateWithoutPackage(template, countdownTime, CountdownComponent.CountFrom.HOURS)

        assertThat(result).isEqualTo("48:30:15")
    }

    @Test
    fun `product variables return empty string without a package`() {
        val countdownTime = CountdownTime(days = 2, hours = 5, minutes = 30, seconds = 15)
        val template = "Price: {{ product.price }} - Time: {{ count_days_without_zero }}d"

        val result = processTemplateWithoutPackage(template, countdownTime)

        assertThat(result).isEqualTo("Price:  - Time: 2d")
    }

    private fun processTemplate(
        template: String,
        countdownTime: CountdownTime,
        countFrom: CountdownComponent.CountFrom = CountdownComponent.CountFrom.DAYS,
    ): String {
        val variableDataProvider = VariableDataProvider(MockResourceProvider())
        return VariableProcessorV2.processVariables(
            template = template,
            localizedVariableKeys = variableLocalizationKeysForEnUs(),
            variableConfig = UiConfig.VariableConfig(),
            variableDataProvider = variableDataProvider,
            packageContext = PackageContext(
                discountRelativeToMostExpensivePerMonth = null,
                showZeroDecimalPlacePrices = false,
            ),
            rcPackage = packageYearlyUsd,
            currencyLocale = Locale.US,
            dateLocale = Locale.US,
            date = Date(),
            countdownTime = countdownTime,
            countFrom = countFrom,
        )
    }

    private fun processTemplateWithoutPackage(
        template: String,
        countdownTime: CountdownTime,
        countFrom: CountdownComponent.CountFrom = CountdownComponent.CountFrom.DAYS,
    ): String {
        return VariableProcessorV2.processVariables(
            template = template,
            variableConfig = UiConfig.VariableConfig(),
            dateLocale = Locale.US,
            countdownTime = countdownTime,
            countFrom = countFrom,
        )
    }
}

