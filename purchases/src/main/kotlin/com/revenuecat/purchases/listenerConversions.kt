package com.revenuecat.purchases

import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.PurchaseCompletedListener
import com.revenuecat.purchases.interfaces.ReceiveEntitlementsListener
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener

typealias PurchaseCompletedListenerSuccess = (sku: String, purchaserInfo: PurchaserInfo) -> Unit
typealias PurchaseCompletedListenerError = (error: PurchasesError) -> Unit

typealias ReceiveEntitlementsListenerSuccess = (entitlementMap: Map<String, Entitlement>) -> Unit
typealias ReceiveEntitlementsListenerError = (error: PurchasesError) -> Unit

typealias ReceivePurchaserInfoListenerSuccess = (purchaserInfo: PurchaserInfo) -> Unit
typealias ReceivePurchaserInfoListenerError = (error: PurchasesError) -> Unit


internal fun purchaseCompletedListener(
    onSuccess: PurchaseCompletedListenerSuccess,
    onError: PurchaseCompletedListenerError
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
    onSuccess: ReceiveEntitlementsListenerSuccess,
    onError: ReceiveEntitlementsListenerError
) = object : ReceiveEntitlementsListener {
    override fun onReceived(entitlementMap: MutableMap<String, Entitlement>) {
        onSuccess(entitlementMap)
    }

    override fun onError(error: PurchasesError) {
        onError(error)
    }
}

internal fun receivePurchaserInfoListener(
    onSuccess: ReceivePurchaserInfoListenerSuccess?,
    onError: ReceivePurchaserInfoListenerError?
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