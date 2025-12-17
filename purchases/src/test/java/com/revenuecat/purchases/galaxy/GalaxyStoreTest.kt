package com.revenuecat.purchases.galaxy

import com.samsung.android.sdk.iap.lib.vo.ProductVo
import com.samsung.android.sdk.iap.lib.vo.PromotionEligibilityVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo
import io.mockk.every
import io.mockk.mockk

/**
 * Contains helper utilities that are useful when testing the Galaxy Store.
 */
open class GalaxyStoreTest {
    fun createProductVo(
        itemId: String = "product_id",
        itemName: String = "Test Product",
        itemDescription: String = "Test product description",
        itemPrice: Double = 0.99,
        currencyUnit: String = "$",
        currencyCode: String = "USD",
        type: String = "item",
        itemPriceString: String = "$currencyUnit$itemPrice",
        subscriptionDurationMultiplier: String = "",
        subscriptionDurationUnit: String = "",
        freeTrialPeriod: String = "",
    ): ProductVo {
        return mockk<ProductVo>(relaxed = true).also { productVo ->
            every { productVo.itemId } returns itemId
            every { productVo.itemName } returns itemName
            every { productVo.itemDesc } returns itemDescription
            every { productVo.itemPrice } returns itemPrice
            every { productVo.itemPriceString } returns itemPriceString
            every { productVo.currencyUnit } returns currencyUnit
            every { productVo.currencyCode } returns currencyCode
            every { productVo.type } returns type
            every { productVo.subscriptionDurationMultiplier } returns subscriptionDurationMultiplier
            every { productVo.subscriptionDurationUnit } returns subscriptionDurationUnit
            every { productVo.freeTrialPeriod } returns freeTrialPeriod
        }
    }

    fun createPromotionEligibilityVo(
        itemId: String,
        pricing: String,
    ): PromotionEligibilityVo {
        return mockk<PromotionEligibilityVo>(relaxed = true).also { vo ->
            every { vo.itemId } returns itemId
            every { vo.pricing } returns pricing
        }
    }

    fun createPurchaseVo(
        paymentId: String,
        purchaseId: String,
        orderId: String,
        purchaseDate: String,
        type: String,
        itemId: String,
    ): PurchaseVo {
        return mockk<PurchaseVo>(relaxed = true).also { purchaseVo ->
            every { purchaseVo.paymentId } returns paymentId
            every { purchaseVo.purchaseId } returns purchaseId
            every { purchaseVo.orderId } returns orderId
            every { purchaseVo.purchaseDate } returns purchaseDate
            every { purchaseVo.type } returns type
            every { purchaseVo.itemId } returns itemId
        }
    }
}
