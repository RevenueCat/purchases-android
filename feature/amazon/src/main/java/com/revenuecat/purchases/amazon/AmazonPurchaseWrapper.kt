package com.revenuecat.purchases.amazon

import com.amazon.device.iap.model.Receipt
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.PurchaseWrapper
import com.revenuecat.purchases.common.RevenueCatPurchaseState

class AmazonPurchaseWrapper(
    override val sku: String,
    val containedReceipt: Receipt,
    override val presentedOfferingIdentifier: String? = null,
    override val purchaseState: RevenueCatPurchaseState,
    override val storeUserID: String
) : PurchaseWrapper {
    override val type: ProductType
        get() = containedReceipt.productType.toRevenueCatProductType()
    override val purchaseToken: String
        get() = containedReceipt.receiptId
    override val purchaseTime: Long
        get() = containedReceipt.purchaseDate.time
}
