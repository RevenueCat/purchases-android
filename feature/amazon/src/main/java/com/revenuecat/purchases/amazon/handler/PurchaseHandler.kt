package com.revenuecat.purchases.amazon.handler

import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.AmazonStrings
import com.revenuecat.purchases.amazon.listener.PurchaseResponseListener
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.ProductDetails
import com.revenuecat.purchases.strings.PurchaseStrings

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
        log(LogIntent.PURCHASE, PurchaseStrings.PURCHASING_PRODUCT.format(productDetails.sku))

        synchronized(this@PurchaseHandler) {
            productTypes[productDetails.sku] = productDetails.type
            presentedOfferingsByProductIdentifier[productDetails.sku] = presentedOfferingIdentifier
        }

        val requestId = PurchasingService.purchase(productDetails.sku)
        purchaseCallbacks[requestId] = onSuccess to onError
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        log(LogIntent.DEBUG, AmazonStrings.PURCHASE_REQUEST_FINISHED.format(response.toJSON().toString(1)))

        val requestId = response.requestId
        val callbacks = purchaseCallbacks.remove(requestId)

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
        }
    }

    @SuppressWarnings("ForbiddenComment")
    private fun onSuccessfulPurchase(
        receipt: Receipt,
        userData: UserData,
        onSuccess: (Receipt, UserData) -> Unit
    ) {
        onSuccess(receipt, userData)
    }

    private fun onUnknownError(onError: (PurchasesError) -> Unit) {
        onError(PurchasesError(PurchasesErrorCode.StoreProblemError, AmazonStrings.ERROR_PURCHASE_UNKNOWN))
    }

    private fun onNotSupported(onError: (PurchasesError) -> Unit) {
        onError(PurchasesError(PurchasesErrorCode.StoreProblemError, AmazonStrings.ERROR_PURCHASE_NOT_SUPPORTED))
    }

    private fun onAlreadyPurchased(onError: (PurchasesError) -> Unit) {
        onError(
            PurchasesError(PurchasesErrorCode.ProductAlreadyPurchasedError, AmazonStrings.ERROR_PURCHASE_ALREADY_OWNED)
        )
    }

    private fun onInvalidSku(onError: (PurchasesError) -> Unit) {
        onError(
            PurchasesError(
                PurchasesErrorCode.ProductNotAvailableForPurchaseError,
                AmazonStrings.ERROR_PURCHASE_INVALID_SKU
            )
        )
    }

    private fun onFailed(onError: (PurchasesError) -> Unit) {
        // Indicates that the purchase failed. Can simply mean user cancelled
        onError(PurchasesError(PurchasesErrorCode.PurchaseCancelledError, AmazonStrings.ERROR_PURCHASE_FAILED))
    }
}
