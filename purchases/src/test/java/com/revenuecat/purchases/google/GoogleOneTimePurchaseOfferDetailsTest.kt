package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.models.GoogleOneTimePurchaseOfferDetails
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.utils.mockProductDetails
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleOneTimePurchaseOfferDetailsTest {

    private val productId = "product-id"
    private val offerId = "offer-id"
    private val offerToken = "mock-offer-token"
    private val price = Price(
        formatted = "$4.99",
        amountMicros = 4990000,
        currencyCode = "USD"
    )

    @Test
    fun `GoogleOneTimePurchaseOfferDetails stores properties correctly`() {
        val productDetails = mockProductDetails()
        val presentedOfferingContext = PresentedOfferingContext("offering_a")

        val offerDetails = GoogleOneTimePurchaseOfferDetails(
            productId = productId,
            price = price,
            offerId = offerId,
            offerToken = offerToken,
            offerTags = listOf("tag1", "tag2"),
            productDetails = productDetails,
            presentedOfferingContext = presentedOfferingContext
        )

        assertThat(offerDetails.productId).isEqualTo(productId)
        assertThat(offerDetails.price).isEqualTo(price)
        assertThat(offerDetails.offerId).isEqualTo(offerId)
        assertThat(offerDetails.offerToken).isEqualTo(offerToken)
        assertThat(offerDetails.offerTags).containsExactly("tag1", "tag2")
        assertThat(offerDetails.productDetails).isEqualTo(productDetails)
        assertThat(offerDetails.presentedOfferingContext).isEqualTo(presentedOfferingContext)
        assertThat(offerDetails.presentedOfferingContext?.offeringIdentifier).isEqualTo("offering_a")
    }

    @Test
    fun `GoogleOneTimePurchaseOfferDetails purchasingData returns GooglePurchasingData InAppProduct with selectedOfferToken`() {
        val productDetails = mockProductDetails()
        val offerDetails = GoogleOneTimePurchaseOfferDetails(
            productId = productId,
            price = price,
            offerId = offerId,
            offerToken = offerToken,
            offerTags = emptyList(),
            productDetails = productDetails,
            presentedOfferingContext = null
        )

        val purchasingData = offerDetails.purchasingData
        assertThat(purchasingData).isInstanceOf(GooglePurchasingData.InAppProduct::class.java)
        val inAppPurchasingData = purchasingData as GooglePurchasingData.InAppProduct
        assertThat(inAppPurchasingData.productId).isEqualTo(productId)
        assertThat(inAppPurchasingData.productDetails).isEqualTo(productDetails)
        assertThat(inAppPurchasingData.selectedOfferToken).isEqualTo(offerToken)
    }

    @Test
    fun `GoogleOneTimePurchaseOfferDetails internal copy constructor updates presentedOfferingContext`() {
        val productDetails = mockProductDetails()
        val initialContext = PresentedOfferingContext("offering_a")
        val newContext = PresentedOfferingContext("offering_b")

        val original = GoogleOneTimePurchaseOfferDetails(
            productId = productId,
            price = price,
            offerId = offerId,
            offerToken = offerToken,
            offerTags = listOf("tag1"),
            productDetails = productDetails,
            presentedOfferingContext = initialContext
        )

        val copy = GoogleOneTimePurchaseOfferDetails(original, newContext)

        assertThat(copy.productId).isEqualTo(productId)
        assertThat(copy.price).isEqualTo(price)
        assertThat(copy.offerId).isEqualTo(offerId)
        assertThat(copy.offerToken).isEqualTo(offerToken)
        assertThat(copy.offerTags).containsExactly("tag1")
        assertThat(copy.productDetails).isEqualTo(productDetails)
        assertThat(copy.presentedOfferingContext).isEqualTo(newContext)
        assertThat(copy.presentedOfferingContext?.offeringIdentifier).isEqualTo("offering_b")
    }

    @Test
    fun `Two GoogleOneTimePurchaseOfferDetails with same properties are equal and have same hashCode`() {
        val productDetails = mockProductDetails()

        val offer1 = GoogleOneTimePurchaseOfferDetails(
            productId = productId,
            price = price,
            offerId = offerId,
            offerToken = offerToken,
            offerTags = listOf("tag1"),
            productDetails = productDetails,
            presentedOfferingContext = null
        )

        val offer2 = GoogleOneTimePurchaseOfferDetails(
            productId = productId,
            price = price,
            offerId = offerId,
            offerToken = offerToken,
            offerTags = listOf("tag1"),
            productDetails = productDetails,
            presentedOfferingContext = null
        )

        assertThat(offer1).isEqualTo(offer2)
        assertThat(offer1.hashCode()).isEqualTo(offer2.hashCode())
    }
}
