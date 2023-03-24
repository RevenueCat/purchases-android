package com.revenuecat.purchases.helpers

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.models.StoreProduct
import io.mockk.every
import io.mockk.slot

fun BillingAbstract.mockQuerySkuDetails(
    querySkuDetailsSubsReturn: List<StoreProduct> = emptyList(),
    querySkuDetailsInAppReturn: List<StoreProduct> = emptyList(),
): BillingAbstract {
    return apply {
        val subsReceiveCallbackSlot = slot<StoreProductsCallback>()
        every {
            querySkuDetailsAsync(ProductType.SUBS, any(), capture(subsReceiveCallbackSlot), any())
        } answers {
            subsReceiveCallbackSlot.captured.invoke(querySkuDetailsSubsReturn)
        }

        val inappReceiveCallbackSlot = slot<StoreProductsCallback>()
        every {
            querySkuDetailsAsync(ProductType.INAPP, any(), capture(inappReceiveCallbackSlot), any())
        } answers {
            inappReceiveCallbackSlot.captured.invoke(querySkuDetailsInAppReturn)
        }
    }
}
