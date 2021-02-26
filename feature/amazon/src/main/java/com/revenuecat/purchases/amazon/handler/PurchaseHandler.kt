package com.revenuecat.purchases.amazon.handler

import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.AmazonStrings
import com.revenuecat.purchases.amazon.PurchasingServiceProvider
import com.revenuecat.purchases.amazon.listener.PurchaseResponseListener
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.ProductDetails
import com.revenuecat.purchases.strings.PurchaseStrings

class PurchaseHandler(
    private val purchasingServiceProvider: PurchasingServiceProvider
) : PurchaseResponseListener {

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

        val requestId = purchasingServiceProvider.purchase(productDetails.sku)
        synchronized(this) {
            purchaseCallbacks[requestId] = onSuccess to onError
        }
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        log(LogIntent.DEBUG, AmazonStrings.PURCHASE_REQUEST_FINISHED.format(response.toJSON().toString(1)))

        val requestId = response.requestId
        val callbacks = synchronized(this) { purchaseCallbacks.remove(requestId) }

        callbacks?.let { (onSuccess, onError) ->
            when (response.requestStatus) {
                PurchaseResponse.RequestStatus.SUCCESSFUL ->
                    onSuccessfulPurchase(response.receipt, response.userData, onSuccess)
                PurchaseResponse.RequestStatus.FAILED -> onFailed(onError)
                PurchaseResponse.RequestStatus.INVALID_SKU -> onInvalidSku(onError)
                PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> onAlreadyPurchased(onError)
                PurchaseResponse.RequestStatus.NOT_SUPPORTED -> onNotSupported(onError)
                else -> onUnknownError(onError)
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

    @SuppressWarnings("ForbiddenComment")
    private fun onFailed(onError: (PurchasesError) -> Unit) {
        // TODO: replace link with our own
        // Indicates that the purchase failed. Can simply mean user cancelled.
        //
        // According to Amazon's EU MFA flow documentation, the flow would
        // return a PurchaseResponse.RequestStatus.FAILED if cancelled and a
        // PurchaseResponse.RequestStatus.SUCCESSFUL if the MFA flow is completed successfuly
        //
        // Amazon docs:
        // https://developer.amazon.com/blogs/appstore/post/
        // 72ee4001-9e54-49b1-83ad-f1cede9b40ce/
        // ensuring-your-implementation-of-in-app-purchase-is-ready-for-eu-multi-factor-authentication
        onError(PurchasesError(PurchasesErrorCode.PurchaseCancelledError, AmazonStrings.ERROR_PURCHASE_FAILED))
    }
}
