package com.revenuecat.purchases.galaxy

import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import com.samsung.android.sdk.iap.lib.vo.OwnedProductVo
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
        tieredSubscriptionYN: String = "N",
        tieredPrice: String = "",
        tieredSubscriptionCount: String = "",
        tieredSubscriptionDurationMultiplier: String = "",
        tieredSubscriptionDurationUnit: String = "",
        tieredPriceString: String = tieredPrice.takeIf { it.isNotEmpty() }?.let { "$currencyUnit$it" } ?: "",
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
            every { productVo.tieredSubscriptionYN } returns tieredSubscriptionYN
            every { productVo.tieredPrice } returns tieredPrice
            every { productVo.tieredSubscriptionCount } returns tieredSubscriptionCount
            every { productVo.tieredSubscriptionDurationMultiplier } returns tieredSubscriptionDurationMultiplier
            every { productVo.tieredSubscriptionDurationUnit } returns tieredSubscriptionDurationUnit
            every { productVo.tieredPriceString } returns tieredPriceString
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

    fun createGalaxySubscriptionOption(
        id: String = "productId",
        tags: List<String> = emptyList(),
        price: Price = Price(
            formatted = "$1.00",
            amountMicros = 1_000_000,
            currencyCode = "USD",
        ),
        period: Period = Period.create("P1M"),
        presentedOfferingContext: PresentedOfferingContext? = null,
    ): GalaxySubscriptionOption =
        GalaxySubscriptionOption(
            id = id,
            pricingPhases = listOf(
                PricingPhase(
                    billingPeriod = period,
                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                    billingCycleCount = null,
                    price = price,
                ),
            ),
            tags = tags,
            presentedOfferingContext = presentedOfferingContext,
            purchasingData = GalaxyPurchasingData.Product(productId = id, productType = ProductType.SUBS),
            installmentsInfo = null,
        )

    fun createOwnedProductVo(
        itemId: String,
        purchaseId: String,
        type: String,
        purchaseDate: String,
        jsonString: String = """{ "itemId": "$itemId" }""",
        acknowledgedStatus: HelperDefine.AcknowledgedStatus = HelperDefine.AcknowledgedStatus.NOT_ACKNOWLEDGED,
    ): OwnedProductVo =
        mockk(relaxed = true) {
            every { this@mockk.itemId } returns itemId
            every { this@mockk.purchaseId } returns purchaseId
            every { this@mockk.type } returns type
            every { this@mockk.purchaseDate } returns purchaseDate
            every { this@mockk.jsonString } returns jsonString
            every { this@mockk.acknowledgedStatus } returns acknowledgedStatus
        }
}
