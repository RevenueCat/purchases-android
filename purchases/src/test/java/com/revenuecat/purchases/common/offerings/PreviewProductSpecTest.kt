package com.revenuecat.purchases.common.offerings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.RecurrenceMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PreviewProductSpecTest {

    // region fromPackageType

    @Test
    fun `fromPackageType returns LIFETIME for PackageType LIFETIME`() {
        assertThat(PreviewProductSpec.fromPackageType(PackageType.LIFETIME))
            .isEqualTo(PreviewProductSpec.LIFETIME)
    }

    @Test
    fun `fromPackageType returns ANNUAL for PackageType ANNUAL`() {
        assertThat(PreviewProductSpec.fromPackageType(PackageType.ANNUAL))
            .isEqualTo(PreviewProductSpec.ANNUAL)
    }

    @Test
    fun `fromPackageType returns SIX_MONTH for PackageType SIX_MONTH`() {
        assertThat(PreviewProductSpec.fromPackageType(PackageType.SIX_MONTH))
            .isEqualTo(PreviewProductSpec.SIX_MONTH)
    }

    @Test
    fun `fromPackageType returns THREE_MONTH for PackageType THREE_MONTH`() {
        assertThat(PreviewProductSpec.fromPackageType(PackageType.THREE_MONTH))
            .isEqualTo(PreviewProductSpec.THREE_MONTH)
    }

    @Test
    fun `fromPackageType returns TWO_MONTH for PackageType TWO_MONTH`() {
        assertThat(PreviewProductSpec.fromPackageType(PackageType.TWO_MONTH))
            .isEqualTo(PreviewProductSpec.TWO_MONTH)
    }

    @Test
    fun `fromPackageType returns MONTHLY for PackageType MONTHLY`() {
        assertThat(PreviewProductSpec.fromPackageType(PackageType.MONTHLY))
            .isEqualTo(PreviewProductSpec.MONTHLY)
    }

    @Test
    fun `fromPackageType returns WEEKLY for PackageType WEEKLY`() {
        assertThat(PreviewProductSpec.fromPackageType(PackageType.WEEKLY))
            .isEqualTo(PreviewProductSpec.WEEKLY)
    }

    @Test
    fun `fromPackageType returns DEFAULT for PackageType CUSTOM`() {
        assertThat(PreviewProductSpec.fromPackageType(PackageType.CUSTOM))
            .isEqualTo(PreviewProductSpec.DEFAULT)
    }

    @Test
    fun `fromPackageType returns DEFAULT for PackageType UNKNOWN`() {
        assertThat(PreviewProductSpec.fromPackageType(PackageType.UNKNOWN))
            .isEqualTo(PreviewProductSpec.DEFAULT)
    }

    // endregion

    // region inferFromIdentifier - lifetime

    @Test
    fun `inferFromIdentifier matches lifetime keyword`() {
        assertThat(inferPackageTypeFromIdentifier("com.app.lifetime_access"))
            .isEqualTo(PackageType.LIFETIME)
    }

    @Test
    fun `inferFromIdentifier matches forever keyword`() {
        assertThat(inferPackageTypeFromIdentifier("forever_plan"))
            .isEqualTo(PackageType.LIFETIME)
    }

    @Test
    fun `inferFromIdentifier matches permanent keyword`() {
        assertThat(inferPackageTypeFromIdentifier("permanent_sub"))
            .isEqualTo(PackageType.LIFETIME)
    }

    // endregion

    // region inferFromIdentifier - annual

    @Test
    fun `inferFromIdentifier matches annual keyword`() {
        assertThat(inferPackageTypeFromIdentifier("com.app.annual_sub"))
            .isEqualTo(PackageType.ANNUAL)
    }

    @Test
    fun `inferFromIdentifier matches year keyword and catches yearly`() {
        assertThat(inferPackageTypeFromIdentifier("yearly_plan"))
            .isEqualTo(PackageType.ANNUAL)
    }

    // endregion

    // region inferFromIdentifier - six month

    @Test
    fun `inferFromIdentifier matches six_month keyword`() {
        assertThat(inferPackageTypeFromIdentifier("six_month_plan"))
            .isEqualTo(PackageType.SIX_MONTH)
    }

    @Test
    fun `inferFromIdentifier matches sixmonth keyword`() {
        assertThat(inferPackageTypeFromIdentifier("sixmonth"))
            .isEqualTo(PackageType.SIX_MONTH)
    }

    @Test
    fun `inferFromIdentifier matches 6month keyword`() {
        assertThat(inferPackageTypeFromIdentifier("6month_sub"))
            .isEqualTo(PackageType.SIX_MONTH)
    }

    @Test
    fun `inferFromIdentifier matches semester keyword`() {
        assertThat(inferPackageTypeFromIdentifier("semester_pass"))
            .isEqualTo(PackageType.SIX_MONTH)
    }

    // endregion

    // region inferFromIdentifier - three month

    @Test
    fun `inferFromIdentifier matches three_month keyword`() {
        assertThat(inferPackageTypeFromIdentifier("three_month"))
            .isEqualTo(PackageType.THREE_MONTH)
    }

    @Test
    fun `inferFromIdentifier matches threemonth keyword`() {
        assertThat(inferPackageTypeFromIdentifier("threemonth"))
            .isEqualTo(PackageType.THREE_MONTH)
    }

    @Test
    fun `inferFromIdentifier matches 3month keyword`() {
        assertThat(inferPackageTypeFromIdentifier("3month_sub"))
            .isEqualTo(PackageType.THREE_MONTH)
    }

    @Test
    fun `inferFromIdentifier matches quarter keyword`() {
        assertThat(inferPackageTypeFromIdentifier("quarterly"))
            .isEqualTo(PackageType.THREE_MONTH)
    }

    // endregion

    // region inferFromIdentifier - two month

    @Test
    fun `inferFromIdentifier matches two_month keyword`() {
        assertThat(inferPackageTypeFromIdentifier("two_month"))
            .isEqualTo(PackageType.TWO_MONTH)
    }

    @Test
    fun `inferFromIdentifier matches twomonth keyword`() {
        assertThat(inferPackageTypeFromIdentifier("twomonth"))
            .isEqualTo(PackageType.TWO_MONTH)
    }

    @Test
    fun `inferFromIdentifier matches 2month keyword`() {
        assertThat(inferPackageTypeFromIdentifier("2month_sub"))
            .isEqualTo(PackageType.TWO_MONTH)
    }

    @Test
    fun `inferFromIdentifier matches bimonth keyword`() {
        assertThat(inferPackageTypeFromIdentifier("bimonthly"))
            .isEqualTo(PackageType.TWO_MONTH)
    }

    // endregion

    // region inferFromIdentifier - monthly

    @Test
    fun `inferFromIdentifier matches monthly keyword`() {
        assertThat(inferPackageTypeFromIdentifier("monthly_sub"))
            .isEqualTo(PackageType.MONTHLY)
    }

    @Test
    fun `inferFromIdentifier matches month keyword`() {
        assertThat(inferPackageTypeFromIdentifier("com.app.month_plan"))
            .isEqualTo(PackageType.MONTHLY)
    }

    // endregion

    // region inferFromIdentifier - weekly

    @Test
    fun `inferFromIdentifier matches weekly keyword`() {
        assertThat(inferPackageTypeFromIdentifier("weekly_sub"))
            .isEqualTo(PackageType.WEEKLY)
    }

    @Test
    fun `inferFromIdentifier matches week keyword`() {
        assertThat(inferPackageTypeFromIdentifier("com.app.week_pass"))
            .isEqualTo(PackageType.WEEKLY)
    }

    // endregion

    // region inferFromIdentifier - fallback and edge cases

    @Test
    fun `inferFromIdentifier returns CUSTOM for unrecognized product id`() {
        assertThat(inferPackageTypeFromIdentifier("com.app.some_product"))
            .isEqualTo(PackageType.CUSTOM)
    }

    @Test
    fun `inferFromIdentifier returns CUSTOM for generic product id`() {
        assertThat(inferPackageTypeFromIdentifier("premium_access"))
            .isEqualTo(PackageType.CUSTOM)
    }

    @Test
    fun `inferFromIdentifier is case insensitive for uppercase`() {
        assertThat(inferPackageTypeFromIdentifier("ANNUAL_PLAN"))
            .isEqualTo(PackageType.ANNUAL)
    }

    @Test
    fun `inferFromIdentifier is case insensitive for mixed case`() {
        assertThat(inferPackageTypeFromIdentifier("Monthly_Sub"))
            .isEqualTo(PackageType.MONTHLY)
    }

    @Test
    fun `inferFromIdentifier resolves six_month before month`() {
        assertThat(inferPackageTypeFromIdentifier("six_month_plan"))
            .isEqualTo(PackageType.SIX_MONTH)
    }

    @Test
    fun `inferFromIdentifier resolves three_month before month`() {
        assertThat(inferPackageTypeFromIdentifier("three_month_plan"))
            .isEqualTo(PackageType.THREE_MONTH)
    }

    @Test
    fun `inferFromIdentifier resolves two_month before month`() {
        assertThat(inferPackageTypeFromIdentifier("two_month_plan"))
            .isEqualTo(PackageType.TWO_MONTH)
    }

    // endregion

    // region toTestStoreProduct - price conversion

    @Test
    fun `toTestStoreProduct converts price to correct micros for all specs`() {
        for (spec in PreviewProductSpec.values()) {
            val product = spec.toTestStoreProduct("test_id")
            val expectedMicros = Math.round(spec.price * 1_000_000L)
            assertThat(product.price.amountMicros)
                .describedAs("${spec.name} micros")
                .isEqualTo(expectedMicros)
        }
    }

    @Test
    fun `toTestStoreProduct formats price as USD for all specs`() {
        for (spec in PreviewProductSpec.values()) {
            val product = spec.toTestStoreProduct("test_id")
            assertThat(product.price.currencyCode)
                .describedAs("${spec.name} currency")
                .isEqualTo("USD")
        }
    }

    @Test
    fun `toTestStoreProduct formats price string correctly`() {
        val product = PreviewProductSpec.ANNUAL.toTestStoreProduct("test_id")
        assertThat(product.price.formatted).isEqualTo("$59.99")
    }

    @Test
    fun `toTestStoreProduct formats lifetime price string correctly`() {
        val product = PreviewProductSpec.LIFETIME.toTestStoreProduct("test_id")
        assertThat(product.price.formatted).isEqualTo("$199.99")
    }

    @Test
    fun `toTestStoreProduct formats default price string correctly`() {
        val product = PreviewProductSpec.DEFAULT.toTestStoreProduct("test_id")
        assertThat(product.price.formatted).isEqualTo("$249.99")
    }

    // endregion

    // region toTestStoreProduct - period

    @Test
    fun `toTestStoreProduct sets correct period for subscription specs`() {
        val annualProduct = PreviewProductSpec.ANNUAL.toTestStoreProduct("id")
        assertThat(annualProduct.period).isEqualTo(
            Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
        )

        val monthlyProduct = PreviewProductSpec.MONTHLY.toTestStoreProduct("id")
        assertThat(monthlyProduct.period).isEqualTo(
            Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        )

        val weeklyProduct = PreviewProductSpec.WEEKLY.toTestStoreProduct("id")
        assertThat(weeklyProduct.period).isEqualTo(
            Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
        )
    }

    @Test
    fun `toTestStoreProduct sets null period for LIFETIME`() {
        val product = PreviewProductSpec.LIFETIME.toTestStoreProduct("id")
        assertThat(product.period).isNull()
    }

    @Test
    fun `toTestStoreProduct sets null period for DEFAULT`() {
        val product = PreviewProductSpec.DEFAULT.toTestStoreProduct("id")
        assertThat(product.period).isNull()
    }

    // endregion

    // region toTestStoreProduct - free trial

    @Test
    fun `toTestStoreProduct includes free trial only for ANNUAL`() {
        val annualProduct = PreviewProductSpec.ANNUAL.toTestStoreProduct("id")
        assertThat(annualProduct.subscriptionOptions?.freeTrial?.freePhase).isNotNull

        val nonTrialSpecs = PreviewProductSpec.values().filter { it != PreviewProductSpec.ANNUAL }
        for (spec in nonTrialSpecs) {
            val product = spec.toTestStoreProduct("id")
            assertThat(product.subscriptionOptions?.freeTrial?.freePhase)
                .describedAs("${spec.name} should not have free trial")
                .isNull()
        }
    }

    @Test
    fun `toTestStoreProduct annual free trial has correct properties`() {
        val trial = PreviewProductSpec.ANNUAL.toTestStoreProduct("id").subscriptionOptions?.freeTrial?.freePhase!!
        assertThat(trial.price.amountMicros).isEqualTo(0L)
        assertThat(trial.price.formatted).isEqualTo("Free")
        assertThat(trial.price.currencyCode).isEqualTo("USD")
        assertThat(trial.billingPeriod).isEqualTo(
            Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
        )
        assertThat(trial.recurrenceMode).isEqualTo(RecurrenceMode.FINITE_RECURRING)
        assertThat(trial.billingCycleCount).isEqualTo(1)
    }

    // endregion

    // region toTestStoreProduct - metadata

    @Test
    fun `toTestStoreProduct sets product id from argument`() {
        val product = PreviewProductSpec.MONTHLY.toTestStoreProduct("com.app.monthly_sub")
        assertThat(product.id).isEqualTo("com.app.monthly_sub")
    }

    @Test
    fun `toTestStoreProduct sets name title and description from displayName`() {
        val product = PreviewProductSpec.SIX_MONTH.toTestStoreProduct("id")
        assertThat(product.name).isEqualTo("6 Month")
        assertThat(product.title).isEqualTo("6 Month")
        assertThat(product.description).isEqualTo("6 Month")
    }

    // endregion
}
