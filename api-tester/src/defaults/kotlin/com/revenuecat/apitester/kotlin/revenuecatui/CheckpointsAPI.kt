@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPassedCallback
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ErrorPresenter
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.errorPresenter
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.paywallPresenter

@Suppress("unused", "UNUSED_VARIABLE")
private class CheckpointsAPI {

    fun checkCheckpoint(purchases: Purchases, params: CheckpointParams, callback: CheckpointPassedCallback) {
        purchases.checkpoint("checkpoint_identifier") { result: FlowResult? -> }
        purchases.checkpoint("checkpoint_identifier", params) { result: FlowResult? -> }
        purchases.checkpoint("checkpoint_identifier", params = null) { result: FlowResult? -> }
        purchases.checkpoint("checkpoint_identifier", callback)
        purchases.checkpoint(
            checkpointIdentifier = "checkpoint_identifier",
            params = params,
            callback = callback,
        )
    }

    fun checkParams(presenter: PaywallPresenter, errorPresenter: ErrorPresenter) {
        val fromBuilder: CheckpointParams = CheckpointParams.Builder()
            .setCustomVariables(mapOf("key" to CustomVariableValue.String("value")))
            .setPaywallPresenter(presenter)
            .setPaywallPresenter(null)
            .setErrorPresenter(errorPresenter)
            .setErrorPresenter(null)
            .build()
        val fromDsl: CheckpointParams = CheckpointParams {
            customVariables { "key" to "value" }
            paywallPresenter(presenter)
            paywallPresenter { params: PaywallPresenter.Params, completion: PaywallPresenter.Completion ->
                completion.complete(PaywallPresenter.Completion.Result.Closed)
            }
            errorPresenter(errorPresenter)
            errorPresenter { params: ErrorPresenter.Params, completion: ErrorPresenter.Completion ->
                completion.complete(ErrorPresenter.Completion.Result.Retry)
            }
        }
        val customVariables: Map<String, CustomVariableValue> = fromDsl.customVariables
        val paywallPresenter: PaywallPresenter? = fromDsl.paywallPresenter
        val currentErrorPresenter: ErrorPresenter? = fromDsl.errorPresenter
    }

    fun checkPaywallPresenter(purchases: Purchases, presenter: PaywallPresenter) {
        purchases.paywallPresenter = presenter
        purchases.paywallPresenter = null
        val currentPresenter: PaywallPresenter? = purchases.paywallPresenter
        val lambdaPresenter = PaywallPresenter {
                params: PaywallPresenter.Params, completion: PaywallPresenter.Completion ->
            val offering: Offering = params.offering
            val checkpointIdentifier: String = params.checkpointIdentifier
            val customVariables: Map<String, CustomVariableValue> = params.customVariables
            completion.complete(PaywallPresenter.Completion.Result.Continued)
            completion.complete(PaywallPresenter.Completion.Result.Closed)
            completion.complete(PaywallPresenter.Completion.Result.NavigatedBack)
        }
    }

    fun checkCompletionResult(result: PaywallPresenter.Completion.Result) {
        when (result) {
            PaywallPresenter.Completion.Result.Continued -> {}
            PaywallPresenter.Completion.Result.Closed -> {}
            PaywallPresenter.Completion.Result.NavigatedBack -> {}
            // The hierarchy is closed but not sealed, so consumers must handle cases added later.
            else -> {}
        }
    }

    fun checkErrorPresenter(purchases: Purchases, presenter: ErrorPresenter) {
        purchases.errorPresenter = presenter
        purchases.errorPresenter = null
        val currentPresenter: ErrorPresenter? = purchases.errorPresenter
        val lambdaPresenter = ErrorPresenter {
                params: ErrorPresenter.Params, completion: ErrorPresenter.Completion ->
            val error: PurchasesError = params.error
            val checkpointIdentifier: String = params.checkpointIdentifier
            val customVariables: Map<String, CustomVariableValue> = params.customVariables
            val source: ErrorPresenter.Source = params.source
            val flowCanContinue: Boolean = params.flowCanContinue
            completion.complete(ErrorPresenter.Completion.Result.Retry)
            completion.complete(ErrorPresenter.Completion.Result.Continued)
            completion.complete(ErrorPresenter.Completion.Result.NavigatedBack)
        }
    }

    fun checkErrorSource(source: ErrorPresenter.Source) {
        val purchase: ErrorPresenter.Source = ErrorPresenter.Source.PURCHASE
        val restore: ErrorPresenter.Source = ErrorPresenter.Source.RESTORE
        val presentation: ErrorPresenter.Source = ErrorPresenter.Source.PRESENTATION
        val name: String = source.name
        val isPurchase: Boolean = source == ErrorPresenter.Source.PURCHASE
    }

    fun checkErrorCompletionResult(result: ErrorPresenter.Completion.Result) {
        when (result) {
            ErrorPresenter.Completion.Result.Retry -> {}
            ErrorPresenter.Completion.Result.Continued -> {}
            ErrorPresenter.Completion.Result.NavigatedBack -> {}
            // The hierarchy is closed but not sealed, so consumers must handle cases added later.
            else -> {}
        }
    }
}
