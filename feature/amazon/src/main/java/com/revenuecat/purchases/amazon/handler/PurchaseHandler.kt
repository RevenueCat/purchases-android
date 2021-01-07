package com.revenuecat.purchases.amazon.handler

import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.listener.PurchaseResponseListener
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.models.ProductDetails

class PurchaseHandler : PurchaseResponseListener {

    private val productTypes = mutableMapOf<String, ProductType>()
    private val presentedOfferingsByProductIdentifier = mutableMapOf<String, String?>()
    private val purchaseCallbacks =
        mutableMapOf<RequestId, Pair<(Receipt, UserData) -> Unit, (PurchasesError) -> Unit>>()

    override fun purchase(
        appUserID: String,
        productDetails: ProductDetails,
        presentedOfferingIdentifier: String?,
        onSuccess: (Receipt, UserData) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        debugLog("Making purchase for sku: ${productDetails.sku}")

        synchronized(this@PurchaseHandler) {
            productTypes[productDetails.sku] = productDetails.type
            presentedOfferingsByProductIdentifier[productDetails.sku] = presentedOfferingIdentifier
        }

        val requestId = PurchasingService.purchase(productDetails.sku)
        purchaseCallbacks[requestId] = onSuccess to onError
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        debugLog("Purchase request finished with result ${response.requestStatus.name}")
        debugLog("PurchaseResponse JSON: ${response.toJSON().toString(1)}")

        val requestId = response.requestId
        val callbacks = purchaseCallbacks[requestId]

        callbacks?.let { (onSuccess, onError) ->
            when (response.requestStatus) {
                PurchaseResponse.RequestStatus.SUCCESSFUL ->
                    onSuccessfulPurchase(response.receipt, response.userData, onSuccess)
                PurchaseResponse.RequestStatus.FAILED -> onFailed(onError)
                PurchaseResponse.RequestStatus.INVALID_SKU -> onInvalidSku(onError)
                PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> onAlreadyPurchased(onError)
                PurchaseResponse.RequestStatus.NOT_SUPPORTED -> onNotSupported(onError)
                null -> onUnknownError(onError)
            }
        } ?: errorLog("Couldn't find callbacks for completed purchase")
    }

    private fun onSuccessfulPurchase(
        receipt: Receipt,
        userData: UserData,
        onSuccess: (Receipt, UserData) -> Unit
    ) {
        if (receipt.isCanceled) {
            // TODO: Do we do anything?
        } else {
            onSuccess(receipt, userData)
        }
    }

    private fun onUnknownError(onError: (PurchasesError) -> Unit) {
        PurchasesError(
            PurchasesErrorCode.StoreProblemError,
            "Failed to make purchase. There was an Amazon store problem"
        ).let {
            errorLog(it)
            onError(it)
        }
    }

    private fun onNotSupported(onError: (PurchasesError) -> Unit) {
        PurchasesError(
            PurchasesErrorCode.StoreProblemError,
            "Failed to make purchase. Call is not supported"
        ).let {
            errorLog(it)
            onError(it)
        }
    }

    private fun onAlreadyPurchased(onError: (PurchasesError) -> Unit) {
        PurchasesError(
            PurchasesErrorCode.ProductAlreadyPurchasedError,
            "Failed to make purchase. Product already owns SKU."
        ).let {
            errorLog(it)
            onError(it)
        }
    }

    private fun onInvalidSku(onError: (PurchasesError) -> Unit) {
        PurchasesError(
            PurchasesErrorCode.ProductNotAvailableForPurchaseError,
            "Failed to make purchase. SKU is invalid"
        ).let {
            errorLog(it)
            onError(it)
        }
    }

    private fun onFailed(onError: (PurchasesError) -> Unit) {
        // Indicates that the purchase failed. Can simply mean user cancelled
        PurchasesError(
            PurchasesErrorCode.PurchaseCancelledError,
            "Failed to make purchase. This error normally means that the purchase was cancelled"
        ).let {
            errorLog(it)
            onError(it)
        }
    }
}
