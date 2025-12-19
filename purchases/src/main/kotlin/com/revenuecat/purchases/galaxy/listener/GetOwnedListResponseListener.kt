package com.revenuecat.purchases.galaxy.listener

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.samsung.android.sdk.iap.lib.listener.OnGetOwnedListListener
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.OwnedProductVo

internal interface GetOwnedListResponseListener : OnGetOwnedListListener {

    override fun onGetOwnedProducts(error: ErrorVo, ownedProducts: ArrayList<OwnedProductVo?>) {
        /* intentionally ignored. Use OwnedListHandler instead */
    }

    @GalaxySerialOperation
    fun getOwnedList(
        onSuccess: (ArrayList<OwnedProductVo>) -> Unit,
        onError: (PurchasesError) -> Unit,
    )
}
