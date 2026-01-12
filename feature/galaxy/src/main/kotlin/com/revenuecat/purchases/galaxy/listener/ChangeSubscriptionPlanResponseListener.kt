package com.revenuecat.purchases.galaxy.listener

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.revenuecat.purchases.models.StoreTransaction
import com.samsung.android.sdk.iap.lib.listener.OnChangeSubscriptionPlanListener
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo

internal interface ChangeSubscriptionPlanResponseListener : OnChangeSubscriptionPlanListener {

    override fun onChangeSubscriptionPlan(error: ErrorVo, purchase: PurchaseVo?) {
        /* intentionally ignored. Use ChangeSubscriptionPlanHandler instead */
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Suppress("LongParameterList")
    @GalaxySerialOperation
    fun changeSubscriptionPlan(
        appUserID: String,
        oldPurchase: StoreTransaction,
        newProductId: String,
        prorationMode: GalaxyReplacementMode,
        onSuccess: (PurchaseVo) -> Unit,
        onError: (PurchasesError) -> Unit,
    )
}
