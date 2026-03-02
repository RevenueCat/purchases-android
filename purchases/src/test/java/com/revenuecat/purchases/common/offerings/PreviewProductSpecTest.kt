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

    // region inferFromProductId - lifetime

    @Test
    fun `inferFromProductId matches lifetime keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("com.app.lifetime_access"))
            .isEqualTo(PackageType.LIFETIME)
    }

    @Test
    fun `inferFromProductId matches forever keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("forever_plan"))
            .isEqualTo(PackageType.LIFETIME)
    }

    @Test
    fun `inferFromProductId matches permanent keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("permanent_sub"))
            .isEqualTo(PackageType.LIFETIME)
    }

    // endregion

    // region inferFromProductId - annual

    @Test
    fun `inferFromProductId matches annual keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("com.app.annual_sub"))
            .isEqualTo(PackageType.ANNUAL)
    }

    @Test
    fun `inferFromProductId matches year keyword and catches yearly`() {
        assertThat(PreviewProductSpec.inferFromProductId("yearly_plan"))
            .isEqualTo(PackageType.ANNUAL)
    }

    // endregion

    // region inferFromProductId - six month

    @Test
    fun `inferFromProductId matches six_month keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("six_month_plan"))
            .isEqualTo(PackageType.SIX_MONTH)
    }

    @Test
    fun `inferFromProductId matches sixmonth keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("sixmonth"))
            .isEqualTo(PackageType.SIX_MONTH)
    }

    @Test
    fun `inferFromProductId matches 6month keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("6month_sub"))
            .isEqualTo(PackageType.SIX_MONTH)
    }

    @Test
    fun `inferFromProductId matches semester keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("semester_pass"))
            .isEqualTo(PackageType.SIX_MONTH)
    }

    // endregion

    // region inferFromProductId - three month

    @Test
    fun `inferFromProductId matches three_month keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("three_month"))
            .isEqualTo(PackageType.THREE_MONTH)
    }

    @Test
    fun `inferFromProductId matches threemonth keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("threemonth"))
            .isEqualTo(PackageType.THREE_MONTH)
    }

    @Test
    fun `inferFromProductId matches 3month keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("3month_sub"))
            .isEqualTo(PackageType.THREE_MONTH)
    }

    @Test
    fun `inferFromProductId matches quarter keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("quarterly"))
            .isEqualTo(PackageType.THREE_MONTH)
    }

    // endregion

    // region inferFromProductId - two month

    @Test
    fun `inferFromProductId matches two_month keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("two_month"))
            .isEqualTo(PackageType.TWO_MONTH)
    }

    @Test
    fun `inferFromProductId matches twomonth keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("twomonth"))
            .isEqualTo(PackageType.TWO_MONTH)
    }

    @Test
    fun `inferFromProductId matches 2month keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("2month_sub"))
            .isEqualTo(PackageType.TWO_MONTH)
    }

    @Test
    fun `inferFromProductId matches bimonth keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("bimonthly"))
            .isEqualTo(PackageType.TWO_MONTH)
    }

    // endregion

    // region inferFromProductId - monthly

    @Test
    fun `inferFromProductId matches monthly keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("monthly_sub"))
            .isEqualTo(PackageType.MONTHLY)
    }

    @Test
    fun `inferFromProductId matches month keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("com.app.month_plan"))
            .isEqualTo(PackageType.MONTHLY)
    }

    // endregion

    // region inferFromProductId - weekly

    @Test
    fun `inferFromProductId matches weekly keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("weekly_sub"))
            .isEqualTo(PackageType.WEEKLY)
    }

    @Test
    fun `inferFromProductId matches week keyword`() {
        assertThat(PreviewProductSpec.inferFromProductId("com.app.week_pass"))
            .isEqualTo(PackageType.WEEKLY)
    }

    // endregion

    // region inferFromProductId - fallback and edge cases

    @Test
    fun `inferFromProductId returns CUSTOM for unrecognized product id`() {
        assertThat(PreviewProductSpec.inferFromProductId("com.app.some_product"))
            .isEqualTo(PackageType.CUSTOM)
    }

    @Test
    fun `inferFromProductId returns CUSTOM for generic product id`() {
        assertThat(PreviewProductSpec.inferFromProductId("premium_access"))
            .isEqualTo(PackageType.CUSTOM)
    }

    @Test
    fun `inferFromProductId is case insensitive for uppercase`() {
        assertThat(PreviewProductSpec.inferFromProductId("ANNUAL_PLAN"))
            .isEqualTo(PackageType.ANNUAL)
    }

    @Test
    fun `inferFromProductId is case insensitive for mixed case`() {
        assertThat(PreviewProductSpec.inferFromProductId("Monthly_Sub"))
            .isEqualTo(PackageType.MONTHLY)
    }

    @Test
    fun `inferFromProductId resolves six_month before month`() {
        assertThat(PreviewProductSpec.inferFromProductId("six_month_plan"))
            .isEqualTo(PackageType.SIX_MONTH)
    }

    @Test
    fun `inferFromProductId resolves three_month before month`() {
        assertThat(PreviewProductSpec.inferFromProductId("three_month_plan"))
            .isEqualTo(PackageType.THREE_MONTH)
    }

    @Test
    fun `inferFromProductId resolves two_month before month`() {
        assertThat(PreviewProductSpec.inferFromProductId("two_month_plan"))
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
