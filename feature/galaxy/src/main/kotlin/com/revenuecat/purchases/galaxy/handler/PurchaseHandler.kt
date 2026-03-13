package com.revenuecat.purchases.galaxy.handler

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.listener.PurchaseResponseListener
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.galaxy.utils.isError
import com.revenuecat.purchases.galaxy.utils.toPurchasesError
import com.revenuecat.purchases.strings.PurchaseStrings
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo

@OptIn(InternalRevenueCatAPI::class)
internal class PurchaseHandler(
    private val iapHelper: IAPHelperProvider,
) : PurchaseResponseListener {

    @get:Synchronized
    private var inFlightRequest: Request? = null

    private data class Request(
        val productId: String,
        val onSuccess: (PurchaseVo) -> Unit,
        val onError: (PurchasesError) -> Unit,
    )

    @GalaxySerialOperation
    override fun purchase(
        appUserID: String,
        productId: String,
        onSuccess: (PurchaseVo) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        if (inFlightRequest != null) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.ANOTHER_PURCHASE_REQUEST_IN_FLIGHT }
            val error = PurchasesError(
                code = PurchasesErrorCode.OperationAlreadyInProgressError,
                underlyingErrorMessage = GalaxyStrings.ANOTHER_PURCHASE_REQUEST_IN_FLIGHT,
            )
            onError(error)
            return
        }

        this.inFlightRequest = Request(
            productId = productId,
            onSuccess = onSuccess,
            onError = onError,
        )

        log(LogIntent.PURCHASE) { PurchaseStrings.PURCHASING_PRODUCT.format(productId) }

        // startPayment returns false if the request was not sent to server and was not processed. When this happens,
        // the onPaymentListener is never invoked.
        val requestWasDispatched = iapHelper.startPayment(
            itemId = productId,
            obfuscatedAccountId = appUserID.sha256(),
            obfuscatedProfileId = null,
            onPaymentListener = this,
        )

        if (!requestWasDispatched) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_PAYMENT_REQUEST }
            onError(
                PurchasesError(
                    code = PurchasesErrorCode.StoreProblemError,
                    underlyingErrorMessage = "The Galaxy Store failed to accept the purchase request.",
                ),
            )
            clearInFlightRequest()
            return
        }
    }

    override fun onPayment(error: ErrorVo, purchase: PurchaseVo?) {
        super.onPayment(error, purchase)

        if (error.isError()) {
            handleUnsuccessfulPurchaseResponse(error = error)
        } else {
            handleSuccessfulPurchaseResponse(purchase = purchase)
        }
    }

    private fun handleSuccessfulPurchaseResponse(purchase: PurchaseVo?) {
        if (purchase == null) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.PURCHASE_RETURNED_SUCCESS_BUT_NO_PURCHASE_RESULT }
            val purchasesError = PurchasesError(
                PurchasesErrorCode.StoreProblemError,
                GalaxyStrings.PURCHASE_RETURNED_SUCCESS_BUT_NO_PURCHASE_RESULT,
            )
            val onError = inFlightRequest?.onError
            clearInFlightRequest()
            onError?.invoke(purchasesError)
            return
        }

        val onSuccess = inFlightRequest?.onSuccess
        clearInFlightRequest()
        onSuccess?.invoke(purchase)
    }

    private fun handleUnsuccessfulPurchaseResponse(error: ErrorVo) {
        val underlyingErrorMessage = error.errorString
        log(LogIntent.GALAXY_ERROR) {
            GalaxyStrings.PURCHASE_REQUEST_ERRORED.format(
                inFlightRequest?.productId ?: "[none]",
                underlyingErrorMessage,
            )
        }

        val onError = inFlightRequest?.onError
        clearInFlightRequest()
        onError?.invoke(error.toPurchasesError())
    }

    private fun clearInFlightRequest() {
        inFlightRequest = null
    }
}
