package com.revenuecat.purchases.helpers

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.ProductDetailsListCallback
import com.revenuecat.purchases.models.ProductDetails
import io.mockk.every
import io.mockk.slot

fun BillingAbstract.mockQuerySkuDetails(
    querySkuDetailsSubsReturn: List<ProductDetails> = emptyList(),
    querySkuDetailsInAppReturn: List<ProductDetails> = emptyList(),
): BillingAbstract {
    return apply {
        val subsReceiveCallbackSlot = slot<ProductDetailsListCallback>()
        every {
            querySkuDetailsAsync(ProductType.SUBS, any(), capture(subsReceiveCallbackSlot), any())
        } answers {
            subsReceiveCallbackSlot.captured.invoke(querySkuDetailsSubsReturn)
        }

        val inappReceiveCallbackSlot = slot<ProductDetailsListCallback>()
        every {
            querySkuDetailsAsync(ProductType.INAPP, any(), capture(inappReceiveCallbackSlot), any())
        } answers {
            inappReceiveCallbackSlot.captured.invoke(querySkuDetailsInAppReturn)
        }
    }
}
