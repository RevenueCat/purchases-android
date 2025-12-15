package com.revenuecat.purchases.galaxy.listener

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.models.StoreTransaction
import com.samsung.android.sdk.iap.lib.listener.OnConsumePurchasedItemsListener
import com.samsung.android.sdk.iap.lib.vo.ConsumeVo
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import java.util.ArrayList

internal interface ConsumePurchaseResponseListener : OnConsumePurchasedItemsListener {

    override fun onConsumePurchasedItems(error: ErrorVo, consumptionResults: ArrayList<ConsumeVo>) {
        /* intentionally ignored. Use ConsumePurchaseHandler instead */
    }

    @GalaxySerialOperation
    fun consumePurchase(
        transaction: StoreTransaction,
        onSuccess: (ConsumeVo) -> Unit,
        onError: (PurchasesError) -> Unit,
    )
}
