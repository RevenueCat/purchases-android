package com.revenuecat.purchases.galaxy.handler

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.listener.GetOwnedListResponseListener
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.galaxy.utils.isError
import com.revenuecat.purchases.galaxy.utils.toPurchasesError
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.OwnedProductVo

internal class GetOwnedListHandler(
    private val iapHelper: IAPHelperProvider,
) : GetOwnedListResponseListener {

    @get:Synchronized
    private var inFlightRequest: Request? = null

    private data class Request(
        val onSuccess: (ArrayList<OwnedProductVo>) -> Unit,
        val onError: (PurchasesError) -> Unit,
    )

    @SuppressWarnings("ReturnCount")
    @GalaxySerialOperation
    override fun getOwnedList(
        onSuccess: (ArrayList<OwnedProductVo>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        if (inFlightRequest != null) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.ANOTHER_GET_OWNED_LIST_REQUEST_IN_FLIGHT }
            val error = PurchasesError(
                code = PurchasesErrorCode.OperationAlreadyInProgressError,
                underlyingErrorMessage = GalaxyStrings.ANOTHER_GET_OWNED_LIST_REQUEST_IN_FLIGHT,
            )
            onError(error)
            return
        }

        this.inFlightRequest = Request(
            onSuccess = onSuccess,
            onError = onError,
        )

        log(LogIntent.DEBUG) { GalaxyStrings.REQUESTING_OWNED_LIST }
        val requestWasDispatched = iapHelper.getOwnedList(
            onGetOwnedListListener = this,
        )

        if (!requestWasDispatched) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_OWNED_LIST_REQUEST }
            onError(
                PurchasesError(
                    code = PurchasesErrorCode.StoreProblemError,
                    underlyingErrorMessage = GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_OWNED_LIST_REQUEST,
                ),
            )
            clearInFlightRequest()
            return
        }
    }

    override fun onGetOwnedProducts(error: ErrorVo, ownedProducts: ArrayList<OwnedProductVo?>) {
        super.onGetOwnedProducts(error, ownedProducts)
        if (error.isError()) {
            handleUnsuccessfulGetOwnedProductsRequest(error = error)
        } else {
            handleSuccessfulGetOwnedProductsRequest(ownedProducts = ownedProducts)
        }
    }

    private fun handleSuccessfulGetOwnedProductsRequest(ownedProducts: ArrayList<OwnedProductVo?>) {
        val nonNullOwnedProducts = ArrayList(ownedProducts.mapNotNull { it })

        val onSuccess = inFlightRequest?.onSuccess
        clearInFlightRequest()
        onSuccess?.invoke(nonNullOwnedProducts)
    }

    private fun handleUnsuccessfulGetOwnedProductsRequest(error: ErrorVo) {
        val underlyingErrorMessage = error.errorString
        log(LogIntent.GALAXY_ERROR) {
            GalaxyStrings.GET_OWNED_LIST_REQUEST_ERRORED.format(underlyingErrorMessage)
        }

        val onError = inFlightRequest?.onError
        clearInFlightRequest()
        onError?.invoke(error.toPurchasesError())
    }

    private fun clearInFlightRequest() {
        inFlightRequest = null
    }
}
