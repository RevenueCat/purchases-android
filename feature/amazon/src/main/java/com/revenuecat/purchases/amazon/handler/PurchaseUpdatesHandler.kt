package com.revenuecat.purchases.amazon.handler

import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.revenuecat.purchases.amazon.listener.PurchaseUpdatesResponseListener

class PurchaseUpdatesHandler : PurchaseUpdatesResponseListener {

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        // TODO
    }
}
