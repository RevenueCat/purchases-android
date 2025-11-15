package com.revenuecat.purchases.utils

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify

fun BillingClient.mockQueryPurchaseHistory(
    result: BillingResult,
    history: List<PurchaseHistoryRecord>,
): Any {
    mockkStatic(QueryPurchaseHistoryParams::class)

    val mockBuilder = mockk<QueryPurchaseHistoryParams.Builder>(relaxed = true)
    every {
        QueryPurchaseHistoryParams.newBuilder()
    } returns mockBuilder

    every {
        mockBuilder.setProductType(any())
    } returns mockBuilder

    val params = mockk<QueryPurchaseHistoryParams>(relaxed = true)
    every {
        mockBuilder.build()
    } returns params

    val billingClientPurchaseHistoryListenerSlot = slot<PurchaseHistoryResponseListener>()

    every {
        queryPurchaseHistoryAsync(
            params,
            capture(billingClientPurchaseHistoryListenerSlot),
        )
    } answers {
        billingClientPurchaseHistoryListenerSlot.captured.onPurchaseHistoryResponse(
            result,
            history,
        )
    }

    return mockBuilder
}

fun BillingClient.verifyQueryPurchaseHistoryCalledWithType(
    @BillingClient.ProductType googleType: String,
    builder: Any,
) {
    verify(exactly = 1) {
        (builder as QueryPurchaseHistoryParams.Builder).setProductType(googleType)
    }

    verify {
        queryPurchaseHistoryAsync(any<QueryPurchaseHistoryParams>(), any())
    }

    clearStaticMockk(QueryPurchasesParams::class)
}
