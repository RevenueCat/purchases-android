package com.revenuecat.checkpointssample.paywall

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An app-owned paywall presenter: presenting parks the request in a StateFlow, the app root renders a paywall over
 * everything while one is set, and the paywall reports back through [Request.finish].
 */
@OptIn(InternalRevenueCatAPI::class)
class ParkedPaywallPresenter : PaywallPresenter {

    inner class Request(
        val params: PaywallPresenter.Params,
        private val completion: PaywallPresenter.Completion,
    ) {
        fun finish(result: PaywallPresenter.Completion.Result) {
            completion.complete(result)
            _request.value = null
        }
    }

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request.asStateFlow()

    override fun present(params: PaywallPresenter.Params, completion: PaywallPresenter.Completion) {
        _request.value = Request(params, completion)
    }
}

object SamplePaywallPresenters {
    /** Registered on Purchases, so it presents any offering a checkpoint resolves to. */
    val global = ParkedPaywallPresenter()

    /** Passed in the CheckpointParams of the play_game call, ahead of [global]. */
    val playGame = ParkedPaywallPresenter()
}
