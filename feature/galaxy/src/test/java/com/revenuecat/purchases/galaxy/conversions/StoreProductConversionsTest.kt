package com.revenuecat.purchases.galaxy.conversions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.galaxy.GalaxyPurchasingData
import com.revenuecat.purchases.galaxy.GalaxyStoreTest
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.RecurrenceMode
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
    fun `toStoreProduct sets presentedOfferingContext to null`() {
        val storeProduct = createProductVo().toStoreProduct()

        assertThat(storeProduct.presentedOfferingContext).isNull()
    }

    @Test
    fun `toStoreProduct sets subscriptionOptions and defaultOption to null for INAPP products`() {
        val storeProduct = createProductVo(type = "item").toStoreProduct()

        assertThat(storeProduct.subscriptionOptions).isNull()
        assertThat(storeProduct.defaultOption).isNull()
    }

    @Test
    fun `toStoreProduct creates subscriptionOptions and defaultOption for subscriptions with parsable period`() {
        val productId = "sub_product"
        val storeProduct = createProductVo(
            itemId = productId,
            type = "subscription",
            subscriptionDurationMultiplier = "1MONTH",
            subscriptionDurationUnit = "MONTH",
        ).toStoreProduct()

        assertThat(storeProduct.type).isEqualTo(ProductType.SUBS)
        assertThat(storeProduct.period).isNotNull

        val subscriptionOptions = storeProduct.subscriptionOptions
        assertThat(subscriptionOptions).isNotNull
        assertThat(subscriptionOptions!!).hasSize(1)

        val option = subscriptionOptions.first()
        assertThat(option.id).isEqualTo(productId)
        assertThat(option.tags).isEmpty()
        assertThat(option.presentedOfferingContext).isNull()
        assertThat(option.purchasingData).isEqualTo(
            GalaxyPurchasingData.Product(
                productId = productId,
                productType = ProductType.SUBS,
            ),
        )

        assertThat(storeProduct.defaultOption).isSameAs(option)
    }

    @Test
    fun `toStoreProduct keeps subscriptionOptions null for subscriptions with unparseable period`() {
        val storeProduct = createProductVo(
            type = "subscription",
            subscriptionDurationMultiplier = "MONTH6",
            subscriptionDurationUnit = "MONTH",
        ).toStoreProduct()

        assertThat(storeProduct.period).isNull()
        assertThat(storeProduct.subscriptionOptions).isNull()
        assertThat(storeProduct.defaultOption).isNull()
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
    fun `toStoreProduct uses itemPriceString even when decimals are missing`() {
        val productVo = createProductVo(
            itemPrice = 3.0,
            currencyUnit = "$",
            currencyCode = "USD",
            itemPriceString = "$3",
        )

        val storeProduct = productVo.toStoreProduct()

        assertThat(storeProduct.price.formatted).isEqualTo("$3")
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
    fun `toStoreProduct keeps itemPriceString formatting even with extra decimals`() {
        val productVo = createProductVo(
            itemPrice = 1.2345,
            currencyUnit = "$",
            currencyCode = "USD",
        )

        val storeProduct = productVo.toStoreProduct()

        assertThat(storeProduct.price.formatted).isEqualTo("$1.2345")
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

    // region toStoreProduct pricing phases (subscriptions)

    @Test
    fun `toStoreProduct creates a single pricing phase for a sub when there are no promotion eligibilities`() {
        val storeProduct = createProductVo(
            type = "subscription",
            itemPrice = 3.0,
            currencyUnit = "$",
            currencyCode = "USD",
            subscriptionDurationMultiplier = "1MONTH",
            subscriptionDurationUnit = "MONTH",
        ).toStoreProduct(promotionEligibilities = null)

        val pricingPhases = storeProduct.defaultOption!!.pricingPhases
        assertThat(pricingPhases).hasSize(1)

        val normalPhase = pricingPhases.single()
        assertThat(normalPhase.billingPeriod).isEqualTo(storeProduct.period)
        assertThat(normalPhase.recurrenceMode).isEqualTo(RecurrenceMode.INFINITE_RECURRING)
        assertThat(normalPhase.billingCycleCount).isNull()
        assertThat(normalPhase.price).isEqualTo(storeProduct.price)
    }

    @Test
    fun `toStoreProduct adds a free trial pricing phase for a sub before the normal phase when eligible`() {
        val productId = "trial_sub"
        val promotionEligibilities = listOf(
            createPromotionEligibilityVo(itemId = productId, pricing = "FreeTrial"),
        )

        val storeProduct = createProductVo(
            itemId = productId,
            type = "subscription",
            itemPrice = 5.0,
            currencyUnit = "$",
            currencyCode = "USD",
            freeTrialPeriod = "7",
            subscriptionDurationMultiplier = "1MONTH",
            subscriptionDurationUnit = "MONTH",
        ).toStoreProduct(promotionEligibilities = promotionEligibilities)

        val pricingPhases = storeProduct.defaultOption!!.pricingPhases
        assertThat(pricingPhases).hasSize(2)

        val trialPhase = pricingPhases.first()
        assertThat(trialPhase.billingPeriod).isEqualTo(
            Period(
                value = 7,
                unit = Period.Unit.DAY,
                iso8601 = "P7D",
            ),
        )
        assertThat(trialPhase.recurrenceMode).isEqualTo(RecurrenceMode.NON_RECURRING)
        assertThat(trialPhase.billingCycleCount).isNull()
        assertThat(trialPhase.price.amountMicros).isEqualTo(0L)
        assertThat(trialPhase.price.currencyCode).isEqualTo("USD")
        assertThat(trialPhase.price.formatted).isEqualTo("$0.00")

        val normalPhase = pricingPhases.last()
        assertThat(normalPhase.billingPeriod).isEqualTo(storeProduct.period)
        assertThat(normalPhase.recurrenceMode).isEqualTo(RecurrenceMode.INFINITE_RECURRING)
        assertThat(normalPhase.price).isEqualTo(storeProduct.price)
    }

    @Test
    fun `toStoreProduct ignores free trial eligibility for sub when freeTrialPeriod cannot be parsed`() {
        val productId = "trial_bad_period"
        val promotionEligibilities = listOf(
            createPromotionEligibilityVo(itemId = productId, pricing = "FreeTrial"),
        )

        val storeProduct = createProductVo(
            itemId = productId,
            type = "subscription",
            freeTrialPeriod = "_-_-_-_-_asdf_-_-_-_-_",
            subscriptionDurationMultiplier = "1MONTH",
            subscriptionDurationUnit = "MONTH",
        ).toStoreProduct(promotionEligibilities = promotionEligibilities)

        val pricingPhases = storeProduct.defaultOption!!.pricingPhases
        assertThat(pricingPhases).hasSize(1)
        assertThat(pricingPhases.single().recurrenceMode).isEqualTo(RecurrenceMode.INFINITE_RECURRING)
    }

    @Test
    fun `toStoreProduct ignores promotion eligibilities for different products`() {
        val storeProduct = createProductVo(
            itemId = "sub_a",
            type = "subscription",
            freeTrialPeriod = "7",
            subscriptionDurationMultiplier = "1MONTH",
            subscriptionDurationUnit = "MONTH",
        ).toStoreProduct(
            promotionEligibilities = listOf(
                createPromotionEligibilityVo(itemId = "sub_b", pricing = "FreeTrial"),
            ),
        )

        val pricingPhases = storeProduct.defaultOption!!.pricingPhases
        assertThat(pricingPhases).hasSize(1)
    }

    @Test
    fun `toStoreProduct ignores tiered pricing eligibility and only includes the normal phase`() {
        val productId = "tiered_sub"
        val storeProduct = createProductVo(
            itemId = productId,
            type = "subscription",
            subscriptionDurationMultiplier = "1MONTH",
            subscriptionDurationUnit = "MONTH",
        ).toStoreProduct(
            promotionEligibilities = listOf(
                createPromotionEligibilityVo(itemId = productId, pricing = "TieredPrice"),
            ),
        )

        val pricingPhases = storeProduct.defaultOption!!.pricingPhases
        assertThat(pricingPhases).hasSize(1)
    }

    @Test
    fun `toStoreProduct includes free trial phase when both free trial and tiered pricing are present`() {
        val productId = "trial_and_tiered"
        val storeProduct = createProductVo(
            itemId = productId,
            type = "subscription",
            freeTrialPeriod = "14",
            subscriptionDurationMultiplier = "1MONTH",
            subscriptionDurationUnit = "MONTH",
        ).toStoreProduct(
            promotionEligibilities = listOf(
                createPromotionEligibilityVo(itemId = productId, pricing = "FreeTrial"),
                createPromotionEligibilityVo(itemId = productId, pricing = "TieredPrice"),
            ),
        )

        val pricingPhases = storeProduct.defaultOption!!.pricingPhases
        assertThat(pricingPhases).hasSize(2)
        assertThat(pricingPhases.first().price.amountMicros).isEqualTo(0L)
        assertThat(pricingPhases.last().recurrenceMode).isEqualTo(RecurrenceMode.INFINITE_RECURRING)
    }

    @Test
    fun `toStoreProduct adds tiered pricing phase before normal phase when user is eligible`() {
        val productId = "tiered_with_data"
        val storeProduct = createProductVo(
            itemId = productId,
            type = "subscription",
            itemPrice = 9.99,
            currencyUnit = "$",
            currencyCode = "USD",
            subscriptionDurationMultiplier = "1MONTH",
            subscriptionDurationUnit = "MONTH",
            tieredSubscriptionYN = "Y",
            tieredPrice = "4.99",
            tieredPriceString = "$4.99",
            tieredSubscriptionCount = "2",
            tieredSubscriptionDurationMultiplier = "1MONTH",
            tieredSubscriptionDurationUnit = "MONTH",
        ).toStoreProduct(
            promotionEligibilities = listOf(
                createPromotionEligibilityVo(itemId = productId, pricing = "TieredPrice"),
            ),
        )

        val pricingPhases = storeProduct.defaultOption!!.pricingPhases
        assertThat(pricingPhases).hasSize(2)

        val tieredPhase = pricingPhases.first()
        assertThat(tieredPhase.billingPeriod).isEqualTo(
            Period(
                value = 1,
                unit = Period.Unit.MONTH,
                iso8601 = "P1M",
            ),
        )
        assertThat(tieredPhase.recurrenceMode).isEqualTo(RecurrenceMode.FINITE_RECURRING)
        assertThat(tieredPhase.billingCycleCount).isEqualTo(2)
        assertThat(tieredPhase.price.formatted).isEqualTo("$4.99")
        assertThat(tieredPhase.price.amountMicros).isEqualTo(4_990_000)

        val normalPhase = pricingPhases.last()
        assertThat(normalPhase.billingPeriod).isEqualTo(storeProduct.period)
        assertThat(normalPhase.recurrenceMode).isEqualTo(RecurrenceMode.INFINITE_RECURRING)
        assertThat(normalPhase.price.formatted).isEqualTo("$9.99")
    }

    @Test
    fun `toStoreProduct adds free trial and tiered phases when eligible for both`() {
        val productId = "trial_and_tiered_with_data"
        val storeProduct = createProductVo(
            itemId = productId,
            type = "subscription",
            itemPrice = 12.99,
            currencyUnit = "$",
            currencyCode = "USD",
            freeTrialPeriod = "14",
            subscriptionDurationMultiplier = "1MONTH",
            subscriptionDurationUnit = "MONTH",
            tieredSubscriptionYN = "Y",
            tieredPrice = "6.99",
            tieredSubscriptionCount = "5",
            tieredSubscriptionDurationMultiplier = "1MONTH",
            tieredSubscriptionDurationUnit = "MONTH",
        ).toStoreProduct(
            promotionEligibilities = listOf(
                createPromotionEligibilityVo(itemId = productId, pricing = "FreeTrial"),
                createPromotionEligibilityVo(itemId = productId, pricing = "TieredPrice"),
            ),
        )

        val pricingPhases = storeProduct.defaultOption!!.pricingPhases
        assertThat(pricingPhases).hasSize(3)

        val trialPhase = pricingPhases[0]
        assertThat(trialPhase.price.amountMicros).isEqualTo(0)
        assertThat(trialPhase.billingPeriod).isEqualTo(
            Period(
                value = 14,
                unit = Period.Unit.DAY,
                iso8601 = "P14D",
            ),
        )
        assertThat(trialPhase.recurrenceMode).isEqualTo(RecurrenceMode.NON_RECURRING)

        val tieredPhase = pricingPhases[1]
        assertThat(tieredPhase.recurrenceMode).isEqualTo(RecurrenceMode.FINITE_RECURRING)
        assertThat(tieredPhase.billingCycleCount).isEqualTo(5)
        assertThat(tieredPhase.price.formatted).isEqualTo("$6.99")

        val normalPhase = pricingPhases[2]
        assertThat(normalPhase.recurrenceMode).isEqualTo(RecurrenceMode.INFINITE_RECURRING)
        assertThat(normalPhase.price.formatted).isEqualTo("$12.99")
    }

    @Test
    fun `toStoreProduct assumes tiered pricing eligibility when free trial is eligible and product supports tiering`() {
        val productId = "trial_and_tiered_implied"
        val storeProduct = createProductVo(
            itemId = productId,
            type = "subscription",
            itemPrice = 7.99,
            currencyUnit = "$",
            currencyCode = "USD",
            freeTrialPeriod = "7",
            subscriptionDurationMultiplier = "1MONTH",
            subscriptionDurationUnit = "MONTH",
            tieredSubscriptionYN = "Y",
            tieredPrice = "2.99",
            tieredSubscriptionCount = "3",
            tieredSubscriptionDurationMultiplier = "1MONTH",
            tieredSubscriptionDurationUnit = "MONTH",
        ).toStoreProduct(
            promotionEligibilities = listOf(
                createPromotionEligibilityVo(itemId = productId, pricing = "FreeTrial"),
            ),
        )

        val pricingPhases = storeProduct.defaultOption!!.pricingPhases
        assertThat(pricingPhases).hasSize(3)

        val trialPhase = pricingPhases[0]
        assertThat(trialPhase.recurrenceMode).isEqualTo(RecurrenceMode.NON_RECURRING)
        assertThat(trialPhase.billingPeriod).isEqualTo(
            Period(
                value = 7,
                unit = Period.Unit.DAY,
                iso8601 = "P7D",
            ),
        )
        assertThat(trialPhase.price.amountMicros).isEqualTo(0L)

        val tieredPhase = pricingPhases[1]
        assertThat(tieredPhase.recurrenceMode).isEqualTo(RecurrenceMode.FINITE_RECURRING)
        assertThat(tieredPhase.billingCycleCount).isEqualTo(3)
        assertThat(tieredPhase.billingPeriod).isEqualTo(
            Period(
                value = 1,
                unit = Period.Unit.MONTH,
                iso8601 = "P1M",
            ),
        )
        assertThat(tieredPhase.price.formatted).isEqualTo("$2.99")
        assertThat(tieredPhase.price.amountMicros).isEqualTo(2_990_000)

        val normalPhase = pricingPhases[2]
        assertThat(normalPhase.recurrenceMode).isEqualTo(RecurrenceMode.INFINITE_RECURRING)
        assertThat(normalPhase.billingPeriod).isEqualTo(storeProduct.period)
        assertThat(normalPhase.price.formatted).isEqualTo("$7.99")
    }

    @Test
    fun `toStoreProduct skips tiered pricing when data cannot be parsed`() {
        val productId = "tiered_with_invalid_data"
        val storeProduct = createProductVo(
            itemId = productId,
            type = "subscription",
            subscriptionDurationMultiplier = "1MONTH",
            subscriptionDurationUnit = "MONTH",
            tieredSubscriptionYN = "Y",
            tieredPrice = "not_a_price",
            tieredSubscriptionCount = "two",
            tieredSubscriptionDurationMultiplier = "1MONTH",
            tieredSubscriptionDurationUnit = "MONTH",
        ).toStoreProduct(
            promotionEligibilities = listOf(
                createPromotionEligibilityVo(itemId = productId, pricing = "TieredPrice"),
            ),
        )

        val pricingPhases = storeProduct.defaultOption!!.pricingPhases
        assertThat(pricingPhases).hasSize(1)
        assertThat(pricingPhases.single().recurrenceMode).isEqualTo(RecurrenceMode.INFINITE_RECURRING)
    }

    // endregion
}
