package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.GoogleDiscountDisplayInfo
import com.revenuecat.purchases.models.GoogleOneTimePurchaseOfferDetails
import com.revenuecat.purchases.models.OneTimePurchaseOfferDetailsList
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.utils.mockProductDetails
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OneTimePurchaseOfferDetailsListTest {

    private val productId = "product-id"

    private fun createOffer(
        offerId: String,
        amountMicros: Long,
        tags: List<String> = emptyList()
    ) = GoogleOneTimePurchaseOfferDetails(
        productId = productId,
        price = Price(formatted = "$$amountMicros", amountMicros = amountMicros, currencyCode = "USD"),
        offerId = offerId,
        offerToken = "token-$offerId",
        offerTags = tags,
        productDetails = mockProductDetails(),
        presentedOfferingContext = null
    )

    @Test
    fun `defaultOffer selects cheapest offer`() {
        val expensiveOffer = createOffer("expensive", 9990000)
        val cheapestOffer = createOffer("cheapest", 1990000)
        val midOffer = createOffer("mid", 4990000)

        val list = OneTimePurchaseOfferDetailsList(listOf(expensiveOffer, cheapestOffer, midOffer))

        assertThat(list.defaultOffer).isEqualTo(cheapestOffer)
    }

    @Test
    fun `defaultOffer filters out rc-ignore-offer tag`() {
        val cheapIgnoredOffer = createOffer("cheap-ignored", 1990000, listOf("rc-ignore-offer"))
        val validOffer = createOffer("valid", 4990000)

        val list = OneTimePurchaseOfferDetailsList(listOf(cheapIgnoredOffer, validOffer))

        assertThat(list.defaultOffer).isEqualTo(validOffer)
    }

    @Test
    fun `defaultOffer filters out rc-customer-center tag`() {
        val cheapCustomerCenterOffer = createOffer("cheap-cc", 1990000, listOf("rc-customer-center"))
        val validOffer = createOffer("valid", 4990000)

        val list = OneTimePurchaseOfferDetailsList(listOf(cheapCustomerCenterOffer, validOffer))

        assertThat(list.defaultOffer).isEqualTo(validOffer)
    }

    @Test
    fun `defaultOffer returns null when all offers are ignored`() {
        val ignoredOffer1 = createOffer("ignored-1", 1990000, listOf("rc-ignore-offer"))
        val ignoredOffer2 = createOffer("ignored-2", 4990000, listOf("rc-customer-center"))

        val list = OneTimePurchaseOfferDetailsList(listOf(ignoredOffer1, ignoredOffer2))

        assertThat(list.defaultOffer).isNull()
    }

    @Test
    fun `defaultOffer returns null for empty list`() {
        val list = OneTimePurchaseOfferDetailsList(emptyList())

        assertThat(list.defaultOffer).isNull()
    }

    @Test
    fun `withTag filters offers correctly`() {
        val offerWithTagA = createOffer("offer-a", 1990000, listOf("promo", "tagA"))
        val offerWithTagB = createOffer("offer-b", 4990000, listOf("tagB"))
        val offerWithBoth = createOffer("offer-c", 9990000, listOf("tagA", "tagB"))

        val list = OneTimePurchaseOfferDetailsList(listOf(offerWithTagA, offerWithTagB, offerWithBoth))

        assertThat(list.withTag("tagA")).containsExactly(offerWithTagA, offerWithBoth)
        assertThat(list.withTag("tagB")).containsExactly(offerWithTagB, offerWithBoth)
        assertThat(list.withTag("nonexistent")).isEmpty()
    }

    @Test
    fun `basePlan returns offer detail with null discountDisplayInfo`() {
        val discountInfo = GoogleDiscountDisplayInfo(percentageDiscount = 20)
        val discountedOffer = GoogleOneTimePurchaseOfferDetails(
            productId = productId,
            price = Price(formatted = "$2.99", amountMicros = 2990000, currencyCode = "USD"),
            offerId = "discounted",
            offerToken = "token-discounted",
            offerTags = emptyList(),
            discountDisplayInfo = discountInfo,
            productDetails = mockProductDetails(),
            presentedOfferingContext = null
        )
        val basePlanOffer = GoogleOneTimePurchaseOfferDetails(
            productId = productId,
            price = Price(formatted = "$4.99", amountMicros = 4990000, currencyCode = "USD"),
            offerId = null,
            offerToken = "token-base",
            offerTags = emptyList(),
            discountDisplayInfo = null,
            productDetails = mockProductDetails(),
            presentedOfferingContext = null
        )

        val list = OneTimePurchaseOfferDetailsList(listOf(discountedOffer, basePlanOffer))

        assertThat(list.basePlan).isEqualTo(basePlanOffer)
    }

    @Test
    fun `equals and hashCode work as expected`() {
        val offer1 = createOffer("offer-1", 1990000)
        val offer2 = createOffer("offer-2", 4990000)

        val list1 = OneTimePurchaseOfferDetailsList(listOf(offer1, offer2))
        val list2 = OneTimePurchaseOfferDetailsList(listOf(offer1, offer2))

        assertThat(list1).isEqualTo(list2)
        assertThat(list1.hashCode()).isEqualTo(list2.hashCode())
    }
}
