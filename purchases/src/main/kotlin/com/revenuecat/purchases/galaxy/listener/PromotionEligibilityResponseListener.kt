package com.revenuecat.purchases.galaxy.listener

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.samsung.android.sdk.iap.lib.listener.OnGetPromotionEligibilityListener
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.PromotionEligibilityVo
import java.util.ArrayList

internal interface PromotionEligibilityResponseListener : OnGetPromotionEligibilityListener {

    override fun onGetPromotionEligibility(
        error: ErrorVo,
        promotionEligibilities:
        ArrayList<PromotionEligibilityVo>,
    ) {
        /* intentionally ignored. Use PromotionEligibilityHandler instead */
    }

    @GalaxySerialOperation
    fun getPromotionEligibilities(
        productIds: List<String>,
        onSuccess: (List<PromotionEligibilityVo>) -> Unit,
        onError: (PurchasesError) -> Unit,
    )
}
