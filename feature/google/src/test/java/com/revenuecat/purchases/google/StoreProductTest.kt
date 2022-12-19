package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.utils.mockProductDetails
import com.revenuecat.purchases.utils.mockSubscriptionOfferDetails
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreProductTest {
    @Test
    fun `list of INAPP ProductDetails maps to StoreProducts`() {
        val productDetail1 = mockProductDetails(productId = "iap_1", type = BillingClient.ProductType.INAPP, subscriptionOfferDetails = null)
        val productDetails = listOf(productDetail1)

        val storeProducts = productDetails.toStoreProducts()
        assertThat(storeProducts.size).isEqualTo(1)
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
}
