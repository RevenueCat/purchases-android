package com.revenuecat.purchases.helpers

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.factories.StoreProductFactory
import com.revenuecat.purchases.models.StoreProduct
import io.mockk.every
import io.mockk.slot

internal fun BillingAbstract.mockQueryProductDetails(
    queryProductDetailsSubsReturn: List<StoreProduct> = listOf(StoreProductFactory.createGoogleStoreProduct()),
    queryProductDetailsInAppReturn: List<StoreProduct> = emptyList(),
): BillingAbstract {
    return apply {
        val subsReceiveCallbackSlot = slot<StoreProductsCallback>()
        every {
            queryProductDetailsAsync(
                productType = ProductType.SUBS,
                productIds = any(),
                appInBackground = any(),
                onReceive = capture(subsReceiveCallbackSlot),
                onError = any(),
            )
        } answers {
            subsReceiveCallbackSlot.captured.invoke(queryProductDetailsSubsReturn)
        }

        val inappReceiveCallbackSlot = slot<StoreProductsCallback>()
        every {
            queryProductDetailsAsync(
                productType = ProductType.INAPP,
                productIds = any(),
                appInBackground = any(),
                capture(inappReceiveCallbackSlot),
                any(),
            )
        } answers {
            inappReceiveCallbackSlot.captured.invoke(queryProductDetailsInAppReturn)
        }
    }
}
