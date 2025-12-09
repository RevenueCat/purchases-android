package com.revenuecat.purchases.galaxy

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.samsung.android.sdk.iap.lib.vo.ProductVo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreProductConversionsTest {

    // region createRevenueCatProductTypeFromSamsungIAPTypeString
    @Test
    fun `createRevenueCatProductTypeFromSamsungIAPTypeString maps item to INAPP`() {
        val productType = "item".createRevenueCatProductTypeFromSamsungIAPTypeString()

        assertThat(productType).isEqualTo(ProductType.INAPP)
    }

    @Test
    fun `createRevenueCatProductTypeFromSamsungIAPTypeString maps subscription to SUBS`() {
        val productType = "subscription".createRevenueCatProductTypeFromSamsungIAPTypeString()

        assertThat(productType).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `createRevenueCatProductTypeFromSamsungIAPTypeString handles uppercase values`() {
        val upperCaseItem = "ITEM".createRevenueCatProductTypeFromSamsungIAPTypeString()
        val upperCaseSubscription = "SUBSCRIPTION".createRevenueCatProductTypeFromSamsungIAPTypeString()

        assertThat(upperCaseItem).isEqualTo(ProductType.INAPP)
        assertThat(upperCaseSubscription).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `createRevenueCatProductTypeFromSamsungIAPTypeString handles mixed case values`() {
        val mixedCaseItem = "ItEm".createRevenueCatProductTypeFromSamsungIAPTypeString()
        val mixedCaseSubscription = "SubScription".createRevenueCatProductTypeFromSamsungIAPTypeString()

        assertThat(mixedCaseItem).isEqualTo(ProductType.INAPP)
        assertThat(mixedCaseSubscription).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `createRevenueCatProductTypeFromSamsungIAPTypeString returns UNKNOWN for unexpected value`() {
        val productType = "unknown-type".createRevenueCatProductTypeFromSamsungIAPTypeString()

        assertThat(productType).isEqualTo(ProductType.UNKNOWN)
    }

    @Test
    fun `createRevenueCatProductTypeFromSamsungIAPTypeString returns UNKNOWN for empty string`() {
        val productType = "".createRevenueCatProductTypeFromSamsungIAPTypeString()

        assertThat(productType).isEqualTo(ProductType.UNKNOWN)
    }

    // endregion

    // region createPrice

    @Test
    fun `createPrice builds formatted string with two decimals when itemPriceString omits them`() {
        val productVo = createProductVo(
            itemPrice = 3.0,
            currencyUnit = "$",
            currencyCode = "USD",
            itemPriceString = "$3",
        )

        val price = productVo.createPrice()

        assertThat(price.formatted).isEqualTo("$3.00")
        assertThat(price.amountMicros).isEqualTo(3_000_000)
        assertThat(price.currencyCode).isEqualTo("USD")
    }

    @Test
    fun `createPrice uses itemPrice to compute micros and preserves currency info`() {
        val productVo = createProductVo(
            itemPrice = 3.25,
            currencyUnit = "$",
            currencyCode = "USD",
        )

        val price = productVo.createPrice()

        assertThat(price.formatted).isEqualTo("$3.25")
        assertThat(price.amountMicros).isEqualTo(3_250_000)
        assertThat(price.currencyCode).isEqualTo("USD")
    }

    @Test
    fun `createPrice rounds formatted price but keeps raw micros multiplication`() {
        val productVo = createProductVo(
            itemPrice = 1.2345,
            currencyUnit = "$",
            currencyCode = "USD",
        )

        val price = productVo.createPrice()

        assertThat(price.formatted).isEqualTo("$1.23")
        assertThat(price.amountMicros).isEqualTo(1_234_500)
    }

    // endregion

    private fun createProductVo(
        itemPrice: Double,
        currencyUnit: String,
        currencyCode: String,
        itemPriceString: String = "$currencyUnit$itemPrice",
    ): ProductVo {
        val json = """
            {
                "mItemId": "product_id",
                "mItemName": "Test Product",
                "mItemPrice": $itemPrice,
                "mItemPriceString": "$itemPriceString",
                "mCurrencyUnit": "$currencyUnit",
                "mCurrencyCode": "$currencyCode",
                "mItemDesc": "Test product description",
                "mType": "item"
            }
        """.trimIndent()

        return ProductVo(json)
    }
}
