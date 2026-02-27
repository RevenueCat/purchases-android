package com.revenuecat.purchases.galaxy.handler

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.listener.PromotionEligibilityResponseListener
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.galaxy.utils.isError
import com.revenuecat.purchases.galaxy.utils.toPurchasesError
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.PromotionEligibilityVo
import java.util.ArrayList

internal class PromotionEligibilityHandler(
    private val iapHelper: IAPHelperProvider,
) : PromotionEligibilityResponseListener {

    @get:Synchronized
    private var inFlightRequest: Request? = null

    private data class Request(
        val productIds: List<String>,
        val onSuccess: (List<PromotionEligibilityVo>) -> Unit,
        val onError: (PurchasesError) -> Unit,
    )

    @SuppressWarnings("ReturnCount")
    @GalaxySerialOperation
    override fun getPromotionEligibilities(
        productIds: List<String>,
        onSuccess: (List<PromotionEligibilityVo>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        if (productIds.isEmpty()) {
            log(LogIntent.DEBUG) { GalaxyStrings.EMPTY_GET_PROMOTION_ELIGIBILITY_REQUEST }
            onSuccess(emptyList())
            return
        }

        if (inFlightRequest != null) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.ANOTHER_GET_PROMOTION_ELIGIBILITY_REQUEST_IN_FLIGHT }
            val error = PurchasesError(
                code = PurchasesErrorCode.OperationAlreadyInProgressError,
                underlyingErrorMessage = GalaxyStrings.ANOTHER_GET_PROMOTION_ELIGIBILITY_REQUEST_IN_FLIGHT,
            )
            onError(error)
            return
        }

        this.inFlightRequest = Request(
            productIds = productIds,
            onSuccess = onSuccess,
            onError = onError,
        )

        val requestString = productIds.joinToString(separator = ",")
        log(LogIntent.DEBUG) { GalaxyStrings.REQUESTING_PROMOTION_ELIGIBILITY.format(requestString) }
        val requestWasDispatched = iapHelper.getPromotionEligibility(
            itemIDs = requestString,
            onGetPromotionEligibilityListener = this,
        )

        if (!requestWasDispatched) {
            log(LogIntent.GALAXY_ERROR) {
                GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_PROMOTION_ELIGIBILITY_REQUEST
            }
            onError(
                PurchasesError(
                    code = PurchasesErrorCode.StoreProblemError,
                    underlyingErrorMessage = GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_PROMOTION_ELIGIBILITY_REQUEST,
                ),
            )
            clearInFlightRequest()
            return
        }
    }

    override fun onGetPromotionEligibility(
        error: ErrorVo,
        promotionEligibilities: ArrayList<PromotionEligibilityVo>,
    ) {
        super.onGetPromotionEligibility(error, promotionEligibilities)

        if (error.isError()) {
            handleUnsuccessfulGetPromotionEligibilityRequest(error = error)
        } else {
            handleSuccessfulGetPromotionEligibilityResponse(promotionEligibilities = promotionEligibilities)
        }
    }

    private fun handleSuccessfulGetPromotionEligibilityResponse(
        promotionEligibilities: ArrayList<PromotionEligibilityVo>,
    ) {
        if (promotionEligibilities.isEmpty()) {
            log(LogIntent.GALAXY_ERROR) {
                GalaxyStrings.PROMOTION_ELIGIBILITY_RETURNED_SUCCESS_BUT_NO_ACKNOWLEDGEMENT_RESULTS
            }
            val purchasesError = PurchasesError(
                PurchasesErrorCode.StoreProblemError,
                GalaxyStrings.PROMOTION_ELIGIBILITY_RETURNED_SUCCESS_BUT_NO_ACKNOWLEDGEMENT_RESULTS,
            )
            val onError = inFlightRequest?.onError
            clearInFlightRequest()
            onError?.invoke(purchasesError)
            return
        }

        val onSuccess = inFlightRequest?.onSuccess
        clearInFlightRequest()
        onSuccess?.invoke(promotionEligibilities)
    }

    private fun handleUnsuccessfulGetPromotionEligibilityRequest(error: ErrorVo) {
        val underlyingErrorMessage = error.errorString

        log(LogIntent.GALAXY_ERROR) {
            GalaxyStrings.PROMOTION_ELIGIBILITY_REQUEST_ERRORED.format(
                inFlightRequest?.productIds?.joinToString(", ") ?: "[none]",
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
