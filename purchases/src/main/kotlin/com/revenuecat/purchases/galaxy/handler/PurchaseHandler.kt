package com.revenuecat.purchases.galaxy.handler

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.listener.PurchaseResponseListener
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.models.StoreProduct
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo

internal class PurchaseHandler(
    private val iapHelper: IAPHelperProvider,
) : PurchaseResponseListener {

    @get:Synchronized
    private var inFlightRequest: Request? = null

    private val lock = Any()

    private data class Request(
        val onSuccess: (PurchaseVo) -> Unit,
        val onError: (PurchasesError) -> Unit,
    )

    override fun onPayment(error: ErrorVo, purchase: PurchaseVo?) {
        super.onPayment(error, purchase)

        clearInFlightRequest()
    }

    @OptIn(GalaxySerialOperation::class)
    override fun purchase(
        appUserID: String,
        storeProduct: StoreProduct,
        onSuccess: (PurchaseVo) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        // startPayment returns false if the request was not sent to server and was not processed. In this case,
        // the onPaymentListener is never invoked.

        synchronized(lock) {
            this.inFlightRequest = Request(
                onSuccess = onSuccess,
                onError = onError,
            )
        }
        val requestWasDispatched = iapHelper.startPayment(
            itemId = storeProduct.id,
            obfuscatedAccountId = TODO(),
            obfuscatedProfileId = TODO(),
            onPaymentListener = this,
        )

        if(!requestWasDispatched) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_PAYMENT_REQUEST }
            onError(
                PurchasesError(
                    code = PurchasesErrorCode.StoreProblemError,
                    underlyingErrorMessage = "The Galaxy Store failed to accept the purchase request."
                )
            )
            clearInFlightRequest()
            return
        }
    }

    private fun clearInFlightRequest() {
        synchronized(lock) {
            inFlightRequest = null
        }
    }
}