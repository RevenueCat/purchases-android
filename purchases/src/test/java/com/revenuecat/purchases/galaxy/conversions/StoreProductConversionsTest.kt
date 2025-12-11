package com.revenuecat.purchases.galaxy.conversions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.galaxy.GalaxyStoreTest
import com.revenuecat.purchases.models.Period
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreProductConversionsTest : GalaxyStoreTest() {

    // region toStoreProduct basic fields

    @Test
    fun `toStoreProduct maps id from ProductVo`() {
        val productId = "custom_id"

        val storeProduct = createProductVo(itemId = productId).toStoreProduct()

        assertThat(storeProduct.id).isEqualTo(productId)
    }

    @Test
    fun `toStoreProduct maps name from ProductVo`() {
        val productName = "Custom Name"

        val storeProduct = createProductVo(itemName = productName).toStoreProduct()

        assertThat(storeProduct.name).isEqualTo(productName)
    }

    @Test
    fun `toStoreProduct maps title from ProductVo`() {
        val productTitle = "Custom Title"

        val storeProduct = createProductVo(itemName = productTitle).toStoreProduct()

        assertThat(storeProduct.title).isEqualTo(productTitle)
    }

    @Test
    fun `toStoreProduct maps description from ProductVo`() {
        val productDescription = "Custom Description"

        val storeProduct = createProductVo(itemDescription = productDescription).toStoreProduct()

        assertThat(storeProduct.description).isEqualTo(productDescription)
    }

    @Test
    fun `toStoreProduct sets subscriptionOptions, defaultOption, and presentedOfferingContext to null`() {
        val storeProduct = createProductVo().toStoreProduct()

        assertThat(storeProduct.subscriptionOptions).isNull()
        assertThat(storeProduct.defaultOption).isNull()
        assertThat(storeProduct.presentedOfferingContext).isNull()
    }

    // endregion

    // region toStoreProduct type mapping
    @Test
    fun `toStoreProduct maps item type to INAPP`() {
        val storeProduct = createProductVo(
            type = "item"
        ).toStoreProduct()

        assertThat(storeProduct.type).isEqualTo(ProductType.INAPP)
    }

    @Test
    fun `toStoreProduct maps subscription type to SUBS`() {
        val storeProduct = createProductVo(type = "subscription").toStoreProduct()

        assertThat(storeProduct.type).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `toStoreProduct handles uppercase type values`() {
        val upperCaseItem = createProductVo(type = "ITEM").toStoreProduct()
        val upperCaseSubscription = createProductVo(type = "SUBSCRIPTION").toStoreProduct()

        assertThat(upperCaseItem.type).isEqualTo(ProductType.INAPP)
        assertThat(upperCaseSubscription.type).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `toStoreProduct handles mixed case type values`() {
        val mixedCaseItem = createProductVo(type = "ItEm").toStoreProduct()
        val mixedCaseSubscription = createProductVo(type = "SubScription").toStoreProduct()

        assertThat(mixedCaseItem.type).isEqualTo(ProductType.INAPP)
        assertThat(mixedCaseSubscription.type).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `toStoreProduct returns UNKNOWN type for unexpected value`() {
        val storeProduct = createProductVo(type = "unknown-type").toStoreProduct()

        assertThat(storeProduct.type).isEqualTo(ProductType.UNKNOWN)
    }

    @Test
    fun `toStoreProduct returns UNKNOWN type for empty string`() {
        val storeProduct = createProductVo(type = "").toStoreProduct()

        assertThat(storeProduct.type).isEqualTo(ProductType.UNKNOWN)
    }

    // endregion

    // region toStoreProduct price creation

    @Test
    fun `toStoreProduct builds formatted price with two decimals when itemPriceString omits them`() {
        val productVo = createProductVo(
            itemPrice = 3.0,
            currencyUnit = "$",
            currencyCode = "USD",
            itemPriceString = "$3",
        )

        val storeProduct = productVo.toStoreProduct()

        assertThat(storeProduct.price.formatted).isEqualTo("$3.00")
        assertThat(storeProduct.price.amountMicros).isEqualTo(3_000_000)
        assertThat(storeProduct.price.currencyCode).isEqualTo("USD")
    }

    @Test
    fun `toStoreProduct uses itemPrice to compute micros and preserves currency info`() {
        val productVo = createProductVo(
            itemPrice = 3.25,
            currencyUnit = "$",
            currencyCode = "USD",
        )

        val storeProduct = productVo.toStoreProduct()

        assertThat(storeProduct.price.formatted).isEqualTo("$3.25")
        assertThat(storeProduct.price.amountMicros).isEqualTo(3_250_000)
        assertThat(storeProduct.price.currencyCode).isEqualTo("USD")
    }

    @Test
    fun `toStoreProduct rounds formatted price but keeps raw micros multiplication`() {
        val productVo = createProductVo(
            itemPrice = 1.2345,
            currencyUnit = "$",
            currencyCode = "USD",
        )

        val storeProduct = productVo.toStoreProduct()

        assertThat(storeProduct.price.formatted).isEqualTo("$1.23")
        assertThat(storeProduct.price.amountMicros).isEqualTo(1_234_500)
    }

    // endregion

    // region toStoreProduct period creation

    @Test
    fun `toStoreProduct builds period for supported subscription duration units`() {
        data class ExpectedPeriod(
            val multiplier: String,
            val unit: String,
            val expectedValue: Int,
            val expectedUnit: Period.Unit,
            val expectedIso8601: String,
        )

        val expectedPeriods = listOf(
            ExpectedPeriod(
                multiplier = "1YEAR",
                unit = "YEAR",
                expectedValue = 1,
                expectedUnit = Period.Unit.YEAR,
                expectedIso8601 = "P1Y",
            ),
            ExpectedPeriod(
                multiplier = "6month",
                unit = "month",
                expectedValue = 6,
                expectedUnit = Period.Unit.MONTH,
                expectedIso8601 = "P6M",
            ),
            ExpectedPeriod(
                multiplier = "3Week",
                unit = "Week",
                expectedValue = 3,
                expectedUnit = Period.Unit.WEEK,
                expectedIso8601 = "P3W",
            ),
        )

        expectedPeriods.forEach { expectation ->
            val storeProduct = createProductVo(
                type = "subscription",
                subscriptionDurationMultiplier = expectation.multiplier,
                subscriptionDurationUnit = expectation.unit,
            ).toStoreProduct()

            val period = storeProduct.period
            assertThat(period)
                .describedAs("subscriptionDurationMultiplier=%s", expectation.multiplier)
                .isNotNull
            assertThat(period!!.value).isEqualTo(expectation.expectedValue)
            assertThat(period.unit).isEqualTo(expectation.expectedUnit)
            assertThat(period.iso8601).isEqualTo(expectation.expectedIso8601)
        }
    }

    @Test
    fun `toStoreProduct returns null period when multiplier has no leading number`() {
        val storeProduct = createProductVo(
            type = "subscription",
            subscriptionDurationMultiplier = "MONTH6",
            subscriptionDurationUnit = "MONTH",
        ).toStoreProduct()

        assertThat(storeProduct.period).isNull()
    }

    @Test
    fun `toStoreProduct returns null period when multiplier is non numeric`() {
        val storeProduct = createProductVo(
            type = "subscription",
            subscriptionDurationMultiplier = "abcMONTH",
            subscriptionDurationUnit = "MONTH",
        ).toStoreProduct()

        assertThat(storeProduct.period).isNull()
    }

    @Test
    fun `toStoreProduct returns null period when duration unit is unsupported`() {
        val storeProduct = createProductVo(
            type = "subscription",
            subscriptionDurationMultiplier = "3MONTH",
            subscriptionDurationUnit = "DAY",
        ).toStoreProduct()

        assertThat(storeProduct.period).isNull()
    }

    @Test
    fun `toStoreProduct returns null period when duration unit maps to unknown`() {
        val storeProduct = createProductVo(
            type = "subscription",
            subscriptionDurationMultiplier = "2UNKNOWN",
            subscriptionDurationUnit = "UNKNOWN",
        ).toStoreProduct()

        assertThat(storeProduct.period).isNull()
    }

    @Test
    fun `toStoreProduct returns null period when subscription duration is missing`() {
        val storeProduct = createProductVo(
            type = "subscription",
            subscriptionDurationMultiplier = "",
            subscriptionDurationUnit = "",
        ).toStoreProduct()

        assertThat(storeProduct.period).isNull()
    }

    // endregion
}