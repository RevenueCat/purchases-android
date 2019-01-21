package com.revenuecat.purchases

import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.PurchaseCompletedListener
import com.revenuecat.purchases.interfaces.ReceiveEntitlementsListener
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener

typealias PurchaseCompletedSuccessFunction = (sku: String, purchaserInfo: PurchaserInfo) -> Unit
typealias ReceiveEntitlementsSuccessFunction = (entitlementMap: Map<String, Entitlement>) -> Unit
typealias ReceivePurchaserInfoSuccessFunction = (purchaserInfo: PurchaserInfo) -> Unit
typealias ErrorFunction = (error: PurchasesError) -> Unit

internal fun purchaseCompletedListener(
    onSuccess: PurchaseCompletedSuccessFunction,
    onError: ErrorFunction
) = object : PurchaseCompletedListener {
    override fun onCompleted(sku: String, purchaserInfo: PurchaserInfo) {
        onSuccess(sku, purchaserInfo)
    }

    override fun onError(error: PurchasesError) {
        onError(error)
    }
}

internal fun getSkusResponseListener(
    onReceived: (skus: List<SkuDetails>) -> Unit
) = GetSkusResponseListener { onReceived(it) }

internal fun receiveEntitlementsListener(
    onSuccess: ReceiveEntitlementsSuccessFunction,
    onError: ErrorFunction
) = object : ReceiveEntitlementsListener {
    override fun onReceived(entitlementMap: MutableMap<String, Entitlement>) {
        onSuccess(entitlementMap)
    }

    override fun onError(error: PurchasesError) {
        onError(error)
    }
}

internal fun receivePurchaserInfoListener(
    onSuccess: ReceivePurchaserInfoSuccessFunction?,
    onError: ErrorFunction?
) = object : ReceivePurchaserInfoListener {
    override fun onReceived(purchaserInfo: PurchaserInfo) {
        onSuccess?.invoke(purchaserInfo)
    }

    override fun onError(error: PurchasesError) {
        onError?.invoke(error)
    }
}

internal fun updatedPurchaserInfoListener(
    onSuccess: (purchaserInfo: PurchaserInfo) -> Unit
) = UpdatedPurchaserInfoListener { onSuccess(it) }