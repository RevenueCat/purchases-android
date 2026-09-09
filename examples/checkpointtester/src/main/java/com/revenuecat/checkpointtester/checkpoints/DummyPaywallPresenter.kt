package com.revenuecat.checkpointtester.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The simplest possible app-owned paywall: presenting parks the request in a StateFlow, the app root renders
 * a DummyPaywallDialog while one is set, and the dialog's buttons report back through the completion.
 */
@OptIn(InternalRevenueCatAPI::class)
object DummyPaywallPresenter : PaywallPresenter {

    class Request(
        val params: PaywallPresenter.Params,
        val completion: PaywallPresenter.Completion,
    )

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request.asStateFlow()

    override fun present(params: PaywallPresenter.Params, completion: PaywallPresenter.Completion) {
        _request.value = Request(params, completion)
    }

    fun clear() {
        _request.value = null
    }
}
