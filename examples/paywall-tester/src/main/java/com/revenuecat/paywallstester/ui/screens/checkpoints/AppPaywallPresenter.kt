package com.revenuecat.paywallstester.ui.screens.checkpoints

import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.PaywallPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Parks each presentation request in [request] so the screen can draw the app's own paywall; the paywall reports
 * how the user left it through [Request.finish].
 */
@OptIn(InviteOnlyCheckpointsAPI::class)
class AppPaywallPresenter {

    inner class Request(
        val params: PaywallPresenter.Params,
        private val completion: PaywallPresenter.Completion,
        private val onFinished: (PaywallPresenter.Completion.Result) -> Unit,
    ) {
        fun finish(result: PaywallPresenter.Completion.Result) {
            if (_request.value === this) _request.value = null
            completion.complete(result)
            onFinished(result)
        }
    }

    private val _request = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = _request.asStateFlow()

    fun present(
        params: PaywallPresenter.Params,
        completion: PaywallPresenter.Completion,
        onFinished: (PaywallPresenter.Completion.Result) -> Unit,
    ) {
        _request.value = Request(params, completion, onFinished)
    }
}
