package com.revenuecat.purchases.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GooglePurchasingDataTest {

    private val inApp = GooglePurchasingData.InAppProduct(productId = "123", productDetails = mockk())
    private val subscription = GooglePurchasingData.Subscription(
        productId = "456",
        optionId = "abc",
        productDetails = mockk(),
        token = "abc",
    )

    @Test
    fun `productType is INAPP for ProductWithAddOns with InAppProduct baseProduct`() {
        val productWithAddOns = GooglePurchasingData.ProductWithAddOns(
            productId = "123|456",
            baseProduct = inApp,
            addOnProducts = listOf(subscription),
            replacementMode = GoogleReplacementMode.DEFERRED,
        )

        assertThat(productWithAddOns.productType).isEqualTo(ProductType.INAPP)
    }

    @Test
    fun `productType is SUBS for ProductWithAddOns with Subscription baseProduct`() {
        val productWithAddOns = GooglePurchasingData.ProductWithAddOns(
            productId = "456|123",
            baseProduct = subscription,
            addOnProducts = listOf(inApp),
            replacementMode = GoogleReplacementMode.DEFERRED,
        )

        assertThat(productWithAddOns.productType).isEqualTo(ProductType.SUBS)
    }
}
