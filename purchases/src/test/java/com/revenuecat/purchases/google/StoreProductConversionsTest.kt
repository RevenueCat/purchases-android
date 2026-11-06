package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.models.googleProduct
import com.revenuecat.purchases.utils.mockOneTimePurchaseOfferDetails
import com.revenuecat.purchases.utils.mockPricingPhase
import com.revenuecat.purchases.utils.mockProductDetails
import com.revenuecat.purchases.utils.mockSubscriptionOfferDetails
import io.mockk.every
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreProductConversionsTest {
    @Test
    fun `list of invalid ProductDetails with no price maps to empty list`() {
        val productDetail1 = mockProductDetails(
            productId = "iap_1",
            type = BillingClient.ProductType.INAPP,
            oneTimePurchaseOfferDetails = null,
            subscriptionOfferDetails = null)
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()
        assertThat(storeProducts.size).isEqualTo(0)
    }

    @Test
    fun `list of INAPP ProductDetails maps to StoreProducts`() {
        val productDetail1 = mockProductDetails(
            productId = "iap_1",
            type = BillingClient.ProductType.INAPP,
            oneTimePurchaseOfferDetails = mockOneTimePurchaseOfferDetails(),
            subscriptionOfferDetails = null)
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()
        assertThat(storeProducts.size).isEqualTo(1)
    }

    @Test
    fun `list of INAPP ProductDetails with oneTimePurchaseOfferDetails maps defaultOneTimeOffer and oneTimePurchaseOfferDetailsList`() {
        val offerDetails = mockOneTimePurchaseOfferDetails(offerTokenProvided = "mock-offer-token")
        val productDetail1 = mockProductDetails(
            productId = "iap_1",
            type = BillingClient.ProductType.INAPP,
            oneTimePurchaseOfferDetails = offerDetails,
            subscriptionOfferDetails = null
        )

        val storeProducts = listOf(productDetail1).toStoreProducts()
        assertThat(storeProducts.size).isEqualTo(1)
        val googleStoreProduct = storeProducts.first().googleProduct
        assertThat(googleStoreProduct).isNotNull
        assertThat(googleStoreProduct?.oneTimePurchaseOfferDetailsList).isNotNull
        assertThat(googleStoreProduct?.defaultOneTimeOffer).isNotNull
        assertThat(googleStoreProduct?.defaultOneTimeOffer?.offerToken).isEqualTo("mock-offer-token")
    }

    @Test
    fun `INAPP ProductDetails with null singular oneTimePurchaseOfferDetails but valid oneTimePurchaseOfferDetailsList maps to StoreProduct`() {
        val offerDetails = mockOneTimePurchaseOfferDetails(price = 4.99, offerTokenProvided = "list-token")
        val productDetail1 = mockProductDetails(
            productId = "iap_1",
            type = BillingClient.ProductType.INAPP,
            oneTimePurchaseOfferDetails = null,
            subscriptionOfferDetails = null
        ).apply {
            every { oneTimePurchaseOfferDetailsList } returns listOf(offerDetails)
        }

        val storeProducts = listOf(productDetail1).toStoreProducts()
        assertThat(storeProducts.size).isEqualTo(1)
        val storeProduct = storeProducts.first()
        assertThat(storeProduct.price.amountMicros).isEqualTo(4990000)
        assertThat(storeProduct.googleProduct?.defaultOneTimeOffer?.offerToken).isEqualTo("list-token")
    }

    @Test
    fun `list of SUBS ProductDetails maps to StoreProducts`() {
        val productDetail1 = mockProductDetails(productId = "sub_1", type = BillingClient.ProductType.SUBS)
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()
        assertThat(storeProducts.size).isEqualTo(1)
    }

    @Test
    fun `list of INAPP and SUBS ProductDetails maps to StoreProducts`() {
        val productDetail1 = mockProductDetails(
            productId = "iap_1",
            type = BillingClient.ProductType.INAPP,
            oneTimePurchaseOfferDetails = mockOneTimePurchaseOfferDetails(),
            subscriptionOfferDetails = null)
        val productDetail2 = mockProductDetails(
            productId = "sub_1",
            type = BillingClient.ProductType.SUBS)
        val productDetails = listOf(productDetail1, productDetail2)

        val storeProducts = productDetails.toStoreProducts()
        assertThat(storeProducts.size).isEqualTo(2)
    }

    @Test
    fun `list of SUBS ProductDetails with multiple subscription offers maps to multiple StoreProducts`() {
        val monthlyBasePlan = mockSubscriptionOfferDetails(offerId = "monthly-offer", basePlanId = "monthly")
        val yearlyBasePlan = mockSubscriptionOfferDetails(offerId = "yearly-offer", basePlanId = "yearly")

        val productDetail1 = mockProductDetails(
            productId = "sub_1",
            type = BillingClient.ProductType.SUBS,
            subscriptionOfferDetails = listOf(monthlyBasePlan, yearlyBasePlan))
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()
        assertThat(storeProducts.size).isEqualTo(2)
    }

    @Test
    fun `list of SUB ProductDetails allows recurring and prepaid with same billing cycle`() {
        val monthlyBasePlan = mockSubscriptionOfferDetails(
            offerId = "",
            basePlanId = "monthly",
            pricingPhases = listOf(mockPricingPhase(
                billingPeriod = "P1M",
                recurrenceMode = 1
            ))
        )
        val yearlyBasePlan = mockSubscriptionOfferDetails(
            offerId = "",
            basePlanId = "yearly",
            pricingPhases = listOf(mockPricingPhase(
                billingPeriod = "P1Y",
                recurrenceMode = 1
            ))
        )
        val yearlyPrepaidBasePlan = mockSubscriptionOfferDetails(
            offerId = "",
            basePlanId = "yearly-prepaid",
            pricingPhases = listOf(mockPricingPhase(
                billingPeriod = "P1Y",
                recurrenceMode = 3
            ))
        )


        val productDetail1 = mockProductDetails(
            productId = "sub_1",
            type = BillingClient.ProductType.SUBS,
            subscriptionOfferDetails = listOf(monthlyBasePlan, yearlyBasePlan, yearlyPrepaidBasePlan))
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()

        // Verifies that all the StoreProducts created
        val googleStoreProducts = storeProducts.map { it.googleProduct!! }
        assertThat(googleStoreProducts[0].basePlanId).isEqualTo("monthly")
        assertThat(googleStoreProducts[1].basePlanId).isEqualTo("yearly")
        assertThat(googleStoreProducts[2].basePlanId).isEqualTo("yearly-prepaid")
    }
}
