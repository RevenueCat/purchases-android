@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.apitester.kotlin.revenuecatui

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointParams
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointPassedCallback
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpoint
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

    fun checkPaywallPresenter(purchases: Purchases, presenter: PaywallPresenter) {
        purchases.paywallPresenter = presenter
        purchases.paywallPresenter = null
        val currentPresenter: PaywallPresenter? = purchases.paywallPresenter
        val lambdaPresenter = PaywallPresenter {
                params: PaywallPresenter.Params, completion: PaywallPresenter.Completion ->
            val offering: Offering = params.offering
            val checkpointIdentifier: String = params.checkpointIdentifier
            val customVariables: Map<String, CustomVariableValue> = params.customVariables
            completion.complete(PaywallPresenter.Completion.Result.Purchased)
            completion.complete(PaywallPresenter.Completion.Result.Closed)
            completion.complete(PaywallPresenter.Completion.Result.NavigatedBack)
            completion.complete(PaywallPresenter.Completion.Result.ContinuedWithoutPurchasing)
        }
    }

    fun checkCompletionResult(result: PaywallPresenter.Completion.Result) {
        when (result) {
            PaywallPresenter.Completion.Result.Purchased -> {}
            PaywallPresenter.Completion.Result.Closed -> {}
            PaywallPresenter.Completion.Result.NavigatedBack -> {}
            PaywallPresenter.Completion.Result.ContinuedWithoutPurchasing -> {}
            // The hierarchy is closed but not sealed, so consumers must handle cases added later.
            else -> {}
        }
    }
}
