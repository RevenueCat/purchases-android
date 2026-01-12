package com.revenuecat.purchases.galaxy.listener

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.models.StoreTransaction
import com.samsung.android.sdk.iap.lib.listener.OnAcknowledgePurchasesListener
import com.samsung.android.sdk.iap.lib.vo.AcknowledgeVo
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import java.util.ArrayList

internal interface AcknowledgePurchaseResponseListener : OnAcknowledgePurchasesListener {

    override fun onAcknowledgePurchases(error: ErrorVo, acknowledgementResults: ArrayList<AcknowledgeVo?>) {
        /* intentionally ignored. Use AcknowledgePurchaseHandler instead */
    }

    @GalaxySerialOperation
    fun acknowledgePurchase(
        transaction: StoreTransaction,
        onSuccess: (AcknowledgeVo) -> Unit,
        onError: (PurchasesError) -> Unit,
    )
}
