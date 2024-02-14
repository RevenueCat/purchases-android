package com.revenuecat.purchases.amazon.handler

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.AmazonStrings
import com.revenuecat.purchases.amazon.PurchasingServiceProvider
import com.revenuecat.purchases.amazon.listener.PurchaseResponseListener
import com.revenuecat.purchases.amazon.purchasing.ProxyAmazonBillingActivity
import com.revenuecat.purchases.amazon.purchasing.ProxyAmazonBillingActivityBroadcastReceiver
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.strings.PurchaseStrings

internal class PurchaseHandler(
    private val purchasingServiceProvider: PurchasingServiceProvider,
    private val applicationContext: Context,
) : PurchaseResponseListener {

    private val productTypes = mutableMapOf<String, ProductType>()
    private val purchaseCallbacks =
        mutableMapOf<RequestId, Pair<(Receipt, UserData) -> Unit, (PurchasesError) -> Unit>>()

    override fun purchase(
        mainHandler: Handler,
        activity: Activity,
        appUserID: String,
        storeProduct: StoreProduct,
        presentedOfferingContext: PresentedOfferingContext,
        onSuccess: (Receipt, UserData) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        log(LogIntent.PURCHASE, PurchaseStrings.PURCHASING_PRODUCT.format(storeProduct.id))

        startProxyActivity(mainHandler, activity, storeProduct, onSuccess, onError)
    }

    @SuppressWarnings("LongParameterList")
    private fun startProxyActivity(
        mainHandler: Handler,
        activity: Activity,
        storeProduct: StoreProduct,
        onSuccess: (Receipt, UserData) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        val resultReceiver =
            createRequestIdResultReceiver(mainHandler, storeProduct, onSuccess, onError)
        val intent = ProxyAmazonBillingActivity.newStartIntent(
            activity,
            resultReceiver,
            storeProduct.id,
            purchasingServiceProvider,
        )
        // ProxyAmazonBillingActivity will initiate the purchase
        activity.startActivity(intent)
    }

    private fun createRequestIdResultReceiver(
        mainHandler: Handler,
        storeProduct: StoreProduct,
        onSuccess: (Receipt, UserData) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) = object : ResultReceiver(mainHandler) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            synchronized(this@PurchaseHandler) {
                val requestId = resultData?.get(ProxyAmazonBillingActivity.EXTRAS_REQUEST_ID) as? RequestId
                if (requestId != null) {
                    purchaseCallbacks[requestId] = onSuccess to onError
                    productTypes[storeProduct.id] = storeProduct.type
                } else {
                    errorLog("No RequestId coming from ProxyAmazonBillingActivity")
                }
            }
        }
    }

    override fun onPurchaseResponse(response: PurchaseResponse) {
        // Amazon is catching all exceptions and swallowing them so we have to catch ourselves and log
        try {
            log(LogIntent.DEBUG, AmazonStrings.PURCHASE_REQUEST_FINISHED.format(response.toJSON().toString(1)))

            val intent =
                ProxyAmazonBillingActivityBroadcastReceiver.newPurchaseFinishedIntent(applicationContext.packageName)
            applicationContext.sendBroadcast(intent)

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
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog("Exception in onPurchaseResponse", e)
            throw e
        }
    }

    @SuppressWarnings("ForbiddenComment")
    private fun onSuccessfulPurchase(
        receipt: Receipt,
        userData: UserData,
        onSuccess: (Receipt, UserData) -> Unit,
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
            PurchasesError(PurchasesErrorCode.ProductAlreadyPurchasedError, AmazonStrings.ERROR_PURCHASE_ALREADY_OWNED),
        )
    }

    private fun onInvalidSku(onError: (PurchasesError) -> Unit) {
        onError(
            PurchasesError(
                PurchasesErrorCode.ProductNotAvailableForPurchaseError,
                AmazonStrings.ERROR_PURCHASE_INVALID_SKU,
            ),
        )
    }

    @SuppressWarnings("ForbiddenComment")
    private fun onFailed(onError: (PurchasesError) -> Unit) {
        // TODO: replace link with our own
        // Indicates that the purchase failed. Can simply mean user cancelled.
        //
        // According to Amazon's EU MFA flow documentation, the flow would
        // return a PurchaseResponse.RequestStatus.FAILED if cancelled and a
        // PurchaseResponse.RequestStatus.SUCCESSFUL if the MFA flow is completed successfully
        //
        // Amazon docs:
        // https://developer.amazon.com/blogs/appstore/post/
        // 72ee4001-9e54-49b1-83ad-f1cede9b40ce/
        // ensuring-your-implementation-of-in-app-purchase-is-ready-for-eu-multi-factor-authentication
        onError(PurchasesError(PurchasesErrorCode.PurchaseCancelledError, AmazonStrings.ERROR_PURCHASE_FAILED))
    }
}
