package com.revenuecat.purchases.galaxy.handler

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.conversions.toSamsungProrationMode
import com.revenuecat.purchases.galaxy.listener.ChangeSubscriptionPlanResponseListener
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.galaxy.utils.isError
import com.revenuecat.purchases.galaxy.utils.toPurchasesError
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo

internal class ChangeSubscriptionPlanHandler(
    private val iapHelper: IAPHelperProvider,
) : ChangeSubscriptionPlanResponseListener {

    @get:Synchronized
    private var inFlightRequest: Request? = null

    private data class Request(
        val newProductId: String,
        val oldProductId: String,
        val onSuccess: (PurchaseVo) -> Unit,
        val onError: (PurchasesError) -> Unit,
    )

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    @Suppress("ReturnCount")
    @GalaxySerialOperation
    override fun changeSubscriptionPlan(
        appUserID: String,
        oldPurchase: StoreTransaction,
        newProductId: String,
        prorationMode: GalaxyReplacementMode,
        onSuccess: (PurchaseVo) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        if (inFlightRequest != null) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.ANOTHER_CHANGE_SUBSCRIPTION_PLAN_REQUEST_IN_FLIGHT }
            val error = PurchasesError(
                code = PurchasesErrorCode.OperationAlreadyInProgressError,
                underlyingErrorMessage = GalaxyStrings.ANOTHER_CHANGE_SUBSCRIPTION_PLAN_REQUEST_IN_FLIGHT,
            )
            onError(error)
            return
        }

        val oldProductId = oldPurchase.productIds.firstOrNull()
        if (oldProductId == null) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.CHANGE_SUBSCRIPTION_PLAN_NO_OLD_PRODUCT_ID }
            val error = PurchasesError(
                code = PurchasesErrorCode.PurchaseInvalidError,
                underlyingErrorMessage = GalaxyStrings.CHANGE_SUBSCRIPTION_PLAN_NO_OLD_PRODUCT_ID,
            )
            onError(error)
            return
        }

        inFlightRequest = Request(
            newProductId = newProductId,
            oldProductId = oldProductId,
            onSuccess = onSuccess,
            onError = onError,
        )

        log(LogIntent.PURCHASE) {
            PurchaseStrings.UPGRADING_SKU.format(oldProductId, newProductId)
        }

        // changeSubscriptionPlan returns false if the request was not sent to server and was not processed.
        // When this happens, the onChangeSubscriptionPlanListener is never invoked.
        val requestWasDispatched = iapHelper.changeSubscriptionPlan(
            oldItemId = oldProductId,
            newItemId = newProductId,
            prorationMode = prorationMode.toSamsungProrationMode(),
            obfuscatedAccountId = appUserID.sha256(),
            obfuscatedProfileId = null,
            onChangeSubscriptionPlanListener = this,
        )

        if (!requestWasDispatched) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.GALAXY_STORE_FAILED_TO_ACCEPT_CHANGE_SUBSCRIPTION_PLAN_REQUEST }
            onError(
                PurchasesError(
                    code = PurchasesErrorCode.StoreProblemError,
                    underlyingErrorMessage = GalaxyStrings
                        .GALAXY_STORE_FAILED_TO_ACCEPT_CHANGE_SUBSCRIPTION_PLAN_REQUEST,
                ),
            )
            clearInFlightRequest()
            return
        }
    }

    override fun onChangeSubscriptionPlan(error: ErrorVo, purchase: PurchaseVo?) {
        super.onChangeSubscriptionPlan(error, purchase)

        if (error.isError()) {
            handleUnsuccessfulResponse(error = error)
        } else {
            handleSuccessfulResponse(purchase = purchase)
        }
    }

    private fun handleSuccessfulResponse(purchase: PurchaseVo?) {
        if (purchase == null) {
            log(LogIntent.GALAXY_ERROR) { GalaxyStrings.CHANGE_SUBSCRIPTION_PLAN_RETURNED_SUCCESS_BUT_NO_RESULT }
            val purchasesError = PurchasesError(
                PurchasesErrorCode.StoreProblemError,
                GalaxyStrings.CHANGE_SUBSCRIPTION_PLAN_RETURNED_SUCCESS_BUT_NO_RESULT,
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

    private fun handleUnsuccessfulResponse(error: ErrorVo) {
        val underlyingErrorMessage = error.errorString
        log(LogIntent.GALAXY_ERROR) {
            GalaxyStrings.CHANGE_SUBSCRIPTION_PLAN_REQUEST_ERRORED.format(
                inFlightRequest?.oldProductId ?: "[none]",
                inFlightRequest?.newProductId ?: "[none]",
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
