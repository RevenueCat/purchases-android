package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.device.iap.internal.model.ProductBuilder
import com.amazon.device.iap.model.ProductType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ProductConverterTest {

    @Test
    fun `priceAmountMicros is correctly calculated`() {
        val product = ProductBuilder()
            .setSku("sku")
            .setProductType(ProductType.SUBSCRIPTION)
            .setDescription("description")
            .setPrice("$39.99")
            .setSmallIconUrl("http://smalliconurl")
            .setTitle("title")
            .setCoinsRewardAmount(0).build()
        val productDetails = product.toProductDetails("US")
        assertThat(productDetails.priceAmountMicros).isEqualTo(39_990_000)
    }
}
